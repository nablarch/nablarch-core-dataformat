package nablarch.core.dataformat;

import java.util.Arrays;
import java.util.Map;

import nablarch.core.util.StringUtil;

/**
 * データレコードに対する真偽条件を表すクラス。
 * 
 * @author Iwauo Tajima
 */
public interface DataRecordPredicate {
    /**
     * 渡されたレコードが条件を満たすかどうかを返却する。
     * @param record 検査対象のレコード
     * @param checkUnsetValues 未設定項目に対する検証をスキップするかどうか
     * @return 条件を満たす場合はtrue
     * @throws IllegalArgumentException
     *     渡されたレコードがnullであったり、レコード中に検査対象のフィールドが
     *     存在しない等、条件を満たすかどうかを判定することが不可能な場合
     */
    boolean apply(Map<String, ?> record, boolean checkUnsetValues)
    throws IllegalArgumentException;
    

    /**
     * データレコード中の特定のフィールドの内容が、
     * 指定された値と一致することを表すPredicate。
     */
    public static class Equals implements DataRecordPredicate {
        
        /** 検証対象のフィールド名  */
        private final String   fieldName;
        /** 検証値 */
        private final Object[] expectingValues;
        
        /**
         * コンストラクタ
         * @param fieldName       検証対象のフィールド名
         * @param expectingValues 検証値（複数指定した場合はいずれかが一致すればOK)
         */
        public Equals(String fieldName, Object... expectingValues) {
            this.fieldName       = fieldName;
            this.expectingValues = expectingValues;
        }
        
        /** {@inheritDoc} */
        public boolean apply(Map<String, ?> record, boolean checkUnsetValues)
        throws IllegalArgumentException {
            if (!checkUnsetValues && record.get(fieldName) == null) {
                return true;
            }
            Object actual = record.get(fieldName);
            for (Object expecting : expectingValues) {
                if (actual == null) {
                    continue;
                }
                if (expecting.toString().equals(StringUtil.toString(actual))) {
                    return true;
                }
            }
            return false;
        }
        
        /** {@inheritDoc} */
        public String toString() {
            return fieldName + " = " + Arrays.toString(expectingValues);
        }
    }
}

