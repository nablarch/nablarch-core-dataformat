package nablarch.core.dataformat;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.StructuredDataRecordFormatterSupport.StructuredDataDirective;
import nablarch.core.util.JsonParser;

import static nablarch.core.util.Builder.concat;

/**
 * JSONパーサー。<br>
 * この実装では{@link JsonParser}を使用してJSONデータの解析を行います。
 *
 * @author TIS
 */
public class JsonDataParser extends StructuredDataEditorSupport implements StructuredDataParser {

    /** 初期容量（文字数） */
    private static final int INITIAL_CAPACITY = 10 * 1024;

    /**
     * コンストラクタ
     */
    public JsonDataParser() {
        super();
    }

    /**
     * フラットマップを作成します。
     *
     * @param in 入力ストリーム
     * @param layoutDef フォーマット定義
     * @return フラットマップ
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    public Map<String, ?> parseData(InputStream in, LayoutDefinition layoutDef)
            throws IOException, InvalidDataFormatException {

        Charset enc = Charset.forName(
                StructuredDataDirective.getTextEncoding(layoutDef.getDirective()));
        String jsonString = readAll(in, enc);

        Map<String, Object> record = new HashMap<String, Object>();

        try {
            Map<String, ?> parsedMap = new JsonParser().parse(jsonString);
            RecordDefinition rd = layoutDef.getRecords().get(0);
            makeFlatMap("", record, layoutDef, rd, parsedMap);

        } catch (IllegalArgumentException e) {
            throw new InvalidDataFormatException("JSON Parse Error. " + e.getMessage(), e);
        }

        return record;
    }

    /**
     * フラットMap作成処理</br> 階層構造を持ったMapから、キーで階層構造を表現した１階層のMapを作成します。
     *
     * @param currentKeyBase キー名ベース
     * @param outMap 出力対象マップ
     * @param layoutDef フォーマット定義
     * @param recordDef レコードタイプ定義
     * @param structuredMap 階層構造を持ったMap
     * @throws InvalidDataFormatException targetObjectがNullで必須項目の場合
     */
    @SuppressWarnings("unchecked")
    private void makeFlatMap(String currentKeyBase, Map<String, Object> outMap,
            LayoutDefinition layoutDef, RecordDefinition recordDef, Map<String, ?> structuredMap)
            throws InvalidDataFormatException {

        for (FieldDefinition fieldDef : recordDef.getFields()) {
            // レコード定義取得

            // Mapに格納する際のKeyを作成
            String mapKey = buildMapKey(currentKeyBase, fieldDef.getName());
            if (fieldDef.isArray()) {
                if (isObjectType(fieldDef)) {
                    // ObjectArray
                    List<Object> list = (List<Object>) structuredMap.get(fieldDef.getName());
                    if (list == null) {
                        list = new ArrayList<Object>();
                    }

                    // Listの長さチェック実行
                    checkArrayLength(fieldDef, list.size(), currentKeyBase);

                    for (int i = 0; i < list.size(); i++) {
                        Object o = list.get(i);
                        if (o instanceof Map) {
                            RecordDefinition next = layoutDef.getRecordType(fieldDef.getName());
                            makeFlatMap(
                                    concat(mapKey, "[", i, "]"),
                                    outMap, layoutDef, next, (Map<String, Object>) o);
                        } else {
                            throw new InvalidDataFormatException(String.format(
                                    "BaseKey = %s,Field %s is Object Array but other item detected",
                                    currentKeyBase, fieldDef.getName()));
                        }
                    }
                    outMap.put(mapKey + "Size", Integer.toString(list.size()));
                } else {
                    // StringArray
                    List<String> list = (List<String>) structuredMap.get(fieldDef.getName());
                    if (list == null) {
                        list = new ArrayList<String>();
                    }

                    // Listの長さチェック実行
                    checkArrayLength(fieldDef, list.size(), currentKeyBase);
                    String[] arr = list.toArray(new String[list.size()]);
                    outMap.put(mapKey, arr);
                }
            } else {
                if (isObjectType(fieldDef)) {
                    // 子オブジェクト書き込み
                    Map<String, Object> childMap = (Map<String, Object>) structuredMap.get(fieldDef.getName());
                    // 必須チェック実施
                    checkIndispensable(currentKeyBase, fieldDef, childMap);

                    if (childMap != null) {
                        // Object
                        RecordDefinition next = layoutDef.getRecordType(fieldDef.getName());
                        makeFlatMap(mapKey, outMap, layoutDef, next, childMap);
                    }
                } else {
                    // 読み込みオブジェクト取得
                    Object readTarget = convertToFieldOnRead((String) structuredMap.get(fieldDef.getName()), fieldDef);
                    // 必須チェック実施
                    checkIndispensable(currentKeyBase, fieldDef, readTarget);

                    outMap.put(mapKey, readTarget);
                }
            }
        }
    }

    /**
     * 指定された文字コードで、ストリームから文字列を読み取る。
     *
     * @param in 入力ストリーム
     * @param encoding 文字コード
     * @return 読み取った文字列
     * @throws IOException 予期しない入出力例外
     */
    private String readAll(InputStream in, Charset encoding) throws IOException {
        Reader reader = new InputStreamReader(in, encoding);
        StringBuilder result = new StringBuilder(INITIAL_CAPACITY);
        int c;
        while ((c = reader.read()) != -1) {
            result.append((char) c);
        }
        return result.toString();
    }
}
