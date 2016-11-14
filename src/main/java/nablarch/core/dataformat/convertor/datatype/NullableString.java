package nablarch.core.dataformat.convertor.datatype;


import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 文字ストリームでNULLデータを許容するデータタイプ。
 * <p>
 * 入力時には文字列に対して何もせずそのまま返却し、
 * 出力時にはオブジェクトを文字列に変換して返却する。
 * なお、出力時にオブジェクトがnullの場合は空文字を返却する。
 * </p>
 * @author TIS
 */
@Published(tag = "architect")
public class NullableString extends CharacterStreamDataString {

    /** 出力時に、nullが渡された場合に変換する空文字 */
    private static final String EMPTY = "";
    
    /** {@inheritDoc}
     * この実装では、入力時に、引数の文字列に対して何もせずに返却する。
     * @param data フィールドの値データ
     * @return 変換後の値
     */
    @Override
    public String convertOnRead(String data) {
        return data;
    }

    /**
     * この実装では、出力時に、引数のオブジェクトを文字列に変換して返却する。
     * <p/>
     * 引数がnullの場合は、空文字列を返却する。
     * @param data 書き込みを行うデータ
     * @return 変換後の値
     */
    @Override
    public String convertOnWrite(Object data) {
        if (data == null) {
            return EMPTY;
        }
        return StringUtil.toString(data);
    }

}
