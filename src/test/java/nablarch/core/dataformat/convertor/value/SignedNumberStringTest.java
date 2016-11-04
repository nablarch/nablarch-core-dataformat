package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import nablarch.core.dataformat.InvalidDataFormatException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link SignedNumberString}のテスト。
 */
public class SignedNumberStringTest {

    private final SignedNumberString sut = new SignedNumberString();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void writeTest() throws Exception {
        assertThat(sut.convertOnWrite("100"), is("100"));
        assertThat(sut.convertOnWrite("-1.123"), is("-1.123"));
        assertThat(sut.convertOnWrite("+500"), is("+500"));
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(-1.123), is("-1.123"));
        assertThat(sut.convertOnWrite(0x00), is("0"));
        assertThat(sut.convertOnWrite(100L), is("100"));
    }

    @Test
    public void writeNull() throws Exception {
        assertThat(sut.convertOnWrite(null), isEmptyString());
    }

    @Test
    public void writeNotNumeric_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified." 
                + " parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[abc].");
        sut.convertOnWrite("abc");
    }

    @Test
    public void writeEmptyString_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified." 
                + " parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[].");
        sut.convertOnWrite("");
    }

    @Test
    public void readTest() throws Exception {
        assertThat(sut.convertOnRead("100"), is(new BigDecimal("100")));
        assertThat(sut.convertOnRead("-1.123"), is(new BigDecimal("-1.123")));
        assertThat(sut.convertOnRead("+500"), is(new BigDecimal("500")));
    }

    @Test
    public void readNull() throws Exception {
        assertThat(sut.convertOnRead(null), is(nullValue()));
    }

    @Test
    public void readEmptyString() throws Exception {
        assertThat(sut.convertOnRead(""), is(nullValue()));
    }

    @Test
    public void readNotNumeric_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified." 
                + " parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[abc].");
        sut.convertOnRead("abc");
    }
}