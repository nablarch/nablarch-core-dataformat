package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link NullableString}のテスト
 * 
 * @author TIS
 */
public class NullableStringTest {

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        NullableString converter = new NullableString();
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
        NullableString converter = new NullableString();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * {@link BigDecimal}書き込みのテスト
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final NullableString sut = new NullableString();

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000002")), is("0.0000000002"));
    }
}
