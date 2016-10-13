package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.InvalidDataFormatException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link ExponentialNumberString}のテスト
 * 
 * @author TIS
 */
public class ExponentialNumberStringTest {

    /**
     * 符号なしおよび指数付き数値が正しくバリデーションされることのチェックを行います。
     */
    @Test
    public void testValidateNumericString() {
        ExponentialNumberString convertor = new ExponentialNumberString();
        
        // 正常系(指数なし)
        convertor.validateNumericString("1");
        convertor.validateNumericString("32");
        convertor.validateNumericString("324");
        convertor.validateNumericString("1.23");
        convertor.validateNumericString("0.15");
        convertor.validateNumericString("100.158");

        // 正常系(指数あり)
        convertor.validateNumericString("1e10");
        convertor.validateNumericString("32e2");
        convertor.validateNumericString("324e42");
        convertor.validateNumericString("1.23e53");
        convertor.validateNumericString("0.15e2");
        convertor.validateNumericString("100.158e41");

        // 正常系(+符号指数あり)
        convertor.validateNumericString("1e+10");
        convertor.validateNumericString("32e+2");
        convertor.validateNumericString("324e+42");
        convertor.validateNumericString("1.23e+53");
        convertor.validateNumericString("0.15e+2");
        convertor.validateNumericString("100.158e+41");
        
        // 正常系(-符号指数あり)
        convertor.validateNumericString("1e-10");
        convertor.validateNumericString("32e-2");
        convertor.validateNumericString("324e-42");
        convertor.validateNumericString("1.23e-53");
        convertor.validateNumericString("0.15e-2");
        convertor.validateNumericString("100.158e-41");
        
        // 異常系(非数値)
        try {
            convertor.validateNumericString("abc");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[abc]."));
        }
        try {
            convertor.validateNumericString("e");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[e]."));
        }
        
        // 異常系(整数部がマイナス)
        try {
            convertor.validateNumericString("-1e-10");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e-10]."));
        }
        
        // 異常系(小数部が不正)
        try {
            convertor.validateNumericString("-1.Fe-10");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1.Fe-10]."));
        }
        
        // 異常系(小数部が複数)
        try {
            convertor.validateNumericString("-1.1.3e5");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1.1.3e5]."));
        }
        
        // 異常系(指数部が不正)
        try {
            convertor.validateNumericString("-1e");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e]."));
        }
        
        // 異常系(指数部が複数)
        try {
            convertor.validateNumericString("-1e-10e5");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e-10e5]."));
        }
        
        // 異常系(指数部が小数)
        try {
            convertor.validateNumericString("-1e1.9");
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue( e.getMessage().contains("value=[-1e1.9]."));
        }
    }

}
