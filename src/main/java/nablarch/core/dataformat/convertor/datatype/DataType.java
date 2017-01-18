package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.util.annotation.Published;

/**
 * ファイルや電文のストリームを読み書きし、
 * フィールドへの変換を行うデータタイプが継承すべき抽象基底クラス。
 * 
 * @param <T> 入出力時のデータの型
 * @param <F> 入力データが変換されるオブジェクトの型
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public abstract class DataType<F, T> {

    /** フィールド定義 */
    private FieldDefinition field;

    /** 未入力を{@code null}に変換するフラグ */
    protected boolean convertEmptyToNull = true;

    // ----------------------------------------------------- abstract methods
    /**
     * 初期化処理を行う。
     * @param args  データタイプのパラメータ
     * @return 初期化されたデータタイプ （通常はthisをリターンする）
     */
    public abstract DataType<F, T> initialize(Object... args);
    
    /**
     * 入力時に読み込んだデータを変換する。
     * @param data フィールドの値データ
     * @return 変換後の値
     */
    public abstract F convertOnRead(T data);
    
    /**
     * 出力時に書き込むデータの変換を行う。
     * @param data 書き込みを行うデータ
     * @return 変換後の値
     */
    public abstract T convertOnWrite(Object data);

    
    // ------------------------------------------------------ helper methods
    /**
     * 初期化処理を行う。
     * @param field フィールド定義
     * @param args  データタイプのパラメータ
     * @return 初期化されたデータタイプ （通常はthisをリターンする）
     */
    public DataType<F, T> init(FieldDefinition field, Object... args) {
        this.field = field;
        return initialize(args);
    }
    
    // ------------------------------------------------------ accessors
    /**
     * 扱うデータ型に応じたデータサイズを返却する。
     * （固定長データを扱う場合はバイト長、可変長データを扱う場合は文字列長を返却する）
     * @return データサイズ
     */
    public abstract Integer getSize();

    /**
     * フィールド定義を取得する。
     * @return フィールド定義
     */
    public FieldDefinition getField() {
        return field;
    }

    /**
     * パディングを取り除く。
     *
     * @param data 対象データ
     * @return パディング除去後のデータ
     */
    public F removePadding(Object data) {
        T t = convertOnWrite(data);
        F f = convertOnRead(t);
        return f;
    }

    /**
     * 未入力値を{@code null}に変換するかを設定する。
     * @param convertEmptyToNull 未入力値を{@code null}に変換するならtrue
     */
    public void setConvertEmptyToNull(boolean convertEmptyToNull) {
        this.convertEmptyToNull = convertEmptyToNull;
    }

}
