package nablarch.core.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON解析用ユーティリティ
 *
 * @author TIS
 */
public final class JsonParser {

    /** 文字列読み込み用リーダー */
    private StringReader reader;

    /** 解析中オブジェクトのスタック */
    private final Stack<Map<String, Object>> mapStack = new Stack<Map<String, Object>>();
    /** 解析中配列のスタック */
    private final Stack<List<Object>> listStack = new Stack<List<Object>>();

    /** ルートマップ */
    private Map<String, Object> rootMap = null;

    /** 解析中オブジェクト */
    private Map<String, Object> currentMap = null;
    /** 解析中配列 */
    private List<Object> currentList = null;
    /** 解析中キー */
    private String currentKey = null;

    /** 前回のトークン */
    private String lastToken = null;
    /** 前回のトークン種別 */
    private TokenType lastTokenType = null;

    /** トークン種別 */
    public enum TokenType {
        /** 文字列 */
        STRING("^\"(.*)\"$") {
            /**{@inheritDoc}<br>
             * この実装ではトークンのクオーテーションを除去したものに対して、
             * アンエスケープ処理を実施します。
             */
            @Override
            String editTokenValue(String token) {
                return unescape(token.substring(1, token.length() - 1));
            }
        },
        /** 数値 */
        NUMERIC("-?[0-9]+(\\.[0-9]+)?([eE][-+]?[0-9]+)?"),
        /** 真偽値 */
        BOOL("false|true"),
        /** null */
        NULL("null") {
            /**{@inheritDoc}<br>
             * この実装ではnullを返却します。
             */
            @Override
            String editTokenValue(String token) {
                return null;
            }
        },
        /** セパレータ */
        SEPARATOR("[\\[\\]\\{\\},:]");

        /** トークンパターン */
        private final Pattern tokenPattern;

        /**
         * コンストラクタ
         * @param patternRegex トークンパターン
         */
        TokenType(String patternRegex) {
            tokenPattern = Pattern.compile(patternRegex);
        }

        /**
         * トークンの値を編集します。
         * デフォルトでは何もせずトークンをそのまま返却します。
         * @param token トークン
         * @return 編集されたトークン
         */
        String editTokenValue(String token) {
            return token;
        }

        /**
         * トークンがパターンにマッチするか判定します
         * @param token トークン
         * @return マッチする場合true
         */
        public boolean matches(String token) {
            return tokenPattern.matcher(token).matches();
        }
    }

    /**
     * コンストラクタ
     */
    public JsonParser() {
        super();
    }

    /**
     * JSONを解析し、単純なMapを作成します。
     *
     * @param text JSON文字列
     * @return データ形式変換および階層構造変換を行った単純なMap
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     */
    public Map<String, ?> parse(String text) throws IOException {
        // ルート要素の形式を確認
        checkTypeOfRootElement(text);

        String token;
        reader = new StringReader(text);

        // 行中からパターンにマッチするトークンを取得する
        while ((token = readNextToken()) != null) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }

            if ("{".equals(token)) {
                // オブジェクトの開始
                onStartObject();

            } else if ("}".equals(token)) {
                // オブジェクトの終了
                onEndObject();

            } else if ("[".equals(token)) {
                // 配列の開始
                onStartArray();

            } else if ("]".equals(token)) {
                // 配列の終了
                onEndArray();

            } else if (":".equals(token)) {
                // キーセパレータの検出
                onKeySeparator();

            } else if (",".equals(token)) {
                // 項目セパレータの検出
                onItemSeparator();

            } else if (lastTokenType != TokenType.SEPARATOR) {
                // 前回トークンがセパレータでない場合はエラー
                throw new IllegalArgumentException("last token is not separator. token:[" + token + "]");

            } else if (currentMap == null) {
                throw new IllegalArgumentException("current object is null. token:[" + token + "]");
            }

            // 前回トークンとして保持
            setLastToken(token);
        }

        return rootMap;
    }

    /**
     * 次のトークンを読み込みます
     * @return 次のトークン
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     *
     */
    private String readNextToken() throws IOException {
            int c;
            StringBuilder sb = new StringBuilder();
            boolean isInQuote = false;
            boolean isEscape = false;
            while (true) {
                reader.mark(1);
                c = reader.read();

                if (c == -1) {
                    // 文字列の終了
                    if (sb.length() > 0) {
                        return sb.toString();
                    } else {
                        return null;
                    }
                }

                if (c == '\\') {
                    // バックスラッシュ
                    // エスケープが未開始の場合はエスケープを開始し、既にエスケープ中の場合はバックスラッシュをエスケープ対象として解釈し、エスケープを終了する。
                    isEscape = !isEscape;

                    sb.append((char) c);

                } else {
                    if (c == '\"') {
                        // ダブルクォートの場合
                        if (isEscape) {
                            // エスケープされている場合はそのまま保持
                            sb.append((char) c);

                        } else if (!isInQuote) {
                            // クォート開始
                            isInQuote = true;
                            sb.append((char) c);

                        } else {
                            // クォート終了
                            sb.append((char) c);
                            return sb.toString();
                        }


                    } else if (c == ' ' || c == '\t') {
                        // ホワイトスペース
                        if (isInQuote) {
                            sb.append((char) c);
                        }

                    } else if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                        // セパレータ
                        if (isInQuote) {
                            sb.append((char) c);

                        } else {
                            if (sb.length() > 0) {
                                // バッファにデータが存在する場合一旦リセットして次回読み込み時にセパレータを返却
                                reader.reset();
                            } else {
                                sb.append((char) c);
                            }
                            return sb.toString();
                        }

                    } else {
                        // その他の文字はバッファに保持
                        sb.append((char) c);
                    }

                    isEscape = false;
                }

            }
    }

    /**
     * ルート要素の形式を確認します
     * @param text JSONデータ
     */
    private void checkTypeOfRootElement(String text) {
        String trimmedText = text.trim();

        // オブジェクト形式で始まっているか確認
        if (!trimmedText.startsWith("{")) {
            throw new IllegalArgumentException("JSON data must starts with '{'");
        }

        // オブジェクト形式で終了しているか確認
        if (!trimmedText.endsWith("}")) {
            throw new IllegalArgumentException("JSON data must ends with '}'");
        }
    }

    /**
     * オブジェクト開始時の処理です。
     */
    private void onStartObject() {
        Map<String, Object> newMap = new HashMap<String, Object>();
        if (lastToken == null) {
            // 何も無い (一番最初)
            rootMap = newMap;
        } else if (lastTokenType == TokenType.SEPARATOR) {
            if (":".equals(lastToken)) {
                // 値の要素がオブジェクトのパターン
                currentMap.put(currentKey, newMap);
            } else if ("[".equals(lastToken)) {
                // 配列の要素がオブジェクトのパターン
                currentList.add(newMap);
            } else if (",".equals(lastToken)) {
                // 配列の要素で２つ目以降のパターン
                currentList.add(newMap);
            } else {
                // 不正な開始位置
                throw new IllegalArgumentException("incorrect object starting position");
            }
        } else {
            // 不正な開始位置
            throw new IllegalArgumentException("incorrect object starting position");
        }

        mapStack.push(currentMap);
        currentMap = newMap;
    }

    /**
     * オブジェクト終了時の処理です。
     */
    private void onEndObject() {
        if (currentMap == null) {
            throw new IllegalArgumentException("object ending but current object is null");

        } else if (lastTokenType == TokenType.SEPARATOR
                && ":".equals(lastToken)) {
            throw new IllegalArgumentException("incorrect object ending position");

        } else if (lastTokenType != TokenType.SEPARATOR
                || (!"]".equals(lastToken)
                && !"}".equals(lastToken)
                && !"{".equals(lastToken))) {
            currentMap.put(currentKey, lastToken);
        }

        currentMap = pop(mapStack);
        currentKey = null;
    }

    /**
     * 配列開始時の処理です。
     */
    private void onStartArray() {
        if (lastTokenType == TokenType.SEPARATOR && ":".equals(lastToken)) {
            List<Object> newList = new ArrayList<Object>();
            currentMap.put(currentKey, newList);
            listStack.push(currentList);
            currentList = newList;
        } else {
            throw new IllegalArgumentException("array is need start after :");
        }
        currentKey = null;
    }

    /**
     * 配列終了時の処理です。
     */
    private void onEndArray() {
        if (lastTokenType != TokenType.SEPARATOR
                || (!"}".equals(lastToken) && !"[".equals(lastToken))
        ) {
            if (currentList == null) {
                throw new IllegalArgumentException("array end detected, but not started");
            } else {
                currentList.add(lastToken);
            }
        }
        currentList = pop(listStack);
    }

    /**
     * キーセパレータ検出時の処理です。
     */
    private void onKeySeparator() {
        if (lastTokenType != TokenType.STRING) {
            throw new IllegalArgumentException("key is not string");
        }
        currentKey = lastToken;
    }

    /**
     * 項目セパレータ検出時の処理です。
     */
    private void onItemSeparator() {
        if (lastTokenType == TokenType.SEPARATOR && ("]".equals(lastToken) || "}".equals(lastToken))) {
            // オブジェクト、配列の終了処理内で必要な処理は完了しているので何もしない。
            return;
        }
        if (lastTokenType == TokenType.SEPARATOR && ("[".equals(lastToken) || "{".equals(lastToken)
                || ",".equals(lastToken) || ":".equals(lastToken))) {
            throw new IllegalArgumentException("value is requires");
        }
        if (currentList != null && currentKey == null) {
            // 配列の要素
            currentList.add(lastToken);
        } else {
            // オブジェクトのプロパティ
            currentMap.put(currentKey, lastToken);
            currentKey = null;
        }
    }

    /**
     * スタックからデータを取り出します。
     * スタックが空の場合はnullを返します。
     *
     * @param <T> スタック内のデータ型
     * @param stack 対象スタック
     * @return データ
     */
    private <T> T pop(Stack<T> stack) {
        if (stack.isEmpty()) {
            return null;
        } else {
            return stack.pop();
        }
    }

    /** コードポイントエスケープ検出用のパターン */
    private static final Pattern CODE_POINT_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    /**
     * 文字列のリテラルに対してアンエスケープ処理を行います。
     *
     * @param token トークン
     * @return アンエスケープ処理後のトークン
     */
    private static String unescape(String token) {

        StringBuilder unescapeToken = new StringBuilder();
        char[] tokenCharArray = token.toCharArray();
        for (int i = 0; i < tokenCharArray.length; i++) {
            char tokenChar = tokenCharArray[i];
            if (tokenChar == '\\') {
                char next  = tokenCharArray[i + 1];
                switch (next) {
                    case '"': unescapeToken.append("\""); i++; break;
                    case '\\': unescapeToken.append("\\");  i++; break;
                    case '/': unescapeToken.append("/");  i++; break;
                    case 'b': unescapeToken.append("\b"); i++; break;
                    case 'f': unescapeToken.append("\f"); i++; break;
                    case 'n': unescapeToken.append("\n"); i++; break;
                    case 'r': unescapeToken.append("\r"); i++; break;
                    case 't': unescapeToken.append("\t"); i++; break;
                    case 'u':

                    if (tokenCharArray.length < i + 6) {
                        throw new IllegalArgumentException(
                                "found invalid unicode string :" + token);
                    }

                    String unicodeChar = token.substring(i, i + 6); // \\uXXXXの形式を想定
                        Matcher m = CODE_POINT_ESCAPE.matcher(unicodeChar);
                        StringBuffer sb = new StringBuffer();
                        while (m.find()) {
                            int codePoint = Integer.parseInt(m.group(1), 16);
                            if (codePoint == 92) {
                                // appendReplacement は\(u005C)のみの場合、エスケープ処理をしようと後ろのエスケープ対象を探し、エラーが発生する。\\を置換文字列として渡し、appendReplacement にエスケープ処理をさせることで回避。
                                m.appendReplacement(sb, "\\\\");
                            } else {
                                m.appendReplacement(sb, new String(Character.toChars(codePoint)));
                            }
                        }
                        m.appendTail(sb);
                    unescapeToken.append(sb); i = i + 5; break;
                    default :
                        throw new IllegalArgumentException(
                                "found invalid json format :" + token);
                }
            } else {
                unescapeToken.append(tokenChar);
            }
        }

        return unescapeToken.toString();
    }

    /**
     * 前回トークンを設定します。
     *
     * @param token トークン
     */
    private void setLastToken(String token) {
        lastToken = null;
        lastTokenType = null;

        for (TokenType type : TokenType.values()) {
            if (type.matches(token)) {
                lastTokenType = type;
                lastToken  = type.editTokenValue(token);
                break;
            }
        }
        if (lastTokenType == null) {
            throw new IllegalArgumentException("found invalid token:" + token);
        }
    }

}
