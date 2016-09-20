package nablarch.core.dataformat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.initialization.Initializable;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;

/**
 * 寄せ字変換処理を行うクラス。
 * <p>
 * 本クラスは、文字列中のある特定の文字を等価な代替文字に変換する用途での使用を想定している。<br/>
 * 想定する変換の例：　髙→高　碕→崎<br/>
 * このような文字列変換処理のことを、寄せ時変換処理と呼ぶ。
 * </p>
 * <p>
 * 禁則文字を一括で豆腐（■）に変換するような処理は、本クラスでは想定していないので、<br/>
 * 文字を検知した場合に、ワーニングログを出力する機能や、呼び出し元に検知したことを通知する機能は提供しない。<br/>
 * 想定しない変換の例：　唖→■  \→[ 
 * <p>
 * 寄せ字変換処理は、寄せ字タイプごとに定義された寄せ字変換定義ファイルの情報をもとに行う。<br/>
 * 寄せ字変換定義ファイルは初期化時に読み込み、メモリ上にキャッシュする。
 * </p>
 * <p>
 * 本クラスは、文字列中の特定の1文字を、特定の1文字に変換することしかサポートしない（サロゲートペアの変換もサポートしない）。<br/>
 * よって、寄せ字変換定義ファイルに、「ﾍ゜」のような半角2文字で1文字を表現する合字などの複数の文字や、サロゲートペアが定義された場合は、例外をスローする。
 * </p>
 * @author Masato Inoue
 */
public class CharacterReplacementManager implements Initializable {

    /** ロガー **/
    private static final Logger LOGGER = LoggerManager.get(CharacterReplacementManager.class);

    /** 本クラスのコンポーネント設定ファイル上の名前 */
    private static final String REPOSITORY_KEY = "characterReplacementManager";
    
    /** 寄せ字変換処理の設定を保持するList */
    private List<CharacterReplacementConfig> configList = new ArrayList<CharacterReplacementConfig>();

    /** 寄せ字変換テーブルを保持するクラスのMap */
    private Map<String, CharacterReplacementDefinition> replacementDefinitionMap = new HashMap<String, CharacterReplacementDefinition>();

    
    // --------------------------------------------------- managing singleton
    /**
     * FormatterFactoryクラスのインスタンスをリポジトリより取得する。
     * リポジトリより取得できなかった場合は、デフォルトで本クラスのインスタンスを返却する。
     * @return {@link CharacterReplacementManager}のインスタンス
     */
    public static CharacterReplacementManager getInstance() {
        return SystemRepository.get(REPOSITORY_KEY);
    }
    
    /**
     * 初期化処理を行う。
     */
    public void initialize() {
        createReplacementTables();
    }

    /**
     * 寄せ字変換定義ファイルを読み込み、寄せ字変換テーブルを生成する。
     * <p>
     * 寄せ字変換定義ファイルに定義された変換前および変換後の文字列が1文字でない（Stringのlengthが1でない）場合は、
     * 例外をスローする。
     * </p>
     * <p>
     * 変換前と変換後の文字のバイト長一致チェックが有効な場合、
     * 寄せ字変換定義ファイルに定義された変換前および変換後の文字列を指定されたエンコーディングに従いバイト配列に変換し、
     * 変換前と変換後のバイト長が一致しない場合は、例外をスローする。
     * </p>
     */
    protected void createReplacementTables() {
        for (CharacterReplacementConfig config : configList) {
            
            // 寄せ字タイプ名が重複している場合、例外をスローする
            if (replacementDefinitionMap.containsKey(config.getTypeName())) {
                throw new IllegalStateException(String.format(
                        "duplicate replacement type was set. type name=[%s].", config.getTypeName()));
            }
            
            checkProperty("typeName", config.getTypeName(), config);
            checkProperty("filePath", config.getFilePath(), config);
            
            // 寄せ字変換テーブルを生成する
            CharacterReplacementDefinition definition = createReplacementTable(config);
            
            replacementDefinitionMap.put(config.getTypeName(), definition);
        }
    }


    /**
     * 寄せ字変換定義ファイルを読み込み、寄せ字変換テーブルを生成する。
     * @param config 寄せ字変換処理の設定を保持するクラス
     * @return 寄せ字変換テーブルを保持するクラス
     */
    protected CharacterReplacementDefinition createReplacementTable(CharacterReplacementConfig config) {
        
        CharacterReplacementDefinition table = new CharacterReplacementDefinition();

        // 寄せ字変換定義ファイル（プロパティファイル）をロードする
        String filePath = config.getFilePath();
        Properties properties = loadPropertyFile(filePath);

        HashMap<Character, Character> tableMap = new HashMap<Character, Character>();
        
        for (Map.Entry<Object, Object> propertiesEntry : properties.entrySet()) {
            String fromStr = (String) propertiesEntry.getKey();
            String toStr = (String) propertiesEntry.getValue();
            
            // 変換前文字列と変換後文字列の文字列長チェック
            checkReplacementCharacterLength(fromStr, toStr, config);
            
            // 変換前文字列と変換後文字列のバイト長チェック
            checkByteLength(fromStr, toStr, config);
            
            // 変換前の文字列と変換後の文字列を、寄せ字変換テーブルに設定する
            tableMap.put(fromStr.charAt(0), toStr.charAt(0));
        }
        table.setTable(tableMap);
        
        setEncodingToTable(config, table);
        
        return table;
    }

    /**
     * 寄せ字変換テーブルに、文字エンコーディングを設定する。
     * @param config 寄せ字変換処理の設定を保持するクラス
     * @param definition 寄せ字変換テーブルを保持するクラス
     */
    protected void setEncodingToTable(CharacterReplacementConfig config,
            CharacterReplacementDefinition definition) {
        String encoding = config.getEncoding();
        if (StringUtil.hasValue(encoding)) {
            definition.setEncoding(Charset.forName(encoding));
        }
    }
    
    /**
     * 寄せ字変換定義ファイルをロードする。
     * @param filePath 寄せ字変換定義ファイルのパス
     * @return  寄せ字変換定義ファイルをロードしたPropertiesクラス
     */
    protected Properties loadPropertyFile(String filePath) {
        InputStream resource = FileUtil.getResource(filePath);
        Properties properties = new Properties();
        try {
            properties.load(resource);
        } catch (IOException e) {
            throw new RuntimeException("failed to load replacement character definition file.", e);
        }
        return properties;
    }

    /**
     * 寄せ字変換定義ファイルに設定された変換前文字列と変換後文字列の文字列長が「1」であることを確認する。
     * @param fromStr 寄せ字変換前の文字列
     * @param toStr 寄せ字変換後の文字列
     * @param config 寄せ字変換処理の設定を保持するクラス
     */
    protected void checkReplacementCharacterLength(String fromStr, String toStr, CharacterReplacementConfig config) {
        // 変換前の文字列が1文字でない場合、例外をスローする
        if (fromStr.length() != 1) {
            throw new IllegalStateException(String.format(
                    CHARACTER_LENGTH_INVALID_MESSAGE, fromStr.length(),
                    config.getFilePath(), fromStr, toHexString(fromStr), fromStr,
                    toHexString(fromStr)));
        }
        // 変換後の文字列が1文字でない場合、例外をスローする
        if (toStr.length() != 1) {
            throw new IllegalStateException(String.format(
                    CHARACTER_LENGTH_INVALID_MESSAGE, toStr.length(), config.getFilePath(),
                    fromStr, toHexString(fromStr), toStr, toHexString(toStr)));
        }
    }

    /**
     * 引数の文字列を16進数に変換する。
     * @param str 変換前の文字列
     * @return 16進数に変換した文字列
     */
    private String toHexString(String str) {
        StringBuilder builder = new StringBuilder();
        for (char c : str.toCharArray()) {
            builder.append(String.format("\\u%04x", (int) c));
        }
        return builder.toString();
    }
    
    /** 文字長が異なる場合にスローする例外のメッセージ */
    private static final String CHARACTER_LENGTH_INVALID_MESSAGE = "invalid character was specified. "
                + "replacement character length must be '1', but was '%s'. "
                + "property file=[%s], key=[%s](%s), invalid str=[%s](%s).";

    /**
     * 変換前文字列と変換後文字列のバイト長チェックを行う。
     * バイト長チェックは、文字エンコーディングに従い行う。
     * @param fromStr 変換前文字列
     * @param toStr 変換後文字列
     * @param config 寄せ字変換処理の設定を保持するクラス
     */
    protected void checkByteLength(String fromStr, String toStr, CharacterReplacementConfig config) {
        if (!config.isByteLengthCheck()) {
            return;
        }

        checkProperty("encoding", config.getEncoding(), config);
        
        // 文字エンコーディングに従い、変換前文字列と変換後文字列をバイト配列に変換する
        byte[] fromBytes; 
        byte[] toBytes; 
        try {
            fromBytes = fromStr.getBytes(config.getEncoding());
            toBytes = toStr.getBytes(config.getEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    String.format("invalid encoding was specified. property file=[%s], type name=[%s], encoding=[%s].",
                            config.getFilePath(), config.getTypeName(), config.getEncoding()), e);
        }
        
        // バイト配列の長さをチェックし、一致しない場合、例外をスローする
        if (fromBytes.length != toBytes.length) {
            String convertedFromStr;
            String convertedToStr;
            try {
                convertedFromStr = new String(fromBytes, config.getEncoding());
                convertedToStr = new String(toBytes, config.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // can not happen.
            }
            throw new IllegalStateException(String.format(
                    "length of the byte string after the conversion is invalid. property file=[%s], type name=[%s], encoding=[%s], "
                  + "from: {character=[%s], bytes=%s, byte length=[%s], unicode character=[%s](\\u%04x)}, "
                  + "to: {character=[%s], bytes=%s, bytes length=[%s]}, unicode character=[%s](\\u%04x)}.", 
                  config.getFilePath(), config.getTypeName(), config.getEncoding(), 
                  convertedFromStr, Arrays.toString(fromBytes), fromBytes.length, fromStr, (int) fromStr.charAt(0), 
                  convertedToStr, Arrays.toString(toBytes), toBytes.length, toStr, (int) toStr.charAt(0)));
        }
    }

    /**
     * 設定されたプロパティの妥当性をチェックする。
     * @param name  プロパティ名
     * @param value プロパティの値
     * @param config 寄せ字変換処理の設定を保持するクラス
     * @throws IllegalStateException プロパティが設定されていない場合
     */
    protected void checkProperty(String name, String value, CharacterReplacementConfig config) throws IllegalStateException {
        if (StringUtil.isNullOrEmpty(value)) {
            throw new IllegalStateException(String.format(
                    "property '%s' was not set. property '%s' must be set. class=[%s] type name=[%s]."
                    , name, name, config.getClass().getName(), config.getTypeName()));
        }
    }

    /**
     * 引数で渡された文字列に対して、寄せ字変換処理を行う。
     * @param typeName 寄せ字タイプ名
     * @param input 入力文字列
     * @return 寄せ字変換処理後の文字列
     */
    public String replaceCharacter(String typeName, String input) {
        
        // 寄せ字タイプがnullまたは空文字の場合、例外をスローする
        if (StringUtil.isNullOrEmpty(typeName)) {
            throw new IllegalArgumentException(String.format(
                    "type name was blank. type name must not be null. input=[%s].", input));
        }

        // 引数で指定された寄せ字タイプ名が定義されていない場合、例外をスローする
        if (!replacementDefinitionMap.containsKey(typeName)) {
            throw new IllegalArgumentException(String.format(
                    "type name was not found. type name=[%s], settable type name=%s. input=[%s].", 
                    typeName, replacementDefinitionMap.keySet().toString(), input));
        }

        // 寄せ字変換前の文字列がnullまたは空文字の場合は、そのまま返却する
        if (StringUtil.isNullOrEmpty(input)) {
            return input;
        }
        
        Map<Character, Character> table = replacementDefinitionMap.get(typeName).getTable();
        
        StringBuilder result = new StringBuilder();

        // 変換対象の文字列が寄せ字変換定義テーブルに存在する場合は、定義に従い、寄せ字変換処理を行う
        for (char c : input.toCharArray()) {
            // 変換対象文字列が寄せ字変換定義テーブルに存在する場合
            if (table.containsKey(c)) {
                // 寄せ字変換テーブルに従い、寄せ字変換後の文字を保持するバッファに代替文字を追加する
                result.append(table.get(c));
                outputLog(typeName, c, table.get(c), input);
            } else {
                // 入力された文字をそのままバッファに追加する
                result.append(c);
            } 
        }
        return result.toString();
    }

    /**
     * 文字を変換した際のログを出力する。
     * @param typeName 寄せ字変換タイプ名
     * @param from 寄せ字変換前の文字
     * @param to 寄せ字変換後の文字
     * @param input 入力文字列
     */
    protected void outputLog(String typeName, char from, char to, String input) {
        // デバッグログを出力する
        if (LOGGER.isDebugEnabled()) {
            LOGGER.logDebug(String.format(
                    "replace character. from=[%s](\\u%04x), to=[%s](\\u%04x) input=[%s], typeName=[%s].",
                    from, (int) from, to, (int) to, input, typeName));
        } 
    }
    
    /**
     * 寄せ字変換テーブルおよび寄せ字変換の際に使用するエンコーディングを保持するクラス。
     * @author Masato Inoue
     */
    private static class CharacterReplacementDefinition {
        
        /** 寄せ字変換テーブル */
        private Map<Character, Character> table;

        /** 寄せ字変換テーブルに対応する文字エンコーディング */
        private Charset encoding;
        
        /**
         * 寄せ字変換テーブルを取得する。
         * @return 寄せ字変換テーブル
         */
        public Map<Character, Character> getTable() {
            return table;
        }
        
        /**
         * 寄せ字変換テーブルを設定する。
         * @param table 寄せ字変換テーブル
         * @return このオブジェクト自体
         */
        public CharacterReplacementDefinition setTable(Map<Character, Character> table) {
            this.table = table;
            return this;
        }
        
        /**
         * 文字エンコーディングを取得する。
         * @return 文字エンコーディング
         */
        public Charset getEncoding() {
            return encoding;
        }
        
        /**
         * 文字エンコーディングを設定する。
         * @param encoding 文字エンコーディング
         * @return このオブジェクト自体
         */
        public CharacterReplacementDefinition setEncoding(Charset encoding) {
            this.encoding = encoding;
            return this;
        }
    }


    /**
     * 寄せ字変換処理の設定を保持するListを設定する。
     * @param configList 寄せ字変換処理の設定を保持するList
     * @return このオブジェクト自体
     * 
     */
    public CharacterReplacementManager setConfigList(
            List<CharacterReplacementConfig> configList) {
        this.configList = configList;
        return this;
    }

    /**
     * 引数で指定された寄せ字タイプ名が、寄せ字タイプ名として定義されているかどうかチェックする。
     * @param typeName 寄せ字タイプ名
     * @return 引数で指定された寄せ字タイプ名が定義されている場合、true
     */
    public boolean containsReplacementType(String typeName) {
        return replacementDefinitionMap.containsKey(typeName);
    }
    
    /**
     * 引数で指定された寄せ字タイプ名と文字エンコーディングの組み合わせが、定義された組み合わせと一致するかどうかをチェックする。
     * <p>
     * 寄せ字タイプ名と、文字エンコーディングの組み合わせは、通常コンポーネント設定ファイルで定義される。
     * </p>
     * @param typeName 寄せ字タイプ名
     * @param encoding 文字エンコーディング
     * @return 寄せ字タイプ名が
     */
    public boolean checkReplacementTypeEncoding(String typeName,
            Charset encoding) {
        if (!replacementDefinitionMap.containsKey(typeName)) {
            throw new IllegalArgumentException(String.format(
                    "type name was not found. type name=[%s], settable type name=%s.", 
                    typeName, replacementDefinitionMap.keySet().toString()));
        }
        return encoding.equals(replacementDefinitionMap.get(typeName).getEncoding());
    }
}
