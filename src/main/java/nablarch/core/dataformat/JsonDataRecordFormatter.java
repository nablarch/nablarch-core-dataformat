package nablarch.core.dataformat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.convertor.JsonDataConvertorSetting;
import nablarch.core.repository.SystemRepository;


/**
 * フォーマット定義ファイルの内容に従い、JSONデータの読み書きを行うクラス。
 * <p>
 * 本クラスはスレッドセーフを考慮した実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * <b>ディレクティブの設定</b>
 * <p>
 * JSONデータを読み込む際は、以下のディレクティブの設定が必須となる。
 * <ul>
 * <li>ファイルの文字エンコーディング</li>
 * </ul>
 * </p>
 * 
 * @author TIS
 */
public class JsonDataRecordFormatter extends StructuredDataRecordFormatterSupport {

    /** 許容エンコーディングリスト */
    private static final List<String> ALLOW_ENCODING_LIST = Arrays.asList("UTF-8", "UTF-16LE", "UTF-32LE", "UTF-16BE", "UTF-32BE");
    
    /**
     * デフォルトコンストラクタ。
     * デフォルトでは、JsonDataConvertorSettingをコンバータとして使用する。
     * また、JsonParserをデータパーサーとして使用する。
     */
    public JsonDataRecordFormatter() {
        setConvertorSetting(JsonDataConvertorSetting.getInstance());
        
        StructuredDataParser dataParser = SystemRepository.get("JsonDataParser");
        if (dataParser == null) {
            dataParser = new JsonDataParser();
        }
        setDataParser(dataParser);
        
        StructuredDataBuilder dataBuilder = SystemRepository.get("JsonDataBuilder");
        if (dataBuilder == null) {
            dataBuilder = new JsonDataBuilder();
        }
        setDataBuilder(dataBuilder);
    }

    /**
     * {@inheritDoc}
     * この実装では、以下の検証を行う。
     * <ul>
     * <li>エンコーディングがUTF-8であること</li>
     * </ul>
     */
    @Override
    protected void validateDirectives(Map<String, Object> directive) {
        super.validateDirectives(directive);

        // JSONデータを読み込む場合、エンコーディングはUnicode系限定
        String encoding = StructuredDataDirective.getTextEncoding(directive);
        if (!ALLOW_ENCODING_LIST.contains(encoding)) {
            throw new SyntaxErrorException(String.format(
                    "when file-type is 'JSON', directive '%s' must be specified by %s."
                    , StructuredDataDirective.TEXT_ENCODING.getName()
                    , ALLOW_ENCODING_LIST.toString()));
        }
    }

    /**
     * {@inheritDoc}<br>
     * この実装では"application/json"を返却する。
     */
    public String getMimeType() {
        return "application/json";
    }
}

