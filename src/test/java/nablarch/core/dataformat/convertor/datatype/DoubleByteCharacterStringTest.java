package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * ダブルバイト文字のコンバートテスト。
 * 
 * 観点：
 * 正常系はフォーマッタのテストで確認しているので、ここではオプション設定関連のテストを行う。
 *   ・全角文字のパディング、トリムのテスト。
 * 
 * @author Masato Inoue
 */
public class DoubleByteCharacterStringTest {

    private DoubleByteCharacterString sut = new DoubleByteCharacterString();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[DoubleByteCharacterString].");

        sut.initialize(null);
    }

    /**
     * 初期化時の第一引数にnullが渡されたときのテスト。
     */
    @Test
    public void testInitialize1stParameterNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[DoubleByteCharacterString].");

        sut.initialize(null, "hoge");
    }

    /**
     * 初期化時の第一引数（バイト長）が２の倍数でないときのテスト。
     */
    @Test
    public void testInitializeNotDoubleByteLength() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid field size was specified. the length of DoubleByteCharacter data field must be a even number. " +
                "field size=[3]. convertor=[DoubleByteCharacterString].");

        sut.initialize(3, "hoge");
    }

    /**
     * パディング文字にシングルバイトの文字を設定した場合、例外が発生する。
     */
    @Test
    public void testSingleBytePaddingString() {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf8")).setPaddingValue("a"), 10);

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter was specified. the length of padding string must be 2. but specified one was 1 byte long.");

        sut.convertOnWrite("");
    }

    /**
     * 読込のテスト。
     * 空文字とそれ以外をテストする。
     */
    @Test
    public void testRead() {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf8")), 10);

        assertThat(sut.convertOnRead("".getBytes()), is(""));
        assertThat(sut.convertOnRead("あいう".getBytes()), is("あいう"));
    }

    /**
     * 読込時のトリムのテスト。
     */
    @Test
    public void testTrim() {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("sjis")), 10);

        assertThat(sut.convertOnRead("あいう　　".getBytes(Charset.forName("sjis"))), is("あいう"));
    }

    /**
     * 書き込みのテスト。
     * 文字列, null, 空白の場合のテスト。
     */
    @Test
    public void testWrite() throws Exception {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("MS932")), new Object[]{10});

        assertThat(sut.convertOnWrite("あいう"), is("あいう　　".getBytes("MS932")));
        assertThat(sut.convertOnWrite(null), is("　　　　　".getBytes("MS932")));
        assertThat(sut.convertOnWrite(""), is("　　　　　".getBytes("MS932")));
    }

    /**
     * 書き込み時のパディングのテスト。
     */
    @Test
    public void testPadding() {
        sut.init(new FieldDefinition().setEncoding(Charset.forName("sjis")), 10);

        assertThat(sut.convertOnWrite("あいう"), is("あいう　　".getBytes(Charset.forName("sjis"))));
    }
}
