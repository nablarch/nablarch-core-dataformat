package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.util.annotation.Published;

/**
 * 一定のルールで変換を行うコンバータのサポートクラス。
 * @author Masato Inoue
 * @param <F> ファイル入力時：入力したオブジェクトを変換した値、ファイル出力時：出力するオブジェクトの変換前の値
 * @param <T> ファイル入出力時のオブジェクト型
 */
@Published(tag = "architect")
public abstract class ValueConvertorSupport<F, T> implements
        ValueConvertor<F, T> {

    /** フィールド定義 */
    private FieldDefinition field;
    
    /** {@inheritDoc}*/   
    public ValueConvertor<F, T> initialize(FieldDefinition field,
            Object... args) {
        this.field = field;
        return this;
    }
    
    /**
     * フィールド定義を取得する。
     * @return フィールド定義
     */
    protected FieldDefinition getField() {
        return field;
    }
    
}
