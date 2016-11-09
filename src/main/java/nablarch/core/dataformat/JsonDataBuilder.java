package nablarch.core.dataformat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import nablarch.core.dataformat.StructuredDataRecordFormatterSupport.StructuredDataDirective;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.datatype.JsonBoolean;
import nablarch.core.dataformat.convertor.datatype.JsonNumber;
import nablarch.core.dataformat.convertor.datatype.JsonObject;
import nablarch.core.dataformat.convertor.datatype.JsonString;
import nablarch.core.util.StringUtil;

/**
 * JSONビルダー。<br>
 * この実装では独自実装によりJSONデータの解析を行います。
 * 
 * @author TIS
 */
public class JsonDataBuilder extends StructuredDataEditorSupport implements StructuredDataBuilder {

    /**
     * コンストラクタ
     */
    public JsonDataBuilder() {
        super();
    }

    /**
     * JSON文字列を作成します。
     * 
     * @param map フラットマップ
     * @param layoutDef フォーマット定義
     * @param out JSON文字列出力先ストリーム
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    public void buildData(Map<String, ?> map, LayoutDefinition layoutDef, OutputStream out) 
            throws IOException, InvalidDataFormatException {

        RecordDefinition rd = layoutDef.getRecords().get(0);
        String json = createJsonString("", map, layoutDef, rd, true);
        if (json == null) {
            // 何も出力されなかったら空のオブジェクトとする
            json = "";
        }
        json = "{" + json + "}";
        out.write(
                json.getBytes(
                        StructuredDataDirective.getTextEncoding(layoutDef.getDirective())));
    }

    /**
     * JSON文字列を構築します
     * 
     * @param currentKeyBase キー名ベース
     * @param map 出力対象マップ
     * @param ld フォーマット定義
     * @param rd レコードタイプ定義
     * @param checkIndispensable 必須チェック実施可否
     * @return JSON文字列
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    private String createJsonString(String currentKeyBase, Map<String, ?> map,
            LayoutDefinition ld, RecordDefinition rd, boolean checkIndispensable)
            throws InvalidDataFormatException {
        StringBuilder sb = new StringBuilder();
        int outCount = 0;
        for (FieldDefinition fd : rd.getFields()) {
            if (outCount > 0) {
                sb.append(",");
            }
            // レコード定義取得
            RecordDefinition nrd = ld.getRecordType(fd.getName());
            // Mapに格納する際のKeyを作成
            String mapKey = buildMapKey(currentKeyBase, fd.getName());
            if (fd.isArray()) {
                if (nrd != null) {
                    // ObjectArray
                    outCount += writeObjectArray(sb, ld, nrd, fd, currentKeyBase, mapKey, map);
                        
                } else {
                    // StringArray
                    outCount += writeStringArray(sb, fd, currentKeyBase, mapKey, map);
                }
            } else {
                if (nrd != null) {
                    // 子オブジェクト出力
                    // 必須チェック実施
                    boolean containsKey = false;
                    if (map != null) {
                        for (String key : map.keySet()) {
                            if (key.startsWith(mapKey)) {
                                containsKey = true;
                                break;
                            }
                        }
                    }
                    
                    if (!containsKey) {
                        checkRequired(currentKeyBase, fd, null, checkIndispensable);
                    }
                    
                    // Object
                    if (containsKey) {
                        outCount += writeObject(sb, ld, nrd, fd, mapKey, map);
                    }
                    
                } else {
                    Object writeVal = (map == null) ? null
                                                    : convertToFieldOnWrite(map.get(mapKey), fd);
                    // 必須チェック実施
                    checkRequired(currentKeyBase, fd, writeVal, checkIndispensable);
                    
                    outCount += writeValue(sb, fd, mapKey, map, writeVal);
                }
            }
            
            // 任意項目が出力されなかった場合に余分な","が出力される可能性があるので、除去する
            int len = sb.length();
            if (len > 0 && sb.charAt(len - 1) == ',') {
                sb.delete(len - 1, len);
            }
        }

        if (outCount == 0) {
            return null;
            
        } else {
            return sb.toString();
        }
    }
    
    /**
     * オブジェクト配列の出力処理です
     * @param sb 出力中バッファ
     * @param ld フォーマット定義
     * @param nrd レコードタイプ定義
     * @param fd フィールド定義
     * @param currentKeyBase キー名ベース
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @return 出力カウント
     */
    private int writeObjectArray(StringBuilder sb, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd, 
            String currentKeyBase, String mapKey, Map<String, ?> map) {
        int outCount = 0;
        
        StringBuilder innerSb = new StringBuilder();
        
        for (int i = 0;; i++) {
            // ベースキーを作成
            String innerBaseKey = mapKey + "[" + i + "]";
            boolean containsKey = false;
            if (map != null) {
                for (String key : map.keySet()) {
                    if (key.startsWith(innerBaseKey)) {
                        containsKey = true;
                        break;
                    }
                }
            }
            // マップにインナーのベースキーを含むデータがない場合はループを抜ける
            if (!containsKey) {
                break;
            }
            
            String innerStr = createJsonString(innerBaseKey, map, ld, nrd, false);
            if (innerStr == null) {
                break;
            } else {
                if (i > 0) {
                    innerSb.append(",");
                }
                innerSb.append(editJsonDataString(innerStr, fd));
                outCount++;
            }
        }
        
        // Listの長さチェック実行
        checkArrayLength(fd, outCount, currentKeyBase);

        if (innerSb.length() > 0) {
            sb.append(editJsonKey(fd.getName()) + ":[");
            sb.append(innerSb);
            sb.append("]");
        }
        
        return outCount;
    }
    
    /**
     * 文字列配列の出力処理です
     * @param sb 出力中バッファ
     * @param fd フィールド定義
     * @param currentKeyBase キー名ベース
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @return 出力カウント
     */
    private int writeStringArray(StringBuilder sb, FieldDefinition fd, String currentKeyBase, String mapKey, Map<String, ?> map) {
        int outCount = 0;
        int arraySize = 0;
        if (map != null && map.containsKey(mapKey)) {
            String[] arr = (String[]) map.get(mapKey);
            if (arr != null) {
                arraySize = arr.length;
                sb.append(editJsonKey(fd.getName()) + ":[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Object writeVal = convertToFieldOnWrite(arr[i], fd);
                    sb.append(editJsonDataString(writeVal, fd));
                    outCount++;
                }
                sb.append("]");
            } else {
                sb.append(editJsonKey(fd.getName()) + ':' + editJsonDataString(arr, fd));
                outCount++;
            }
        }
        // Listの長さチェック実行
        checkArrayLength(fd, arraySize, currentKeyBase);
        
        return outCount;
    }
    
    /**
     * オブジェクトの出力処理です
     * @param sb 出力中バッファ
     * @param ld フォーマット定義
     * @param nrd レコードタイプ定義
     * @param fd フィールド定義
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @return 出力カウント
     */
    private int writeObject(StringBuilder sb, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd, 
            String mapKey, Map<String, ?> map) {
        int outCount = 0;

        String jsonString;
        if (map.containsKey(mapKey) && map.get(mapKey) == null) {
            jsonString = null;
        } else {
            jsonString = createJsonString(mapKey, map, ld, nrd, true);
            if (jsonString == null) {
                jsonString = "";
            }
        }
        outCount++;
        sb.append(editJsonKey(fd.getName()) + ":" + editJsonDataString(jsonString, fd));
        return outCount;
    }
    
    /**
     * 値の出力処理です
     * @param sb 出力中バッファ
     * @param fd フィールド定義
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @param writeVal 書き込み値
     * @return 出力カウント
     */
    private int writeValue(StringBuilder sb, FieldDefinition fd, String mapKey, Map<String, ?> map, Object writeVal) {
        int outCount = 0;
        if (map != null && map.containsKey(mapKey) || writeVal != null) {
            sb.append(editJsonKey(fd.getName()) + ":" + editJsonDataString(writeVal, fd));
            outCount++;
        }
        return outCount;
    }
    
    /**
     * JSON用キーとしてキーデータを編集します
     * @param key キーデータ
     * @return JSON用キーデータ
     */
    private String editJsonKey(String key) {
        return "\"" + escape(key) + "\"";
    }
    
    /**
     * JSON用データとして出力データを編集します
     * @param o 出力データ
     * @param fd フィールド定義
     * @return JSON用データ
     */
    private String editJsonDataString(Object o, FieldDefinition fd) {
        DataType<?, ?> type = fd.getDataType();
        String prefix;
        String suffix;
        boolean isEscape;
        
        if (type instanceof JsonString) {
            prefix = "\"";
            suffix = "\"";
            isEscape = true;
            
        } else if (type instanceof JsonNumber
                || type instanceof JsonBoolean) {
            prefix = "";
            suffix = "";
            isEscape = false;
            
        } else if (type instanceof JsonObject) {
            prefix = "{";
            suffix = "}";
            isEscape = false;
            
        } else {
            throw new InvalidDataFormatException("Invalid data type definition. type=" + type.getClass().getName());
        }
        
        if (o == null) {
            return "null";
        } else {
            if (isEscape) {
                return prefix + escape(StringUtil.toString(o)) + suffix;
            } else {
                return prefix + StringUtil.toString(o) + suffix;
            }
        }
    }
    
    /**
     * トークンのエスケープ処理を行います。
     * 
     * @param token トークン
     * @return エスケープ処理後のトークン
     */
    private String escape(String token) {
        token = token.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("/", "\\/")
                     .replace("\b", "\\b")
                     .replace("\f", "\\f")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        return token;
    }
}
