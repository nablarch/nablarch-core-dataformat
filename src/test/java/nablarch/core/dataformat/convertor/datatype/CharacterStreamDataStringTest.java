package nablarch.core.dataformat.convertor.datatype;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

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
        sut.initialize((Object[]) null);
    }

    /**
     * 読込のテスト。
     * 空文字, 文字列をテストする。
     */
    @Test
    public void testRead() {
        assertThat(sut.convertOnRead(""), is(nullValue()));
        assertThat(sut.convertOnRead("abc"), is("abc"));
        assertThat(sut.convertOnRead("\uD840\uDC0B\uD844\uDE3D"), is("\uD840\uDC0B\uD844\uDE3D"));
    }

    /**
     * 空文字として読み込む設定をした場合の読み込みテスト。
     */
    @Test
    public void testReadEmpty() {
        sut.setConvertEmptyToNull(false);

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
