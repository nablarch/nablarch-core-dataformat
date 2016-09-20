package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.dataformat.convertor.datatype.JsonBoolean;
import nablarch.core.dataformat.convertor.datatype.JsonNumber;
import nablarch.core.dataformat.convertor.datatype.JsonObject;
import nablarch.core.dataformat.convertor.datatype.JsonString;
import nablarch.core.dataformat.convertor.value.CharacterReplacer;
import nablarch.core.dataformat.convertor.value.DefaultValue;
import nablarch.core.dataformat.convertor.value.ExponentialNumberString;
import nablarch.core.dataformat.convertor.value.ExponentialSignedNumberString;
import nablarch.core.util.map.CaseInsensitiveMap;

/**
 * JSONデータコンバータのファクトリクラス。
 * @author TIS
 */
public class JsonDataConvertorFactory extends ConvertorFactorySupport {

    /**
     * JSONデータのデフォルトのコンバータ名とコンバータ実装クラスの対応表を返却する。
     * @return JSONデータのデフォルトのコンバータ名とコンバータ実装クラスの対応表
     */
    @Override
    protected Map<String, Class<?>> getDefaultConvertorTable() {
        return DEFAULT_CONVERTOR_TABLE;
    }
    
    /** デフォルトのコンバータ名とコンバータ実装クラスの対応表*/
    private static final Map<String, Class<?>>
    DEFAULT_CONVERTOR_TABLE = new CaseInsensitiveMap<Class<?>>() {
        {
        // ------------------------------ DataType
        put("X",                JsonString.class);
        put("N",                JsonString.class);
        put("XN",               JsonString.class);
        put("X9",               JsonNumber.class);
        put("SX9",              JsonNumber.class);
        put("BL",               JsonBoolean.class);
        put("OB",               JsonObject.class);
        // ------------------------------ ValueConvertor
        put("_LITERAL_",        DefaultValue.class);
        put("number",           ExponentialNumberString.class);
        put("signed_number",    ExponentialSignedNumberString.class);
        put("replacement",      CharacterReplacer.class);
        }
    };
}
