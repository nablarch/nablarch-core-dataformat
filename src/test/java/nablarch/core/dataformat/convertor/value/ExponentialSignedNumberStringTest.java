package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import nablarch.core.dataformat.InvalidDataFormatException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link ExponentialSignedNumberString}のテスト
 * 
 * @author TIS
 */
public class ExponentialSignedNumberStringTest {

    private ExponentialSignedNumberString sut = new ExponentialSignedNumberString();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void writeTest() throws Exception {
        assertThat(sut.convertOnWrite("12345"), is("12345"));
        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(-100), is("-100"));
        assertThat(sut.convertOnWrite(null), isEmptyString());
        assertThat(sut.convertOnWrite(new BigDecimal("-100.1")), is("-100.1"));
        assertThat(sut.convertOnWrite("-1e10"), is("-1e10"));
    }

    @Test
    public void writeNotNumeric_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified."
                + " parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?([eE][-+]?[0-9]+)?$]. value=[abc].");
        sut.convertOnWrite("abc");
    }

    @Test
    public void readTest() throws Exception {
        assertThat(sut.convertOnRead("100.1"), is(new BigDecimal("100.1")));
        assertThat(sut.convertOnRead("-1000"), is(new BigDecimal("-1000")));
        assertThat(sut.convertOnRead(null), is(nullValue()));
        assertThat(sut.convertOnRead(""), is(nullValue()));
    }

    @Test
    public void readNotNumeric_shouldThrowException() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter format was specified."
                + " parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?([eE][-+]?[0-9]+)?$]. value=[abc].");
        sut.convertOnRead("abc");
    }

    /**
     * 符号ありおよび指数付き数値が正しくバリデーションされることのチェックを行います。
     */
    @Test
    public void testValidateNumericString() {

        // 正常系(指数なし)
        sut.validateNumericString("1");
        sut.validateNumericString("32");
        sut.validateNumericString("324");
        sut.validateNumericString("1.23");
        sut.validateNumericString("0.15");
        sut.validateNumericString("100.158");

        // 正常系(指数あり)
        sut.validateNumericString("1e10");
        sut.validateNumericString("32e2");
        sut.validateNumericString("324e42");
        sut.validateNumericString("1.23e53");
        sut.validateNumericString("0.15e2");
        sut.validateNumericString("100.158e41");

        // 正常系(+符号指数あり)
        sut.validateNumericString("1e+10");
        sut.validateNumericString("32e+2");
        sut.validateNumericString("324e+42");
        sut.validateNumericString("1.23e+53");
        sut.validateNumericString("0.15e+2");
        sut.validateNumericString("100.158e+41");
        
        // 正常系(-符号指数あり)
        sut.validateNumericString("1e-10");
        sut.validateNumericString("32e-2");
        sut.validateNumericString("324e-42");
        sut.validateNumericString("1.23e-53");
        sut.validateNumericString("0.15e-2");
        sut.validateNumericString("100.158e-41");
        
        // 正常系(指数なし/整数部がマイナス)
        sut.validateNumericString("-1");
        sut.validateNumericString("-32");
        sut.validateNumericString("-324");
        sut.validateNumericString("-1.23");
        sut.validateNumericString("-0.15");
        sut.validateNumericString("-100.158");
        
        // 正常系(指数あり/整数部がマイナス)
        sut.validateNumericString("-1e10");
        sut.validateNumericString("-32e2");
        sut.validateNumericString("-324e42");
        sut.validateNumericString("-1.23e53");
        sut.validateNumericString("-0.15e2");
        sut.validateNumericString("-100.158e41");
        
        // 正常系(+符号指数あり/整数部がマイナス)
        sut.validateNumericString("-1e+10");
        sut.validateNumericString("-32e+2");
        sut.validateNumericString("-324e+42");
        sut.validateNumericString("-1.23e+53");
        sut.validateNumericString("-0.15e+2");
        sut.validateNumericString("-100.158e+41");
        
        // 正常系(-符号指数あり/整数部がマイナス)
        sut.validateNumericString("-1e-10");
        sut.validateNumericString("-32e-2");
        sut.validateNumericString("-324e-42");
        sut.validateNumericString("-1.23e-53");
        sut.validateNumericString("-0.15e-2");
        sut.validateNumericString("-100.158e-41");
        
        // 異常系(非数値)
        try {
            sut.validateNumericString("abc");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[abc]."));
        }
        try {
            sut.validateNumericString("e");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[e]."));
        }
        try {
            sut.validateNumericString("-12-3");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-12-3]."));
        }
        
        // 異常系(小数部が不正)
        try {
            sut.validateNumericString("-1.Fe-10");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1.Fe-10]."));
        }
        
        // 異常系(小数部が複数)
        try {
            sut.validateNumericString("-1.1.3e5");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1.1.3e5]."));
        }
        
        // 異常系(指数部が不正)
        try {
            sut.validateNumericString("-1e");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e]."));
        }
        
        // 異常系(指数部が複数)
        try {
            sut.validateNumericString("-1e-10e5");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e-10e5]."));
        }
        
        // 異常系(指数部が小数)
        try {
            sut.validateNumericString("-1e1.9");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e1.9]."));
        }
    }

}
