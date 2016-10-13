package nablarch.core.dataformat.convertor;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.convertor.value.ValueConvertor;

/**
 * IllegalAccessExceptionのテストをするためのスタブ 
 * @author Masato Inoue
 */
public class ValueConverterPrivateStub implements ValueConvertor<Object, Object>{
    private ValueConverterPrivateStub() {
    }

    public ValueConvertor<Object, Object> initialize(FieldDefinition field,
            Object... args) {
        return null;
    }

    public Object convertOnRead(Object data) {
        return null;
    }

    public Object convertOnWrite(Object data) {
        return null;
    }
}
