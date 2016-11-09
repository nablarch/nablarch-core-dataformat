package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonString}のテスト
 * 
 * @author TIS
 */
public class JsonStringTest {

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonString}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        JsonString dataType = new JsonString();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        JsonString converter = new JsonString();
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
        JsonString converter = new JsonString();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        assertEquals("", converter.convertOnWrite(""));
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * {@link BigDecimal}の書き込みテスト。
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final JsonString sut = new JsonString();

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000003")), is("0.0000000003"));
    }
}
