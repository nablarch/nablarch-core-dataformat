package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.dataformat.convertor.datatype.ByteStreamDataString;
import nablarch.core.dataformat.convertor.datatype.Bytes;
import nablarch.core.dataformat.convertor.datatype.DoubleByteCharacterString;
import nablarch.core.dataformat.convertor.datatype.PackedDecimal;
import nablarch.core.dataformat.convertor.datatype.SignedPackedDecimal;
import nablarch.core.dataformat.convertor.datatype.SignedNumberStringDecimal;
import nablarch.core.dataformat.convertor.datatype.SignedZonedDecimal;
import nablarch.core.dataformat.convertor.datatype.SingleByteCharacterString;
import nablarch.core.dataformat.convertor.datatype.NumberStringDecimal;
import nablarch.core.dataformat.convertor.datatype.ZonedDecimal;
import nablarch.core.dataformat.convertor.value.CharacterReplacer;
import nablarch.core.dataformat.convertor.value.DefaultValue;
import nablarch.core.dataformat.convertor.value.NumberString;
import nablarch.core.dataformat.convertor.value.Padding;
import nablarch.core.dataformat.convertor.value.SignedNumberString;
import nablarch.core.dataformat.convertor.value.UseEncoding;
import nablarch.core.util.map.CaseInsensitiveMap;

/**
 * 固定長ファイルの読み書きを行う際に使用するコンバータのファクトリクラス。
 * @author Iwauo Tajima
 */
public class FixedLengthConvertorFactory extends ConvertorFactorySupport {
   
    /**
     * 固定長ファイルのデフォルトのコンバータ名とコンバータ実装クラスの対応表を返却する。
     * @return 固定長ファイルのデフォルトのコンバータ名とコンバータ実装クラスの対応表
     */
    @Override
    protected Map<String, Class<?>> getDefaultConvertorTable() {
        return DEFAULT_CONVERTOR_TABLE;
    }
    
    /** デフォルトのコンバータ名とコンバータ実装クラスの対応表 */
    private static final Map<String, Class<?>>
    DEFAULT_CONVERTOR_TABLE = new CaseInsensitiveMap<Class<?>>() {
        {
        // ------------------------------ DataType
        put("X",         SingleByteCharacterString.class);
        put("N",         DoubleByteCharacterString.class);
        put("XN",         ByteStreamDataString.class);
        put("Z",         ZonedDecimal.class);
        put("SZ",        SignedZonedDecimal.class);
        put("P",         PackedDecimal.class);
        put("SP",        SignedPackedDecimal.class);
        put("X9",         NumberStringDecimal.class);
        put("SX9",        SignedNumberStringDecimal.class);
        put("B",         Bytes.class);
        // ------------------------------ ValueConvertor
        put("pad",       Padding.class);
        put("encoding",  UseEncoding.class);
        put("_LITERAL_", DefaultValue.class);
        put("number",    NumberString.class);
        put("signed_number",    SignedNumberString.class);
        put("replacement", CharacterReplacer.class);
        }
    };
}
