package nablarch.core.dataformat.convertor.datatype;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link SignedNumberStringDecimal}の固定長テスト。
 *
 * @author TIS
 */
public class SignedNumberStringDecimalTest {

    private SignedNumberStringDecimal sut = new SignedNumberStringDecimal();
    private final FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[SignedNumberStringDecimal].");

        sut.initialize(null);
    }

    /**
     * 初期化時にnullが渡されたときのテスト。
     */
    @Test
    public void testInitialize1stParameterNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[SignedNumberStringDecimal].");

        sut.initialize(null, "hoge");
    }

    /**
     * 初期化時のバイト長が不正。
     * 空の配列。
     */
    @Test
    public void testInitializeEmpty() throws Exception {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[SignedNumberStringDecimal].");

        sut.initialize(new Object[]{});
    }

    /**
     * 初期化時のバイト長が不正。
     * 文字列。
     */
    @Test
    public void testInitializeString() throws Exception {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter type was specified. 1st parameter must be Integer. parameter=[abc]. convertor=[SignedNumberStringDecimal].");

        sut.initialize("abc");
    }

    /**
     * 空文字列を0として読み込む場合のテスト。
     */
    @Test
    public void testReadEmpty() {
        sut.init(field, 10, "");
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead(""), is(BigDecimal.ZERO));
    }

    /**
     * 空文字列を{@code null}として読み込む場合のテスト。
     */
    @Test
    public void testReadEmptyToNull() {
        sut.init(field, 10, "");

        assertThat(sut.convertOnRead(""), is(nullValue()));
    }

    /**
     * null, 空文字を書き込む場合のテスト。
     */
    @Test
    public void testWriteNullOrEmpty() {
        sut.init(field, 10, "");

        assertThat(sut.convertOnWrite(null), is("0000000000".getBytes()));
        assertThat(sut.convertOnWrite(""), is("0000000000".getBytes()));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系読み込みテスト。
     * トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNormalNotRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnRead(toBytes("0000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("+000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("-000012340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の異常系読み込みテスト。
     * 符号が不正。
     */
    @Test
    public void testReadAbnormalNotRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [[+-]?0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[■00012340].");

        sut.convertOnRead(toBytes("■00012340"));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の異常系読み込みテスト。
     * 符号の位置が不正。
     */
    @Test
    public void testReadAbnormalNotRequiredSignIllegalSignPosition() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [[+-]?0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000+12340].");

        sut.convertOnRead(toBytes("0000+12340"));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の異常系読み込みテスト。
     * 数値部分に符号が含まれる。
     */
    @Test
    public void testReadAbnormalNotRequiredSignIllegalSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [[+-]?0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[+000+12340].");

        sut.convertOnRead(toBytes("+000+12340"));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系読み込みテスト。
     * パディング（トリム）文字を変更しても正常にトリムされることを確認。
     */
    @Test
    public void testReadNormalNotRequiredSignWithOtherTrimString() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        field.setPaddingValue(" ");
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnRead(toBytes("     12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("+    12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("-    12340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系読み込みテスト。
     * クラス拡張することで、符号文字を変更できることを確認。
     */
    @Test
    public void testReadNormalNotRequiredSignExtendDataType() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, "");

        assertThat(sut.convertOnRead(toBytes("0000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("■00012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("▲00012340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置固定かつ符号必須の場合の正常系読み込みテスト。
     * トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNormalFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "", true, true);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnRead(toBytes("+000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("-000012340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置固定かつ符号必須の場合の異常系読み込みテスト。
     * 符号が指定されていないケース。
     */
    @Test
    public void testReadAbnormalFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "", true, true);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(true);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [[+-]0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000012340].");

        sut.convertOnRead(toBytes("0000012340"));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の正常系読み込みテスト。
     * トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNormalNonFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, "", false, false);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnRead(toBytes("0000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("0000+12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("0000-12340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の異常系読み込みテスト。
     * 符号の右にトリム文字があるケース。
     */
    @Test
    public void testReadAbnormalNonFixSignPositionNotRequiredSign() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        field.setPaddingValue(" ");
        sut.init(field, 10, "", false, false);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [ *[+-]?[0-9]+(\\.[0-9]*[0-9])?]. parameter=[+    12340].");

        sut.convertOnRead(toBytes("+    12340"));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の正常系読み込みテスト。
     * クラス拡張することで、符号文字を変更できることを確認。
     * 現状、先頭の符号以外には対応していない。
     */
    @Ignore
    @Test
    public void testReadNormalNonFixSignPositionNotRequiredSignExtendDataType() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, "");
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnRead(toBytes("0000012340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("000■12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("000▲12340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置非固定かつ符号必須の場合の正常系読み込みテスト。
     * トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNormalNonFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "", false, true);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnRead(toBytes("0000+12340")), is(new BigDecimal("12340")));
        assertThat(sut.convertOnRead(toBytes("0000-12340")), is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置非固定かつ符号必須の場合の異常系読み込みテスト。
     * 符号が指定されていない。
     */
    @Test
    public void testReadAbnormalNonFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "", false, false);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(true);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter format was specified. parameter format must be [0*[+-][0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000012340].");

        sut.convertOnRead(toBytes("0000012340"));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系書き込みテスト。
     * 出力データは符号あり整数（文字列）。
     */
    @Test
    public void testWriteNumberStringFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("+1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("-000001234")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系書き込みテスト。
     * 出力データは符号あり小数（文字列）。
     */
    @Test
    public void testWriteDecimalFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, 2);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("+1234.56"), is(toBytes("0001234.56")));
        assertThat(sut.convertOnWrite("-1234.56"), is(toBytes("-001234.56")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系書き込みテスト。
     * 出力データはBigDecimal（文字列）。
     */
    @Test
    public void testWriteBigDecimalFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, 0);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite(new BigDecimal("-1234")), is(toBytes("-000001234")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系書き込みテスト。
     * パディング文字を変更。
     */
    @Test
    public void testWriteOtherPaddingStringFixSignPositionNotRequiredSign() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        field.setPaddingValue(" ");
        sut.init(field, 10, 2);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite(new BigDecimal("-1234.56")), is(toBytes("-  1234.56")));
    }

    /**
     * 符号位置固定かつ符号非必須の場合の正常系書き込みテスト。
     * 拡張したデータタイプによって符号を変更。
     */
    @Test
    public void testWriteWithExtendDataTypeFixSignPositionNotRequiredSign() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, 0, true, false);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("+1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("▲00001234")));
    }

    /**
     * 符号位置固定かつ符号必須の場合の書き込みテスト。
     * 出力データは符号あり整数（文字列）。
     */
    @Test
    public void testWriteNumberStringFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnWrite("1234"), is(toBytes("+000001234")));
        assertThat(sut.convertOnWrite("+1234"), is(toBytes("+000001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("-000001234")));
    }

    /**
     * 符号位置固定かつ符号必須の場合の正常系書き込みテスト。
     * 拡張したデータタイプによって符号を変更。
     */
    @Test
    public void testWriteWithExtendDataTypeFixSignPositionRequiredSign() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, 0, true, false);
        sut.setFixedSignPosition(true);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnWrite("+1234"), is(toBytes("■00001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("▲00001234")));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の書き込みテスト。
     * 出力データは符号あり整数（文字列）。
     */
    @Test
    public void testWriteNumberStringNonFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("+1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("00000-1234")));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の正常系書き込みテスト。
     * 出力データは符号あり小数（文字列）。
     */
    @Test
    public void testWriteDecimaNonFixSignPositionNotRequiredSign() throws Exception {
        sut.init(field, 10, 2);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("+1234.56"), is(toBytes("0001234.56")));
        assertThat(sut.convertOnWrite("-1234.56"), is(toBytes("00-1234.56")));
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の正常系書き込みテスト。
     * 拡張したデータタイプによって符号を変更。
     */
    @Test
    public void testWriteWithExtendDataTypeNonFixSignPositionNotRequiredSign() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, 0, false, false);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(false);

        assertThat(sut.convertOnWrite("+1234"), is(toBytes("0000001234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("0000▲1234")));
    }

    /**
     * 符号位置非固定かつ符号必須の場合の書き込みテスト。
     * 出力データは符号あり整数（文字列）。
     */
    @Test
    public void testWriteNumberStringNonFixSignPositionRequiredSign() throws Exception {
        sut.init(field, 10, "");
        sut.setRequiredDecimalPoint(true);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnWrite("1234"), is(toBytes("00000+1234")));
        assertThat(sut.convertOnWrite("+1234"), is(toBytes("00000+1234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("00000-1234")));
    }

    /**
     * 符号位置非固定かつ符号必須の場合の正常系書き込みテスト。
     * 拡張したデータタイプによって符号を変更。
     */
    @Test
    public void testWriteWithExtendDataTypeNonFixSignPositionRequiredSign() throws Exception {
        SignedNumberStringDecimal sut = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, 10, 0, true, false);
        sut.setRequiredDecimalPoint(true);
        sut.setFixedSignPosition(false);
        sut.setRequiredPlusSign(true);

        assertThat(sut.convertOnWrite("+1234"), is(toBytes("0000■1234")));
        assertThat(sut.convertOnWrite("-1234"), is(toBytes("0000▲1234")));
    }

    /**
     * 書き込み時、出力対象のバイト長が大きい場合に例外を送出すること。
     */
    @Test
    public void testWriteLargeByteLength() throws Exception {
        sut.init(field, 10);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. too large data. field size = '10' data size = '11'. data: 12345678901");

        sut.convertOnWrite("12345678901");
    }

    /**
     * 負数で桁数を超過した場合に例外が送出されること。
     */
    @Test
    public void testWriteNegativeLargeByteLength() throws Exception {
        sut.init(field, 10);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. too large data. field size = '10' data size = '11'. data: -1234512345");

        sut.convertOnWrite("-1234512345");
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

    /**
     * 符号として'■'および'▲'を使用する拡張クラス。
     *
     * @author TIS
     */
    private class SignedNumberStringDecimalExtends extends SignedNumberStringDecimal {
        @Override
        protected String getPlusSign() {
            return "■";
        }
        @Override
        protected String getMinusSign() {
            return "▲";
        }
    }

    /** 文字列をバイトに変換する */
    private byte[] toBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("ms932");
    }
    
}
