package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.util.annotation.Published;

/**
 * 文字ストリームで入出力するデータタイプが継承すべき抽象規定クラス。
 * 
 * @param <F> 入力データ（文字列）が変換されるオブジェクトの型
 * 
 * @author Masato Inoue
 */
@Published(tag = "architect")
public abstract class CharacterStreamDataSupport<F> extends DataType<F, String> {
    
    /** {@inheritDoc}
     * この実装では、サイズは固定で1を返却する。
     * @return サイズ（固定で1）
     */
    @Override
    public Integer getSize() {
        return 1;
    }
    
}
