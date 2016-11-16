package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link CharacterStreamDataSupport}のテスト。
 * 
 * @author TIS
 */
public class CharacterStreamDataStringTest {

    private CharacterStreamDataString sut = new CharacterStreamDataString();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link CharacterStreamDataSupport}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        sut.initialize(null);
    }

    /**
     * 読込のテスト。
     * null, 空文字, 文字列をテストする。
     */
    @Test
    public void testRead() {
        assertThat(sut.convertOnRead(null), is(nullValue()));
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

    /**
     * {@link BigDecimal}の出力テスト。
     */
    @Test
    public void testWriteObject_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされないのでそのまま。
     */
    @Test
    public void testRemovePadding() {
        String expected = "expected  ";

        assertThat(sut.removePadding(expected), is(expected));
    }
}
