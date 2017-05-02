package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.dataformat.convertor.datatype.NullableString;
import nablarch.core.dataformat.convertor.value.CharacterReplacer;
import nablarch.core.dataformat.convertor.value.DefaultValue;
import nablarch.core.dataformat.convertor.value.ExponentialNumberString;
import nablarch.core.dataformat.convertor.value.ExponentialSignedNumberString;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.CaseInsensitiveMap;

/**
 * XMLデータコンバータのファクトリクラス。
 * @author TIS
 */
public class XmlDataConvertorFactory extends ConvertorFactorySupport {

    /**
     * XMLデータのデフォルトのコンバータ名とコンバータ実装クラスの対応表を返却する。
     * @return XMLデータのデフォルトのコンバータ名とコンバータ実装クラスの対応表
     */
    @Override
    @Published(tag = "architect")
    protected Map<String, Class<?>> getDefaultConvertorTable() {
        return DEFAULT_CONVERTOR_TABLE;
    }
    
    /** デフォルトのコンバータ名とコンバータ実装クラスの対応表*/
    private static final Map<String, Class<?>>
    DEFAULT_CONVERTOR_TABLE = new CaseInsensitiveMap<Class<?>>() {
        {
        // ------------------------------ DataType
        put("X",                NullableString.class);
        put("N",                NullableString.class);
        put("XN",               NullableString.class);
        put("X9",               NullableString.class);
        put("SX9",              NullableString.class);
        put("BL",               NullableString.class);
        put("OB",               NullableString.class);
        // ------------------------------ ValueConvertor
        put("_LITERAL_",        DefaultValue.class);
        put("number",           ExponentialNumberString.class);
        put("signed_number",    ExponentialSignedNumberString.class);
        put("replacement",      CharacterReplacer.class);
        }
    };
}
