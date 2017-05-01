package nablarch.core.dataformat.convertor.logicbased;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nablarch.core.dataformat.convertor.VariableLengthConvertorFactory;
import nablarch.core.util.map.CaseInsensitiveMap;

public class LogicBasedVariableLengthConvertorFactory extends VariableLengthConvertorFactory {

    @Override
    protected Map<String, Class<?>> getDefaultConvertorTable() {
        final Map<String, Class<?>> defaultConvertorTable = new CaseInsensitiveMap<Class<?>>(
                new ConcurrentHashMap<String, Class<?>>(super.getDefaultConvertorTable()));
        defaultConvertorTable.put("custom", CustomType.class);
        return Collections.unmodifiableMap(defaultConvertorTable);
    }
}
