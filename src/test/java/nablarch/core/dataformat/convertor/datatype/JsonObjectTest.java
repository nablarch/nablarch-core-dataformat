package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonObject}のテスト
 * 
 * @author TIS
 */
public class JsonObjectTest {

    private JsonObject sut = new JsonObject();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonObject}では初期化時になにもしないため、nullを許容する。
     * 例外が発生しないこと。
     */
    @Test
    public void testInitializeNull() {
        sut.initialize(null);
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
     * {@link BigDecimal}を書き込む場合のテスト。
     * @throws Exception
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat("指数表記とならないこと", sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
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
