package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.util.annotation.Published;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * フォーマット定義ファイル内の、レコード内の各フィールドの定義情報を保持するクラス。
 * フォーマット定義ファイルのパース結果として生成される。
 * 
 * 各フィールド定義に関連するコンバータは、パース後に{@link DataRecordFormatter}が本クラスに設定する。
 * 
 * @see LayoutDefinition
 * @see RecordDefinition
 * @see DataRecordFormatter
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class FieldDefinition {
    // --------------------------------- properties
    /** 開始位置  */
    private Integer position = null;
    
    /** フィールド名 */
    private String name = null;
    
    /** 文字エンコーディング */
    private Charset encoding = null;
    
    /** パディング/トリムに使用する値 */
    private Object paddingValue = null;
    
    /** データタイプ */
    private DataType<?, ?> dataType = null;
    
    /** FILLER項目指定 */
    private boolean isFiller = false;
    
    /** 必須項目指定 */
    private boolean isRequired = true;
    
    /** 属性項目指定 */
    private boolean isAttribute = false;
    
    /** 配列項目指定 */
    private boolean isArray = false;
    
    /** 最小配列要素数 */
    private int minArraySize = -1;
    
    /** 最大配列要素数 */
    private int maxArraySize = -1;
    
    /** コンバータ定義のリストを保持 */
    private Map<String, Object[]> convertorSettingList = new LinkedHashMap<String, Object[]>();


    /** コンバーターのリスト */
    @SuppressWarnings("rawtypes")
    private List<ValueConvertor> convertors = new ArrayList<ValueConvertor>();

    
    // ----------------------------------- accessors

    /**
     * フォーマット定義ファイルで指定されたコンバータの定義を追加する。
     * @param convertor コンバータ名
     * @param convertorArgs 引数
     * @return このオブジェクト自体
     */
    public FieldDefinition addConvertorSetting(String convertor, Object[] convertorArgs) {
        convertorSettingList.put(convertor, convertorArgs);
        return this;
    }
    
    /**
     * フォーマット定義ファイルで指定されたコンバータの定義を取得する。
     * @return フォーマット定義ファイルで指定されたコンバータ
     */
    public Map<String, Object[]> getConvertorSettingList() {
        return convertorSettingList;
    }
  
    /**
     * フィールド名称を返却する。
     * @return フィールド名
     */
    public String getName() {
        return name;
    }
    
    /**
     * フィールド名称を設定する。
     * @param name フィールド名称
     * @return このオブジェクト自体
     */
    public FieldDefinition setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * フィールドの文字エンコーディングを返却する。
     * @return フィールドの文字エンコーディング
     */
    public Charset getEncoding() {
        return encoding;
    }
    
    /**
     * フィールドの文字エンコーディングを設定する。
     * @param encoding フィールドの文字エンコーディング
     * @return このオブジェクト自体
     */
    public FieldDefinition setEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }
    
    /**
     * フィールドのパディング/トリムに使用する値を返却する。
     * @return フィールドのパディング/トリムに使用する値
     */
    public Object getPaddingValue() {
        return paddingValue;
    }
    
    /**
     * フィールドのパディング/トリムに使用する値を設定する。
     * @param  value フィールドのパディング/トリムに使用する値
     * @return このオブジェクト自体
     */
    public FieldDefinition setPaddingValue(Object value) {
        paddingValue = value;
        return this;
    }
    
    /**
     * このフィールドがFILLER項目であればtrueを返却する。
     * @return FILLER項目であればtrue
     */
    public boolean isFiller() {
        return isFiller;
    }
    
    /**
     * このフィールドがFILLER項目に設定する。
     * @return このオブジェクト自体
     */
    public FieldDefinition markAsFiller() {
        this.isFiller = true;
        return this;
    }
    
    /**
     * このフィールドが必須項目であればtrueを返却する。
     * @return 必須項目であればtrue
     */
    public boolean isRequired() {
        return isRequired;
    }
    
    /**
     * このフィールドが任意項目に設定する。
     * @return このオブジェクト自体
     */
    public FieldDefinition markAsNotRequired() {
        this.isRequired = false;
        return this;
    }
    
    /**
     * このフィールドが属性項目であればtrueを返却する。
     * @return 属性項目であればtrue
     */
    public boolean isAttribute() {
        return isAttribute;
    }
    
    /**
     * このフィールドが属性項目に設定する。
     * @return このオブジェクト自体
     */
    public FieldDefinition markAsAttribute() {
        this.isAttribute = true;
        return this;
    }
    
    /**
     * このフィールドが配列項目であればtrueを返却する。
     * @return 配列項目であればtrue
     */
    public boolean isArray() {
        return isArray;
    }
    
    /**
     * このフィールドが配列項目に設定する。
     * @return このオブジェクト自体
     */
    public FieldDefinition markAsArray() {
        this.isArray = true;
        return this;
    }
    
    /**
     * フィールドの最小配列要素数を返却する。
     * @return フィールドの最小配列要素数
     */
    public int getMinArraySize() {
        return minArraySize;
    }
    
    /**
     * フィールドの最小配列要素数を設定する。
     * @param minArraySize フィールドの最小配列要素数
     * @return このオブジェクト自体
     */
    public FieldDefinition setMinArraySize(int minArraySize) {
        this.minArraySize = minArraySize;
        return this;
    }
    
    /**
     * フィールドの最大配列要素数を返却する。
     * @return フィールドの最大配列要素数
     */
    public int getMaxArraySize() {
        return maxArraySize;
    }
    
    /**
     * フィールドの最大配列要素数を設定する。
     * @param maxArraySize フィールドの最大配列要素数
     * @return このオブジェクト自体
     */
    public FieldDefinition setMaxArraySize(int maxArraySize) {
        this.maxArraySize = maxArraySize;
        return this;
    }
    
    /**
     * このフィールドの開始位置を設定する。
     * @param position 開始位置
     * @return このオブジェクト自体
     */
    public FieldDefinition setPosition(int position) {
        this.position = position;
        return this;
    }
    
    /**
     * このフィールドの開始位置を返却する。
     * @return 開始位置
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * このフィールドの長さを返却する。
     * <pre>
     * バイナリモード：バイト長
     * キャラクタモード：1
     * </pre>
     * @return フィールドの長さ
     */
    public int getSize() {
        if (dataType == null) {
            throw new IllegalStateException("data type was not set. data type must be set before run this method.");
        }
        return dataType.getSize(); 
    }
    /**
     * コンバータを追加する。
     * nullが渡された場合は何もしない。
     * @param convertor 追加するコンバータ
     * @return このオブジェクト自体
     */
    public FieldDefinition addConvertor(ValueConvertor<?, ?> convertor) {
        if (convertor != null) {
            convertors.add(convertor);
        }
        return this;
    }
    
    /**
     * コンバータのリストを取得する。
     * @return コンバータのリスト
     */
    @SuppressWarnings("rawtypes")
    public List<ValueConvertor> getConvertors() {
        return convertors;
    }
    
    /**
     * フォーマット定義ファイルで指定されたデータタイプ名に対応するデータタイプを設定する。
     * @param dataType データタイプ名
     * @return このオブジェクト自体
     */
    public FieldDefinition setDataType(DataType<?, ?> dataType) {
        this.dataType = dataType;
        return this;
    }
    
    /**
     * フォーマット定義ファイルで指定されたデータタイプ名に対応するデータタイプを取得する。
     * @return データタイプ
     */
    public DataType<?, ?> getDataType() {
        return dataType;
    }
    
}
