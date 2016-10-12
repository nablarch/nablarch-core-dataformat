package nablarch.core.dataformat.convertor;

import nablarch.core.dataformat.convertor.datatype.DataType;

/** 
 * IllegalAccessExceptionのテストをするためのスタブ
 * @author Masato Inoue
 */
public class DataTypePrivateStub extends DataType<Object, Object>{
    private DataTypePrivateStub() {
    }

    @Override
    public DataType<Object, Object> initialize(Object... args) {
        return null;
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
