package nablarch.core.dataformat.convertor.datatype;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.util.Builder;

/**
 * 符号付き数値のデータタイプ。
 * <p>
 * 入力時にはバイトデータを数値（BigDecimal）に変換し、
 * 出力時にはオブジェクトをバイトデータに変換して返却する。
 * <p>
 * {@link NumberStringDecimal}を継承し、符号付き数値を読み書きする機能を追加している。
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
 * 本データタイプは符号付き数値の入出力データを扱うことを前提として設計されている。<br/>
 * 入出力データとして符号なし数値のみを扱う場合は、{@link NumberStringDecimal}を使用すること。
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
 * 符号位置は#setFixedSignPosition(boolean)によって指定できる。<br/>
 * trueを設定した場合、符号位置は固定となる。<br/>
 * 符号位置が固定の場合、入力データの先頭に符号が存在すると想定してデータを読み込み、また、符号を先頭に付与した出力データを書き込む。
 * </p>
 * <p>
 * 正の符号の要否は#setFixedSignPosition(boolean)によって指定できる。<br/>
 * trueを設定した場合、正の符号は必須となる。<br/>
 * 正の符号が必須の場合、入力データに正の符号が含まれないと例外をスローし、また出力データの先頭に正の符号を付与する。
 * </p>
 * <p>
 * セッターを使用して設定可能なパラメータの一覧を以下に示す。これらのパラメータはディレクティブを使用して設定することを想定している。
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
 * <tr>
 * <td>符号位置の固定/非固定</td>
 * <td>boolean</td>
 * <td>任意</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>正の符号の要否</td>
 * <td>boolean</td>
 * <td>任意</td>
 * <td>false</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * 入力時にはトリム処理を、出力時にはパディング処理を行う。<br/>
 * パディング/トリム文字として、デフォルトでは"0"を使用するが、個別にパディング/トリム文字を使用することもできる。<br/>
 * （※パディング/トリム文字として指定できるのは1バイトの文字のみである。また、パディング/トリム文字として1～9の文字を使用することはできない）
 * </p>
 * <b>設定例</b>
 * <p>
 * 入力データを読み込む場合の設定例を以下に示す。</br>
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>入力データ</th>
 * <th>バイト長</th>
 * <th>小数点位置</th>
 * <th>符号位置の固定/非固定</th>
 * <th>正の符号の要否</th>
 * <th>パディング/トリム文字</th>
 * <th>入力データ変換後の値（BigDecimal）</th>
 * </tr>
 * <tr>
 * <td>0000012345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>+000012345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>-000012345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>-12345</td>
 * </tr>
 * <tr>
 * <td>0000+12345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>0000-12345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>-12345</td>
 * </tr>
 * <tr>
 * <td>(半角スペース4個)+12345</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>不要</td>
 * <td>半角スペース</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>-000012345</td>
 * <td>10</td>
 * <td>2</td>
 * <td>固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>-123.45</td>
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
 * <th>符号位置の固定/非固定</th>
 * <th>正の符号の要否</th>
 * <th>パディング/トリム文字</th>
 * <th>出力データ変換後の値（byte[]）</th>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>0000012345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>+000012345</td>
 * </tr>
 * <tr>
 * <td>-12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>-000012345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000+12345</td>
 * </tr>
 * <tr>
 * <td>-12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>不要</td>
 * <td>0</td>
 * <td>0000-12345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>String</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>必要</td>
 * <td>半角スペース</td>
 * <td>(半角スペース4個)+12345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>0</td>
 * <td>非固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>0000+12345</td>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>BigDecimal</td>
 * <td>10</td>
 * <td>2</td>
 * <td>非固定</td>
 * <td>必要</td>
 * <td>0</td>
 * <td>000+123.45</td>
 * </tr>
 * </table>
 * <br/>
 * 小数点の要否の設定例については、親クラス{@link NumberStringDecimal}のjavadocを参照すること。
 * </p>
 * @author Masato Inoue
 */
public class SignedNumberStringDecimal extends NumberStringDecimal {

    /** 正の符号 */
    private static final String PLUS_SIGN = "+";

    /** 負の符号 */
    private static final String MINUS_SIGN = "-";

    /** 数値のパターン */
    private static final String NUMBER_PATTERN = "[0-9]+(\\.[0-9]*[0-9])?";

    /** 符号位置が固定かどうか */
    private boolean isFixedSignPosition = true;

    /** 正の符号が必要かどうか */
    private boolean isRequiredPlusSign = false;

    /** 正の符号のバイトデータ */
    private byte[] plusSignBytes;
    
    /** 負の符号のバイトデータ */
    private byte[] minusSignBytes;
    
    /** 符号位置固定、正の符号不要の数値のパターン */
    private Pattern dataPatternFixedSign;
    
    /** 符号位置固定、正の符号必要の数値のパターン */
    private Pattern dataPatternFixedAndRequiredPlusSign;
    
    /** 符号位置非固定、正の符号不要の数値のパターン */
    private Pattern dataPatternNonFixedSign;
    
    /** 符号位置非固定、正の符号必要の数値のパターン */
    private Pattern dataPatternNonFixedAndRequiredPlusSign;

    
    /** {@inheritDoc}
     */
    @Override
    public DataType<BigDecimal, byte[]> initialize(Object... args) {
        
        super.initialize(args);
        
        plusSignBytes = convertToBytes(getPlusSign());
        minusSignBytes = convertToBytes(getMinusSign());
        
        dataPatternFixedSign = createPattern(getPlusOrMinusPattern(), "?", getPaddingStr(), "*");
        dataPatternFixedAndRequiredPlusSign = createPattern(getPlusOrMinusPattern(), getPaddingStr(), "*");
        dataPatternNonFixedSign = createPattern(getPaddingStr(), "*", getPlusOrMinusPattern(), "?");
        dataPatternNonFixedAndRequiredPlusSign = createPattern(getPaddingStr(), "*", getPlusOrMinusPattern());
        
        return this;
    }
    
    /**
     * パターンを生成する。
     * @param elements 要素
     * @return パターン
     */
    private Pattern createPattern(Object... elements) {
        return Pattern.compile(Builder.concat(elements) + NUMBER_PATTERN);
    }
    
    /**
     * 正負の符号を結合したパターンを返却する。
     * @return 正負の符号を結合したパターン
     */
    private String getPlusOrMinusPattern() {
        return "[" + getPlusSign() + getMinusSign() + "]";
    }

    /**
     * 入力データフォーマットの妥当性を検証する。
     * @param strData 入力データ
     */
    @Override
    protected void validateReadDataFormat(String strData) {
        if (isFixedSignPosition) {
            if (isRequiredPlusSign) {
                validateFormat(strData, dataPatternFixedAndRequiredPlusSign);
            } else {
                validateFormat(strData, dataPatternFixedSign);
            }
        } else {
            if (isRequiredPlusSign) {
                validateFormat(strData, dataPatternNonFixedAndRequiredPlusSign);
            } else {
                validateFormat(strData, dataPatternNonFixedSign);
            }
        }
    }

    /**
     * 入力データの妥当性を検証する。
     * @param strData 入力データ
     * @param pattern パターン
     */
    private void validateFormat(String strData, Pattern pattern) {
        if (!pattern.matcher(strData).matches() && !"".equals(strData)) {
            throw new InvalidDataFormatException(Builder.concat(
                    "invalid parameter format was specified. parameter format must be [", pattern, "]."
                  , " parameter=[", strData, "]."));
        }
    }
    
    /**
     * トリム処理を行う。
     * @param str トリム対象の文字列
     * @return トリム後の文字列
     */
    protected String trim(String str) {
        if (isFixedSignPosition) {
            // 符号位置が固定の場合のトリム処理（符号を削ってからトリムする）
            if (str.startsWith(getPlusSign())) {
                return super.trim(str.substring(getPlusSign().length()));
            } 
            if (str.startsWith(getMinusSign())) {
                return MINUS_SIGN + super.trim(str.substring(getPlusSign().length()));
            }
        }
        return super.trim(str);
    }
    

    /**
     * 出力時に書き込むデータの変換を行う。
     * <p/>
     * この実装では、出力データ（数値）をバイトデータに変換する。
     * @param data 書き込みを行うデータ
     * @return 変換後のバイトデータ
     */
    @Override
    public byte[] convertOnWrite(Object data) {

        BigDecimal bigDecimal = DecimalHelper.toBigDecimal(data);
        
        boolean isNegative = isNegative(bigDecimal);

        String formattedData = formatWriteData(bigDecimal);
        
        checkBytesSize(convertToBytes(formattedData));

        return padding(convertToBytes(formattedData, isNegative), isNegative);
    }

    /**
     * 文字列をエンコーディングに従いバイトデータに変換する。
     * <p/>
     * 負数の場合は符号を削除したバイトデータを返却する。
     * @param strData 文字列
     * @param isMinus 負数かどうか
     * @return 文字列を変換したバイトデータ
     */
    protected byte[] convertToBytes(String strData, boolean isMinus) {
        if (isMinus) {
            return super.convertToBytes(strData.substring(1));
        }
        return super.convertToBytes(strData);
    }
    
    
    /**
     * パディングを行う。
     * @param bytes 出力データのバイトデータ
     * @param isMinus 負数かどうか
     * @return パディング後のバイトデータ
     */
    private byte[] padding(byte[] bytes, boolean isMinus) {
        
        if (isFixedSignPosition) {
            byte[] paddedBytes;
            // 符号位置固定の場合、パディング後に符号を付与する
            if (isMinus) {
                // パディング後に負の符号を付与する
                paddedBytes = padding(bytes, getSize() - minusSignBytes.length);
                return margeBytes(minusSignBytes, paddedBytes);
            } 
            if (isRequiredPlusSign) {
                // パディング後に正の符号を付与する
                paddedBytes = padding(bytes, getSize() - plusSignBytes.length);
                return margeBytes(plusSignBytes, paddedBytes);
            } 
        } else {
            // 符号位置固定の場合、データの先頭に符号を付与してからパディングする
            if (isMinus) {
                // 出力データの先頭に負の符号を付与してからパディングを行う
                return padding(margeBytes(minusSignBytes, bytes), getSize());
            }
            if (isRequiredPlusSign) {
                // 正の符号が必須の場合、出力データの先頭に正の符号を付与してからパディングを行う
                return padding(margeBytes(plusSignBytes, bytes), getSize());
            }
        }
        // 符号が存在しない場合、通常のパディングを行う
        return padding(bytes, getSize());
    }
    

    /**
     * 符号とデータをマージする。
     * @param sign 符号
     * @param data データ
     * @return 符号とデータをマージしたバイトデータ
     */
    private byte[] margeBytes(byte[] sign, byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(sign.length + data.length);
        return byteBuffer.put(sign).put(data).array();
    }

    /**
     * 正の符号を取得する。
     * <p/>
     * 本メソッドをオーバーライドすることで正の符号を変更することが可能である。
     * @return 正の符号
     */
    protected String getPlusSign() {
        return PLUS_SIGN;
    }
    
    /**
     * 負の符号を取得する。
     * <p/>
     * 本メソッドをオーバーライドすることで負の符号を変更することが可能である。
     * たとえば、本メソッドを「▲」を返却するようにオーバーライドすれば、負の符号として▲が使用される。
     * @return 負の符号
     */
    protected String getMinusSign() {
        return MINUS_SIGN;
    }

    /**
     * 正の符号の要否を設定する。
     * @param isRequiredPlusSign 正の符号の要否（trueの場合、必要）
     * @return このオブジェクト自体
     */
    public SignedNumberStringDecimal setRequiredPlusSign(boolean isRequiredPlusSign) {
        this.isRequiredPlusSign = isRequiredPlusSign;
        return this;
    }
    
    /**
     * 符号位置の固定/非固定を設定する。
     * @param isFixedSignPosition 符号位置の固定/非固定（trueの場合、固定）
     * @return このオブジェクト自体
     */
    public SignedNumberStringDecimal setFixedSignPosition(boolean isFixedSignPosition) {
        this.isFixedSignPosition = isFixedSignPosition;
        return this;
    }
}
