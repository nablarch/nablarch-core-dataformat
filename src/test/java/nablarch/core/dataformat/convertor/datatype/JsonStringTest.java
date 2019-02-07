package nablarch.core.dataformat.convertor.datatype;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * {@link JsonString}のテスト
 * 
 * @author TIS
 */
public class JsonStringTest {

    private JsonString sut = new JsonString();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonString}では初期化時になにもしないため、nullを許容する。
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
        assertThat(sut.convertOnRead("\"data\uD840\uDC0B\""), is("\"data\uD840\uDC0B\""));
        assertThat(sut.convertOnRead("data\uD840\uDC0B"), is("data\uD840\uDC0B"));
        assertThat(sut.convertOnRead("\"data\uD840\uDC0B"), is("\"data\uD840\uDC0B"));
        assertThat(sut.convertOnRead("data\uD840\uDC0B\""), is("data\uD840\uDC0B\""));
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
        assertThat(sut.convertOnWrite("\"data\uD840\uDC0B\""), is("\"data\uD840\uDC0B\""));
        assertThat(sut.convertOnWrite("\"data\uD840\uDC0B"), is("\"data\uD840\uDC0B"));
        assertThat(sut.convertOnWrite("data\uD840\uDC0B\""), is("data\uD840\uDC0B\""));
        assertThat(sut.convertOnWrite(""), is(""));
        // nullはnull
        assertThat(sut.convertOnWrite(null), is(nullValue()));
    }

    /**
     * {@link BigDecimal}の書き込みテスト。
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000003")), is("0.0000000003"));
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
