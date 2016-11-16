package nablarch.core.dataformat.convertor.datatype;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;

/**
 * 符号なし数値のデータタイプ。
 * <p>
 * 入力時にはバイトデータを数値（BigDecimal）に変換し、
 * 出力時にはオブジェクトをバイトデータに変換して返却する。
 * </p>
 * <p>
 * 本データタイプは符号なし数値の入出力データを扱うことを前提として設計されている。<br/>
 * 入出力データとして符号付き数値を扱う場合は、{@link SignedNumberStringDecimal}を使用すること。
 * </p>
 * <p>
 * 本データタイプは符号なし数値の入出力データを扱うことを前提として設計されている。<br/>
 * よって符号（'+'または'-'）が存在する入力データや、負数の出力データを扱うことはできない。
 * </p>
 * <p>
 * 入力時にはトリム処理を、出力時にはパディング処理を行う。<br/>
 * パディング/トリム文字として、デフォルトでは"0"を使用するが、個別にパディング/トリム文字を使用することもできる。<br/>
 * （※パディング/トリム文字として指定できるのは1バイトの文字のみである。また、パディング/トリム文字として1～9の文字を使用することはできない）
 * </p>
 * <b>データタイプの引数として設定可能なパラメータ</b>
 * <p>
 * データタイプの第1引数にはバイト長を指定する。必須項目である。
 * </p>
 * <p>
 * データタイプの第2引数には小数点位置を指定する。ここで指定された小数点位置に従い、データの読み書きを行う。<br/>
 * ただし、入力データに小数点が含まれている場合（例：123.45）は、ここで指定した小数点位置は無視され、入力データ内の小数点をもとにデータの読み込みが行われる。
 * </p>
 * <p>
 * データタイプの引数の一覧を以下に示す。</br>
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>引数</th>
 * <th>パラメータ名</th>
 * <th>パラメータの型</th>
 * <th>必須/任意</th>
 * <th>デフォルト値</th>
 * </tr>
 * <tr>
 * <td>第1引数</td>
 * <td>バイト長</td>
 * <td>Integer</td>
 * <td>必須</td>
 * <th>-</th>
 * </tr>
 * <tr>
 * <td>第2引数</td>
 * <td>小数点位置</td>
 * <td>Integer</td>
 * <td>任意</td>
 * <td>0</td>
 * </tr>
 * </table>
 * </p>
 * <b>セッターを使用して設定可能なパラメータ</b>
 * <p>
 * 小数点の要否は#setRequiredDecimalPoint(boolean)によって指定できる。<br/>
 * trueを設定した場合、出力データに小数点が付与される。
 * </p>
 * <p>
 * セッタを使用して設定可能なパラメータの一覧を以下に示す。これらのパラメータはディレクティブを使用して設定することを想定している。
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>パラメータ名</th>
 * <th>パラメータの型</th>
 * <th>必須/任意</th>
 * <th>デフォルト値</th>
 * </tr>
 * <tr>
 * <td>小数点の要否</td>
 * <td>boolean</td>
 * <td>任意</td>
 * <td>true</td>
 * </tr>
 * </table>
 * </p>
 * <b>設定例</b>
 * <p>
 * 入力データを読み込む場合の設定例を以下に示す。</br>
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>入力データ</th>
 * <th>バイト長</th>
 * <th>小数点位置</th>
 * <th>小数点の要否</th>
 * <th>パディング/トリム文字</th>
 * <th>入力データ変換後の値（BigDecimal）</th>
 * </tr>
 * <tr>
 * <td>0000012345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>0000123.45</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>123.45</td>
 * </tr>
 * <tr>
 * <td>0000012345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>(半角スペース5個)12345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>半角スペース</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>0000012345</td>
 * <td>10</td>
 * <td>2</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>123.45</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * 出力データを書き込む場合の設定例を以下に示す。</br>
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>出力データ</th>
 * <th>出力データの型</th>
 * <th>バイト長</th>
 * <th>小数点位置</th>
 * <th>小数点の要否</th>
 * <th>パディング/トリム文字</th>
 * <th>出力データ変換後の値（byte[]）</th>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000012345</td>
 * </tr>
 * <tr>
 * <td>123.45</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000012345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>半角スペース</td>
 * <td>(半角スペース5個)12345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>0</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000012345</td>
 * </tr>
 * <tr>
 * <td>123.45</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>2</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000123.45</td>
 * </tr>
 * <tr>
 * <td>123.45</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>3</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>000123.450</td>
 * </tr>
 * <tr>
 * <td>123.45</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>3</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>0000123450</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>3</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>012345.000</td>
 * </tr>
 * <tr>
 * <td>123000</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>-3</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>0000000123</td>
 * </tr>
 * <tr>
 * <td>123000</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>-4</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>00000012.3</td>
 * </tr>
 * </table>
 * </p>
 * @author Masato Inoue
 */
public class NumberStringDecimal extends ByteStreamDataSupport<BigDecimal> {

    /** データのパターン */
    private Pattern dataPattern;

    /** デフォルトのパディング/トリム文字 */
    private static final char DEFAULT_PADDING_VALUE = '0';

    /** パディング/トリム文字のサイズ */
    protected static final int PADDING_CHAR_LENGTH = 1;

    /** パディング/トリム文字として許容しない文字のパターン */
    private static final Pattern NON_ALLOWED_PADDING_PATTERN = Pattern.compile("[1-9]");
    
    /** 小数点位置（第2引数） */
    private int scale = 0;

    /** 小数点要否 */
    private boolean isRequiredDecimalPoint = true;

    /** パディング/トリム文字のバイト配列 */
    private byte[] paddingBytes;

    
    /** {@inheritDoc} */
    @Override
    public DataType<BigDecimal, byte[]> init(FieldDefinition field, Object... args) {
        DataType<BigDecimal, byte[]> dataType = super.init(field, args);

        String paddingStr = getPaddingStr();
        paddingBytes = getPaddingBytes(paddingStr);
        
        if (NON_ALLOWED_PADDING_PATTERN.matcher(paddingStr).find()) {
            throw new SyntaxErrorException(
                    Builder.concat(
                            "invalid padding character was specified. padding character must not be [1-9] pattern."
                          , " padding character=[", paddingStr, "], convertor=[", getClass().getSimpleName(), "]."));
        }

        
        dataPattern = Pattern.compile(Builder.concat(getPaddingStr(), "*[0-9]+(\\.[0-9]*[0-9])?"));
        
        return dataType;
    }
    
    /**
     * パディング/トリム文字のバイト配列を返却する。
     * @param paddingStr パディング/トリム文字
     * @return パディング/トリム文字のバイト配列
     */
    protected byte[] getPaddingBytes(String paddingStr) {
        byte[] bytes = null;
        bytes = convertToBytes(paddingStr);
        if (bytes.length != PADDING_CHAR_LENGTH) {
            throw new SyntaxErrorException(
                    Builder.concat(
                            "invalid parameter was specified. the length of padding bytes must be '"
                          , PADDING_CHAR_LENGTH, "', but was '", bytes.length
                          , "'. padding string=[", getPaddingStr(), "]."));
        }
        return bytes;
    }
    
    /** {@inheritDoc} */
    @Override
    public DataType<BigDecimal, byte[]> initialize(Object... args) {
        if (args == null) {
            throw new SyntaxErrorException(Builder.concat(
                    "initialize parameter was null. parameter must be specified. convertor=[",
                    getClass().getSimpleName(), "]."));
        }
        // 第1引数はバイト長（必須項目）
        if (args.length == 0) {
            throw new SyntaxErrorException(Builder.concat(
                    "parameter was not specified. parameter must be specified. convertor=["
                  , getClass().getSimpleName(), "]."));
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(Builder.concat(
                    "1st parameter was null. parameter=", Arrays.toString(args), ". convertor=[", getClass().getSimpleName(), "].")
                );
        }
        if (!(args[0] instanceof Integer)) {
            throw new SyntaxErrorException(Builder.concat(
                    "invalid parameter type was specified. 1st parameter must be Integer. "
                  , "parameter=", Arrays.toString(args), ". convertor=[", getClass().getSimpleName(), "].")
            );
        }

        setSize((Integer) args[0]);
        
        // 第2引数は小数点位置（任意項目）
        if (args.length >= 2 && args[1] != null) {
            if (StringUtil.hasValue(args[1].toString())) {
                if (!(args[1] instanceof Integer)) {
                    throw new SyntaxErrorException(Builder.concat(
                                    "invalid parameter type was specified. 2nd parameter type must be Integer. parameter=", Arrays.toString(args)
                                  , ". convertor=[", getClass().getSimpleName(), "]."));
                }
                this.scale = (Integer) args[1];
            }
        }

        return this;
    }
    
    /** 
     * {@inheritDoc}
     * <p>
     * この実装では、入力時に、入力データをBigDecimalに変換して返却する。
     * </p>
     * <p>
     * 変換の際に指定された文字でトリムを行う。デフォルトのトリム文字として'0'を使用する。
     * </p>
     * <p>
     * 入力データの読み込みは、{@link #setRequiredDecimalPoint(boolean)}で指定された小数点位置にしたがって行う。<br/>
     * ただし、入力データに小数点が含まれている場合（例：123.45）は、入力データ内の小数点にしたがってデータの読み込みが行われる。
     * </p>
     */
    public BigDecimal convertOnRead(byte[] data) {
        String strData;
        try {
            strData = new String(data, getField().getEncoding().name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can not happen.
        }
        return convertOnRead(strData);
    }

    
    /** 
     * 文字列に変換した入力データに対してトリム処理を行い、BigDecimalに変換する。
     * <p>
     * 入力データに符号（'+'または'-'）が存在する場合、例外をスローする。
     * </p>
     * @param strData 入力データ
     * @return 数値
     */
    protected BigDecimal convertOnRead(String strData) {
        validateReadDataFormat(strData);
        String trimmedStr = trim(strData);
        BigDecimal bigDecimal = toBigDecimal(trimmedStr);
        return bigDecimal;
    }

    /**
     * 入力データが妥当であることをチェックする。
     * <p/>
     * 数値型で無い場合は、例外をスローする。
     * @param data 入力データまたは出力データ
     */
    protected void validateReadDataFormat(String data) {
        if (!dataPattern.matcher(data).matches() && !"".equals(data)) {
            throw new InvalidDataFormatException(Builder.concat(
                    "invalid parameter format was specified. parameter format must be [", dataPattern, "]."
                   , " parameter=[", data, "]."));
        }
    }
    
    /**
     * トリム処理を行う。
     * <p/>
     * もし、パディング/トリム文字が'0'かつ、トリム対象のデータが0のみで構成される数値（0や000000）の場合、0を返却する。
     * @param str トリム対象のデータ
     * @return トリム後の文字列
     */
    protected String trim(String str) {
        if (str.length() == 0) {
            return str;
        }
        char padChar = getPaddingStr().charAt(0);
        int chopPos = 0;
        while ((chopPos < str.length()) && (str.charAt(chopPos) == padChar)) {
            chopPos++;
        }
        String trimmedStr = str.substring(chopPos);
        
        // パディング/トリム文字が'0'かつ、トリム対象のデータが0のみで構成される数値（0や000000）の場合、0を返却する
        if (padChar == '0' && trimmedStr.length() == 0) {
            return String.valueOf('0');
        } else {
            return trimmedStr;
        }
    }

    /**
     * トリム後の文字列をBigDecimalに変換する。
     * @param trimmedStr トリム後の文字列
     * @return 変換後のBigDecimal
     */
    protected BigDecimal toBigDecimal(String trimmedStr) {
        if (trimmedStr.contains(".")) {
            // 入力データが小数点を含む場合は、小数点位置を無視して読み込む
            return DecimalHelper.toBigDecimal(trimmedStr);
        } else {
            // 入力データが小数点を含まない場合は、入力データがの小数点位置をもとに固定小数点として処理する
            return DecimalHelper.toBigDecimal(trimmedStr, scale);
        }
    }
    
    /**
     * 出力時にフィールドデータの変換を行う。
     * <p/>
     * もし書き込みを行うデータが負数の場合、例外をスローする。
     * @param data 書き込みを行うデータ
     * @return 変換後のバイトデータ
     */
    public byte[] convertOnWrite(Object data) {
        
        BigDecimal bigDecimal = DecimalHelper.toBigDecimal(data);

        validateWriteDataFormat(bigDecimal);
        
        String formattedData = formatWriteData(bigDecimal);
        
        byte[] bytesData = convertToBytes(formattedData);

        checkBytesSize(bytesData);
        
        return padding(bytesData, getSize());
    }

    /**
     * 書き込みを行うデータの形式をチェックする。
     * <p>
     * データが負の符号を持つ場合、例外をスローする。
     * </p>
     * <p>
     * 符号を許容するサブクラスでは、本メソッドをオーバーライドし、無効化すること。
     * </p>
     * @param bigDecimal 書き込みを行うデータ
     */
    protected void validateWriteDataFormat(BigDecimal bigDecimal) {
        if (isNegative(bigDecimal)) {
            // データが負数の場合、例外をスローする
            throw new InvalidDataFormatException(Builder.concat(
                            "invalid parameter was specified. parameter must not be minus. parameter=[", bigDecimal, "]."));
        }
    }

    /**
     * 書き込みを行うデータをフォーマットする。
     * <p>
     * 小数点が必要な場合は小数点を付与した文字列を返却し、<br/>
     * 不要な場合は小数点を削除した文字列を返却する。
     * @param bigDecimal BigDecimal変換後の書き込みデータ
     * @return フォーマット後のデータ
     */
    protected String formatWriteData(BigDecimal bigDecimal) {

        // 小数点位置が出力データのスケールより小さい場合、例外をスローする
        if (getScale() >= 0 && getScale() < bigDecimal.scale()) {
            throw new InvalidDataFormatException(Builder.concat(
                    "invalid scale was specified. specify scale must be greater than the parameter scale."
                  , " specified scale=[", getScale(), "], parameter scale=[", bigDecimal.scale(), "], parameter=[", bigDecimal.toPlainString(), "]."));
        }
                
        if (isRequiredDecimalPoint) {
            // 小数点を削除せず整数データを返却する
            if (scale >= 0) {
                return getScaledOutputFormat().format(bigDecimal);
            } else {
                BigDecimal divide = bigDecimal.divide(BigDecimal.TEN.pow(scale * -1));
                DecimalFormat scaledOutputFormat = new DecimalFormat("0");
                scaledOutputFormat.setMinimumFractionDigits(divide.scale());
                return scaledOutputFormat.format(divide);
            }
        } else {
            // 小数点を削除した整数データを返却する
            if (scale >= 0) {
                return getNonScaledOutputFormat().format(bigDecimal.movePointRight(scale));
            } else {
                BigDecimal divide = bigDecimal.divide(BigDecimal.TEN.pow(scale * -1));
                if (divide.scale() > 0) {
                    throw new InvalidDataFormatException(Builder.concat(
                            "invalid scale was specified. scaled data should not have a decimal point."
                          , " scale=[", scale, "], scaled data=[", divide.toPlainString(), "], write data=[", bigDecimal.toPlainString(), "]."));
                }
                return getNonScaledOutputFormat().format(divide);
            }
        }
    }
    
    /**
     * 整数（小数点がない値）を出力する際のDecimalFormatを取得する。
     * @return 整数（小数点がない値）を出力する際のDecimalFormat
     */
    private NumberFormat getNonScaledOutputFormat() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
        format.applyPattern("0");
        return format;
    }

    /**
     * 小数点位置が設定されたフォーマットを生成する。
     * @return 小数点位置が設定されたフォーマット
     */
    private NumberFormat getScaledOutputFormat() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
        format.applyPattern("0");
        format.setMinimumFractionDigits(scale);
        return format;
    }
    
    /**
     * BigDecimalが負数かどうか。
     * @param bigDecimal BigDecimal
     * @return 負数かどうか
     */
    protected boolean isNegative(BigDecimal bigDecimal) {
        return bigDecimal.compareTo(BigDecimal.ZERO) == -1;
    }
    
    /**
     * 文字列をエンコーディングに従いバイトデータに変換する。
     * @param strData 文字列
     * @return 文字列を変換したバイトデータ
     */
    protected byte[] convertToBytes(String strData) {
        try {
            return strData.getBytes(getField().getEncoding().name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can not happen.
        }
    }
    
    /**
     * 出力データのサイズをチェックする。
     * <p/>
     * 出力データのサイズがバイト長を超える場合、例外をスローする。
     * @param bytes 出力データ
     */
    protected void checkBytesSize(byte[] bytes) {
        if (bytes.length > getSize()) {
            try {
                throw new InvalidDataFormatException(Builder.concat(
                        "invalid parameter was specified. too large data. ", 
                        "field size = '", getSize(), "' data size = '", bytes.length, "'.", " data: ",
                        new String(bytes, getField().getEncoding().name()))
                );
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // can not happen.
            }
        }
    }

    /**
     * パディング処理を行う。
     * @param bytes パディング前の値
     * @param size パディングを行うサイズ
     * @return パディング後の値
     */
    protected byte[] padding(byte[] bytes, int size) {
        if (bytes.length >= size) {
            return bytes;
        }
        ByteBuffer buff = ByteBuffer.wrap(new byte[size]);
        int padSize = (size - bytes.length);
        for (int i = 0; i < padSize; i++) {
            buff.put(paddingBytes);
        }
        buff.put(bytes).array();
        return buff.array();
    }
    
    /**
     * パディング/トリム文字を返却する。
     * パディング/トリム文字が指定されていない場合、デフォルトで半角スペースを使用する。
     * @return パディング/トリム文字
     */
    protected String getPaddingStr() {
        Object padding = getField().getPaddingValue();
        return (padding == null) ? String.valueOf(DEFAULT_PADDING_VALUE)
                                 : padding.toString();
    }
    
    /**
     * 小数点の要否を設定する。
     * @param requiredDecimalPoint 小数点の要否（trueの場合、必要）
     * @return このオブジェクト自体
     */
    public NumberStringDecimal setRequiredDecimalPoint(boolean requiredDecimalPoint) {
        this.isRequiredDecimalPoint = requiredDecimalPoint;
        return this;
    }
    
    /**
     * 小数点の位置を取得する。
     * @return 小数点の位置
     */
    public int getScale() {
        return scale;
    }
}
