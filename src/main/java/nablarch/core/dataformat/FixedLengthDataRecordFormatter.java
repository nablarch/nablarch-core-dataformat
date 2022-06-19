package nablarch.core.dataformat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import nablarch.core.dataformat.convertor.ConvertorSetting;
import nablarch.core.dataformat.convertor.FixedLengthConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.ByteStreamDataSupport;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.datatype.NumberStringDecimal;
import nablarch.core.dataformat.convertor.datatype.PackedDecimal;
import nablarch.core.dataformat.convertor.datatype.SignedNumberStringDecimal;
import nablarch.core.dataformat.convertor.datatype.ZonedDecimal;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;


/**
 * フォーマット定義ファイルの内容に従い、固定長ファイルデータの読み書きを行うクラス。
 * <p>
 * 本クラスはスレッドセーフを考慮した実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * <b>ディレクティブの設定</b>
 * <p>
 * 固定長ファイルデータを読み込む際は、以下のディレクティブの設定が必須となる。
 * <ul>
 * <li>ファイルの文字エンコーディング</li>
 * <li>レコード長</li>
 * </ul>
 * </p>
 * <p>
 * また、任意で以下のディレクティブの設定を行うことができる。
 * <ul>
 * <li>レコード終端文字列（\n、\r、\r\nなど）</li>
 * <li>ゾーン数値、パック数値の符号ビット（正）</li>
 * <li>ゾーン数値、パック数値の符号ビット（負）</li>
 * <li>数値文字列の小数点の要否</li>
 * <li>数値文字列の符号位置の固定/非固定</li>
 * <li>数値文字列の正の符号の必須/非必須</li>
 * </ul>
 * </p>
 * <p>
 * レコード終端文字列は、許容する文字列のみ指定できる。<br/>
 * デフォルトでは「\n」「\r」「\r\n」の3種類の文字列をレコード終端文字列として許容するが、
 * それを変更したい場合は、任意の許容する文字列のリストを{@link FormatterFactory}クラスのallowedRecordSeparatorListプロパティに設定すること。
 * </p>
 * <p>
 * ゾーンおよびパック数値の符号ビットは、フィールド単位のみでなく、ファイル単位およびシステム単位でも指定できる。<br/>
 * 「フィールド単位 > ファイル単位 > システム単位」の優先度順で、使用する符号ビットが決定される。<br/>
 * 符号ビットをファイル単位で指定する場合はディレクティブ、システム単位で指定する場合はコンポーネント設定ファイルで定義する。
 * </p>
 * <p>
 * 数値文字列のディレクティブについての詳細は
 * {@link nablarch.core.dataformat.convertor.datatype.NumberStringDecimal NumberStringDecimal}の
 * Javadocを参照すること。
 * </p>
 * 
 * @author Iwauo Tajima
 */
public class FixedLengthDataRecordFormatter extends DataRecordFormatterSupport {

    /** ロガー * */
    private static final Logger LOGGER = LoggerManager.get(FixedLengthDataRecordFormatter.class);

    /** 符号ビットのパターン */
    private static final Pattern SIGN_BIT_FORMAT = Pattern.compile("[a-fA-F0-9]");

    /** 固定長データのコンバータの設定情報保持クラス */
    private FixedLengthConvertorSetting convertorSetting;

    /**
     * 固定長データのコンバータの設定情報保持クラスを取得する。
     *
     * @return 固定長データのコンバータの設定情報保持クラス
     */
    public ConvertorSetting getConvertorSetting() {
        return convertorSetting;
    }

    /** ゾーンNibble */
    private Byte zoneNibble = null;

    /** ゾーンNibble （符号ビット:＋） */
    private Byte zoneSignNibblePositive = null;

    /** ゾーンNibble （符号ビット:ー） */
    private Byte zoneSignNibbleNegative = null;

    /** パックNibble */
    private Byte packNibble = null;

    /** パックNibble （符号ビット:＋） */
    private Byte packSignNibblePositive = null;

    /** パックNibble （符号ビット:ー） */
    private Byte packSignNibbleNegative = null;

    /** 小数点の要否 */
    private boolean isRequiredDecimalPoint = true;
    
    /** 符号位置の固定/非固定 */
    private boolean isFixedSignPosition = true;

    /** 正の符号の必須/非必須 */
    private boolean isRequiredPlusSign = false;
    
    /** 入力ストリーム */
    private InputStream source;

    /** 出力ストリーム */
    private OutputStream dest;

    /** レコード長 */
    private Integer recordLength;

    /** レコード終端文字列（バイト） */
    private byte[] recordSeparatorByte;

    /**
     * 固定長ファイルフォーマッタが使用するディレクティブの名前と値の型。
     * 以下に一覧を示す。<br>
     * <ul>
     * <li>positive-zone-sign-nibble：String</li>
     * <li>negative-zone-sign-nibble：String</li>
     * <li>positive-pack-sign-nibble：String</li>
     * <li>negative-pack-sign-nibble：String</li>
     * <li>fixed-sign-position：Boolean</li>
     * <li>required-plus-sign：Boolean</li>
     * </ul>
     *
     * @author Masato Inoue
     */
    public static class FixedLengthDirective extends Directive {
        /** 1レコードあたりのバイト長 */
        public static final Directive RECORD_LENGTH = new Directive("record-length", Integer.class);
        /** ゾーン数値の符号ビット（正）のデフォルト設定 */
        public static final Directive POSITIVE_ZONE_SIGN_NIBBLE = new Directive("positive-zone-sign-nibble",
                                                                                String.class);
        /** ゾーン数値の符号ビット（負）のデフォルト設定 */
        public static final Directive NEGATIVE_ZONE_SIGN_NIBBLE = new Directive("negative-zone-sign-nibble",
                                                                                String.class);
        /** パック数値の符号ビット（正）のデフォルト設定 */
        public static final Directive POSITIVE_PACK_SIGN_NIBBLE = new Directive("positive-pack-sign-nibble",
                                                                                String.class);
        /** パック数値の符号ビット（負）のデフォルト設定 */
        public static final Directive NEGATIVE_PACK_SIGN_NIBBLE = new Directive("negative-pack-sign-nibble",
                                                                                String.class);
        /** 小数点の要否のデフォルト設定 */
        public static final Directive REQUIRED_DECIMAL_POINT = new Directive("required-decimal-point", Boolean.class);
        
        /** 符号位置の固定/非固定のデフォルト設定 */
        public static final Directive FIXED_SIGN_POSITION = new Directive("fixed-sign-position", Boolean.class);
        
        /** 正の符号の要否のデフォルト設定 */
        public static final Directive REQUIRED_PLUS_SIGN = new Directive("required-plus-sign", Boolean.class);

        
        /** 列挙型の全要素(親クラスの要素を含む） */
        public static final Map<String, Directive> VALUES = Directive.createDirectiveMap(
                RECORD_LENGTH,
                POSITIVE_ZONE_SIGN_NIBBLE,
                NEGATIVE_ZONE_SIGN_NIBBLE,
                POSITIVE_PACK_SIGN_NIBBLE,
                NEGATIVE_PACK_SIGN_NIBBLE,
                REQUIRED_DECIMAL_POINT, 
                FIXED_SIGN_POSITION,
                REQUIRED_PLUS_SIGN
        );

        /**
         * コンストラクタ。
         *
         * @param name ディレクティブ名
         * @param type ディレクティブの値の型
         */
        public FixedLengthDirective(String name, Class<?> type) {
            super(name, type);
        }

        /**
         * 1レコードあたりのバイト長を取得する。
         *
         * @param directive ディレクティブ
         * @return 1レコードあたりのバイト長
         */
        public static Integer getRecordLength(Map<String, Object> directive) {
            return (Integer) directive.get(RECORD_LENGTH.getName());
        }

        /**
         * ゾーン数値の符号ビット（正）のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return ゾーン数値の符号ビット（正）のデフォルト設定
         */
        public static String getPositiveZoneSignNibble(Map<String, Object> directive) {
            return (String) directive.get(POSITIVE_ZONE_SIGN_NIBBLE.getName());
        }

        /**
         * ゾーン数値の符号ビット（負）のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return ゾーン数値の符号ビット（負）のデフォルト設定
         */
        public static String getNegativeZoneSignNibble(Map<String, Object> directive) {
            return (String) directive.get(NEGATIVE_ZONE_SIGN_NIBBLE.getName());
        }

        /**
         * パック数値の符号ビット（正）のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return パック数値の符号ビット（正）のデフォルト設定
         */
        public static String getPositivePackSignNibble(Map<String, Object> directive) {
            return (String) directive.get(POSITIVE_PACK_SIGN_NIBBLE.getName());
        }

        /**
         * パック数値の符号ビット（正）のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return パック数値の符号ビット（負）のデフォルト設定
         */
        public static String getNegativePackSignNibble(Map<String, Object> directive) {
            return (String) directive.get(NEGATIVE_PACK_SIGN_NIBBLE.getName());
        }

        /**
         * 小数点の要否のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return 小数点の要否
         */
        public static Boolean getRequiredDecimalPoint(Map<String, Object> directive) {
            return (Boolean) directive.get(REQUIRED_DECIMAL_POINT.getName());
        }
        
        /**
         * 符号位置の固定/非固定のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return 符号位置の固定/非固定のデフォルト設定
         */
        public static Boolean getFixedSignPosition(Map<String, Object> directive) {
            return (Boolean) directive.get(FIXED_SIGN_POSITION.getName());
        }

        /**
         * 正の符号の要否のデフォルト設定を取得する。
         *
         * @param directive デフォルト値
         * @return 正の符号の要否のデフォルト設定
         */
        public static Boolean getRequiredPlusSign(Map<String, Object> directive) {
            return (Boolean) directive.get(REQUIRED_PLUS_SIGN.getName());
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
    @Override
    protected Map<String, Directive> createDirectiveMap() {
        return FixedLengthDirective.VALUES;
    }

    /**
     * デフォルトコンストラクタ。
     * デフォルトでは、FixedLengthConvertorSettingをコンバータとして使用する。
     */
    @Published(tag = "architect")
    public FixedLengthDataRecordFormatter() {
        convertorSetting = FixedLengthConvertorSetting.getInstance();
    }

    /** {@inheritDoc} */
    public DataRecordFormatter initialize() {
        super.initialize();

        initializeDefinition();

        return this;
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setInputStream(InputStream stream) {
        // InputStreamはバッファリングするものでラップせずに使うこと。
        // バッファリングするもので読み込んだ場合、このクラスを使用している一部機能(MOM)が動作しなくなる…
        source = stream;
        return this;
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setOutputStream(OutputStream stream) {
        dest = stream;
        return this;
    }

    /**
     * {@inheritDoc}
     * この実装では、以下の検証を行う。
     * <ul>
     * <li>ディレクティブの値のデータ型が正しいこと</li>
     * <li>符号ビットの形式が正しいこと</li>
     * </ul>
     */
    @Override
    protected void validateDirectives(Map<String, Object> directive) {
        super.validateDirectives(directive);

        // 固定長ファイルレコードを読み込む場合、レコード長の指定は必須
        Integer recordLength = FixedLengthDirective.getRecordLength(directive);
        if (recordLength == null) {
            throw new SyntaxErrorException(String.format(
                    "directive '%s' was not specified. directive '%s' must be specified."
                    , FixedLengthDirective.RECORD_LENGTH.getName(), FixedLengthDirective.RECORD_LENGTH.getName()));
        }

        String positiveZoneSignNibbleStr = FixedLengthDirective.getPositiveZoneSignNibble(directive);
        // ゾーン数値の符号ビット（正）が設定されている場合、形式チェックを行う
        if (positiveZoneSignNibbleStr != null) {
            validateNibble(FixedLengthDirective.POSITIVE_ZONE_SIGN_NIBBLE.getName(),
                           positiveZoneSignNibbleStr);
        }
        String negativeZoneSignNibbleStr = FixedLengthDirective.getNegativeZoneSignNibble(directive);
        // ゾーン数値の符号ビット（負）が設定されている場合、形式チェックを行う
        if (negativeZoneSignNibbleStr != null) {
            validateNibble(FixedLengthDirective.NEGATIVE_ZONE_SIGN_NIBBLE.getName(),
                           negativeZoneSignNibbleStr);
        }
        String positivePackSignNibbleStr = FixedLengthDirective.getPositivePackSignNibble(directive);
        // パック数値の符号ビット（正）が設定されている場合、形式チェックを行う
        if (positivePackSignNibbleStr != null) {
            validateNibble(FixedLengthDirective.POSITIVE_PACK_SIGN_NIBBLE.getName(),
                           positivePackSignNibbleStr);
        }
        String negativePackSignNibbleStr = FixedLengthDirective.getNegativePackSignNibble(directive);
        // ゾーン数値の符号ビット（負）が設定されている場合、形式チェックを行う
        if (negativePackSignNibbleStr != null) {
            validateNibble(FixedLengthDirective.NEGATIVE_PACK_SIGN_NIBBLE.getName(),
                           negativePackSignNibbleStr);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * {@link FixedLengthDataRecordFormatter}では、ディレクティブまたはコンポーネント設定ファイルに設定された以下の値をフィールドに設定する。
     * <ul>
     * <li>1レコードあたりのバイト長</li>
     * <li>レコード終端文字列（バイト）</li>
     * <li>ゾーンNibbleと符号ビット</li>
     * <li>パックNibbleと符号ビット</li>
     * <li>数値項目の小数点の要否</li>
     * <li>数値項目の正の符号の要否</li>
     * <li>数値項目の符号位置の固定/非固定</li>
     * </ul>
     *
     * @param directive ディレクティブ
     */
    public void initializeField(Map<String, Object> directive) {
        super.initializeField(directive);
        recordLength = FixedLengthDirective.getRecordLength(directive);

        // レコード終端文字列はバイトに変換して保持する
        String separator = (String) directive
                .get(FixedLengthDirective.RECORD_SEPARATOR.getName());
        if (separator != null) {
            recordSeparatorByte
                    = StringUtil.getBytes(separator, getDefaultEncoding());
        }

        determineZoneNibble(directive);
        determinePackNibble(directive);
        determineRequiredDecimalPoint(directive);
        determineFixedSignPosition(directive);
        determineRequiredPlusSign(directive);

    }

    /**
     * 小数点の要否の設定を行う。
     * <p/>
     * ディレクティブで小数点の要否が指定されている場合はその値を、指定されていない場合はtrueを設定する。
     * @param directive ディレクティブ
     */
    private void determineRequiredDecimalPoint(Map<String, Object> directive) {
        Boolean requiredDecimalPoint = FixedLengthDirective.getRequiredDecimalPoint(directive);
        if (requiredDecimalPoint == null) {
            isRequiredDecimalPoint = true;
        } else {
            isRequiredDecimalPoint = requiredDecimalPoint;
        }
    }
    
    /**
     * 符号位置の固定/非固定の設定を行う。
     * <p/>
     * ディレクティブで符号位置の固定/非固定が指定されている場合はその値を、指定されていない場合はtrue（符号位置は固定）を設定する。
     * @param directive ディレクティブ
     */
    private void determineRequiredPlusSign(Map<String, Object> directive) {
        Boolean fixedSignPosition = FixedLengthDirective.getFixedSignPosition(directive);
        if (fixedSignPosition == null) {
            isFixedSignPosition = true;
        } else {
            isFixedSignPosition = fixedSignPosition;
        }
    }

    /**
     * 正の符号の要否の設定を行う。
     * <p/>
     * ディレクティブで正の符号の要否が指定されている場合はその値を、指定されていない場合はfalseを設定する。
     * @param directive ディレクティブ
     */
    private void determineFixedSignPosition(Map<String, Object> directive) {
        Boolean requiredPlusSign = FixedLengthDirective.getRequiredPlusSign(directive);
        if (requiredPlusSign == null) {
            isRequiredPlusSign = false;
        } else {
            isRequiredPlusSign = requiredPlusSign;
        }
    }

    /**
     * 符号ビットが半角英数字で構成されているか検証する。
     *
     * @param key   符号ビットが設定されたディレクティブのキー
     * @param value 符号ビットの設定値
     */
    protected void validateNibble(String key, String value) {
        if (!SIGN_BIT_FORMAT.matcher(value).matches()) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid sign nibble was specified by '%s' directive. value=[%s]. "
                                    + "sign nibble format must be [[0-9a-fA-F]].",
                            key, value));
        }
    }

    /**
     * {@inheritDoc}
     * この実装では、固定長フィールドの合計の長さが、ディレクティブで定義されたレコード長と一致するか検証する。
     */
    @Override
    protected void validateRecordLength(int head, RecordDefinition record) {
        // レコード長の整合性チェック（固定長ファイルの場合のみ）
        if (head != recordLength + 1) {
            throw newSyntaxError(
                    "invalid record length was specified by '",
                    FixedLengthDirective.RECORD_LENGTH.getName(), "' directive. ",
                    "sum of length of fields must be '", recordLength, "' byte ",
                    "but was '", (head - 1), "'.");
        }
    }

    /**
     * ゾーン/パック数値の符号ビットの設定および、
     * 数値文字列の小数点の要否、符号位置の固定/非固定、正の符号の要否の設定を行う。
     * また、各データタイプに空文字列を{@code null}に変換するかどうかの設定を行う。
     * 
     * @param dataType データタイプ
     * @return 自分自身
     */
    @Override
    public DataRecordFormatterSupport setDataTypeProperty(DataType<?, ?> dataType) {
        if (ZonedDecimal.class.isAssignableFrom(dataType.getClass())) {
            ZonedDecimal zoneType = (ZonedDecimal) dataType;
            zoneType.setZoneNibble(zoneNibble);
            zoneType.setDefaultZoneSignNibbleNegative(zoneSignNibbleNegative);
            zoneType.setDefaultZoneSignNibblePositive(zoneSignNibblePositive);
        }
        if (PackedDecimal.class.isAssignableFrom(dataType.getClass())) {
            PackedDecimal packType = (PackedDecimal) dataType;
            packType.setPackNibble(packNibble);
            packType.setDefaultPackSignNibbleNegative(packSignNibbleNegative);
            packType.setDefaultPackSignNibblePositive(packSignNibblePositive);
        }

        if (dataType instanceof NumberStringDecimal) {
            final NumberStringDecimal numberStringDecimal = (NumberStringDecimal) dataType;
            numberStringDecimal.setRequiredDecimalPoint(isRequiredDecimalPoint);
        }
        
        if (dataType instanceof SignedNumberStringDecimal) {
            SignedNumberStringDecimal signedNumberType = (SignedNumberStringDecimal) dataType;
            signedNumberType.setFixedSignPosition(isFixedSignPosition);
            signedNumberType.setRequiredPlusSign(isRequiredPlusSign);
        }

        dataType.setConvertEmptyToNull(convertorSetting.isConvertEmptyToNull());
        return this;
    }
    
    /** {@inheritDoc} */
    public DataRecord readRecord() throws IOException,
            InvalidDataFormatException {

        if (source == null) {
            throw new IllegalStateException("input stream was not set. input stream must be set before reading.");
        }

        byte[] buff = new byte[recordLength];
        int readBytes = source.read(buff);
        // これ以上読み込むレコードがない場合、nullを返却する
        if (readBytes == -1) {
            return null;
        }

        incrementRecordNumber(); // レコード番号をインクリメントする

        if (readBytes != recordLength) {
            throw newInvalidDataFormatException(
                    "invalid data record found. ",
                    "the length of a record must be ", recordLength, " byte ",
                    "but read data was only ", readBytes, " byte."
            );
        }

        // レコード終端データの読み込み
        readRecordSeparator(source, recordSeparatorByte);

        // シングルフォーマットの場合
        if (getDefinition().getRecordClassifier() == null) {
            return convertToRecord(buff, getDefinition().getRecords().get(0));
        }

        // マルチフォーマットの場合
        Map<String, Object> record = convertToRecord(buff, getDefinition().getRecordClassifier());
        for (RecordDefinition recordDef : getDefinition().getRecords()) {
            if (recordDef.isApplicableTo(record)) {
                return convertToRecord(buff, recordDef);
            }
        }
        throw newInvalidDataFormatException(
                "an applicable layout definition was not found in the record. ",
                "record=[", record, "]."
        );
    }

    /**
     * レコード区切り文字を読み込む。<br/>
     * レコード区切り文字無しの場合は何もしない。
     *
     * @param source          入力ストリーム
     * @param recordSeparator レコード区切り文字(nullまたは要素数0の場合、区切り文字無しとする)
     * @throws IOException 入出力例外
     */
    private void readRecordSeparator(InputStream source, byte[] recordSeparator)
            throws IOException {

        // レコード区切り文字を使用しない場合は何もしない
        boolean noRecordSeparator =
                recordSeparator == null || recordSeparator.length == 0;
        if (noRecordSeparator) {
            return;
        }

        // レコード区切り文字を読み込む
        byte[] bytes = new byte[recordSeparator.length];
        source.read(bytes); // 読み込めたサイズがレコード区切り文字の長さに満たない場合、例外がスローされる。実際に何バイトのデータが読み込めたかどうかを確認する必要はないので、readメソッドの戻り値の確認は行わない
        if (!Arrays.equals(recordSeparator, bytes)) {
            throw newInvalidDataFormatException(
                    "invalid record separator was specified by '",
                    Directive.RECORD_SEPARATOR.getName(), "'",
                    " directive. value=[", Arrays.toString(bytes), "].");
        }
    }


    /**
     * 1レコード分の固定長レコードを読み込み、
     * DataRecord型のオブジェクトとして返却する。
     * 入力ストリームが既に終端に達していた場合はnullを返却する。
     *
     * @param bytes     入力データ
     * @param recordDef レコード定義情報保持クラス
     * @return 読み込んだレコード
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合。
     */
    protected DataRecord convertToRecord(byte[] bytes, RecordDefinition recordDef)
            throws IOException {
        DataRecord record = new DataRecord().setRecordType(recordDef
                                                                   .getTypeName());
        record.setRecordNumber(getRecordNumber());

        ByteBuffer buff = ByteBuffer.wrap(bytes);

        for (FieldDefinition field : recordDef.getFields()) {

            // ストリームから、フィールド長の長さだけbyte配列に読み込む
            byte[] fieldBytes = new byte[field.getSize()];
            buff.position(field.getPosition() - 1);
            buff.get(fieldBytes);

            Object value = convertToField(fieldBytes, field);

            String name = field.getName();
            if (!field.isFiller()) {
                record.put(name, value);
            }
        }
        return record;
    }

    /**
     * 入力ストリームから1レコード分のフィールドの内容を読み込み、コンバータを用いて変換したオブジェクトを返却する。
     * 入力ストリームが既に終端に達していた場合はnullを返却する。
     *
     * @param source 入力ストリーム
     * @param field  フィールド定義情報保持クラス
     * @return 読み込んだフィールドの内容
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object convertToField(byte[] source, FieldDefinition field)
            throws IOException {
        ByteStreamDataSupport<?> dataType = (ByteStreamDataSupport<?>) field.getDataType();

        Object value;
        try {
            value = dataType.convertOnRead(source);

            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) {
                value = convertor.convertOnRead(value);
            }
        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対して、ファイル名、レコード番号とフィールド名の情報を付与する
            e.setFieldName(field.getName());
            throw addFormatAndRecordNumberTo(e);
        }
        return value;
    }

    /** {@inheritDoc} */
    public void writeRecord(Map<String, ?> record)
            throws IOException, InvalidDataFormatException {

        if (dest == null) {
            throw new IllegalStateException("output stream was not set. output stream must be set before writing.");
        }

        // レコード内にデータタイプが明示的に指定されている場合は、そのデータタイプに沿って出力処理を行う
        if (record instanceof DataRecord) {
            String recordType = ((DataRecord) record).getRecordType();
            if (recordType != null && recordType.length() != 0) {
                writeRecord(recordType, record);
                return;
            }
        }

        // シングルレイアウトの場合
        if (getDefinition().getRecordClassifier() == null) {
            writeRecord(getDefinition().getRecords().get(0).getTypeName(), record);
            return;
        }

        // マルチレイアウトの場合
        for (RecordDefinition recordDef : getDefinition().getRecords()) {
            if (recordDef.isApplicableTo(record)) {
                writeRecord(recordDef.getTypeName(), record);
                return;
            }
        }
        // エラー。レイアウト定義が見つからない。
        incrementRecordNumber();    // エラー行を正しく表示するため
        throw newInvalidDataFormatException(
                "an applicable layout definition was not found in the record. ",
                "record=[", record, "].");
    }

    /** {@inheritDoc} */
    public void writeRecord(String recordType, Map<String, ?> record)
            throws IOException {

        if (dest == null) {
            throw new IllegalStateException("output stream was not set. output stream must be set before writing.");
        }

        if (StringUtil.isNullOrEmpty(recordType)) {
            throw new IllegalArgumentException("record type was blank. record type must not be blank.");
        }

        incrementRecordNumber(); // レコード番号をインクリメントする

        RecordDefinition recordDef = getDefinition().getRecordType(recordType);

        if (recordDef == null) {
            throw newInvalidDataFormatException(
                    "an applicable layout definition was not found. ",
                    "specified record type=[", recordType, "].");
        }
        // 指定されたレコードタイプと実際の値が矛盾していないことを検証する
        if (!recordDef.isApplicableTo(record, false)) {
            throw newInvalidDataFormatException(
                    "this record could not be applied to the record format. ",
                    "record=[", record, "]. following conditions must be met: ",
                    recordDef.getConditionsToApply(), ".");
        }
        writeRecord(record, recordDef);
        if (DataFormatConfigFinder.getDataFormatConfig().isFlushEachRecordInWriting()) {
            dest.flush();
        }
    }

    /**
     * このオブジェクトのフォーマット情報に従って、
     * 出力ストリームに1レコード分の内容を書き込む。
     *
     * @param record    出力するレコードの内容を格納したMap
     * @param recordDef レコード定義情報保持クラス
     * @throws IOException 書き込みに伴うIO処理で問題が発生した場合。
     */
    protected void writeRecord(Map<String, ?> record,
                               RecordDefinition recordDef) throws IOException {
        for (FieldDefinition field : recordDef.getFields()) {
            writeField(record, field);
        }
        if (recordSeparatorByte != null) {
            dest.write(recordSeparatorByte);
        }
    }

    /**
     * このオブジェクトのフォーマット定義に従って、
     * 出力ストリームにフィールドの内容を書き込む。
     *
     * @param record 出力するレコードの内容を格納したMap
     * @param field  フィールド定義情報保持クラス
     * @throws IOException 書き込みに伴うIO処理で問題が発生した場合。
     */
    @SuppressWarnings("rawtypes")
    protected void writeField(Map<String, ?> record, FieldDefinition field)
            throws IOException {

        Object value = record.get(field.getName());

        byte[] outData;

        try {
            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) {
                value = convertor.convertOnWrite(value);
            }

            // データタイプを実行する
            ByteStreamDataSupport<?> dataType = (ByteStreamDataSupport<?>) field.getDataType();
            outData = dataType.convertOnWrite(value);
        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対してフィールド名の情報を付与する
            throw addFormatAndRecordNumberTo(e).setFieldName(field.getName());
        }

        dest.write(outData);
    }

    /**
     * ゾーンNibbleおよび符号ビットの設定を行う。
     * <p>
     * ゾーンNibbleは使用するエンコーディングに従って決定する。
     * </p>
     * <p>
     * システム単位で使用する符号ビットはコンポーネント設定ファイルに、
     * ファイル単位で使用する符号ビットはフォーマット定義ファイルのディレクティブに定義する。<br>
     * 「ファイル単位＞システム単位」の優先度順で使用する符号ビットが決定される。<br>
     * システム単位およびファイル単位、どちらにも符号ビットが定義されていない場合は、以下の既定値を使用する。
     * <pre>
     * - ascii互換のエンコーディングを使用する場合
     *     正） 0x30 負） 0x70
     * - ebcdic互換のエンコーディングを使用する場合
     *     正） 0xC0 負） 0xD0
     * </pre>
     * もしフィールド単位で符号ビットが設定された場合は、それが最優先で符号ビットとして使用される。
     * </p>
     *
     * @param directive ディレクティブ
     */
    protected void determineZoneNibble(Map<String, Object> directive) {

        zoneNibble = (byte) (getDefaultEncoding().encode("1").get() & 0xF0);

        // デフォルト値を設定する
        if (zoneNibble == 0x30) {
            zoneSignNibblePositive = 0x30;
            zoneSignNibbleNegative = 0x70;
        } else if (zoneNibble == (byte) 0xF0) {
            zoneSignNibblePositive = (byte) 0xC0;
            zoneSignNibbleNegative = (byte) 0xD0;
        } else {
            throw new SyntaxErrorException(String.format("unsupported encoding was specified. value=[%s].",
                                                         getDefaultEncoding()) // can not happen...
            );
        }

        String positiveZoneSignNibbleStr = FixedLengthDirective.getPositiveZoneSignNibble(directive);
        if (positiveZoneSignNibbleStr != null) {
            // ファイル単位（ディレクティブ）でゾーンの符号ビット（正）が明示的に設定されている場合は、それを使用する
            zoneSignNibblePositive = (byte) (Integer.parseInt(
                    positiveZoneSignNibbleStr, 16) << 4);
        } else if (convertorSetting.getDefaultPositiveZoneSignNibble() != null) {
            // システム全体でゾーンの符号ビット（正）が明示的に設定されている場合は、それを使用する
            zoneSignNibblePositive = convertorSetting
                    .getDefaultPositiveZoneSignNibble();
        }
        String negativeZoneSignNibbleStr = FixedLengthDirective.getNegativeZoneSignNibble(directive);
        if (negativeZoneSignNibbleStr != null) {
            // ファイル単位（ディレクティブ）でゾーンの符号ビット（負）が明示的に設定されている場合は、それを使用する
            zoneSignNibbleNegative = (byte) (Integer.parseInt(
                    negativeZoneSignNibbleStr, 16) << 4);
        } else if (convertorSetting.getDefaultNegativeZoneSignNibble() != null) {
            // システム全体でゾーンの符号ビット（負）が明示的に設定されている場合は、それを使用する
            zoneSignNibbleNegative = convertorSetting
                    .getDefaultNegativeZoneSignNibble();
        }

    }


    /**
     * パックNibbleと、それぞれの符号ビットの設定を行う。
     * <p>
     * パックNibbleは使用するエンコーディングに従って決定する。
     * </p>
     * <p>
     * システム単位で使用する符号ビットはコンポーネント設定ファイルに、
     * ファイル単位で使用する符号ビットはフォーマット定義ファイルのディレクティブに定義する。<br>
     * 「ファイル単位＞システム単位」の優先度順で使用する符号ビットが決定される。<br>
     * システム単位およびファイル単位、どちらにも符号ビットが定義されていない場合は、以下の既定値を使用する。
     * <pre>
     * - ascii互換のエンコーディングを使用する場合
     *     正） 0x03 負） 0x07
     * - ebcdic互換のエンコーディングを使用する場合
     *     正） 0x0C 負） 0x0D
     * </pre>
     * もしフィールド単位で符号ビットが設定された場合は、それが最優先で符号ビットとして使用される。
     * </p>
     *
     * @param directive ディレクティブ
     */
    protected void determinePackNibble(Map<String, Object> directive) {

        packNibble = (byte) ((getDefaultEncoding().encode("1").get() & 0xF0) >>> 4);

        // デフォルト値を設定する
        if (packNibble == 0x03) {
            packSignNibblePositive = 0x03;
            packSignNibbleNegative = 0x07;
        } else if (packNibble == (byte) 0x0F) {
            packSignNibblePositive = (byte) 0x0C;
            packSignNibbleNegative = (byte) 0x0D;
        } else {
            throw new SyntaxErrorException(String.format("unsupported encoding was specified. value=[%s].",
                                                         getDefaultEncoding()) // can not happen...
            );
        }

        String positivePackSignNibbleStr = FixedLengthDirective.getPositivePackSignNibble(directive);
        if (positivePackSignNibbleStr != null) {
            // ファイル単位（ディレクティブ）でパックの符号ビット（正）が明示的に設定されている場合は、それを使用する
            packSignNibblePositive = (byte) (Integer.parseInt(
                    positivePackSignNibbleStr, 16));
        } else if (convertorSetting.getDefaultPositiveZoneSignNibble() != null) {
            // システム全体でパックの符号ビット（正）が明示的に設定されている場合は、それを使用する
            packSignNibblePositive = convertorSetting
                    .getDefaultPositivePackSignNibble();
        }
        String negativePackSignNibbleStr = FixedLengthDirective.getNegativePackSignNibble(directive);
        if (negativePackSignNibbleStr != null) {
            // ファイル単位（ディレクティブ）でパックの符号ビット（負）が明示的に設定されている場合は、それを使用する
            packSignNibbleNegative = (byte) (Integer.parseInt(
                    negativePackSignNibbleStr, 16));
        } else if (convertorSetting.getDefaultNegativeZoneSignNibble() != null) {
            // システム全体でパックの符号ビット（負）が明示的に設定されている場合は、それを使用する
            packSignNibbleNegative = convertorSetting
                    .getDefaultNegativePackSignNibble();
        }
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
