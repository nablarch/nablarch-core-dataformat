package nablarch.core.dataformat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import nablarch.core.dataformat.convertor.ConvertorSetting;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;



/**
 * フォーマット定義ファイルの内容に従い、構造化データの読み書きを行うクラス。
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
public abstract class StructuredDataRecordFormatterSupport extends DataRecordFormatterSupport {

    /** ロガー * */
    private static final Logger LOGGER = LoggerManager.get(StructuredDataRecordFormatterSupport.class);

    /** 構造化データパーサー */
    private StructuredDataParser dataParser;
    
    /** 構造化データビルダー */
    private StructuredDataBuilder dataBuilder;
    
    /** 入力ストリーム */
    private InputStream source;

    /** 出力ストリーム */
    private OutputStream dest;

    /**
     * 構造化データパーサーを設定する
     * @param dataParser 構造化データパーサー
     */
    public void setDataParser(StructuredDataParser dataParser) {
        this.dataParser = dataParser;
    }
    
    /**
     * 構造化データパーサーを返却する
     * @return 構造化データパーサー
     */
    protected StructuredDataParser getDataParser() {
        return dataParser;
    }
    
    /**
     * 構造化データビルダーを設定する
     * @param dataBuilder 構造化データビルダー
     */
    public void setDataBuilder(StructuredDataBuilder dataBuilder) {
        this.dataBuilder = dataBuilder;
    }
    
    /**
     * 構造化データビルダーを返却する
     * @return 構造化データビルダー
     */
    protected StructuredDataBuilder getDataBuilder() {
        return dataBuilder;
    }
    
    /** 構造化データのコンバータの設定情報保持クラス */
    private ConvertorSetting convertorSetting;
    
    /**
     * 構造化データのコンバータの設定情報保持クラスを取得する。
     *
     * @return 構造化データのコンバータの設定情報保持クラス
     */
    public ConvertorSetting getConvertorSetting() {
        return convertorSetting;
    }

    /**
     * 構造化データのコンバータの設定情報保持クラスを取得する。
     *
     * @param convertorSetting 構造化データのコンバータの設定情報保持クラス
     */
    protected void setConvertorSetting(ConvertorSetting convertorSetting) {
        this.convertorSetting = convertorSetting;
    }

    /**
     * XMLデータフォーマッタが使用するディレクティブの名前と値の型。
     * 以下に一覧を示す。<br>
     * <ul>
     * <li>root-element：String</li>
     * </ul>
     *
     * @author TIS
     */
    public static class StructuredDataDirective extends Directive {
        /** 列挙型の全要素(親クラスの要素を含む） */
        public static final Map<String, Directive> VALUES = Directive.createDirectiveMap(
                TEXT_ENCODING
        );

        /**
         * コンストラクタ。
         *
         * @param name ディレクティブ名
         * @param type ディレクティブの値の型
         */
        public StructuredDataDirective(String name, Class<?> type) {
            super(name, type);
        }

        /**
         * ディレクティブの値を取得する。
         *
         * @param name ディレクティブの名前
         * @return ディレクティブの値
         */
        public static Directive valueOf(String name) {
            return VALUES.get(name);
        }
    }

    /** {@inheritDoc} */
    public DataRecordFormatter initialize() {
        super.initialize();

        initializeDefinition();

        return this;
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setInputStream(InputStream stream) {
        source = stream;
        return this;
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setOutputStream(OutputStream stream) {
        dest = stream;
        return this;
    }

    /** {@inheritDoc} */
    public DataRecord readRecord() throws IOException,
            InvalidDataFormatException {

        if (source == null) {
            throw new IllegalStateException("input stream was not set. input stream must be set before reading.");
        }

        if (!hasNext()) {
            return null;
        }

        incrementRecordNumber(); // レコード番号をインクリメントする

        DataRecord record = new DataRecord();
        
        Map<String, ?> flatMap = getDataParser().parseData(source, getDefinition());
        record.putAll(flatMap);

        return record;
    }
    
    /** {@inheritDoc} */
    public void writeRecord(Map<String, ?> record)
            throws IOException, InvalidDataFormatException {

        writeRecord(null, record);
    }

    /** {@inheritDoc} */
    public void writeRecord(String recordType, Map<String, ?> record)
            throws IOException {

        if (dest == null) {
            throw new IllegalStateException("output stream was not set. output stream must be set before writing.");
        }

        incrementRecordNumber(); // レコード番号をインクリメントする

        getDataBuilder().buildData(record, getDefinition(), dest);
        
        dest.flush();
    }

    /**
     * {@inheritDoc}
     * この実装では、{@link #setInputStream}メソッドおよび{@link #setOutputStream}メソッドで渡されたストリームをクローズする。
     */
    public void close() {
        if (source != null) {
            try {
                source.close();
                source = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the input stream.", e);
            }
        }
        if (dest != null) {
            try {
                dest.close();
                dest = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the output stream.", e);
            }
        }
    }

    /** {@inheritDoc} */
    public boolean hasNext() throws IOException {
        if (source == null) {
            return false;
        }
        
        source.mark(1);
        int readByte = source.read();
        source.reset();
        return (readByte != -1);
    }
}
