package nablarch.core.dataformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 構造化データの解析を行うクラスが実装するインタフェース。
 * 
 * @author TIS
 */
public interface StructuredDataParser {

    /**
     * フラットマップを作成します。
     * 
     * @param in 構造化データ入力ストリーム
     * @param layoutDef フォーマット定義
     * @return フラットマップ
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    Map<String, ?> parseData(InputStream in, LayoutDefinition layoutDef) throws IOException, InvalidDataFormatException;
}
