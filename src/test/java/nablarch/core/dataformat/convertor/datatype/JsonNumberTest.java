package nablarch.core.dataformat.convertor.datatype;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

/**
 * {@link JsonNumber}のテスト
 * 
 * @author TIS
 */
public class JsonNumberTest {

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        JsonNumber converter = new JsonNumber();
        assertEquals("\"data\"", converter.convertOnRead("\"data\""));
        assertEquals("data", converter.convertOnRead("data"));
        assertEquals("\"data", converter.convertOnRead("\"data"));
        assertEquals("data\"", converter.convertOnRead("data\""));
        assertEquals(null, converter.convertOnRead(null));
    }
    
    /**
     * 書き込み時のテスト
     */
    @Test
    public void testConvertOnWrite() {
        // 入力値がそのまま返却される
        JsonNumber converter = new JsonNumber();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * BigDecimalの書き込みのテスト
     * @throws Exception
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final JsonNumber sut = new JsonNumber();
        Assert.assertThat(sut.convertOnWrite(new BigDecimal(("1"))), is("1"));
        Assert.assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }
}
