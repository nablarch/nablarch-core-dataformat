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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * バイト文字コンバータ{@link ByteStreamDataString}のテスト。
 * 
 * @author TIS
 */
public class ByteStreamDataStringTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ByteStreamDataString sut = new ByteStreamDataString();

    final private FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("utf-8"));

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[ByteStreamDataString].");

        sut.initialize(null);
    }

    /**
     * 初期化時のパラメータ不正テスト。
     */
    @Test
    public void initializeArgError(){
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[ByteStreamDataString].");

        sut.initialize(null, "hoge");
    }

    /**
     * 入力時にパラメータが空文字の場合のテスト。
     * 読込時はバイト長のチェックをせず、空文字列はnullとして読み込む。
     * 1. 空文字列はnullとして読み込む
     * 2. トリム文字のみの場合はnullとして読み込む
     */
    @Test
    public void testReadNotEntered() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead("".getBytes()), is(nullValue()));
        assertThat(sut.convertOnRead("          ".getBytes()), is(nullValue()));
    }

    /**
     * 空文字列を空文字列として読み込む設定が入っている場合の読み込みテスト。
     */
    @Test
    public void testReadNotEnteredEmpty() throws Exception {
        sut.init(field, 10);
        sut.setConvertEmptyToNull(false);

        assertThat(sut.convertOnRead("".getBytes()), is(""));
        assertThat(sut.convertOnRead("          ".getBytes()), is(""));
        assertThat(sut.convertOnRead("01234abcde".getBytes()), is("01234abcde"));

        sut.setConvertEmptyToNull(true);
    }

    /**
     * シングル・ダブル・マルチバイト混合文字が読み込めること。
     */
    @Test
    public void testReadCombinationByteString() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnRead("01α名武2β4羅5678".getBytes("utf-8")), is("01α名武2β4羅5678"));
    }

    /**
     * 読込時、左トリムはされないこと。
     */
    @Test
    public void testReadNoLeftTrim() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnRead("    0α名".getBytes("utf-8")), is("    0α名"));
    }

    /**
     * 読込時、デフォルトで半角スペースでトリムされること。
     */
    @Test
    public void testReadTrimDefault() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnRead("0α名    ".getBytes("utf-8")), is("0α名"));
    }

    /**
     * 読込時、指定した文字でトリムされること。
     */
    @Test
    public void testReadTrim() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")).setPaddingValue("0"), 10);
        assertThat(sut.convertOnRead("1α名0000".getBytes("utf-8")), is("1α名"));
    }

    /**
     * 出力時にパラメータがnullまたは空文字の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("MS932")), 10);
        assertThat(sut.convertOnWrite(null), is("          ".getBytes("MS932")));
        assertThat(sut.convertOnWrite(""), is("          ".getBytes("MS932")));
    }

    /**
     * シングルバイト文字が書き込めること。
     */
    @Test
    public void testWriteSingleByteString() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnWrite("0123456789"), is("0123456789".getBytes("utf-8")));
    }

    /**
     * ダブルバイト文字が書き込めること。
     */
    @Test
    public void testWriteDoubleByteString() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnWrite("αβγδε"), is("αβγδε".getBytes("utf-8")));
    }

    /**
     * ３バイト文字が書き込めること。
     */
    @Test
    public void testWriteMultiByteString() throws Exception {
        sut.init(field, 9);
        assertThat(sut.convertOnWrite("名武羅"), is("名武羅".getBytes("utf-8")));
    }

    /**
     * シングル・ダブル・マルチバイト混合文字が書き込めること。
     */
    @Test
    public void testWriteCombinationByteString() throws Exception {
        sut.init(field, 20);
        assertThat(sut.convertOnWrite("01α名武2β4羅567"), is("01α名武2β4羅567".getBytes("utf-8")));
    }

    /**
     * 書き込み時、バイト長が同じならばパディングされないこと。
     */
    @Test
    public void testWriteNoPadding() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnWrite("1000000001"), is("1000000001".getBytes("utf-8")));
    }

    /**
     * 書き込み時、デフォルトで半角スペースでパディングされること。
     */
    @Test
    public void testWritePaddingDefault() throws Exception {
        sut.init(field, 10);
        assertThat(sut.convertOnWrite("10001"), is("10001     ".getBytes("utf-8")));
    }

    /**
     * 書き込み時、指定した文字でパディングされること。
     */
    @Test
    public void testWritePadding() throws Exception {
        sut.init(field.setPaddingValue("0"), 10);
        assertThat(sut.convertOnWrite("1α名"), is("1α名0000".getBytes("utf-8")));
    }

    /**
     * 書き込み時に出力対象が指定バイト長より大きかった場合に例外を送出すること。
     */
    @Test
    public void testWriteLargeBytes() throws Exception {
        sut.init(field, 10);

        exception.expect(allOf(
                instanceOf(InvalidDataFormatException.class),
                hasProperty("message", is(Matchers.containsString("too large data."))),
                hasProperty("message", is(Matchers.containsString("field size = '10' data size = '11"))),
                hasProperty("message", is(Matchers.containsString("data: 01234567890")))
        ));

        sut.convertOnWrite("01234567890");
    }

    /**
     * 書き込み時に2バイトのパディング文字が指定された場合に例外を送出すること。
     */
    @Test
    public void testWriteLargePaddingString() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")).setPaddingValue("α"), 10);

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter was specified. the length of padding string must be 1. but specified one was 2 byte long.");

        sut.convertOnWrite("012345678");
    }

    /**
     * 書き込み時に空文字のパディング文字が指定された場合に例外を送出すること。
     */
    @Test
    public void testWriteEmptyPaddingString() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")).setPaddingValue(""), 10);

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid padding character was specified. Length of padding character must be '1', but was '0'. padding str = []");

        sut.convertOnWrite("012345678");
    }

    /**
     * 書き込み時に2文字のパディング文字が指定された場合に例外を送出すること。
     */
    @Test
    public void testWriteTwoCharPaddingString() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")).setPaddingValue("00"), 10);

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid padding character was specified. Length of padding character must be '1', but was '2'. padding str = [00]");

        sut.convertOnWrite("012345678");
    }

    /**
     * {@link BigDecimal}書き込みのテスト
     * @throws Exception
     */
    @Test
    public void testWriteBigDecimal() throws Exception {
        sut.init(field, 12);

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1           ".getBytes(Charset.forName("utf-8"))));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")),
                is("0.0000000001".getBytes(Charset.forName("utf-8"))));
    }

    /**
     * {@link BigDecimal}書き込みでサイズを超過した場合のテスト。
     */
    @Test
    public void testWriteBigDecimal_SizeOver() throws Exception {
        sut.init(field, 11);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("field size = '11' data size = '12'. data: 0.0000000001");

        sut.convertOnWrite(new BigDecimal("0.0000000001"));
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされていたらトリム。されていなければ、そのまま。
     */
    @Test
    public void testRemovePadding() {
        sut.init(field, 10);

        String data = "expected  ";
        String expected = "expected";

        assertThat(sut.removePadding(data), is(expected));
        assertThat(sut.removePadding(expected), is(expected));
    }
}
