package nablarch.core.dataformat.convertor.datatype;

import static nablarch.core.util.Builder.concat;

import java.math.BigDecimal;
import java.util.Arrays;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;


/**
 * パック10進数のデータタイプ。
 * <p>
 * 入力時にはバイト配列（パック10進数）をBigDecimal型に変換し、
 * 出力時にはオブジェクト（BigDecimalや文字列型の数値など）をバイト配列（パック10進数）に変換して返却する。
 * </p>
 * <p>
 * 出力対象のデータ（#convertOnWrite(Object)の引数）として使用できるオブジェクトの種類を以下に示す。nullは許容しない。
 * <ul>
 * <li>BigDecimal</li>
 * <li>BigDecimalに変換可能なオブジェクト（数値や文字列など）</li>
 * </ul>
 * </p>
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
public class PackedDecimal extends ByteStreamDataSupport<BigDecimal> {

    /** 符号付き数値であればtrue */
    private boolean signed = false;
    
    /** 小数点の位置 */
    private int scale = 0;
    
    /** パックNibble */
    private Byte packNibble = null; 
    
    /** デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）*/
    private Byte defaultPackSignNibblePositive = null; 

    /** デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）*/
    private Byte defaultPackSignNibbleNegative = null;

    /** 符号付きパック10進数値の末尾桁の下位4ビット（負数の場合） */
    private Byte packSignNibbleNegative = null;

    /** 符号付きパック10進数値の末尾桁の下位4ビット（正数の場合） */
    private Byte packSignNibblePositive = null; 

    /** {@inheritDoc} */
    public PackedDecimal initialize(Object... args) {
        if (args.length == 0) {
            throw new SyntaxErrorException("parameter was not specified. parameter must be specified. convertor=[PackedDecimal].");
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(concat(
                    "1st parameter was null. parameter=", Arrays.toString(args), ". convertor=[PackedDecimal]."));
        }
        if (!(args[0] instanceof Integer)) {
            throw new SyntaxErrorException(concat(
                            "invalid parameter type was specified. "
                          , "1st parameter type must be 'Integer' but was: '", args[0].getClass().getName(), "'. "
                          , "parameter=", Arrays.toString(args), ". convertor=[PackedDecimal]."));
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

        long num = 0;
        long scale = 1;
        boolean negative = false;
        for (int digits = 1; digits <= getSize(); digits++) {
            byte digit = buff[getSize() - digits];
            byte upperNibble = (byte) ((digit & 0xF0) >>> 4);
            byte lowerNibble = (byte) (digit & 0x0F);
            
            if (digits == 1) {
                if (signed) {
                    if (lowerNibble == getPackSignNibbleNegative()) {
                        negative = true;
                    } else if (lowerNibble == getPackSignNibblePositive())  {
                        negative = false;
                    } else {
                        throw new InvalidDataFormatException("invalid pack bits was specified.");
                    }
                } else {
                    if (lowerNibble != packNibble) {
                        throw new InvalidDataFormatException("invalid pack bits was specified.");
                    }
                }
                num += upperNibble;
                scale *= 10;
            } else {
                num += lowerNibble * scale;
                scale *= 10;
                num += upperNibble * scale;
                scale *= 10;
            }
        }
        if (negative) {
            num *= -1;
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
        
        byte[] bytes = new byte[getSize()];
        long scale = 1;
        boolean negative = (digits < 0);
        if (negative) {
            digits *= -1;
        }
        byte signNibble = !signed  ? this.packNibble
                        : negative ? getPackSignNibbleNegative()
                        :            getPackSignNibblePositive();
                        
        for (int i = 1; i <= getSize(); i++) {
            byte lowerNibble;
            byte upperNibble;
            if (i == 1) {
                lowerNibble = (byte) signNibble;
                upperNibble = (byte) (digits % 10);
                scale *= 10;
            } else {
                lowerNibble = (byte) (digits % (scale * 10) / scale);
                scale *= 10;
                upperNibble = (byte) (digits % (scale * 10) / scale);
                scale *= 10;
            }
            bytes[getSize() - i] = (byte) ((upperNibble << 4) | lowerNibble);
        }
        
        return bytes;
    }

    
    /**
     * 符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）を返却する。
     * @param nibble 符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）
     * @return このオブジェクト自体
     */
    public PackedDecimal setPackSignNibblePositive(Integer nibble) {
        packSignNibblePositive = nibble.byteValue();
        return this;
    }
    
    /**
     * 符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）を返却する。
     * @param nibble 符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）
     * @return このオブジェクト自体
     */
    public PackedDecimal setPackSignNibbleNegative(Integer nibble) {
        packSignNibbleNegative = nibble.byteValue();
        return this;
    }
    
    /**
     * 符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）を返却する。
     * @return パック10進数値の下位4ビット（負数の場合）
     */
    public Byte getPackSignNibbleNegative() {
        return (packSignNibbleNegative == null)
              ? defaultPackSignNibbleNegative
              : packSignNibbleNegative;
    }

    /**
     * 符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）を返却する。
     * @return パック10進数値の下位4ビット（正数の場合）
     */
    public Byte getPackSignNibblePositive() {
        return (packSignNibblePositive == null)
              ? defaultPackSignNibblePositive
              : packSignNibblePositive;
    }
    
    /**
     * 符号付きの整数として処理するかどうかを設定する。
     * @param signed 符号付きの整数として扱う場合はtrue
     * @return このオブジェクト自体
     */
    public PackedDecimal setSigned(boolean signed) {
        this.signed = signed;
        return this;
    }

    /**
     * パックNibbleを設定する。
     * @param packNibble パックNibble
     * @return このオブジェクト自体
     */
    public PackedDecimal setPackNibble(Byte packNibble) {
        this.packNibble = packNibble;
        return this;
    }

    /**
     * デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）を設定する。
     * @param defaultPackSignNibbleNegative デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（負数の場合）
     * @return このオブジェクト自体
     */
    public PackedDecimal setDefaultPackSignNibbleNegative(Byte defaultPackSignNibbleNegative) {
        this.defaultPackSignNibbleNegative = defaultPackSignNibbleNegative;
        return this;
    }

    /**
     * デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）を設定する。
     * @param defaultPackSignNibblePositive デフォルトの符号付きパック10進数値の末尾桁の下位4ビット（正数の場合）
     * @return このオブジェクト自体
     */
    public PackedDecimal setDefaultPackSignNibblePositive(Byte defaultPackSignNibblePositive) {
        this.defaultPackSignNibblePositive = defaultPackSignNibblePositive;
        return this;
    }

}
