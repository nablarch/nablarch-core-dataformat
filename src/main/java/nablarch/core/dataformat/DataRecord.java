package nablarch.core.dataformat;

import java.math.BigDecimal;

import nablarch.core.util.NumberUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.MultipleKeyCaseMap;

/**
 * データレコード1件分の内容を格納するクラス。
 * <p/>
 * 各フィールドの値に{@link java.util.Map}インタフェースを通じてアクセスできる。<br/>
 * 各フィールドの値には、 コンバータによって変換した何れかの型、もしくは{@code null}が格納される。
 * 
 * @author Iwauo Tajima
 */
@Published
public class DataRecord extends MultipleKeyCaseMap<Object> {
    // -------------------------------------------- structure    
    /** 本レコードのレコードタイプ。 */
    private String recordType;
    
    /** 本レコードのレコード番号。 */
    private int recordNumber;
    
    // -------------------------------------------- accessors
    /**
     * 本レコードのレコードタイプを返却する。
     * @return レコードの種別
     */
    public String getRecordType() {
        return recordType;
    }
    
    /**
     * レコードタイプを設定する。
     * @param recordType レコードタイプ
     * @return 本オブジェクト
     */
    public DataRecord setRecordType(String recordType) {
        this.recordType = recordType;
        return this;
    }

    /**
     * 指定されたフィールドの値を返却する。
     *
     * @param <T> 値の型
     * @param key フィールド名称
     * @return フィールドの値
     * @throws ClassCastException 指定した型が実際の型と整合しなかった場合
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Object key) throws ClassCastException {
        return (T) get(key);
    }
    
    /**
     * フィールドの値を設定する。
     * <p/>
     * BigDecimal / String / String[] / byte[] 型のインスタンスはそのまま保持する。<br/>
     * BigDecimal 以外の Number型は、BigDecimalに変換した上で保持する。<br/>
     * それ以外の型のインスタンスは、toString()メソッドの結果を文字列として保持する。
     *
     * @param fieldName  フィールド名
     * @param fieldValue 設定する値
     * @return 指定したフィールド名の元の値（指定したフィールドが存在しない場合は{@code null}）
     * @throws IllegalArgumentException フィールド名が{@code null}または空文字の場合
     */
    @Override
    public Object put(String fieldName, Object fieldValue) {
        if (fieldName == null || fieldName.length() == 0) {
            throw new IllegalArgumentException("field name was blank. field name must not be blank.");
        }
        fieldValue = (fieldValue == null)                 ? null 
                   : (fieldValue instanceof BigDecimal)   ? fieldValue
                   : (fieldValue instanceof byte[])       ? fieldValue
                   : (fieldValue instanceof Number)       ? new BigDecimal(fieldValue.toString())
                   : (fieldValue instanceof String[])     ? fieldValue // ADD
                   : fieldValue.toString();
        return super.put(fieldName, fieldValue);
    }
    
    /**
     * フィールドの値を文字列型に変換して返却する。
     *
     * @param fieldName フィールド名
     * @return フィールドの値（指定したフィールドが存在しない場合は{@code null}）
     */
    public String getString(String fieldName) {
        Object value = get(fieldName);
        return (value == null) ? null : StringUtil.toString(value);
    }
    
    /**
     * フィールドの値を文字列配列型に変換して返却する。
     *
     * @param fieldName フィールド名
     * @return フィールドの値（指定したフィールドが存在しない場合は{@code null}）
     * @throws ClassCastException 指定したフィールドの型がString[]でなかった場合
     */
    public String[] getStringArray(String fieldName) {
        Object value = get(fieldName);
        return (value == null) ? null
                               : (String[]) value;
    }
    
    /**
     * フィールドの値をBigDecimal型に変換して返却する。
     * 
     * @param fieldName フィールド名
     * @return フィールドの値（指定したフィールドが存在しない場合は{@code null}）
     * @throws NumberFormatException
     *          指定したフィールドの値がBigDecimalに変換できなかった場合
     */
    public BigDecimal getBigDecimal(String fieldName)
    throws NumberFormatException {
        Object value = get(fieldName);
        if (value == null) {
            return null;
        } else {
            final BigDecimal result = value instanceof BigDecimal ? BigDecimal.class.cast(value) : new BigDecimal(
                    value.toString());
            NumberUtil.verifyBigDecimalScale(result);
            return result;
        }
    }
    
    /**
     * フィールドの値をバイト列に変換して返却する。
     *
     * @param fieldName フィールド名
     * @return フィールドに格納されているバイト列（指定したフィールドが存在しない場合は{@code null}）
     * @throws ClassCastException 指定したフィールドの型がbyte[]でなかった場合
     */
    public byte[] getBytes(String fieldName) throws ClassCastException {
        Object value = get(fieldName);
        return (value == null) ? null
                               : (byte[]) value;
    }
    
    /**
     * 本レコードのレコード番号を取得する。
     *
     * @return 本レコードのレコード番号
     */
    public int getRecordNumber() {
        return recordNumber;
    }

    /**
     * 本レコードのレコード番号を設定する。
     *
     * @param recordNumber 本レコードのレコード番号
     * @return 本オブジェクト
     */
    public DataRecord setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
        return this;
    }

}
