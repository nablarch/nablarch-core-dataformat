package nablarch.core.dataformat;

import java.util.HashMap;
import java.util.Map;

import nablarch.core.ThreadContext;
import nablarch.core.util.annotation.Published;

/**
 * 文字列置換結果を取得・設定するユーティリティクラス。
 * <p>
 * {@link nablarch.core.dataformat.convertor.value.CharacterReplacer}で置き換えられた情報を保持する。
 * 置き換え情報はスレッド毎に管理されるため、他のスレッドで行われた結果を取得することはできない。<br/>
 *
 * @author Kohei Sawaki
 *
 */
public final class CharacterReplacementUtil {

    /**
     * 隠蔽コンストラクタ。
     */
    private CharacterReplacementUtil() {

    }

    /** {@link CharacterReplacementResult}のキー */
    private static final String REPLACEMENT_RESULT_KEY = "REPLACEMENT_RESULT";

    /**
     * カレントスレッド上で行われたフィールドに対する置き換え結果を取得する。
     *
     * @param fieldName フィールド名
     * @return 置換結果（置換対象外のフィールドを指定した場合はnull）
     */
    @Published
    public static CharacterReplacementResult getResult(String fieldName) {
        Map<String, CharacterReplacementResult> resultMap = (Map<String, CharacterReplacementResult>) ThreadContext.getObject(REPLACEMENT_RESULT_KEY);
        return (resultMap == null) ? null
                : resultMap.get(fieldName);
    }

    /**
     * フィールドに対する置き換え結果をカレントスレッド上の{@link CharacterReplacementResult}に設定する。
     *
     * @param fieldName フィールド名
     * @param inputString 置換前文字列
     * @param resultString 置換後文字列
     */
    @Published(tag = "architect")
    public static void setResult(String fieldName, String inputString, String resultString) {
        CharacterReplacementResult result = new CharacterReplacementResult(inputString, resultString);

        Map<String, CharacterReplacementResult> resultMap = (Map<String, CharacterReplacementResult>) ThreadContext.getObject(REPLACEMENT_RESULT_KEY);
        if (resultMap == null ||  resultMap.isEmpty()) {
            resultMap = new HashMap<String, CharacterReplacementResult>();
        }
        resultMap.put(fieldName, result);
        ThreadContext.setObject(REPLACEMENT_RESULT_KEY, resultMap);
    }

}
