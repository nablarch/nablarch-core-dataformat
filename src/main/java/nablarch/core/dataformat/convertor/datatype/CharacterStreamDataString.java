package nablarch.core.dataformat.convertor.datatype;


import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 文字ストリームで入出力する文字列のデータタイプ。
 * <p>
 * 入力時には文字列に対して何もせずそのまま返却し、
 * 出力時にはオブジェクトを文字列に変換して返却する。
 * </p>
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class CharacterStreamDataString extends CharacterStreamDataSupport<String> {
    
    /** 出力時に、nullが渡された場合に変換する空文字 */
    private static final String EMPTY = "";
    
    /** {@inheritDoc}
     * この実装では、初期化時には何も行わない。
     */
    @Override
    public DataType<String, String> initialize(Object... args) {
        return this;
    }

    /**　{@inheritDoc}
     * この実装では、入力時に、引数の文字列に対して何もせずに返却する。
     */
    @Override
    public String convertOnRead(String data) {
        return data;
    }

    /**　{@inheritDoc}<p/>
     * この実装では、出力時に、引数のオブジェクトを文字列に変換して返却する。
     * <p/>
     * 引数がnullの場合は、空文字に変換して返却する。
     */
    @Override
    public String convertOnWrite(Object data) {
        if (data == null) {
            return EMPTY;
        }
        return StringUtil.toString(data);
    }
}
