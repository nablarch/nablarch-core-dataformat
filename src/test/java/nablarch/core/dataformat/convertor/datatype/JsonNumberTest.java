package nablarch.core.dataformat.convertor.datatype;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
     */
    @Test
    public void testInitializeNull() {
        assertThat(sut.initialize(null), is((DataType<String, String>)sut));
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
     * @throws Exception
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        Assert.assertThat(sut.convertOnWrite(new BigDecimal(("1"))), is("1"));
        Assert.assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }
}
