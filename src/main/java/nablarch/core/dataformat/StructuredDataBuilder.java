package nablarch.core.dataformat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 構造化データの構築を行うクラスが実装するインタフェース。
 * 
 * @author TIS
 */
public interface StructuredDataBuilder {

    /**
     * XML文字列を作成します。
     * 
     * @param map フラットマップ
     * @param layoutDef フォーマット定義
     * @param out 構造化データ出力先ストリーム
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    void buildData(Map<String, ?> map, LayoutDefinition layoutDef, OutputStream out) throws IOException, InvalidDataFormatException;
}
