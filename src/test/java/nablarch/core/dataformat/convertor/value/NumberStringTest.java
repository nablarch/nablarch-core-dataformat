package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Collections;

import nablarch.core.dataformat.InvalidDataFormatException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link NumberString}のテストクラス
 */
public class NumberStringTest {

    private final NumberString sut = new NumberString();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void writeTest() throws Exception {
        assertThat(sut.convertOnWrite("12345"), is("12345"));
        assertThat(sut.convertOnWrite("1000"), is("1000"));
        assertThat(sut.convertOnWrite(new BigDecimal("100.100")), is("100.100"));
        assertThat(sut.convertOnWrite(100), is("100"));
        assertThat(sut.convertOnWrite(1000L), is("1000"));
        assertThat(sut.convertOnWrite(Short.valueOf("10")), is("10"));
        assertThat(sut.convertOnWrite(0x30), is("48"));
    }

    @Test
    public void writeNull() throws Exception {
        assertThat(sut.convertOnWrite(null), isEmptyString());
    }

    @Test
    public void writeNotNumeric_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified."
                + " parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[1.1.1].");
        sut.convertOnWrite("1.1.1");
    }

    @Test
    public void writeSignedNumber_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified."
                + " parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[-100].");
        sut.convertOnWrite("-100");
    }

    @Test
    public void writeEmptyString_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified. parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[].");
        sut.convertOnWrite("");
    }

    @Test
    public void writeInvalidDataType() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter type was specified. parameter must be java.lang.Number class or number string.");
        sut.convertOnWrite(Collections.emptyMap());
    }

    @Test
    public void readTest() throws Exception {
        assertThat(sut.convertOnRead("1.1"), is(new BigDecimal("1.1")));
        assertThat(sut.convertOnRead("1000"), is(new BigDecimal("1000")));
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
                + " parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[abc].");
        sut.convertOnRead("abc");
    }

    @Test
    public void readSignedNumber_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified."
                + " parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[-100].");
        sut.convertOnRead("-100");
    }
}
