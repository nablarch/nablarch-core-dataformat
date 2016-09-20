package nablarch.core.dataformat;

import static nablarch.core.dataformat.LayoutFileParser.TokenType.BINARY_LITERAL;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.COMMA;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.DIRECTIVE_HEADER;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.EOF;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.EOL;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.EQ;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.IDENTIFIER;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.LPAREN;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.LT;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.NUMBER;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.QUESTION_MARK;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.AT_MARK;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.RECORD_TYPE_HEADER;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.RPAREN;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.STRING_LITERAL;
import static nablarch.core.dataformat.LayoutFileParser.TokenType.ARRAY_DEF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * フォーマット定義ファイルのLL(1)パーサ。
 * @author Iwauo Tajima
 */
public class LayoutFileParser {

    /** デフォルトのフォーマット定義ファイルのファイルエンコーディング */
    private static final Charset DEFAULT_FILE_ENCODING = Charset.forName("UTF-8");

    // -------------------------------------------- internal structure
    /** フォーマット定義ファイルのパス  */
    private final String filePath;
    
    /** フォーマット定義ファイルの字句解析器 */
    private final Tokenizer tokenizer;
    
    /** 直近に読み出したトークン **/
    private Token token;
    
    /** パース結果を格納するオブジェクト */
    private LayoutDefinition definition;
    
    // -------------------------------------------- constructors
    /**
     * コンストラクタ。
     * @param filePath フォーマット定義ファイルのパス
     */
    @Published(tag = "architect")
    public LayoutFileParser(String filePath) {
        this(filePath, null);
    }
    
    /**
     * コンストラクタ。
     * フォーマット定義ファイルのエンコーディングを指定する。
     * @param filePath フォーマット定義ファイルのパス
     * @param encoding フォーマット定義ファイルのエンコーディング
     */
    @Published(tag = "architect")
    public LayoutFileParser(String filePath, String encoding) {
        this.filePath = filePath;
        Charset charset;
        if (StringUtil.hasValue(encoding)) {
            try {
                charset = Charset.forName(encoding);
            } catch (UnsupportedCharsetException e) {
                throw new IllegalArgumentException("layout file encoding was invalid. encoding=[" + encoding + "].", e);
            }
        } else {
            charset = DEFAULT_FILE_ENCODING;
        }
        this.tokenizer = new Tokenizer(filePath, charset);
        this.definition = createDefinition(filePath);
    }
    
    /**
     * パース結果を格納するクラスを生成し、フィールドに設定する。
     * @param filePath フォーマット定義ファイルのパス
     * @return パース結果を格納するクラス
     */
    protected LayoutDefinition createDefinition(String filePath) {
        return new LayoutDefinition(filePath);
    }
    
    // --------------------------------------------- Terminals
    /**
     * フォーマット定義ファイルの終端要素(トークン)の種別
     */
    public static enum TokenType {
        /** 左丸括弧 */
        COMMA              (",")
        /** 左丸括弧 */
      , LPAREN             ("\\(")
        /** 右丸括弧 */
      , RPAREN             ("\\)")
        /** 小なり記号 */
      , LT                 ("<")
        /** クエスチョンマーク */
      , QUESTION_MARK      ("\\?")
      /** アットマーク */
      , AT_MARK            ("@")
        /** 等号 */
      , EQ                 ("=")
        /** ディレクティブ定義のヘッダ     */
      , DIRECTIVE_HEADER   ("([a-zA-Z][-a-zA-Z0-9]*)\\:") {
            /** {@inheritDoc}
             * この実装では、ディレクティブ名を返却する。
             */
            @Override public String value(MatchResult image) {
                return image.group(1);
            }
        }
        /** レコードタイプ定義のヘッダ     */
      , RECORD_TYPE_HEADER  ("\\[([a-zA-Z_$][a-zA-Z0-9_\\.:$]*)\\]") {
            /** {@inheritDoc}
             * この実装では、レコードタイプ名を返却する。
             */
            @Override public String value(MatchResult image) {
                return image.group(1);
            }
        }
        /** 数値 */
      , NUMBER             ("[-+]?[0-9]+") {
            @Override
            public Integer value(MatchResult image) {
                return Integer.valueOf(image.group());
            }
        }
        /** 真偽値リテラル */
      , BOOLEAN_LITERAL     ("true|false|TRUE|FALSE") {
            @Override
            public Boolean value(MatchResult image) {
                return Boolean.valueOf(image.group());
            }   
        }
        /** 識別子 */
      , IDENTIFIER         ("[a-zA-Z_$][a-zA-Z0-9_$]*")
        /** 配列指定 */
      , ARRAY_DEF          ("\\[([0-9]+\\.\\.)?([0-9]+|\\*)\\]")
        /** 文字列リテラル */
      , STRING_LITERAL     ("\"(([^\"\\\\]|\\\\.)*)\"") {
            @Override
            public String value(MatchResult image) {
                StringBuffer literal = new StringBuffer();
                Matcher m = Pattern.compile("\\\\.").matcher(image.group(1));
                while (m.find()) {
                    String replacement;
                    char escaped = m.group().charAt(1);
                    switch (escaped) {
                        case 'n':  replacement = "\n";   break;
                        case 't':  replacement = "\t";   break;
                        case 'b':  replacement = "\b";   break;
                        case 'r':  replacement = "\r";   break;
                        case 'f':  replacement = "\f";   break;
                        case '\\': replacement = "\\\\"; break;
                        case '\'': replacement = "'";    break;
                        case '"':  replacement = "\"";   break;
                        default: throw new SyntaxErrorException(
                                     "invalid escape sequence was specified. value=[" + m.group() + "]"
                                 );
                    }
                    m.appendReplacement(literal, replacement);
                }
                m.appendTail(literal);
                return literal.toString();
            }
        }
        /** バイナリリテラル */
      , BINARY_LITERAL     ("0[xX]([0-9a-fA-F]{2})+")
        /** 未定義トークン */
      , UNKNOWN_TOKEN      (".*")
        /** 行終端 */
      , EOL                ("_DUMMY_")
        /** ファイル終端 */
      , EOF                ("_DUMMY_");
      
        /**
         * コンストラクタ
         * @param pattern トークンの書式
         */
        private TokenType(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }
        
        /** トークンの書式 */
        private final Pattern pattern;
        
        /**
         * トークン文字列に対応する値を返却する。
         * @param image  トークン文字列
         * @return トークンの値
         */
        public Object value(MatchResult image) {
              return image.group();
        }
        
        /**
         * トークンを生成して返却する。
         * @param match トークンの一致部分
         * @param line   トークン開始行
         * @param column トークン開始桁
         * @return トークン
         */
        public Token newToken(MatchResult match, int line, int column) {
            return new Token(this, match, line, column);
        } 
    }
    
    // ----------------------------------------------- Non-terminals
    /**
     * フォーマット定義ファイルをパースし、その内容を渡されたオブジェクトに設定する。
     * @return パース結果オブジェクト
     * @throws SyntaxErrorException
     *            フォーマット定義ファイル内に構文エラーが存在した場合。
     */
    @Published(tag = "architect")
    public LayoutDefinition parse() throws SyntaxErrorException {
        try {
            directives(definition);
            recordFormats(definition);
            consume(EOF);
            return definition;
            
        } catch (SyntaxErrorException e) {
            throw e.setToken(token)
                   .setFilePath(filePath);
        }
    }
    
    /**
     * 共通設定部をパースする。
     * @param definition フォーマット定義情報保持クラス
     */
    public void directives(LayoutDefinition definition) {
        while (peek().type() == DIRECTIVE_HEADER) {
            directive(definition);
        }
    }


    /**
     * 動作設定(ディレクティブ)を読み込む。
     * @param definition フォーマット定義情報保持クラス
     */
    public void directive(LayoutDefinition definition) {

        consume(DIRECTIVE_HEADER);
        String directiveName = token.value();
        
        Token next = peek();
        switch (next.type()) {
        case NUMBER:
        case BOOLEAN_LITERAL:
        case STRING_LITERAL:
        case BINARY_LITERAL:
            Object directiveValue = literal();
            consume(EOL);
            definition.getDirective().put(directiveName, directiveValue);
            break;
            
        default:
            throw new SyntaxErrorException(NUMBER, STRING_LITERAL, BINARY_LITERAL).setFilePath(filePath);
        }
    }
    
    /**
     * レコードタイプ定義部をパースする。
     * @param layout フォーマット定義情報保持クラス
     */
    public void recordFormats(LayoutDefinition layout) {
        if (peek().type() != RECORD_TYPE_HEADER) {
            throw new SyntaxErrorException(RECORD_TYPE_HEADER).setFilePath(filePath);
        }
        while (peek().type() == RECORD_TYPE_HEADER) {
            recordFormat(layout);
        }
    }
    
    /**
     * レコード定義を読み込む。
     * @param layout フォーマット定義情報保持クラス
     */
    public void recordFormat(LayoutDefinition layout) {
        RecordDefinition record = new RecordDefinition();
        
        consume(RECORD_TYPE_HEADER);
        String typeName = token.value();
        record.setTypeName(typeName);
        if (consumeIfFollowing(LT)) {
            consume(RECORD_TYPE_HEADER);
            String baseTypeName = token.value();
            RecordDefinition baseType = layout.getRecordType(baseTypeName);
            if (baseType == null) {
                throw new SyntaxErrorException(
                    "undefined base record type name was specified. type name=[" + baseTypeName + "]"
                );
            }
            record.setBaseRecordType(baseType);
        }
        
        consume(EOL);
        if (peek().type() == IDENTIFIER) {
            conditionalStatements(record);
        }
        
        fieldFormats(layout, record);
        
        if (record.isClassifier()) {
            layout.setRecordClassifier(record);
        } else {
            layout.addRecord(record);
        }
    }

    /**
     * フィールド定義部をパースする。
     * @param layout フォーマット定義情報保持クラス
     * @param record レコードフォーマット定義
     */
    public void fieldFormats(LayoutDefinition layout, RecordDefinition record) {
        if (record.isClassifier()) {
            while (peek().type == NUMBER) {
                record.addField(fieldFormat());
            }
            return;
        }
        while (peek().type() == NUMBER) {
            FieldDefinition fieldFormat = fieldFormat();
            record.addField(fieldFormat);
        }
    }
    /**
     * レコードタイプ判定条件定義部を読み込む
     * @param record レコードフォーマット定義
     */
    public void conditionalStatements(RecordDefinition record) {
        while (peek().type() == IDENTIFIER) {
            conditionalStatement(record);
        }
    }
    
    /**
     * レコードタイプ判定条件を読み込む
     * @param record レコードフォーマット定義
     */
    public void conditionalStatement(RecordDefinition record) {
        consume(IDENTIFIER);
        String fieldName = token.value();
        consume(EQ);
        Object fieldValue = literal();
        consume(EOL);
        
        record.addCondition(
            new DataRecordPredicate.Equals(fieldName, fieldValue)
        );
    }

    /**
     * フィールド定義を読み込む。
     * @return レコードフォーマット定義
     */
    public FieldDefinition fieldFormat() {
        FieldDefinition field = new FieldDefinition();

        consume(NUMBER);
        field.setPosition(Integer.valueOf(token.image()));
             
        if (consumeIfFollowing(QUESTION_MARK)) {
            field.markAsFiller();
        }
        
        if (consumeIfFollowing(AT_MARK)) {
            field.markAsAttribute();
        }
        
        String prefix = "";
        if (consumeIfFollowing(DIRECTIVE_HEADER)) {
            prefix = token.image;
        }
        
        consume(IDENTIFIER);
        String fieldName = token.value();
        field.setName(prefix + fieldName);
        
        if (consumeIfFollowing(ARRAY_DEF)) {
            String arrayDef = token.value();
            setupArrayDef(arrayDef, field);
        }
        
        valueConvertors(field);
        
        consume(EOL);
        return field;
    }

    /** 項目の出現回数が可変の場合の区切り文字列。（例：{@code fieldName[1..3]} */
    private static final String VARIABLE_ITEM_SEPARATOR = "..";

    /**
     * フィールドに配列定義を設定する。
     * @param arrayDef 配列定義情報
     * @param field フィールドフォーマット定義
     */
    private void setupArrayDef(String arrayDef, FieldDefinition field) {
        int minCount = 0;
        int maxCount = Integer.MAX_VALUE;
        int sepIndex = arrayDef.indexOf(VARIABLE_ITEM_SEPARATOR);
        
        String minCountStr;
        String maxCountStr;
        if (sepIndex > 0) {
            // ..区切りのある場合
            minCountStr = arrayDef.substring(1, sepIndex);
            maxCountStr = arrayDef.substring(sepIndex + 2, arrayDef.length() - 1);
        } else {
            // ..区切りのない場合
            minCountStr = arrayDef.substring(1, arrayDef.length() - 1);
            maxCountStr = minCountStr;
        }
        
        // 最小値設定
        if (!"*".equals(minCountStr)) {
            try {
                minCount = Integer.parseInt(minCountStr);
            } catch (NumberFormatException e) {
                throw new SyntaxErrorException(
                        String.format("invalid field layout was specified. reason=[%s] type name=[%s]", 
                                "bad min array size", field.getName())
                        );
            }
        }
        field.setMinArraySize(minCount);
        
        // 最大値設定
        if (!"*".equals(maxCountStr)) {
            try {
                maxCount = Integer.parseInt(maxCountStr);
            } catch (NumberFormatException e) {
                throw new SyntaxErrorException(
                        String.format("invalid field layout was specified. reason=[%s] type name=[%s]", 
                                "bad max array size", field.getName())
                        );
            }
        }
        field.setMaxArraySize(maxCount);
        
        // 最小値>最大値はNG
        if (field.getMinArraySize() > field.getMaxArraySize()) {
            throw new SyntaxErrorException(
                    String.format("invalid field layout was specified. reason=[%s] type name=[%s]", 
                            "max array size must be greater than min array size.", field.getName())
                    );
        }

        // 任意項目設定
        if (minCount == 0) {
            // 任意項目かつ最大値が0はNG
            if (maxCount == 0) {
                throw new SyntaxErrorException(
                        String.format("invalid field layout was specified. reason=[%s] type name=[%s]", 
                                "when not required, max count must be greater than 0", field.getName())
                        );
            }
            
            field.markAsNotRequired();
        }
        
        // 配列項目設定
        if (maxCount > 1) {
            // 配列かつ属性はNG
            if (field.isAttribute()) {
                throw new SyntaxErrorException(
                        String.format("invalid field layout was specified. reason=[%s] type name=[%s]", 
                                "attribute field can not be array.", field.getName())
                        );
            }
            
            field.markAsArray();
        }
    }
    
    /**
     * フィールドに設定されているすべてのコンバート定義を読み込む。
     * @param field フィールドフォーマット定義
     */
    public void valueConvertors(FieldDefinition field) {
        while (peek().type() != EOL) {
            valueConvertor(field);
        }
    }
    
    /**
     * フィールド値のタイプ定義およびコンバート定義を読み込む。
     * @param field フィールド定義
     */
    public void valueConvertor(FieldDefinition field) {
        String   convertorName = null;
        Object[] convertorArgs = null;
        
        switch(peek().type()) {
        case NUMBER:
        case STRING_LITERAL:
        case BINARY_LITERAL:
            // コンバータのサイズが0の場合に、リテラルがやってきた場合は、例外をスローする（最初のコンバータ、すなわちデータタイプは、IDENTIFIERでなければならない）
            if (field.getConvertorSettingList().size() == 0) {
                throw new SyntaxErrorException(String.format(
                        "data type format was invalid. value=[\"%s\"]. valid format=[%s]."
                        , literal(), IDENTIFIER.pattern));
            }
            convertorName = "_LITERAL_";
            convertorArgs = new Object[] {literal()};
            break;
        case IDENTIFIER:
            convertorName = consume(IDENTIFIER).image();
            convertorArgs = argsOpt();
            break;
        default:
            throw new SyntaxErrorException(
                    NUMBER, STRING_LITERAL, BINARY_LITERAL, IDENTIFIER
            ).setFilePath(filePath);
        }
        field.addConvertorSetting(convertorName, convertorArgs);
    }
    
    /**
     * コンバータのオプション定義を読み込む。
     * @return オプションの配列
     */
    public Object[] argsOpt() {
        if (!consumeIfFollowing(LPAREN)) {
            return new Object[]{};
        }
        if (consumeIfFollowing(RPAREN)) {
            return new Object[]{};
        }
        List<Object> args = new ArrayList<Object>();
        args.add(literal());
        while (consumeIfFollowing(COMMA)) {
            args.add(literal());
        }
        consume(RPAREN);
        return args.toArray();
    }
    
    /**
     * リテラル値を読み込む。
     * @return リテラル値
     */
    public Object literal() {
        consume();
        
        switch (token.type()) {
        case NUMBER:
        case STRING_LITERAL:
        case BOOLEAN_LITERAL:
        case BINARY_LITERAL:
            return token.value();
            
        default:
            throw new SyntaxErrorException(
                    NUMBER, STRING_LITERAL, BINARY_LITERAL
            ).setFilePath(filePath);
        }
    }
    
    
    // -------------------------------------------- LL(1) parsing main routine
    /**
     * 次のトークンを返却する。
     * ただし、現在の読み出し位置は変更しない。
     * @return 次のトークン
     */
    public Token peek() {
        Token t = tokenizer.peek();
        return t;
    }
    
    /**
     * 次のトークンを返し、読み込んだトークン分のだけ読み出し位置を進める。
     * @return 次のトークン
     * @throws SyntaxErrorException 読み出し位置が既に終端に達している場合
     */
    public Token consume() throws SyntaxErrorException {
        token = tokenizer.consume();
        return token;
    }
    
    /**
     * 次のトークンを読み込み、読み出し位置を進める。
     * このとき、読み込んだトークンの種別が指定したものと異なる場合は実行時例外を送出する。
     * @param type 読み込むトークンの種別
     * @return 読み込んだトークン
     * @throws SyntaxErrorException
     *              読み込んだトークンが指定したものと異なる場合。
     */
    public Token consume(TokenType type) throws SyntaxErrorException {
        token = tokenizer.consume();
        if (token.type() != type) {
            throw new SyntaxErrorException(type).setFilePath(filePath);
        }
        return token;
    }
    
    /**
     * 次のトークンが指定した種別のものである場合のみ、それを読み込んでスキャナの
     * 位置を進める。
     * 種別が一致しない場合は何もしない。
     * @param type 読み込むトークンの種別
     * @return 種別が一致し、トークンを読み込んだ場合はtrue。
     *          そうでなかった場合はfalse。
     */
    public boolean consumeIfFollowing(TokenType type) {
        if (peek().type() == type) {
            consume();
            return true;
        }
        return false;
    }

    /**
     * フォーマット定義ファイルの構文定義上の終端要素(トークン)。
     */
    public static class Token {
        // --------------------------------- properties
        /** トークン種別  */
        private final TokenType type;
        /** トークン文字列 */
        private final String image;
        /** トークンの値 */
        private final Object value;
        /** 開始行 */
        private final int beginLine;
        /** 開始桁 */
        private final int beginColumn;
        
        
        // ---------------------------------- constructors
        /**
         * コンストラクタ
         * @param type   トークン種別
         * @param image  トークン文字列
         * @param line   トークン開始行
         * @param column トークン開始桁
         */
        public Token(TokenType type, MatchResult image, int line, int column) {
            this.type        = type;
            this.image       = (image == null) ? null : image.group();
            this.value       = (image == null) ? null : type.value(image);
            this.beginLine   = line;
            this.beginColumn = column;
        }
        
        // ------------------------------------ accessors
        /**
         * トークン種別を返却する。
         * @return トークン種別
         */
        public TokenType type() { 
            return type;
        }
        
        /**
         * トークン文字列を返却する。
         * @return トークン文字列
         */
        public String image() {
            return image;
        }
        
        /**
         * トークンの値を返却する。
         * @param <V> トークンの値の型
         * @return トークンの値
         */
        @SuppressWarnings("unchecked")
        public <V> V value() {
            return (V) value;
        }
        
        /**
         * トークンの開始行を返却する。
         * @return トークンの開始行
         */
        public int beginLine() {
            return this.beginLine;
        }
        
        /**
         * トークンの開始桁を返却する。
         * @return トークンの開始桁
         */
        public int beginColumn() {
            return this.beginColumn;
        }
    }
    
    
    /**
     * フォーマット定義ファイルの字句要素解析器
     */
    public static class Tokenizer {
        // -------------------------------------------- structure
        /** フォーマット定義ファイルパス */
        private final String filePath;
        
        /** 入力ソース */
        private final BufferedReader stream;
        
        /** 次に読み出されるトークン */
        private Token peeked = null;
        
        /** パターンマッチャー */
        private final Matcher matcher = Pattern.compile("").matcher("").reset();
        
        /** 現在処理中の行 */
        private String line = null;
        
        /** 現在処理中の行番号 */
        private int lineNum = 0;
        
        /** 現在処理中の列番号 */
        private int colNum = 0;
        
        // ------------------------------------------------- constructor
        /**
         * コンストラクタ
         * @param filePath フォーマット定義ファイルのパス
         * @param encoding フォーマット定義ファイルのファイルエンコーディング
         */
        public Tokenizer(String filePath, Charset encoding) {
            try {
                this.filePath = filePath;
                this.stream = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(filePath)), encoding)
                );
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(
                   "invalid layout file path was specified. file path=[" + filePath + "]", e
                );
            }
        }
        
        // ------------------------------------------------- main routine
        /**
         * 次のトークンを読み込む。
         * @return 次のトークン
         */
        public Token consume() {
            // 先読みしたトークンが存在する場合はそれを返却する
            if (peeked != null) {
                Token result = peeked;
                peeked = null;
                return result;
            }
            while (true) {
                // 新規行の読み込みを行う
                if (line == null) {
                    lineNum++;
                    colNum = 0; 
                    
                    try {
                        line = stream.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(
                            "I/O error happened while reading the file. file path=[" + filePath + "]", e
                        );
                    }
                    // ファイル終端
                    if (line == null) {
                        return new Token(TokenType.EOF, null, lineNum, colNum);
                    }
                    // 空行の場合は読み直し
                    if (SKIP.matcher(line).matches()) {
                        line = null;
                        continue;
                    }
                    
                    matcher.reset(line);
                }
                
                // 空白、コメント処理
                proceedMatch(SKIP);
                
                // 直前のマッチで行末に達した場合はEOLトークンを返却する
                if (line != null && matcher.hitEnd()) {
                    line = null;
                    return new Token(TokenType.EOL, null, lineNum, colNum);
                }

                // トークン判定
                for (TokenType type : TokenType.values()) {
                    if (proceedMatch(type.pattern)) {
                        return type.newToken(matcher, lineNum, matcher.start());
                    }
                }

                throw new RuntimeException("can not happen.");
            }
        }

        /** 空白および行コメント */
        private static final Pattern SKIP = Pattern.compile("[\\s\\t]*(#.*)?");
        
        /**
         * 現在の位置を起点として、指定されたパターンにマッチした場合、
         * その分だけ位置をすすめる。
         * (マッチしなかった場合は現在の位置のまま)
         * @param pattern マッチさせるパターン
         * @return        マッチした場合はtrue
         */
        public boolean proceedMatch(Pattern pattern) {
            matcher.region(colNum, line.length())
                   .usePattern(pattern);
            
            if (matcher.lookingAt()) {
                colNum = matcher.end();
                return true;
            }
            return false;
        }
        
        /**
         * 次に読み込まれる予定のトークンを返却する。
         * @return 次に読み込まれる予定のトークン
         */
        public Token peek() {
            if (peeked != null) {
                return peeked;
            }
            peeked = consume();
            return peeked;
        }
    }
    
}
