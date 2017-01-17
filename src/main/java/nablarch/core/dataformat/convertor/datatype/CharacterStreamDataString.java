package nablarch.core.dataformat.convertor.datatype;


import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 文字ストリームで入出力する文字列のデータタイプ。
 * <p>
 * 入力時は入力が空文字列かつ convertEmptyToNull プロパティが{@code true}の場合に{@code null}を返却し、
 * それ以外の場合は文字列をそのまま返却する。
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
     * この実装では、入力時に引数の文字列をそのまま返却する。
     * ただし、空文字列を{@code null}に変換する設定がされ、かつ引数が空文字列の場合は{@code null}を返却する。
     */
    @Override
    public String convertOnRead(String data) {
        if (convertEmptyToNull && data.isEmpty()) {
            return null;
        }
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
