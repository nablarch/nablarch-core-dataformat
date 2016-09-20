package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.XmlDataConvertorSetting;
import nablarch.core.repository.SystemRepository;



/**
 * フォーマット定義ファイルの内容に従い、XMLデータの読み書きを行うクラス。
 * <p>
 * 本クラスはスレッドセーフを考慮した実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * <b>ディレクティブの設定</b>
 * <p>
 * XMLデータを読み込む際は、以下のディレクティブの設定が必須となる。
 * <ul>
 * <li>ファイルの文字エンコーディング</li>
 * </ul>
 * </p>
 * 
 * @author TIS
 */
public class XmlDataRecordFormatter extends StructuredDataRecordFormatterSupport {

    /**
     * デフォルトコンストラクタ。
     * デフォルトでは、XmlDataConvertorSettingをコンバータとして使用する。
     * また、XmlParserをデータパーサーとして使用する。
     */
    public XmlDataRecordFormatter() {
        setConvertorSetting(XmlDataConvertorSetting.getInstance());

        StructuredDataParser dataParser = SystemRepository.get("XmlDataParser");
        if (dataParser == null) {
            dataParser = new XmlDataParser();
        }
        setDataParser(dataParser);
        
        StructuredDataBuilder dataBuilder = SystemRepository.get("XmlDataBuilder");
        if (dataBuilder == null) {
            dataBuilder = new XmlDataBuilder();
        }
        setDataBuilder(dataBuilder);
    }

    /**
     * {@inheritDoc}<br>
     * この実装では"application/xml"を返却する。
     */
    public String getMimeType() {
        return "application/xml";
    }
}
