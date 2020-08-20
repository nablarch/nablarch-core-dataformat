package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;

public class DataFormatConfigFinder {

    /** デフォルトの{@link DataFormatConfig} */
    private static final DataFormatConfig DEFAULT_DATA_FORMAT_CONFIG = new DataFormatConfig();

    /**
     * インスタンス化しない
     */
    private DataFormatConfigFinder() {
        // nop
    }

    /** {@link DataFormatConfig}をリポジトリから取得する際に使用する名前 */
    private static final String WEB_CONFIG_NAME = "dataFormatConfig";

    /**
     * 汎用データフォーマット機能の設定を取得する。
     * @return 汎用データフォーマット機能の設定
     */
    public static DataFormatConfig getDataFormatConfig() {
        DataFormatConfig dataFormatConfig = SystemRepository.get(WEB_CONFIG_NAME);
        if (dataFormatConfig != null) {
            return dataFormatConfig;
        }
        return DEFAULT_DATA_FORMAT_CONFIG;
    }
}
