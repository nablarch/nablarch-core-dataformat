package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link XmlDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class XmlDataBuilderTest  {

    @Rule
    public TestName testNameRule = new TestName();
    
    private StructuredDataBuilder builder = createBuilder();
    
    /**
     * テストケースごとにデフォルト設定でリポジトリを再構築します。
     */
    @Before
    public void setUp() {
        SystemRepository.clear();
        SystemRepository.load(
                new DiContainer(
                        new XmlComponentDefinitionLoader("nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml")));
    }
    
    /**
     * 本クラスではXMLのビルダーを作成します。
     * @return XMLのビルダー
     */
    protected StructuredDataBuilder createBuilder() {
        return new XmlDataBuilder();
    }
    
    /**
     * 本クラスではXMLのフォーマッターを作成します。
     * @return XMLのフォーマッター
     */
    protected DataRecordFormatter createFormatter() {
        return new XmlDataRecordFormatter();
    }
    
    /**
     * フォーマット定義ファイル名を取得します
     * @return
     */
    protected String getFormatFileName() {
        FilePathSetting fps = FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("fortmat", "fmt");
        return Builder.concat(
                   fps.getBasePathSettings().get("format").getPath(),
                   "/", testNameRule.getMethodName(), ".", 
                   fps.getFileExtensions().get("format")
               );
        
    }

    /**
     * フォーマット定義情報を取得します。
     * @return フォーマット定義情報
     */
    protected LayoutDefinition getLayoutDefinition() {
        LayoutDefinition ld = new LayoutFileParser(getFormatFileName()).parse();
        DataRecordFormatter formatter = createFormatter();
        formatter.setDefinition(ld);
        formatter.initialize();
        return ld;
    }
    
    /**
     * 文字セットにUTF-8を使用してデータの構築を行います。
     * @param data 構築対象データ
     * @param charset 文字セット
     * @return 構築された文字列
     * @throws IOException 解析に失敗した場合
     */
    protected String buildData(Map<String, ?> data, String charset) throws IOException {
        if (charset == null) {
            charset = "UTF-8";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        builder.buildData(data, getLayoutDefinition(), baos);
        return baos.toString(charset);
    }
    
    /**
     * 改行と行頭行末の空白を除去します。
     * @param str 対象文字列
     * @return 改行と行頭行末の空白を除去した文字列
     */
    protected String removeLineSeparatorAndWhiteSpace(String str) {
        BufferedReader br = new BufferedReader(new StringReader(str));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // not happen
        }
        return sb.toString();
    }

    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedString 期待結果
     */
    protected void testNormalCase(Map<String, ?> target, String expectedString) throws Exception {
        expectedString = removeLineSeparatorAndWhiteSpace(expectedString);
        testNormalCase(target, expectedString, null);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedString 期待結果
     * @param charset 文字コード
     */
    protected void testNormalCase(Map<String, ?> target, String expectedString, String charset) throws Exception {
        expectedString = removeLineSeparatorAndWhiteSpace(expectedString);
        normalBuildTest(target, expectedString, charset);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(Map<String, ?> target, String expectedErrorMessage) throws Exception {
        abnormalBuildTest(target, expectedErrorMessage);
    }
    
    /**
     * 正常系構築テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     * @param charset 文字セット
     */
    private void normalBuildTest(Map<String, ?> target, String expected, String charset) throws Exception {
        // 構築テスト
        String resultString = buildData(target, charset);
        assertNotNull(resultString);
        assertEquals(expected, resultString);
    }
    
    /**
     * 異常系構築テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void abnormalBuildTest(Map<String, ?> target, String expectedErrorMessage) throws Exception {
        try {
            // 構築テスト
            normalBuildTest(target, null, null);
            
            fail("例外InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedErrorMessage));
        }
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_array() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：任意項目の後ろに必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key3", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key3>value3</key3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目(先頭に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key2>value2</key2>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目(途中に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key3", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key3>value3</key3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目(最後に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key4", "value4");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key4>value4</key4>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
        5 key5 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key5", "value5");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key5>value5</key5>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した任意項目(１つだけ値あり)の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_1_7() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        3 key3 [0..1] X
        4 key4 [0..1] X
        5 key5 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key3", "value3");
                    put("key5", "value5");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key3>value3</key3>
          <key5>value5</key5>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key2>value2</key2>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key2", "value2");
                }}
        ;
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field key1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1</key1>
          </child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", "value1");
                    put("child.key2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1</key1>
            <key2>value2</key2>
          </child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key2", "value2");
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field key1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_7() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", "value1");
                }}
        ;
       
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
          </child>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素自体かつ後ろに必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 X
        [child1]
        1 key X
        [child2]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child3", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child3>value3</child3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目(全て省略)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目(先頭に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child2.key", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child2>
            <key>value2</key>
          </child2>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目(途中に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child3.key", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child3>
            <key>value3</key>
          </child3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目(最後に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child4.key", "value4");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child4>
            <key>value4</key>
          </child4>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        5 child5 OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
        [child5]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child5.key", "value5");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child5>
            <key>value5</key>
          </child5>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：連続した値なしの任意項目(１つだけ値あり)の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_7() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 child3 [0..1] OB
        4 child4 [0..1] OB
        5 child5 OB
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        [child4]
        1 key X
        [child5]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child3.key", "value3");
                    put("child5.key", "value5");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child3>
            <key>value3</key>
          </child3>
          <child5>
            <key>value5</key>
          </child5>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素の最後に任意項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_8() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        2 child2_1 [0..1] OB
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素の最後に任意項目、その後ネスト要素内に必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_9() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        2 child2_1 [0..1] OB
        3 key3 X
        [child2_1]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child2.key", "value2");
                    put("child2.key3", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child2>
            <key>value2</key>
            <key3>value3</key3>
          </child2>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素の最後に任意項目、その後ネスト要素外に必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_8_10() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        3 key3 X
        [child1]
        1 key X
        [child2]
        1 key X
        2 child2_1 [0..1] OB
        [child2_1]
        1 key X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child2.key", "value2");
                    put("key3",       "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child2>
            <key>value2</key>
          </child2>
          <key3>value3</key3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_9() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child2.key", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
          <child2>
            <key>value2</key>
          </child2>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：必須/任意項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_10() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child1 OB
        2 child2 [0..1] OB
        [child1]
        1 key X
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child2.key", "value2");
                }}
        ;
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field child1 is required");
        testInvalidDataFormatCase(null, "Field child1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_11() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("attr1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data attr1="value1">
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_12() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("attr1", "value1");
                    put("attr2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data attr1="value1" attr2="value2">
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_13() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("attr2", "value2");
                }}
        ;

        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field attr1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_14() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.attr1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child attr1="value1"></child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_15() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.attr1", "value1");
                    put("child.attr2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child attr1="value1" attr2="value2"></child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_16() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.attr2", "value2");
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field attr1 is required");
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：属性項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_17() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child", "hoge");
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field child is Object but specified by Attribute");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_18() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1","value1-2","value1-3"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
          <key1>value1-2</key1>
          <key1>value1-3</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：任意配列項目の後ろに必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key2", "value2");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key2>value2</key2>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
        4 key4 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key4", "value4");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key4>value4</key4>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目(先頭に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1","value1-2","value1-3"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
          <key1>value1-2</key1>
          <key1>value1-3</key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目(途中に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key2", new String[]{"value2-1","value2-2","value2-3"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key2>value2-1</key2>
          <key2>value2-2</key2>
          <key2>value2-3</key2>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目(最後に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key3", new String[]{"value3-1","value3-2","value3-3"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key3>value3-1</key3>
          <key3>value3-2</key3>
          <key3>value3-3</key3>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目(１つだけ値あり)の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
        4 key4 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key4", "value4");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key4>value4</key4>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した任意配列項目(１つだけ値あり)の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_19_7() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        2 key2 [0..3] X
        3 key3 [0..3] X
        4 key4 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key2", new String[]{"value2-1","value2-2","value2-3"});
                    put("key4", "value4");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key2>value2-1</key2>
          <key2>value2-2</key2>
          <key2>value2-3</key2>
          <key4>value4</key4>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_20() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1","value1-2","value1-3","value1-4"});
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = ,FieldName=key1:MinCount=1:MaxCount=3:Actual=4");
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_21() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        2 key2 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key2", new String[]{});
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field key1 is required");
        
        // データ異常系テスト実施
        targetMap = null;
        testInvalidDataFormatCase(targetMap, "Field key1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_22() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", new String[]{"value1-1"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1-1</key1>
          </child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_23() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", new String[]{"value1-1","value1-2","value1-3"});
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1-1</key1>
            <key1>value1-2</key1>
            <key1>value1-3</key1>
          </child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_24() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", new String[]{"value1-1","value1-2","value1-3","value1-4"});
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = child,FieldName=key1:MinCount=1:MaxCount=3:Actual=4");
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素内<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_25() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        2 key2 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child.key2", new String[]{"value2-1"});
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Field key1 is required");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：任意ネスト配列項目自体かつ後ろに必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 key1 X
        [child]
        1 key X
        [child1]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目(全て省略)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目(先頭に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child1[0].key", "value1-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child1><key>value1-1</key></child1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目(途中に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child2[0].key", "value2-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child2><key>value2-1</key></child2>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目(最後に値あり)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child3[0].key", "value3-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child3><key>value3-1</key></child3>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        5 key X
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <key>value1</key>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：連続した値なしの任意ネスト配列項目の後に必須項目あり<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_7() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 child2 [0..3] OB
        4 child3 [0..3] OB
        5 key X
        [child]
        1 key X
        [child1]
        1 key X
        [child2]
        1 key X
        [child3]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child2[0].key", "value2-1");
                    put("key", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child2><key>value2-1</key></child2>
          <key>value1</key>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：任意ネスト配列項目内の要素の最後に任意項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_8() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key1 X
        2 child2 [0..3] OB
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child1[0].key1", "value1-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child1><key1>value1-1</key1></child1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：任意ネスト配列項目内の要素の最後に任意項目、その後ネスト要素内に必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_9() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        [child]
        1 key X
        [child1]
        1 key1 X
        2 child2 [0..3] OB
        3 key3 X
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child1[0].key1", "value1-1");
                    put("child1[0].key3", "value1-3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child1>
            <key1>value1-1</key1>
            <key3>value1-3</key3>
          </child1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：任意ネスト配列項目内の要素の最後に任意項目、その後ネスト要素外に必須項目<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_26_10() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        2 child1 [0..3] OB
        3 key1 X
        [child]
        1 key X
        [child1]
        1 key X
        2 child2 [0..3] OB
        [child2]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_27() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child[1].key", "value2");
                    put("child[2].key", "value3");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child><key>value2</key></child>
          <child><key>value3</key></child>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_28() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child[1].key", "value2");
                    put("child[2].key", "value3");
                    put("child[3].key", "value4");
                }}
        ;
       
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=4");
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_29() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                }}
        ;

        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
        testInvalidDataFormatCase(null, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_30() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                }}
        ;

        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
        testInvalidDataFormatCase(null, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
    }
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：データ構造<br>
     * 中項目：配列項目指定<br>
     * 小項目：ネスト要素自体<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase2_31() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("child", "hoge");
                }}
        ;

        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
        testInvalidDataFormatCase(null, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：データ型<br>
     * 小項目：正常値設定<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [0..1] X
        2 widestring  [0..1] N
        3 mixstring   [0..1] XN
        4 unsignedint [0..1] X9
        5 signedint   [0..1] SX9
        6 unsignedflt [0..1] X9
        7 signedflt   [0..1] SX9
        8 unsignedexp [0..1] X9
        9 signedexp1  [0..1] SX9
        10 signedexp2 [0..1] SX9
        11 bool       [0..1] BL
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("halfstring", "abcde1234");
                    put("widestring", "あいうえお１２３４");
                    put("mixstring", "abcdeあいうえお1234");
                    put("unsignedint", "100");
                    put("signedint", "-200");
                    put("unsignedflt", "100.23");
                    put("signedflt", "-200.45");
                    put("unsignedexp", "100.23e5");
                    put("signedexp1", "-200.45e+5");
                    put("signedexp2", "-200.45e-5");
                    put("bool", "true");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <halfstring>abcde1234</halfstring>
          <widestring>あいうえお１２３４</widestring>
          <mixstring>abcdeあいうえお1234</mixstring>
          <unsignedint>100</unsignedint>
          <signedint>-200</signedint>
          <unsignedflt>100.23</unsignedflt>
          <signedflt>-200.45</signedflt>
          <unsignedexp>100.23e5</unsignedexp>
          <signedexp1>-200.45e+5</signedexp1>
          <signedexp2>-200.45e-5</signedexp2>
          <bool>true</bool>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：データ型<br>
     * 小項目：null設定<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [0..1] X
        2 widestring  [0..1] N
        3 mixstring   [0..1] XN
        4 unsignedint [0..1] X9
        5 signedint   [0..1] SX9
        6 unsignedflt [0..1] X9
        7 signedflt   [0..1] SX9
        8 bool        [0..1] BL
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("halfstring", "");
                    put("widestring", "");
                    put("mixstring", "");
                    put("unsignedint", "");
                    put("signedint", "");
                    put("unsignedflt", "");
                    put("signedflt", "");
                    put("bool", "");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <halfstring></halfstring>
          <widestring></widestring>
          <mixstring></mixstring>
          <unsignedint></unsignedint>
          <signedint></signedint>
          <unsignedflt></unsignedflt>
          <signedflt></signedflt>
          <bool></bool>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：データ型<br>
     * 小項目：null設定<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_2_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [0..1] X
        2 widestring  [0..1] N
        3 mixstring   [0..1] XN
        4 unsignedint [0..1] X9
        5 signedint   [0..1] SX9
        6 unsignedflt [0..1] X9
        7 signedflt   [0..1] SX9
        8 bool        [0..1] BL
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("halfstring", null);
                    put("widestring", null);
                    put("mixstring", null);
                    put("unsignedint", null);
                    put("signedint", null);
                    put("unsignedflt", null);
                    put("signedflt", null);
                    put("bool", null);
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <halfstring></halfstring>
          <widestring></widestring>
          <mixstring></mixstring>
          <unsignedint></unsignedint>
          <signedint></signedint>
          <unsignedflt></unsignedflt>
          <signedflt></signedflt>
          <bool></bool>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：空データ<br>
     * 小項目：<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_9() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [0..1] X
        2 widestring  [0..1] N
        3 mixstring   [0..1] XN
        4 unsignedint [0..1] X9
        5 signedint   [0..1] SX9
        6 unsignedflt [0..1] X9
        7 signedflt   [0..1] SX9
        8 bool        [0..1] BL
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = null;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data></data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：コンバータ<br>
     * 小項目：リテラルコンバータ<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_14() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X "def"
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：コンバータ<br>
     * 小項目：リテラルコンバータ<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_14_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..1] X "def"
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>();
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>def</key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：コンバータ<br>
     * 小項目：テスト用エラーコンバータ<br>
     * 
     * 【期待結果】<br>
     * 解析および構築が失敗する<br>
     */
    @Test
    public void testCase3_15() throws Exception {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("temp/XmlParserTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- FormatterFactoryの設定 -->
            <component name="xmlDataConvertorSetting"
                class="nablarch.core.dataformat.convertor.XmlDataConvertorSetting">
                <property name="convertorTable">
                    <map>
                        <entry key="X" value="nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString"/>
                        <entry key="Error" value="nablarch.core.dataformat.StructuredDataParserSupportTest$ErrorConverter"/>
                    </map>
                </property>
            </component>
                     
        </component-configuration>
        */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));
        
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X error
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(targetMap, "DummyException field name=[key1].");
        
        SystemRepository.clear();
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：データ型<br>
     * 小項目：数値コンバータ<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_16() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 unsignedint [0..1] X9  number
        2 signedint   [0..1] SX9 signed_number
        3 unsignedflt [0..1] X9  number
        4 signedflt   [0..1] SX9 signed_number
        5 unsignedexp [0..1] X9  number
        6 signedexp   [0..1] SX9 signed_number
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("unsignedint", new BigDecimal("100"));
                    put("signedint", new BigDecimal("-200"));
                    put("unsignedflt", new BigDecimal("1.23"));
                    put("signedflt", new BigDecimal("-2.45"));
                    put("unsignedexp", new BigDecimal("1.23E+5"));
                    put("signedexp", new BigDecimal("-2.45E+5"));
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <unsignedint>100</unsignedint>
          <signedint>-200</signedint>
          <unsignedflt>1.23</unsignedflt>
          <signedflt>-2.45</signedflt>
          <unsignedexp>123000</unsignedexp>
          <signedexp>-245000</signedexp>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    
    /**
     * PJ要件XMLの構築テスト
     */
    @Test
    public void testCase4_0() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [xxx:tag01]
        1 ?@xmlns:xxx X "http://xxx.yyy.zzz/tag02/xmlns"
        2 xxx:tag02 OB
        [xxx:tag02]
        1 xxx:tag03 OB
        [xxx:tag03]
        1 xxx:tag04 X
        2 xxx:tag05 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("xxxTag02.xxxTag03.xxxTag04", "0000");
                    put("xxxTag02.xxxTag03.xxxTag05", "1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <xxx:tag01 xmlns:xxx="http://xxx.yyy.zzz/tag02/xmlns">
            <xxx:tag02>
                <xxx:tag03>
                    <xxx:tag04>0000</xxx:tag04>
                    <xxx:tag05>1</xxx:tag05>
                </xxx:tag03>
            </xxx:tag02>
        </xxx:tag01>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：単一ネームスペース<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_1() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 testns:key1 X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply">
          <testns:key1>value1</testns:key1>
        </testns:data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：複数ネームスペース１（ルート要素に全てのネームスペースを定義）<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_2() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 ?@xmlns:testns2 X "http://testns2.hoge.jp/apply"
        3 testns:key1 X
        4 testns2:key1 X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1-1");
                    put("testns2Key1", "value2-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply" xmlns:testns2="http://testns2.hoge.jp/apply">
          <testns:key1>value1-1</testns:key1>
          <testns2:key1>value2-1</testns2:key1>
        </testns:data>
        *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：複数ネームスペース２（子要素にもネームスペースを定義）<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_3() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 testns:key1 X
        3 testns:child OB
        [testns:child]
        1 ?@xmlns:testns2 X "http://testns2.hoge.jp/apply"
        2 testns:key1 X
        3 testns2:key1 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1-1");
                    put("testnsChild.testnsKey1", "value1-1");
                    put("testnsChild.testns2Key1", "value2-1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply">
          <testns:key1>value1-1</testns:key1>
          <testns:child xmlns:testns2="http://testns2.hoge.jp/apply">
            <testns:key1>value1-1</testns:key1>
            <testns2:key1>value2-1</testns2:key1>
          </testns:child>
        </testns:data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：デフォルトネームスペース１（デフォルトネームスペースのみ）<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_4() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns X "http://defns.hoge.jp/apply"
        2 key1 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply">
          <key1>value1</key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：デフォルトネームスペース２（デフォルトネームスペースと名前付きネームスペースの混在１）<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_5() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns X "http://defns.hoge.jp/apply"
        2 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        3 key1 X
        4 testns:key1 X
        *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value-def-1");
                    put("testnsKey1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply" xmlns:testns="http://testns.hoge.jp/apply">
          <key1>value-def-1</key1>
          <testns:key1>value1</testns:key1>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：デフォルトネームスペース２（デフォルトネームスペースと名前付きネームスペースの混在２）<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_6() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns X "http://defns.hoge.jp/apply"
        2 key1 X
        3 child OB
        [child]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 testns:key1 X
         *******/
        formatFile.deleteOnExit();
        
        // 変換対象データ
        Map<String, Object> targetMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value-def-1");
                    put("child.testnsKey1", "value1");
                }}
        ;
        
        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply">
          <key1>value-def-1</key1>
          <child xmlns:testns="http://testns.hoge.jp/apply">
            <testns:key1>value1</testns:key1>
          </child>
        </data>
         *********************************************/
        
        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

    /**
     * {@link BigDecimal}を書き込む場合のテスト
     * @throws Exception
     */
    @Test
    public void testWriteBigDecimal() throws Exception {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
         file-type:        "XML"
         text-encoding:    "UTF-8"
         [data]
         1 key1 X
         *******/
        formatFile.deleteOnExit();

        // 変換対象データ
        Map<String, Object> targetMap = Collections.<String, Object>singletonMap("key1", new BigDecimal("0.0000000001"));

        // 期待結果
        String expectedString = Hereis.string();
        /*******************************************
         <?xml version="1.0" encoding="UTF-8"?>
         <data>
         <key1>0.0000000001</key1>
         </data>
         *********************************************/

        // 正常系テスト実施
        testNormalCase(targetMap, expectedString);
    }

}