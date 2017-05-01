package nablarch.core.dataformat.convertor.logicbased;

import nablarch.core.dataformat.convertor.datatype.DataType;

public class CustomType extends DataType<String, String> {

    @Override
    public DataType<String, String> initialize(final Object... args) {
        return null;
    }

    @Override
    public String convertOnRead(final String data) {
        return null;
    }

    @Override
    public String convertOnWrite(final Object data) {
        return null;
    }

    @Override
    public Integer getSize() {
        return null;
    }
}
