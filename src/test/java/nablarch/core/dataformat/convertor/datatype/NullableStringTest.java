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
     * 初期化時にnullが渡されたときのテスト。
     * {@link NullableString}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        NullableString dataType = new NullableString();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

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
        assertEquals("", converter.convertOnRead(""));
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
        assertEquals("", converter.convertOnWrite(""));
        // nullは空文字に変換する
        assertEquals("", converter.convertOnWrite(null));
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
