package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.util.annotation.Published;

/**
 * バイトストリームで入出力するデータタイプが継承すべき抽象基底クラス。
 * 
 * @param <F> 入力データ（byte配列）が変換されるオブジェクトの型
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public abstract class ByteStreamDataSupport<F> extends DataType<F, byte[]> {

    /** バイト長 */
    private Integer size;

    /** {@inheritDoc}
     * <p/>
     * ByteStreamDataSupport
     * この実装では、データサイズ（=バイト長）を返却する。
     * @return データサイズ（=バイト長）
     */
    public Integer getSize() {
        return size;
    }

    /**
     * データサイズ（=バイト長）を設定する。
     * @param size データサイズ（=バイト長）
     * @return このオブジェクト自体
     */
    public ByteStreamDataSupport<F> setSize(Integer size) {
        this.size = size;
        return this;
    }
}
