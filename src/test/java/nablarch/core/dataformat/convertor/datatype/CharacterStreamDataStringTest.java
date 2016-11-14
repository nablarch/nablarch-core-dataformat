package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link CharacterStreamDataSupport}のテスト。
 * 
 * @author Masato Inoue
 */
public class CharacterStreamDataStringTest {

    private CharacterStreamDataString sut = new CharacterStreamDataString();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link CharacterStreamDataSupport}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        assertThat(sut.initialize(null), is((DataType<String, String>)sut));
    }

    /**
     * 読込のテスト。
     * 空文字とそれ以外をテストする。
     */
    @Test
    public void testRead() {
        assertThat(sut.convertOnRead(""), is(""));
        assertThat(sut.convertOnRead("abc"), is("abc"));
    }

    /**
     * 出力のテスト。
     * nullが渡された場合に、空文字に変換が行われること。
     */
    @Test
    public void testWrite() {
        assertThat(sut.convertOnWrite("abc"), is("abc"));
        assertThat(sut.convertOnWrite(null), is(""));
        assertThat(sut.convertOnWrite(""), is(""));
    }

    @Test
    public void testWriteObject_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }
}
