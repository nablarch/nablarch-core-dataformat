package nablarch.core.dataformat.convertor.datatype;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link NumberStringDecimal}の固定長テスト。
 *
 * @author TIS
 */
public class NumberStringDecimalTest {

    private NumberStringDecimal sut = new NumberStringDecimal();

    /** テスト共通で使用するフィールド定義 */
    private static final FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /** 文字列をバイトに変換する */
    private byte[] toBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("ms932");
    }

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("initialize parameter was null. parameter must be specified. convertor=[NumberStringDecimal].");

        sut.initialize((Object[]) null);
    }

    /**
     * 初期化時にnullが渡されたときのテスト。
     */
    @Test
    public void testInitialize1stParameterNull() {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[NumberStringDecimal].");

        sut.initialize(null, "hoge");
    }

    /**
     * 初期化時にnullが渡されたときのテスト。
     * initをオーバーライドしたため、init内でinitializeが呼ばれていることを確かめる。
     */
    @Test
    public void testInitNull() {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[NumberStringDecimal].");

        sut.init(null, null, "hoge");
    }

    /**
     * 初期化時にスケールに文字列が渡されたときのテスト。
     */
    @Test
    public void testInitStringScale() {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid parameter type was specified. 2nd parameter type must be Integer. parameter=[10, abc]. convertor=[NumberStringDecimal].");

        sut.init(field, 10, "abc");
    }

    /**
     * 初期化時にスケールに数字（数値ではない）が渡されたときのテスト。
     */
    @Test
    public void testInitNumberStringScale() {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid parameter type was specified. 2nd parameter type must be Integer. parameter=[10, 0]. convertor=[NumberStringDecimal].");

        sut.init(field, 10, "0");
    }

    /**
     * 初期化時にスケールにnull, 空文字が渡されたときのテスト。
     */
    @Test
    public void testInitNullScale() throws Exception {
        sut.init(field, 10, null);
        assertThat(sut.convertOnRead(toBytes("12300")), is(new BigDecimal("12300")));

        sut.init(field, 10, "");
        assertThat(sut.convertOnRead(toBytes("1230")), is(new BigDecimal("1230")));
    }

    /**
     * 読込テスト。スケールなし。
     * 空文字列を0として受け取るケース。
     */
    @Test
    public void testReadNonScaleEmpty() throws Exception {
        sut.init(field, 10);
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead(toBytes("")), is(new BigDecimal("0")));
    }

    /**
     * 読込テスト。スケールなし。
     * 空文字列を{@code null}として受け取るケース。
     */
    @Test
    public void testReadNonScaleEmptyToNull() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead(toBytes("")), is(nullValue()));
    }

    /**
     * 読込テスト。スケールなし。
     * 不正な数値のケース。
     */
    @Test
    public void testReadNonScaleInvalidNumber() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[1.23.].");

        sut.convertOnRead(toBytes("1.23."));
    }

    /**
     * 読込テスト。スケールなし。
     * 正常な数値のケース。
     */
    @Test
    public void testReadNonScaleValidNumber() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead(toBytes("0")), is(new BigDecimal("0")));
        assertThat(sut.convertOnRead(toBytes("1")), is(new BigDecimal("1")));
        assertThat(sut.convertOnRead(toBytes("12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("1.23")), is(new BigDecimal("1.23")));
    }

    /**
     * 読込テスト。スケールあり（スケール:0）。
     * 空文字列を0として受け取るケース。
     */
    @Test
    public void testReadScale0Empty() throws Exception {
        sut.init(field, 10, 0);
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead(toBytes("")), is(new BigDecimal("0")));
    }

    /**
     * 読込テスト。スケールあり（スケール:0）。
     * 正常な数値のケース。
     */
    @Test
    public void testReadScale0ValidNumber() throws Exception {
        sut.init(field, 10, 0);

        assertThat(sut.convertOnRead(toBytes("0")), is(new BigDecimal("0")));
        assertThat(sut.convertOnRead(toBytes("1")), is(new BigDecimal("1")));
        assertThat(sut.convertOnRead(toBytes("12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("123.4")), is(new BigDecimal("123.4")));
    }

    /**
     * 読込テスト。スケールあり（スケール:3）。
     * 空文字列を0として受け取るケース。
     */
    @Test
    public void testReadScale3Empty() throws Exception {
        sut.init(field, 10, 3);
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead(toBytes("")).toPlainString(), is("0.000"));
    }

    /**
     * 読込テスト。スケールあり（スケール:3）。
     * スケールが設定されていても空文字列を{@code null}として受け取るケース。
     */
    @Test
    public void testReadWithScaleEmptyToNull() throws Exception {
        sut.init(field, 10, 3);

        assertThat(sut.convertOnRead(toBytes("")), is(nullValue()));
    }

    /**
     * 読込テスト。スケールあり（スケール:3）。
     * 正常な数値のケース。
     */
    @Test
    public void testReadScale3ValidNumber() throws Exception {
        sut.init(field, 10, 3);

        assertThat(sut.convertOnRead(toBytes("0")).toPlainString(), is("0.000"));
        assertThat(sut.convertOnRead(toBytes("1")).toPlainString(), is("0.001"));
        assertThat(sut.convertOnRead(toBytes("12340")).toPlainString(), is("12.340"));
        assertThat(sut.convertOnRead(toBytes("123.4")).toPlainString(), is("123.4"));
    }

    /**
     * 読込テスト。スケールあり（スケール:-3）。
     * 空文字のケース。
     */
    @Test
    public void testReadScaleMinus3Empty() throws Exception {
        sut.init(field, 10, -3);
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead(toBytes("")).toPlainString(), is("0"));
    }

    /**
     * トリム文字のみを読み込むテスト。
     */
    @Test
    public void testReadTrimString() throws Exception {
        sut.init(field, 10, 0);

        assertThat(sut.convertOnRead(toBytes("0000000000")), is(BigDecimal.ZERO));
    }

    /**
     * 読込テスト。スケールあり（スケール:-3）。
     * 正常な数値のケース。
     */
    @Test
    public void testReadScaleMinus3ValidNumber() throws Exception {
        sut.init(field, 10, -3);

        assertThat(sut.convertOnRead(toBytes("0")).toPlainString(), is("0"));
        assertThat(sut.convertOnRead(toBytes("1")).toPlainString(), is("1000"));
        assertThat(sut.convertOnRead(toBytes("12340")).toPlainString(), is("12340000"));
        assertThat(sut.convertOnRead(toBytes("123.4")).toPlainString(), is("123.4"));
    }

    /**
     * 読込時、デフォルトで"0"をトリムするテスト。
     */
    @Test
    public void testReadTrimDefault() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead(toBytes("0000012345")), is(new BigDecimal("12345")));
    }

    /**
     * 読込時、トリム（パディング）文字指定するテスト。
     */
    @Test
    public void testReadTrimCustom() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue(" ");
        sut.init(otherField, 10);

        assertThat(sut.convertOnRead(toBytes("     12345")), is(new BigDecimal("12345")));
    }

    /**
     * 読込時、トリム(パディング)文字が0の場合でも正しく読み込むテスト。
     */
    @Test
    public void testReadTrimZero() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue("0");
        sut.init(otherField, 10);

        assertThat(sut.convertOnRead(toBytes("0")), is(new BigDecimal("0")));
        assertThat(sut.convertOnRead(toBytes("0000000000")), is(new BigDecimal("0")));
    }

    /**
     * 読込時、文字の場合例外を発生するテスト。
     */
    @Test
    public void testReadString() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[000abc].");

        sut.convertOnRead(toBytes("000abc"));
    }

    /**
     * 読込時、+符号がある場合例外を発生するテスト。
     */
    @Test
    public void testReadPlusSign() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[000+123].");

        sut.convertOnRead(toBytes("000+123"));
    }

    /**
     * 読込時、-符号がある場合例外を発生するテスト。
     */
    @Test
    public void testReadMinusSign() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[000-321].");

        sut.convertOnRead(toBytes("000-321"));
    }

    /**
     * 書き込み時、正しくパディングされるテスト。
     */
    @Test
    public void testWritePadding() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite("12345"), is("0000012345".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込み時、+符号が付いていても正しくパディングされるテスト。
     */
    @Test
    public void testWritePaddingPlusSign() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite("+12345"), is("0000012345".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込み時、-符号が付いていると例外が発生するテスト。
     */
    @Test
    public void testWritePaddingMinusSign() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter was specified. parameter must not be minus. parameter=[-12345].");

        sut.convertOnWrite("-12345");
    }

    /**
     * 書き込み時、負の数（BigDecimal)の場合、例外が発生するテスト。
     */
    @Test
    public void testWritePaddingMinusValue() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter was specified. parameter must not be minus. parameter=[-123.45].");

        sut.convertOnWrite(BigDecimal.valueOf((long)-12345, 2));
    }

    /**
     * 指定サイズより大きい値を書き込もうとした場合例外が発生すること
     * @throws Exception
     */
    @Test
    public void testWriteLargerSize() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter was specified. " 
                + "too large data. field size = '10' data size = '11'. data: 12345543210");
        
        sut.convertOnWrite(new BigDecimal("12345543210"));
    }

    /**
     * 書き込み時、パディング文字指定するテスト。
     */
    @Test
    public void testWriteTrimCustom() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue(" ");
        sut.init(otherField, 10);

        assertThat(sut.convertOnWrite("12345"), is("     12345".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込みテスト。
     * null, 空文字のケース。
     */
    @Test
    public void testWriteNullOrEmpty() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite(null), is("0000000000".getBytes(Charset.forName("ms932"))));
        assertThat(sut.convertOnWrite(""), is("0000000000".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込みテスト。
     * 文字列のケース。
     */
    @Test
    public void testWriteString() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[abc].");

        sut.convertOnWrite("abc");
    }

    /**
     * 書き込みテスト。
     * 不正な数値のケース。
     */
    @Test
    public void testWriteInvalidNumber() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[1.23.].");

        sut.convertOnWrite("1.23.");
    }

    /**
     * 書き込みテスト。スケールなし。
     * 正常な数値のケース。
     */
    @Test
    public void testWriteNonScaleValidNumber() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite("0"), is("0000000000".getBytes(Charset.forName("ms932"))));
        assertThat(sut.convertOnWrite("1"), is("0000000001".getBytes(Charset.forName("ms932"))));
        assertThat(sut.convertOnWrite("12340"), is("0000012340".getBytes(Charset.forName("ms932"))));
        assertThat(sut.convertOnWrite("1234567890"), is("1234567890".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込みテスト。スケールあり。
     * 正常な数値のケース。
     */
    @Test
    public void testWriteScaleValidNumber() throws Exception {
        sut.init(field, 10, 2);

        assertThat(sut.convertOnWrite("12.34"), is("0000012.34".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 書き込みテスト。スケールあり。
     * 出力対象のスケールが設定値より大きいケース。
     */
    @Test
    public void testWriteInvalidScaleNumber() throws Exception {
        sut.init(field, 10);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid scale was specified. specify scale must be greater than the parameter scale. specified scale=[0], parameter scale=[1], parameter=[1.2].");

        sut.convertOnWrite("1.2");
    }

    /**
     * 書き込みテスト。スケールあり。
     * 正常な数値(BigDecimal)のケース。
     */
    @Test
    public void testWriteScaleValidBigDecimal() throws Exception {
        sut.init(field, 10, 1);
        assertThat(sut.convertOnWrite(new BigDecimal("12.3")), is("00000012.3".getBytes(Charset.forName("ms932"))));

        sut.init(field, 10, 2);
        assertThat(sut.convertOnWrite(BigDecimal.valueOf((long)123, 1)), is("0000012.30".getBytes(Charset.forName("ms932"))));

        sut.init(field, 10, -3);
        assertThat(sut.convertOnWrite(new BigDecimal("123000")), is("0000000123".getBytes(Charset.forName("ms932"))));

        sut.init(field, 10, -4);
        assertThat(sut.convertOnWrite(new BigDecimal("123000")), is("00000012.3".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 小数点が不要な書き込みのテスト。
     * 正常系。
     */
    @Test
    public void testNoDecimalPoint() throws Exception {
        // スケール : 0
        sut.init(field, 10, 0);
        sut.setRequiredDecimalPoint(false);
        assertThat(sut.convertOnWrite(BigDecimal.valueOf((long)123, 0)), is("0000000123".getBytes(Charset.forName("ms932"))));

        // スケール : 1
        sut.init(field, 10, 1);
        sut.setRequiredDecimalPoint(false);
        assertThat(sut.convertOnWrite(BigDecimal.valueOf((long)123, 0)), is("0000001230".getBytes(Charset.forName("ms932"))));

        // スケール : -3
        sut.init(field, 10, -3);
        sut.setRequiredDecimalPoint(false);
        assertThat(sut.convertOnWrite(BigDecimal.valueOf((long)123000, 0)), is("0000000123".getBytes(Charset.forName("ms932"))));
    }

    /**
     * 小数点が不要な書き込みのテスト。
     * 異常系。スケールにより小数となるケース。
     */
    @Test
    public void testNoDecimalPointInvalidValue() throws Exception {
        sut.init(field, 10, -4);
        sut.setRequiredDecimalPoint(false);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid scale was specified. scaled data should not have a decimal point. scale=[-4], scaled data=[12.3], write data=[123000].");
        sut.convertOnWrite(BigDecimal.valueOf((long)123000, 0));
    }

    /**
     * 不正なパディング文字が設定された場合のテスト。
     * 全角数字のケース。
     */
    @Test
    public void testInvalidPaddingStringZenkaku() throws Exception {
        final FieldDefinition paddingField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("０");

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid parameter was specified. the length of padding bytes must be '1', but was '2'. padding string=[０].");

        sut.init(paddingField, 10);
    }

    /**
     * 不正なパディング文字が設定された場合のテスト。
     * 数字下限のケース。
     */
    @Test
    public void testReadTrimNumberBottom() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue("1");

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid padding character was specified. padding character must not be [1-9] pattern."
                + " padding character=[1], convertor=[NumberStringDecimal].");

        sut.init(otherField, 10);
    }

    /**
     * 不正なパディング文字が設定された場合のテスト。
     * 数字上限のケース。
     */
    @Test
    public void testReadTrimNumberTop() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue("9");

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid padding character was specified. padding character must not be [1-9] pattern."
                + " padding character=[9], convertor=[NumberStringDecimal].");

        sut.init(otherField, 10);
    }

    /**
     * 不正なパディング文字が設定された場合のテスト。
     * 数字中間のケース。
     */
    @Test
    public void testReadTrimNumberMiddle() throws Exception {
        final FieldDefinition otherField = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        otherField.setPaddingValue("5");

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid padding character was specified. padding character must not be [1-9] pattern."
                + " padding character=[5], convertor=[NumberStringDecimal].");

        sut.init(otherField, 10);
    }

    /**
     * {@link NumberStringDecimal#trim(String)}のテスト。
     *
     * 空文字列をtrimした場合、空文字列が返却されること。
     */
    @Test
    public void testTrimEmptyString() {
        assertThat(sut.trim(""), is(""));
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされていたらトリム。されていなければ、そのまま。
     */
    @Test
    public void testRemovePadding() {
        sut.init(field, 10);

        String data = "001234";
        String expectedString = "1234";
        BigDecimal expected = new BigDecimal("1234");

        assertThat(sut.removePadding(data), is(expected));
        assertThat(sut.removePadding(expectedString), is(expected));
        assertThat(sut.removePadding(expected), is(expected));
    }
}
