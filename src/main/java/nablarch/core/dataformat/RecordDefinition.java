package nablarch.core.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * フォーマット定義ファイル内の、レコードタイプの定義情報を保持するクラス。
 * フォーマット定義ファイルのパース結果として生成される。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class RecordDefinition {
    // --------------------------------------------- internal structure
    /** レコードタイプ名 */
    private String typeName;
    
    /** このレコードタイプのベースとなるレコードタイプ（差分定義）*/
    private RecordDefinition baseRecordType = null;
    
    /** レコード内のフィールドのフォーマット定義 */
    private List<FieldDefinition>
        fieldDefinitions = new ArrayList<FieldDefinition>();
    
    /** このレコードタイプが適用される条件 */
    private final List<DataRecordPredicate>
        conditionsToApply = new ArrayList<DataRecordPredicate>();

    /**
     * このレコードフォーマットが、渡されたレコードに適用できるかどうかを返却する。
     * @param record データレコード
     * @param checkUnsetValues 未設定項目に対する検証を行うかどうか
     * @return 適用可能な場合は true
     */
    public boolean isApplicableTo(Map<String, ?> record, boolean checkUnsetValues) {
        for (DataRecordPredicate predicate : conditionsToApply) {
            if (!predicate.apply(record, checkUnsetValues)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * このレコードフォーマットが、渡されたレコードに適用できるかどうかを返却する。
     * @param record データレコード
     * @return 適用可能な場合は true
     */
    public boolean isApplicableTo(Map<String, ?> record) {
        return isApplicableTo(record, true);
    }
    
    /**
     * このレコード定義がデータレコードに適用される条件の一覧を返却する。
     * @return このレコード定義がデータレコードに適用される条件の一覧
     */
    public List<DataRecordPredicate> getConditionsToApply() {
        return conditionsToApply;
    }
    
    /**
     * このレコードタイプが適用される条件を追加する。（AND条件）
     * @param conditions このレコードタイプが適用される条件
     * @return このオブジェクト自体
     */
    public RecordDefinition addCondition(DataRecordPredicate... conditions) {
        conditionsToApply.addAll(Arrays.asList(conditions));
        return this;
    }
    
    /**
     * このレコードフォーマットのレコード種別名を設定する。
     * @param typeName レコード種別名
     * @return このオブジェクト自体
     */
    public RecordDefinition setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }
    
    /**
     * このレコードフォーマットのレコードタイプ名を返却する。
     * @return レコード種別名
     */
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * このレコードフォーマットのベースとなるレコードタイプ名を設定する。
     * @param recordType ベースとなるレコードタイプ
     * @return このオブジェクト自体
     */
    public RecordDefinition setBaseRecordType(RecordDefinition recordType) {
        baseRecordType = recordType;
        return this;
    }
    
    /**
     * このレコードフォーマットのベースとなるレコードタイプ名を取得する。
     * @return ベースとなるレコードタイプ
     */
    public RecordDefinition getBaseRecordType() {
        return baseRecordType;
    }
    
    /**
     * 指定されたフィールド定義を追加する。
     * @param fields 追加したフィールド定義
     * @return このオブジェクト自体
     */
    public RecordDefinition addField(FieldDefinition... fields) {
        fieldDefinitions.addAll(Arrays.asList(fields)); 
        return this;
    }
    
    /**
     * 本レコードタイプに紐付くフィールド情報を設定する。
     * @param fields フィールド
     * @return このオブジェクト自体
     */
    public RecordDefinition setFields(List<FieldDefinition> fields) {
        fieldDefinitions = fields;
        return this;
    }

    /**
     * 本レコードタイプに定義されているすべてのフィールド定義を返却する。
     * @return このレコードのフィールド定義
     */
    public List<FieldDefinition> getFields() {
        return fieldDefinitions;
    }
    
    /**
     * 本レコードタイプがレコード種別識別定義かどうか。
     * @return レコード種別識別定義であればtrue
     */
    boolean isClassifier() {
        return typeName.equalsIgnoreCase("classifier");
    }

}
