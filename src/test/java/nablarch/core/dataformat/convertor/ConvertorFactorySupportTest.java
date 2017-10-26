package nablarch.core.dataformat.convertor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.ValueConvertor;

import org.junit.Test;


/**
 * コンバータの生成を行う抽象基底ファクトリクラスのテストケース。
 * 
 * 観点
 * 通常の正常系に関してはフォーマッタのテストで網羅されているので、
 * ここでは、異常系および、独自のコンバータの追加テストを行う。
 * 
 * @author Masato Inoue
 */
public class ConvertorFactorySupportTest {

    /**
     * 不正なタイプ名のパラメータでデータタイプコンバータを生成しようとした場合、例外がスローされる。
     */
    @Test
    public void testInvalidDataTypeName() throws Exception {
        FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
        try {
            factory.typeOf("unknown", new FieldDefinition(), "");
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "unknown data type name was specified. "));
            assertThat(e.getMessage(), containsString("input data type name=[unknown]"));
        }
        try {
            factory.convertorOf("unknown", new FieldDefinition(), "");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("unknown value convertor name was specified. input value convertor name=[unknown].", e.getMessage());
        }
    }
    
    /**
     * データタイプコンバータではないコンバータを生成しようとした場合、例外がスローされる。
     */
    @Test
    public void testInvalidNotDataType() throws Exception {
        FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
        try {
            factory.typeOf("pad", new FieldDefinition(), "");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("the convertor corresponding to the data type name was not a DataType class. input data type name=[pad]. convertor class=[nablarch.core.dataformat.convertor.value.Padding].", e.getMessage());
        }
        try {
            factory.convertorOf("X", new FieldDefinition(), "");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("the convertor corresponding to the data type name was not a ValueConvertor class. input value convertor name=[X]. convertor class=[nablarch.core.dataformat.convertor.datatype.SingleByteCharacterString].", e.getMessage());
        }
    }

    /**
     * 独自で追加したコンバータが生成できることを確認する。
     */
    @Test
    public void testSetClassType() throws Exception {
        FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
        factory.setConvertorTable(new HashMap<String, String>(){{
            put("D_HOGE", "nablarch.core.dataformat.convertor.ConvertorFactorySupportTest$DataTypeHoge");
            put("V_HOGE", "nablarch.core.dataformat.convertor.ConvertorFactorySupportTest$ValueConverterHoge");
        }});
        
        assertSame(ValueConverterHoge.class, factory.convertorOf("V_HOGE", new FieldDefinition(), "").getClass());
        assertSame(DataTypeHoge.class, factory.typeOf("D_HOGE", new FieldDefinition(), "").getClass());
    }
    
    
    /**
     * 生成できない型のコンバータを生成しようとした場合、例外がスローされる。
     */
    @Test
    public void testInvalidClassType() throws Exception {
        FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
        factory.getConvertorTable().put("DataTypeIllegalAccess", DataTypePrivateStub.class);
        factory.getConvertorTable().put("DataTypeInstantiation", AbstractDataTypeHoge.class);
        factory.getConvertorTable().put("ConvertorIllegalAccess", ValueConverterPrivateStub.class);
        factory.getConvertorTable().put("ConvertorInstantiation", AbstractValueConverterHoge.class);
        
        try {
            factory.typeOf("DataTypeIllegalAccess", new FieldDefinition(), "");
            fail();
        } catch (IllegalStateException e) {
            assertSame(IllegalAccessException.class, e.getCause().getClass());
            assertEquals("data type convertor could not be instantiated. data type name=[DataTypeIllegalAccess]. convertor class=[nablarch.core.dataformat.convertor.DataTypePrivateStub].", e.getMessage());
        }
        try {
            factory.typeOf("DataTypeInstantiation", new FieldDefinition(), "");
            fail();
        } catch (IllegalStateException e) {
            assertSame(InstantiationException.class, e.getCause().getClass());
            assertEquals("data type convertor could not be instantiated. data type name=[DataTypeInstantiation]. convertor class=[nablarch.core.dataformat.convertor.ConvertorFactorySupportTest$AbstractDataTypeHoge].", e.getMessage());
        }        
        try {
            factory.convertorOf("ConvertorIllegalAccess", new FieldDefinition(), "");
            fail();
        } catch (IllegalStateException e) {
            assertSame(IllegalAccessException.class, e.getCause().getClass());
            assertEquals("value convertor could not be instantiated. value convertor name=[ConvertorIllegalAccess]. convertor class=[nablarch.core.dataformat.convertor.ValueConverterPrivateStub].", e.getMessage());
        } 
        try {
            factory.convertorOf("ConvertorInstantiation", new FieldDefinition(), "");
            fail();
        } catch (IllegalStateException e) {
            assertSame(InstantiationException.class, e.getCause().getClass());
            assertEquals("value convertor could not be instantiated. value convertor name=[ConvertorInstantiation]. convertor class=[nablarch.core.dataformat.convertor.ConvertorFactorySupportTest$AbstractValueConverterHoge].", e.getMessage());
        } 
    }
    
    
    
    public static class DataTypeHoge extends DataType<Object, Object> {
        @Override
        public DataType<Object, Object> initialize(Object... args) {
            return this;
        }
        @Override
        public Object convertOnRead(Object data) {
            return null;
        }
        @Override
        public Object convertOnWrite(Object data) {
            return null;
        }
        @Override
        public Integer getSize() {
            return null;
        }
    }

    public static class ValueConverterHoge implements ValueConvertor {

        public ValueConvertor initialize(FieldDefinition field, Object... args) {
            return this;
        }

        public Object convertOnRead(Object data) {
             return null;
        }

        public Object convertOnWrite(Object data) {
            return null;
        }
    }
    
    /** InstantiationExceptionのテストをするためのクラス */
    public static abstract class AbstractValueConverterHoge implements ValueConvertor {
    }
    /** InstantiationExceptionのテストをするためのクラス */
    public static abstract class AbstractDataTypeHoge extends DataType {
    }
    

    /**
     * DataTypeでも、ValueConverterでないコンバータを生成しようとした場合、例外がスローされる。
     */
    @Test
    public void testInvalidConverter() throws Exception {
        FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();

        try {
            factory.setConvertorTable(new HashMap<String, String>() {
                {
                    put("test", "nablarch.core.dataformat.convertor.ConvertorFactorySupportTest$AbstractValueConverterHoge");
                    put("hoge", "java.lang.Integer");
                }
            });
            fail();
        } catch (ClassNotFoundException e) {
            assertEquals("invalid class was specified. class name must convertor class. class=[java.lang.Integer].", e.getMessage());
        }
    }
}
