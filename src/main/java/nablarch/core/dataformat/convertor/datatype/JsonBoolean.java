package nablarch.core.dataformat.convertor.datatype;


import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * JSONにおける真偽値を表現するマーカークラス。
 * <p>
 * 入力時には文字列に対して何もせずそのまま返却し、
 * 出力時にはオブジェクトを文字列に変換して返却する。
 * なお、出力時にオブジェクトがnullの場合はnullを返却する。
 * </p>
 * <p>
 * 本クラスはマーカークラスとして存在し、上記以外の特別な処理は行わない。
 * </p>
 * @author TIS
 */
@Published(tag = "architect")
public class JsonBoolean extends CharacterStreamDataString {
    
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
     * 引数がnullの場合は、nullを返却する。
     * @param data 書き込みを行うデータ
     * @return 変換後の値
     */
    @Override
    public String convertOnWrite(Object data) {
        if (data == null) {
            return null;
        }
        return StringUtil.toString(data);
    }

}
