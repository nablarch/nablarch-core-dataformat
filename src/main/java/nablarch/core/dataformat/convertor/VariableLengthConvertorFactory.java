package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.value.CharacterReplacer;
import nablarch.core.dataformat.convertor.value.DefaultValue;
import nablarch.core.dataformat.convertor.value.NumberString;
import nablarch.core.dataformat.convertor.value.SignedNumberString;
import nablarch.core.dataformat.convertor.value.UseEncoding;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.CaseInsensitiveMap;

/**
 * 可変長データコンバータのファクトリクラス。
 * @author Iwauo Tajima
 */
public class VariableLengthConvertorFactory extends ConvertorFactorySupport {

    /**
     * 可変長ファイルのデフォルトのコンバータ名とコンバータ実装クラスの対応表を返却する。
     * @return 固定長ファイルのデフォルトのコンバータ名とコンバータ実装クラスの対応表
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
        put("X",         CharacterStreamDataString.class);
        put("N",         CharacterStreamDataString.class);
        put("XN",         CharacterStreamDataString.class);
        put("Z",         CharacterStreamDataString.class);
        put("X9",         CharacterStreamDataString.class);
        put("SX9",         CharacterStreamDataString.class);
        // ------------------------------ ValueConvertor
        put("encoding",  UseEncoding.class);
        put("_LITERAL_", DefaultValue.class);
        put("number",    NumberString.class);
        put("signed_number",    SignedNumberString.class);
        put("replacement", CharacterReplacer.class);
        }
    };
}
