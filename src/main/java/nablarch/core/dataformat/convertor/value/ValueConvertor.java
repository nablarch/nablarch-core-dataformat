package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.util.annotation.Published;

/**
 * フィールドデータの変換を行う際に、
 * 一定のルールで変換を行うコンバータが実装するインターフェース。
 * <p>
 * コンバータを追加することで、バイト列から読み込んだデータに対して
 * 様々な変換処理を追加できる
 * </p>
 * @param <F> ファイル入力時：入力したオブジェクトの変換後オブジェクトの型<br>
 *            ファイル出力時：出力するオブジェクトの変換後オブジェクトの型
 * @param <T> ファイル入出力時のオブジェクト型

 * @author  Iwauo Tajima
 */
@Published(tag = "architect")
public interface ValueConvertor<F, T> {
    /**
     * 初期化処理を行う。
     * @param field フィールド定義
     * @param args  コンバータのパラメータ
     * @return 初期化されたコンバータ （通常はthisをリターンする）
     */
    ValueConvertor<F, T> initialize(FieldDefinition field, Object... args);
    
    /**
     * 入力時に、フィールドデータを変換する。
     * @param data フィールドの値データ
     * @return 変換後の値
     */
    F convertOnRead(T data);
    
    /**
     * 出力時にフィールドデータの変換を行う。
     * @param data 書き込みを行うデータ
     * @return 変換後の値
     */
    T convertOnWrite(Object data);
}
