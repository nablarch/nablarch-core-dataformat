package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * ゾーン10進整数データタイプのテスト
 * 
 * @author Iwauo Tajima
 */
public class ZonedDecimalTest {

    private ZonedDecimal sut = new ZonedDecimal();

    private FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("sjis")).setName("test");
    private final byte zoneNibbleASCII = 0x30; // (byte) (Charset.forName("sjis").encode("1").get() & 0xF0)

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private void setParameter(byte zoneNibble, int signNibblePositive, int signNibbleNegative) {
        sut.setZoneNibble(zoneNibble);
        sut.setZoneSignNibblePositive(signNibblePositive);
        sut.setZoneSignNibbleNegative(signNibbleNegative);
    }

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[ZonedDecimal].");

        sut.initialize(null);
    }

    /**
     * 初期化時に空オブジェクト配列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeEmpty() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[ZonedDecimal].");

        sut.initialize(new Object[]{});
    }

    /**
     * 初期化時にバイト配列として文字列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeString() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter type was specified. 1st parameter type must be 'Integer' but was: 'java.lang.String'. parameter=[a]. convertor=[ZonedDecimal].");

        sut.initialize("a");
    }

    /**
     * 初期化時にバイト配列としてnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNullByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[ZonedDecimal].");

        sut.initialize(null, "hoge");
    }
    
    /**
     * ASCII規格での符号なしゾーン10進のテスト。
     * 正常系読込。
     */
    @Test
    public void testReadNormalUnsignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        sut.setZoneNibble(zoneNibbleASCII);
        
        byte[] inputBytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12345")));
    }
    /**
     * ASCII規格での符号なしゾーン10進のテスト。
     * 正常系読書き込み。
     */
    @Test
    public void testWriteNormalUnsignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] expected = new byte[]{
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12345")), is(expected));
    }

    /**
     * ASCII規格での符号なしゾーン10進のテスト。
     * 異常系読込。ゾーンビットがくずれている場合。
     */
    @Test
    public void testReadAbnormalUnsignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] errorBytes = new byte[] {
                0x31, 0x32, 0x33, 0x34, (byte)0x75
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid zone bits was specified.");

        sut.convertOnRead(errorBytes);
    }

    /**
     * EBCDIC規格での符号なしゾーン10進のテスト。
     * 正常系読込。
     */
    @Test
    public void testReadNormalUnsignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        setParameter((byte)0xF0, 0xC0, 0xD0);

        byte[] inputBytes = new byte[] {
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12345")));
    }

    /**
     * EBCDIC規格での符号なしゾーン10進のテスト。
     * 正常系書き込み。
     */
    @Test
    public void testWriteNormalUnsignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        setParameter((byte)0xF0, 0xC0, 0xD0);

        byte[] expected = new byte[] {
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12345")), is(expected));
    }

    /**
     * EBCDIC規格での符号なしゾーン10進のテスト。
     * 異常系読込。ゾーンビットがくずれている場合
     */
    @Test
    public void testReadAbnormalUnsignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        setParameter((byte)0xF0, 0xC0, 0xD0);

        byte[] errorBytes = new byte[] {
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0x35
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid zone bits was specified.");

        sut.convertOnRead(errorBytes);
    }

    /**
     * トリム処理のテスト。
     */
    @Test
    public void testTrim() throws Exception {
        sut.init(field, 10, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] inputBytes = new byte[] {
                0x30, 0x30, 0x30, 0x30, 0x30,
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12345")));
    }

    /**
     * パディング処理のテスト。
     */
    @Test
    public void testPadding() throws Exception {
        sut.init(field, 10, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] expected = new byte[] {
                0x30, 0x30, 0x30, 0x30, 0x30,
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12345")), is(expected));
    }

    /**
     * 仮想小数点読込のテスト
     */
    @Test
    public void testReadVirtualDecimalPoint() throws Exception {
        sut.init(field, 5, 3);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] inputBytes = new byte[] {
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12.345")));
    }

    /**
     * 仮想小数点書き込みのテスト
     */
    @Test
    public void testWriteVirtualDecimalPoint() throws Exception {
        sut.init(field, 5, 3);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] expected = new byte[] {
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12.345")), is(expected));
    }

    /**
     * 空文字を入力するテスト。
     */
    @Test
    public void testReadEmpty() {
        sut.init(field, 0, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        assertThat(sut.convertOnRead("".getBytes()), is(new BigDecimal("0")));
    }

    /**
     * null と 空文字を出力するテスト。
     * デフォルト値(0)を出力する。
     */
    @Test
    public void testWriteValidSingularValue() {
        sut.init(field, 5, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        assertThat(sut.convertOnWrite(null), is("00000".getBytes()));
        assertThat(sut.convertOnWrite(""), is("00000".getBytes()));
    }

    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * 文字列のケース。
     */
    @Test
    public void testWriteStringNotBigDecimal() {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[abc].");

        sut.convertOnWrite("abc");
    }

    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * オブジェクトのケース。
     */
    @Test
    public void testWriteObjectNotBigDecimal() {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter must be able to convert to BigDecimal.");

        sut.convertOnWrite(new Object());
    }

    /**
     * 出力時の最大値のテスト。
     * 正常系、整数18桁。
     */
    @Test
    public void testWriteMaxLong() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] expected = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39
        };

        assertThat(sut.convertOnWrite("999999999999999999"), is(expected));
    }

    /**
     * 出力時の最大値のテスト。
     * 異常系、整数19桁。
     */
    @Test
    public void testWriteMaxLongPlus1() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000].");

        sut.convertOnWrite("1000000000000000000");
    }

    /**
     * 出力時の最大桁のテスト。
     * 正常系、小数18桁(小数点は除く)。
     */
    @Test
    public void testWriteMaxLongDecimal() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] expected = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39
        };

        assertThat(sut.convertOnWrite("9999999999.99999999"), is(expected));
    }

    /**
     * 出力時の最大桁のテスト。
     * 異常系、小数19桁(小数点を除く)。
     */
    @Test
    public void testWriteMaxLongDecimalPlus1() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000].");

        sut.convertOnWrite("1000000000000.000000");
    }

    /**
     * 出力時の最大桁のテスト。
     * 正常系、スケールを指定した小数18桁(小数点は除く)。
     */
    @Test
    public void testWriteMaxLongDecimalWithScale() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        byte[] expected = new byte[] {
                0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
        };

        assertThat(sut.convertOnWrite(new BigDecimal("1000000000000").setScale(5)), is(expected));
    }

    /**
     * 出力時の最大桁のテスト。
     * 異常系、小数19桁(小数点を除く)。
     */
    @Test
    public void testWriteMaxLongDecimalPlus1WithScale() throws Exception {
        sut.init(field, 18, 0);
        sut.setZoneNibble(zoneNibbleASCII);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000].");

        sut.convertOnWrite(new BigDecimal("1000000000000").setScale(6));
    }
}
