package nablarch.core.dataformat.convertor;


/**
 * コンバータの設定情報を保持するクラスが実装するインタフェース。
 * @author Masato Inoue
 */
public interface ConvertorSetting {
    
    /**
     * コンバータの生成を行うファクトリクラスを返却する。
     * @return コンバータの生成を行うファクトリクラス
     */
    ConvertorFactorySupport getConvertorFactory();
    
}
