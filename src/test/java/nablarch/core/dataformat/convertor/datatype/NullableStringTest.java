package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link NullableString}のテスト
 *
 * @author TIS
 */
public class NullableStringTest {

    private NullableString sut = new NullableString();

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link NullableString}では初期化時になにもしないため、nullを許容する。
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
        // nullは空文字に変換する
        assertThat(sut.convertOnWrite(null), is(""));
    }

    /**
     * {@link BigDecimal}書き込みのテスト
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000002")), is("0.0000000002"));
    }
}