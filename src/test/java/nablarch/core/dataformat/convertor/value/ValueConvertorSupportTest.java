package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.FieldDefinition;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * ValueConvertorSupportのgetterのテスト。
 * @author Masato Inoue
 */
public class ValueConvertorSupportTest {
    
    /**
     * getterのテスト。
     */
    @Test
    public void testGetter(){
        ValueConvertorSupportStub stub = new ValueConvertorSupportStub();
        FieldDefinition definition = new FieldDefinition();
        stub.initialize(definition, new Object());
        assertSame(stub.getFieldTest(), definition);
    }
    
    private class ValueConvertorSupportStub extends ValueConvertorSupport{

        public Object convertOnRead(Object data) {
            return null;
        }

        public Object convertOnWrite(Object data) {
            return null;
        }
        @Override
        protected FieldDefinition getField() {
            return super.getField();
        };
        
        public FieldDefinition getFieldTest(){
            return getField();
        }
    }
}
