package nablarch.core.dataformat.convertor.datatype;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * {@link JsonNumber}のテスト
 * 
 * @author TIS
 */
public class JsonNumberTest {

    private JsonNumber sut = new JsonNumber();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonNumber}では初期化時になにもしないため、nullを許容する。
     * 例外が発生しないこと。
     */
    @Test
    public void testInitializeNull() {
        sut.initialize((Object[]) null);
    }

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        assertThat(sut.convertOnRead("\"data\""), is("\"data\""));
        assertThat(sut.convertOnRead("data"), is("data"));
        assertThat(sut.convertOnRead("\"data"), is("\"data"));
        assertThat(sut.convertOnRead("data\""), is("data\""));
        assertThat(sut.convertOnRead(""), is(""));
        assertThat(sut.convertOnRead(null), is(nullValue()));
    }
    
    /**
     * 書き込み時のテスト
     */
    @Test
    public void testConvertOnWrite() {
        // 入力値がそのまま返却される
        assertThat(sut.convertOnWrite("data"), is("data"));
        assertThat(sut.convertOnWrite("\"data\""), is("\"data\""));
        assertThat(sut.convertOnWrite("\"data"), is("\"data"));
        assertThat(sut.convertOnWrite("data\""), is("data\""));
        assertThat(sut.convertOnWrite(""), is(""));
        // nullはnull
        assertThat(sut.convertOnWrite(null), is(nullValue()));
    }

    /**
     * BigDecimalの書き込みのテスト
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(new BigDecimal(("1"))), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * パディングされないのでそのまま。
     */
    @Test
    public void testRemovePadding() {
        String expected = "00123";

        assertThat(sut.removePadding(expected), is(expected));
    }
}
