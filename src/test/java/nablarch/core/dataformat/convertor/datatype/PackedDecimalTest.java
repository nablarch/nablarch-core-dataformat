package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * {@link PackedDecimal}のテスト。
 *   
 * @author TIS
 */
public class PackedDecimalTest {

    private PackedDecimal sut = new PackedDecimal();
    private FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("sjis"));
    final byte packNibble = 0x03; // ((Charset.forName("sjis").encode("1").get() & 0xF0) >>> 4)

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[PackedDecimal].");

        sut.initialize(null);
    }

    /**
     * 初期化時に（バイト長として）nullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNullByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, null]. convertor=[PackedDecimal].");

        sut.initialize(null, null);
    }

    /**
     * 初期化時に空配列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeEmptyByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[PackedDecimal].");

        sut.initialize(new Object[0]);
    }

    /**
     * 初期化時に（バイト長として）文字列をわたすと例外がスローされること。
     */
    @Test
    public void testInitializeStringByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter type was specified. 1st parameter type must be 'Integer' but was: 'java.lang.String'. parameter=[5]. convertor=[PackedDecimal].");

        sut.initialize("5");
    }

    /**
     * 初期化時に（スケールとして）文字列をわたすとデフォルトとして処理されること。
     */
    @Test
    public void testInitializeStringScale() {
        sut.init(field, 10, "5");
        sut.setPackNibble(packNibble);

        byte[] inputBytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("87654321")));
    }

    /**
     * null, 空文字を読み込む場合のテスト。
     */
    @Test
    public void testReadNullOrEmpty() {
        sut.init(field, 0, 0);

        assertThat(sut.convertOnRead(null), is(BigDecimal.ZERO));
        assertThat(sut.convertOnRead("".getBytes()), is(BigDecimal.ZERO));
    }

    /**
     * ASCII規格での符号なしパック10進の正常系読込テスト。
     */
    @Test
    public void testReadNormal() throws Exception {
        sut.init(field, 5, 0);
        sut.setPackNibble(packNibble);

        byte[] inputBytes = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("87654321")));
    }

    /**
     * ASCII規格での符号なしパック10進の正常系書き込みテスト。
     */
    @Test
    public void testWriteNormal() throws Exception {
        sut.init(field, 5, 0);
        sut.setPackNibble(packNibble);

        byte[] expected = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(sut.convertOnWrite("87654321"), is(expected));
    }

    /**
     * ASCII規格での符号なしパック10進の異常系読込テスト。
     * 符号ビットが不正。
     */
    @Test
    public void testReadAbnormal() throws Exception {
        sut.init(field, 5, 0);
        sut.setPackNibble(packNibble);

        byte[] inputBytes = new byte[] {
                (byte) 0x98, 0x76, 0x54, 0x32, 0x1C
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid pack bits was specified.");

        sut.convertOnRead(inputBytes);
    }

    /**
     * EBCDIC規格での符号なしパック10進の正常系読込テスト。
     */
    @Test
    public void testReadNormalEBCDIC() throws Exception {
        final FieldDefinition field  = new FieldDefinition();
        field.setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        sut.setPackNibble((byte)0x0F);

        byte[] inputBytes = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x1F
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("87654321")));
    }

    /**
     * EBCDIC規格での符号なしパック10進の正常系書き込みテスト。
     */
    @Test
    public void testWriteNormalEBCDIC() throws Exception {
        final FieldDefinition field  = new FieldDefinition();
        field.setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        sut.setPackNibble((byte)0x0F);

        byte[] expected = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x1F
        };

        assertThat(sut.convertOnWrite("87654321"), is(expected));
    }

    /**
     * EBCDIC規格での符号なしパック10進の異常系読込テスト。
     * 符号ビットが不正。
     */
    @Test
    public void testReadAbnormalEBCDIC() throws Exception {
        final FieldDefinition field  = new FieldDefinition();
        field.setEncoding(Charset.forName("IBM1047"));
        sut.init(field, 5, 0);
        sut.setPackNibble((byte)0x0F);

        byte[] inputBytes = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x1C
        };

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid pack bits was specified.");

        sut.convertOnRead(inputBytes);
    }

    /**
     * 読込時トリムのテスト。
     */
    @Test
    public void testTrim() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        byte[] inputBytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(sut.convertOnRead(inputBytes), is(new BigDecimal("87654321")));
    }

    /**
     * 書き込み時パディングのテスト。
     */
    @Test
    public void testPadding() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        byte[] expected = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(sut.convertOnWrite("87654321"), is(expected));
    }

    /**
     * null と 空文字を出力するテスト。
     * デフォルト値(0)を出力する。
     */
    @Test
    public void testWriteValidSingularValue() {
        sut.init(field, 5);
        sut.setPackNibble(packNibble);

        byte[] bytes = { 0x00, 0x00, 0x00, 0x00, 0x03 };

        assertThat(sut.convertOnWrite(null), is(bytes));
        assertThat(sut.convertOnWrite(""), is(bytes));
    }

    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * 文字列のケース。
     */
    @Test
    public void testWriteString() {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[abc].");

        sut.convertOnWrite("abc");
    }

    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * オブジェクトのケース。
     */
    @Test
    public void testWriteObject() {
        exception.expect(allOf(
                instanceOf(InvalidDataFormatException.class),
                hasProperty("message", startsWith("invalid parameter was specified. parameter must be able to convert to BigDecimal."))
        ));

        sut.convertOnWrite(new Object());
    }

    /**
     * 最大値の書き込みテスト。
     * 正の整数。
     */
    @Test
    public void testWriteMaxValue() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        byte[] maxBytes = new byte[] {
                0x09, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x93
        };

        assertThat(sut.convertOnWrite("999999999999999999"), is(maxBytes));
    }

    /**
     * 最大値+1の書き込みテスト。
     * 正の整数。
     */
    @Test
    public void testWriteMaxValuePlus1() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000].");

        sut.convertOnWrite("1000000000000000000");
    }

    /**
     * 小数点を含む最大桁数の書き込みテスト。
     * 正の小数。
     */
    @Test
    public void testWriteMaxLengthValue() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        byte[] maxBytes = new byte[] {
                0x09, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x93
        };

        assertThat(sut.convertOnWrite("9999999999.99999999"), is(maxBytes));
    }

    /**
     * 小数点を含む最大桁数+1の書き込みテスト。
     * 正の小数。
     */
    @Test
    public void testWriteMaxLengthValuePlus1() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000].");

        sut.convertOnWrite("1000000000000.000000");
    }

    /**
     * スケールありの最大桁数の書き込みテスト。
     * 正の整数。
     */
    @Test
    public void testWriteMaxValueWithScale() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        byte[] maxBytes = new byte[] {
                0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03
        };

        assertThat(sut.convertOnWrite(new BigDecimal("1000000000000").setScale(5)), is(maxBytes));
    }

    /**
     * スケールありの最大桁数+1の書き込みテスト。
     * 正の整数。
     */
    @Test
    public void testWriteMaxValuePlus1WithScale() throws Exception {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000].");

        sut.convertOnWrite(new BigDecimal("1000000000000").setScale(6));
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされていたらトリム。されていなければ、そのまま。
     */
    @Test
    public void testRemovePadding() {
        sut.init(field, 10, 0);
        sut.setPackNibble(packNibble);

        String data = "001234";
        String expectedString = "1234";
        BigDecimal expected = new BigDecimal("1234");

        assertThat(sut.removePadding(data), is(expected));
        assertThat(sut.removePadding(expectedString), is(expected));
        assertThat(sut.removePadding(expected), is(expected));
    }
}
