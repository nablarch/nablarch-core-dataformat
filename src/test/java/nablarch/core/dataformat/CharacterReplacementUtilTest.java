package nablarch.core.dataformat;

import nablarch.core.ThreadContext;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link CharacterReplacementUtil}のテストを行います。
 * 
 * @author Kohei Sawaki
 */
public class CharacterReplacementUtilTest {

    /** CharacterReplacementResultのキー */
    private static final String REPLACEMENT_RESULT_KEY = "REPLACEMENT_RESULT";
	
    @Before
    public void setUp() {
    	ThreadContext.clear();
    }
    
    /**
     * スレッドローカルへの設定をテストする。<br>
     * 
     * 条件：<br>
     *   スレッドローカルの変換前後の項目に設定する。<br>
     *   
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testSetResult() throws Exception {
    
        // 入力値設定
        String fieldName = "fieldName";
        String inputString = "変換前文字列";
        String resultString = "変換後文字列";

        // テスト実行
        CharacterReplacementUtil.setResult(fieldName, inputString, resultString);
        Map<String, CharacterReplacementResult> resultData = (Map<String, CharacterReplacementResult>) ThreadContext.getObject(REPLACEMENT_RESULT_KEY);
        
        // 期待結果設定
        String expectedInputString = "変換前文字列";
        String expectedResultString = "変換後文字列";
        
        // 結果検証
        assertEquals(expectedInputString, resultData.get(fieldName).getInputString());
        assertEquals(expectedResultString, resultData.get(fieldName).getResultString());
        assertEquals(true, resultData.get(fieldName).isReplacement());
        
    }

    /**
     * スレッドローカルへの設定をテストする。<br>
     * 
     * 条件：<br>
     *   スレッドローカルにnullを設定する。<br>
     *   
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testSetResultNull() throws Exception {
    
        // 入力値設定
        String fieldName = "fieldName";
        String inputString = "変換前文字列";
        String resultString = "変換後文字列";

        // テスト実行
        CharacterReplacementUtil.setResult(fieldName, inputString, resultString);
        Map<String, CharacterReplacementResult> resultData = (Map<String, CharacterReplacementResult>) ThreadContext.getObject(REPLACEMENT_RESULT_KEY);
    
        // 期待結果設定
        String expectedInputString = "変換前文字列";
        String expectedResultString = "変換後文字列";
        
        // 結果検証
        assertEquals(expectedInputString, resultData.get(fieldName).getInputString());
        assertEquals(expectedResultString, resultData.get(fieldName).getResultString());
        assertEquals(true, resultData.get(fieldName).isReplacement());
    	
    }
    
    /**
     * スレッドローカルへの設定をテストする。<br>
     * 
     * 条件：<br>
     *   スレッドローカルにEmptyを設定する。<br>
     *   
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testSetResultEmpty() throws Exception {
    	
    	// 入力値設定
        Map<String, CharacterReplacementResult> emptyMap = new HashMap<String, CharacterReplacementResult>();
        ThreadContext.setObject(REPLACEMENT_RESULT_KEY, emptyMap);

    	// 入力値設定
    	String fieldName = "fieldName";
    	String inputString = "変換前文字列";
    	String resultString = "変換後文字列";
        
    	// テスト実行
    	CharacterReplacementUtil.setResult(fieldName, inputString, resultString);
    	Map<String, CharacterReplacementResult> resultData = (Map<String, CharacterReplacementResult>) ThreadContext.getObject(REPLACEMENT_RESULT_KEY);
    	
    	// 期待結果設定
        String expectedInputString = "変換前文字列";
        String expectedResultString = "変換後文字列";
        
        // 結果検証
        assertEquals(expectedInputString, resultData.get(fieldName).getInputString());
        assertEquals(expectedResultString, resultData.get(fieldName).getResultString());
        assertEquals(true, resultData.get(fieldName).isReplacement());
    	
    }
    
    /**
     * スレッドローカルからの取得をテストする。<br>
     * 
     * 条件：<br>
     *   変換前後の文字列を設定する。<br>
     *   
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testGetResult() throws Exception {
    	
    	// 条件設定
    	String fieldName = "field";
        String expectedInputString = "変換前文字列";
        String expectedResultString = "変換後文字列";
        
    	CharacterReplacementResult expectedData = new CharacterReplacementResult(expectedInputString, expectedResultString);
        
        Map<String, CharacterReplacementResult> expectedMap = new HashMap<String, CharacterReplacementResult>();
        expectedMap.put(fieldName, expectedData);
        ThreadContext.setObject(REPLACEMENT_RESULT_KEY, expectedMap);

    	// テスト実行
        CharacterReplacementResult resultData = CharacterReplacementUtil.getResult(fieldName);
        
        // 結果検証
        assertEquals(expectedInputString, resultData.getInputString());
        assertEquals(expectedResultString, resultData.getResultString());
        assertEquals(true, resultData.isReplacement());
    	
    }
    
    /**
     * スレッドローカルからの取得をテストする。<br>
     * 
     * 条件：<br>
     *   変換前後の文字列を設定する。<br>
     *   
     * 期待結果：<br>
     *   想定どおりの結果が返却されること。<br>
     */
    @Test
    public void testGetResultNull() throws Exception {
    	
    	// 条件設定
    	String fieldName = "field";
    	
    	// テスト実行
        CharacterReplacementResult resultData = CharacterReplacementUtil.getResult(fieldName);
        
        // 結果検証
        assertEquals(null, resultData);
    }
    
}
