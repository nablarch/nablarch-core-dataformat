package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonObject}のテスト
 * 
 * @author TIS
 */
public class JsonObjectTest {

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonObject}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        JsonObject dataType = new JsonObject();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        JsonObject converter = new JsonObject();
        assertEquals("\"data\"", converter.convertOnRead("\"data\""));
        assertEquals("data", converter.convertOnRead("data"));
        assertEquals("\"data", converter.convertOnRead("\"data"));
        assertEquals("data\"", converter.convertOnRead("data\""));
        assertEquals("", converter.convertOnRead(""));
        assertEquals(null, converter.convertOnRead(null));
    }
    
    /**
     * 書き込み時のテスト
     */
    @Test
    public void testConvertOnWrite() {
        // 入力値がそのまま返却される
        JsonObject converter = new JsonObject();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        assertEquals("", converter.convertOnWrite(""));
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * {@link BigDecimal}を書き込む場合のテスト。
     * @throws Exception
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final JsonObject sut = new JsonObject();

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat("指数表記とならないこと", sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }
}
