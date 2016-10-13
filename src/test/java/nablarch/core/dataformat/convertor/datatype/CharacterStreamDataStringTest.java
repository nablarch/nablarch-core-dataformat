package nablarch.core.dataformat.convertor.datatype;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link CharacterStreamDataSupport}のテスト。
 * 
 * @author Masato Inoue
 */
public class CharacterStreamDataStringTest {

    /**
     * nullが渡された場合に、空文字に変換が行われることのテスト
     */
    @Test
    public void testWriteObjectNotBigDecimal() {
        CharacterStreamDataString dataType = new CharacterStreamDataString();

        /*
         * nullの場合
         */
        assertThat(dataType.convertOnWrite(null), is(""));
    }

    @Test
    public void testWriteObject_BigDecimal() throws Exception {
        final CharacterStreamDataString sut = new CharacterStreamDataString();

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
                
    }
}
