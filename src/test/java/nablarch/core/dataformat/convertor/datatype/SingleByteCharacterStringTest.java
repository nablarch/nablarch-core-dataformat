package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * シングルバイト文字コンバータのテスト。
 * 
 * 観点：
 * 正常系はフォーマッタのテストで確認しているので、ここでは異常系のテストを行う。
 *   ・レイアウト定義ファイルのパラメータ不正
 *   ・レイアウト定義ファイルで設定したフィールド長を超えるサイズの書き込み
 *   ・パディング文字に２バイト文字を設定
 * 
 * @author Masato Inoue
 */
public class SingleByteCharacterStringTest {

    private SingleByteCharacterString sut = new SingleByteCharacterString();
    private FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("MS932"));

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[SingleByteCharacterString].");

        sut.initialize(null);
    }

    /**
     * 初期化時のパラメータ不正テスト。
     * バイト長がnullのケース。
     */
    @Test
    public void testInitializeByteLengthNull(){
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[SingleByteCharacterString].");

        sut.initialize(null, "hoge");
    }

    /**
     * 初期化時のパラメータ不正テスト。
     * バイト長が指定されないケース。
     */
    @Test
    public void testInitializeByteLengthEmpty(){
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("parameter was not specified. parameter must be specified. convertor=[SingleByteCharacterString].");

        sut.initialize(new Object[]{});
    }

    /**
     * 初期化時のパラメータ不正テスト。
     * バイト長が文字列のケース。
     */
    @Test
    public void testInitializeByteLengthString(){
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter type was specified. 1st parameter must be an integer. parameter=[a]. convertor=[SingleByteCharacterString].");

        sut.initialize("a");
    }

    /**
     * 入力時にパラメータが空白の場合のテスト。
     */
    @Test
    public void testReadEmpty() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead("".getBytes()), is(""));
    }

    /**
     * 正常な入力のテスト。
     */
    @Test
    public void testRead() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnRead("abc0123   ".getBytes()), is("abc0123"));
    }

    /**
     * 出力時にパラメータがnullまたは空白の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite(null), is("          ".getBytes("MS932")));
        assertThat(sut.convertOnWrite(""), is("          ".getBytes("MS932")));
    }

    /**
     * 出力時に出力対象が指定のバイト長より大きい場合のテスト。
     */
    @Test
    public void testWriteOverByteLength() throws Exception {
        sut.init(field, 10);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. too large data. field size = '10' data size = '11'. data: 12345678901");

        sut.convertOnWrite("12345678901");
    }

    /**
     * パディング文字が２バイトの場合のテスト。
     */
    @Test
    public void testWrite2BytePaddingString() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("MS932")).setPaddingValue("　");
        sut.init(field, 10);

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter was specified. the length of padding string must be 1. but specified one was 2 byte long.");

        sut.convertOnWrite("1234567890");
    }

    /**
     * 正常な出力のテスト。
     */
    @Test
    public void testWrite() throws Exception {
        sut.init(field, 10);

        assertThat(sut.convertOnWrite("abc0123"), is("abc0123   ".getBytes()));
    }


}
