package nablarch.core.dataformat.convertor.datatype;

import static nablarch.core.util.Builder.concat;

import java.math.BigDecimal;
import java.util.Arrays;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * ゾーン10進数のデータタイプ。
 * <p>
 * 入力時にはバイト配列（ゾーン10進数）をBigDecimal型に変換し、
 * 出力時にはオブジェクト（BigDecimalや文字列型の数値など）をバイト配列（ゾーン10進数）に変換して返却する。
 * </p>
 * <p>
 * 出力対象のデータ（#convertOnWrite(Object)の引数）として使用できるオブジェクトの種類を以下に示す。nullは許容しない。
 * <ul>
 * <li>BigDecimal</li>
 * <li>BigDecimalに変換可能なオブジェクト（数値や文字列など）</li>
 * </ul>
 * </p>
 * <p>
 * 出力対象のデータに小数点が含まれる場合、小数点を取り除いた値に変換する。<br/>
 * また、出力対象のデータがBigDecimalの場合はスケーリングした値に変換する。<br/>
 * 以下に出力例を示す。</br>
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>出力対象データの型</th>
 * <th>出力対象データの値</th>
 * <th>出力対象データの変換後の値</th>
 * </tr>
 * <tr>
 * <td>Integer</td>
 * <td>123.45</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>String</td>
 * <td>"123.45000"</td>
 * <td>12345000</td>
 * </tr>
 * <tr>
 * <td>BigDecimal</td>
 * <td>123.45</td>
 * <td>12345</td>
 * </tr>
 * <tr>
 * <td>BigDecimal（scale=5)</td>
 * <td>123.45</td>
 * <td>12345000</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * 本クラスが出力対象のデータの桁数として許容するのは18桁までの数値である。<br/>
 * この桁数とは、出力対象のデータから小数点を取り除き、BigDecimalの場合はスケールに従い変換した後の桁数のことを指す。<br/>
 * (※処理を高速化するために内部的に出力対象データをlong型に変換し扱っているので、このような桁数の制約を設けている）
 * </p>
 * @author Iwauo Tajima
 */
public class ZonedDecimal extends ByteStreamDataSupport<BigDecimal> {

    /** 符号付き数値であればtrue */
    private boolean signed = false;
    
    /** 小数点の位置 */
    private int scale = 0;
    
    /** 符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合） */
    private Byte zoneSignNibblePositive = null;

    /** 符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合） */
    private Byte zoneSignNibbleNegative = null;

    /** ゾーンNibble */
    private Byte zoneNibble = null; 
    
    /** デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）*/
    private Byte defaultZoneSignNibblePositive = null; 

    /** デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）*/
    private Byte defaultZoneSignNibbleNegative = null; 

    /** {@inheritDoc} */
    public ZonedDecimal initialize(Object... args) {
        if (args == null) {
            throw new SyntaxErrorException("initialize parameter was null. parameter must be specified. convertor=[" + getClass().getSimpleName() + "].");
        }
        if (args.length == 0) {
            throw new SyntaxErrorException("parameter was not specified. parameter must be specified. convertor=[" + getClass().getSimpleName() + "].");
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(concat(
                    "1st parameter was null. parameter=", Arrays.toString(args), ". convertor=[" + getClass().getSimpleName() + "]."));
        }
        if (!(args[0] instanceof Integer)) {
            throw new SyntaxErrorException(concat(
                    "invalid parameter type was specified. 1st parameter type must be 'Integer' but was: '"
                    , args[0].getClass().getName(), "'. parameter=", Arrays.toString(args), ". convertor=[" + getClass().getSimpleName() + "]."));
        }
        
        setSize((Integer) args[0]);

        if (args.length >= 2 && args[1] instanceof Integer) {
            this.scale = (Integer) args[1];
        }
        return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public BigDecimal convertOnRead(byte[] buff) {
        if (convertEmptyToNull && buff.length == 0) {
            return null;
        }

        long num = 0;
        long scale = 1;
        boolean negative = false;
        for (int i = 1; i <= getSize(); i++) {
            byte digit = buff[getSize() - i];
            byte upperNibble = (byte) (digit & 0xF0);
            byte lowerNibble = (byte) (digit & 0x0F);

            if (i == 1 && signed) {
                if (upperNibble == getZoneSignNibbleNegative()) {
                    negative = true;
                } else if (upperNibble == getZoneSignNibblePositive()) {
                    negative = false;
                } else {
                    throw new InvalidDataFormatException("invalid zone bits was specified.");
                }
            } else {
                if (upperNibble != zoneNibble) {
                    throw new InvalidDataFormatException("invalid zone bits was specified.");
                }
            }
            num += lowerNibble * scale;
            scale *= 10;
        }
        if (negative) {
            num *= (-1);
        }
        return BigDecimal.valueOf(num, this.scale);
    }

    /** {@inheritDoc}
     * <p/>
     * 出力対象のデータが以下の場合、{@link InvalidDataFormatException}をスローする。
     * <ul>
     * <li>null</li>
     * <li>BigDecimalに変換できないオブジェクト</li>
     * <li>データから小数点を取り除き、またBigDecimalの場合はスケーリングした後の桁数が19桁以上の場合</li>
     * </ul>
     */
    @Override
    public byte[] convertOnWrite(Object data) {
        long digits = DecimalHelper.toUnscaledLongValue(data);
        
        boolean negative = (digits < 0);
        if (negative) {
            digits *= -1;
        }
        byte[] bytes = new byte[getSize()];
        long scale = 1;
        for (int i = 1; i <= getSize(); i++) {
            byte digit      = (byte) ((digits % (scale * 10)) / scale);
            byte zoneNibble = (!signed || i != 1) ? this.zoneNibble
                            : negative            ? getZoneSignNibbleNegative()
                            : getZoneSignNibblePositive();
                            
            bytes[getSize() - i] = (byte) ((digit & 0x0F) | zoneNibble);
            scale *= 10;
        }
        return bytes;
    }

    // ------------------------------------------------------ accessors
    /**
     * 符号付きの整数として処理するかどうかを設定する。
     * @param signed 符号付きの整数として扱う場合はtrue
     * @return このオブジェクト自体
     */
    public ZonedDecimal setSigned(boolean signed) {
        this.signed = signed;
        return this;
    }
    
    
    /**
     * 符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）を返却する。
     * @return ゾーン10進数値の上位4ビット（負数の場合）
     */
    public Byte getZoneSignNibbleNegative() {
        return (zoneSignNibbleNegative == null)
              ? defaultZoneSignNibbleNegative
              : zoneSignNibbleNegative;
    }

    /**
     * 符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）を返却する。
     * @return ゾーン10進数値の上位4ビット（正数の場合）
     */
    private Byte getZoneSignNibblePositive() {
        return (zoneSignNibblePositive == null)
              ? defaultZoneSignNibblePositive
              : zoneSignNibblePositive;
    }

    /**
     * 符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）を返却する。
     * @param nibble 符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）
     * @return このオブジェクト自体
     */
    public ZonedDecimal setZoneSignNibblePositive(Integer nibble) {
        zoneSignNibblePositive = (byte) (nibble << 4);
        return this;
    }
    
    /**
     * 符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）を返却する。
     * @param nibble 符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）
     * @return このオブジェクト自体
     */
    public ZonedDecimal setZoneSignNibbleNegative(Integer nibble) {
        zoneSignNibbleNegative = (byte) (nibble << 4);
        return this;
    }

    /**
     * ゾーンNibbleを設定する。
     * @param zoneNibble ゾーンNibble
     * @return このオブジェクト自体
     */
    public ZonedDecimal setZoneNibble(Byte zoneNibble) {
        this.zoneNibble = zoneNibble;
        return this;
    }
    
    /**
     * デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）を設定する。
     * @param defaultZoneSignNibbleNegative デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（負数の場合）
     * @return このオブジェクト自体
     */
    public ZonedDecimal setDefaultZoneSignNibbleNegative(
            Byte defaultZoneSignNibbleNegative) {
        this.defaultZoneSignNibbleNegative = defaultZoneSignNibbleNegative;
        return this;
    }
    
    /**
     * デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）を設定する。
     * @param defaultZoneSignNibblePositive デフォルトの符号付きゾーン10進数値の末尾桁の上位4ビット（正数の場合）
     * @return このオブジェクト自体
     */
    public ZonedDecimal setDefaultZoneSignNibblePositive(
            Byte defaultZoneSignNibblePositive) {
        this.defaultZoneSignNibblePositive = defaultZoneSignNibblePositive;
        return this;
    }
    
    
}
