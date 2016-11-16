package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * {@link SignedZonedDecimal}のテスト。
 *
 * @author TIS
 */
public class SignedZonedDecimalTest {

    private ZonedDecimal sut = new SignedZonedDecimal();
    private FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("sjis"));
    private final byte zoneNibbleASCII = 0x30; // (byte) (Charset.forName("sjis").encode("1").get() & 0xF0);

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
    public void testInitNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[SignedZonedDecimal].");

        sut.init(null);
    }

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[SignedZonedDecimal].");

        sut.init(null, null);
    }

    /**
     * 初期化時に空オブジェクト配列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeEmpty() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[SignedZonedDecimal].");

        sut.init(null, new Object[]{});
    }

    /**
     * 初期化時にバイト配列として文字列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeString() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter type was specified. 1st parameter type must be 'Integer' but was: 'java.lang.String'. parameter=[a]. convertor=[SignedZonedDecimal].");

        sut.init(null, "a");
    }

    /**
     * 初期化時にバイト配列としてnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNullByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[SignedZonedDecimal].");

        sut.init(null, null, "hoge");
    }

    /**
     * ASCII規格での符号つきゾーン10進のテスト
     * 正常系読込。正数。
     */
    @Test
    public void testReadPositiveNumberSignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] inputBytes = new byte[]{
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12345")));
    }

    /**
     * ASCII規格での符号つきゾーン10進のテスト
     * 正常系読込。負数。
     */
    @Test
    public void testReadNegativeNumberSignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] inputBytes = new byte[]{
                0x31, 0x32, 0x33, 0x34, 0x75
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("-12345")));
    }

    /**
     * ASCII規格での符号つきゾーン10進のテスト
     * 異常系読込。ゾーンビットが崩れている場合。
     */
    @Test
    public void testReadAbnormalSignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] inputBytes = new byte[]{
                0x31, 0x32, 0x33, 0x34, (byte)0xF5
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid zone bits was specified.");

        sut.convertOnRead(inputBytes);
    }

    /**
     * ASCII規格での符号つきゾーン10進のテスト
     * 正常系書き込み。正数。
     */
    @Test
    public void testWritePositiveNumberSignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] expected = new byte[]{
                0x31, 0x32, 0x33, 0x34, 0x35
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12345")), is(expected));
    }

    /**
     * ASCII規格での符号つきゾーン10進のテスト
     * 正常系書き込み。負数。
     */
    @Test
    public void testWriteNegativeNumberSignedZonedDigitType_ASCII() throws Exception {
        sut.init(field, 5, 0);
        setParameter(zoneNibbleASCII, 3, 7);

        byte[] expected = new byte[]{
                0x31, 0x32, 0x33, 0x34, 0x75
        };

        assertThat(sut.convertOnWrite(new BigDecimal("-12345")), is(expected));
    }

    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     * 正常系読込。正数。
     */
    @Test
    public void testReadPositiveNumberSignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("Cp1047"));
        sut.init(field, 5, 0);
        setParameter((byte) 0xF0, Integer.parseInt("C", 16), Integer.parseInt("D", 16));

        byte[] inputBytes = new byte[]{
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xC5
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("12345")));
    }

    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     * 正常系読込。負数。
     */
    @Test
    public void testReadNegativeNumberSignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("Cp1047"));
        sut.init(field, 5, 0);
        setParameter((byte) 0xF0, Integer.parseInt("C", 16), Integer.parseInt("D", 16));

        byte[] inputBytes = new byte[]{
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xD5
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("-12345")));
    }

    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     * 異常系読込。ゾーンビットがくずれている場合。
     */
    @Test
    public void testReadAbnormalSignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("Cp1047"));
        sut.init(field, 5, 0);
        setParameter((byte) 0xF0, Integer.parseInt("C", 16), Integer.parseInt("D", 16));

        byte[] inputBytes = new byte[]{
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid zone bits was specified.");

        sut.convertOnRead(inputBytes);
    }

    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     * 正常系書き込み。正数。
     */
    @Test
    public void testWritePositiveNumberSignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("Cp1047"));
        sut.init(field, 5, 0);
        setParameter((byte) 0xF0, Integer.parseInt("C", 16), Integer.parseInt("D", 16));

        byte[] expected = new byte[]{
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xC5
        };

        assertThat(sut.convertOnWrite(new BigDecimal("12345")), is(expected));
    }

    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     * 正常系書き込み。負数。
     */
    @Test
    public void testWriteNegativeNumberSignedZonedDigitType_EBCDIC() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("Cp1047"));
        sut.init(field, 5, 0);
        setParameter((byte) 0xF0, Integer.parseInt("C", 16), Integer.parseInt("D", 16));

        byte[] expected = new byte[]{
                (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xD5
        };

        assertThat(sut.convertOnWrite(new BigDecimal("-12345")), is(expected));
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
                0x31, 0x32, 0x33, 0x34, 0x75
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("-12345")));
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
                0x31, 0x32, 0x33, 0x34, 0x75
        };

        assertThat(sut.convertOnWrite(new BigDecimal("-12345")), is(expected));
    }

    /**
     * 出力時の最大値のテスト。
     * 正常系、正の整数18桁。
     */
    @Test
    public void testWriteMaxLong() throws Exception {
        sut.init(field, 18, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        byte[] expected = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x49
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
        setParameter(zoneNibbleASCII, 4, 7);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000].");

        sut.convertOnWrite("1000000000000000000");
    }

    /**
     * 出力時の最小値のテスト。
     * 正常系、負の整数18桁。
     */
    @Test
    public void testWriteMinLong() throws Exception {
        sut.init(field, 18, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        byte[] expected = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x79
        };

        assertThat(sut.convertOnWrite("-999999999999999999"), is(expected));
    }

    /**
     * 出力時の最小値のテスト。
     * 異常系、負の整数19桁。
     */
    @Test
    public void testWriteMinLongMinus1() throws Exception {
        sut.init(field, 18, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[-1000000000000000000].");

        sut.convertOnWrite("-1000000000000000000");
    }

    /**
     * 出力時の最大桁のテスト。
     * 正常系、負の小数18桁(小数点は除く)。
     */
    @Test
    public void testWriteMinLongDecimal() throws Exception {
        sut.init(field, 18, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        byte[] expected = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x79
        };

        assertThat(sut.convertOnWrite("-9999999999.99999999"), is(expected));
    }

    /**
     * 出力時の最大桁のテスト。
     * 異常系、負の小数19桁(小数点を除く)。
     */
    @Test
    public void testWriteMinLongDecimalMinus1() throws Exception {
        sut.init(field, 18, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[-1000000000000000000], original parameter=[-1000000000000.000000].");

        sut.convertOnWrite("-1000000000000.000000");
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされていたらトリム。されていなければ、そのまま。
     */
    @Test
    public void testRemovePadding() {
        sut.init(field, 10, 0);
        setParameter(zoneNibbleASCII, 4, 7);

        String data = "001234";
        String expectedString = "1234";
        BigDecimal expected = new BigDecimal("1234");

        assertThat(sut.removePadding(data), is(expected));
        assertThat(sut.removePadding(expectedString), is(expected));
        assertThat(sut.removePadding(expected), is(expected));
    }
}
