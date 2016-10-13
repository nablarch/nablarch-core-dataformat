package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * {@link XmlDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class XmlDataParserTest  {

    @Rule
    public TestName testNameRule = new TestName();
    
    private StructuredDataParser parser = createParser();
    
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

    private File formatFile;

    @After
    public void deleteFormatFile() {
        formatFile.delete();
    }
    
    /**
     * 本クラスではXMLのパーサーを作成します。
     * @return XMLのパーサ－
     */
    protected StructuredDataParser createParser() {
        return new XmlDataParser();
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
     * @return ファイル名
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
     * 指定された文字セットを使用してデータの解析を行います。
     * @param data 解析対象データ
     * @param charset 文字セット
     * @return 解析されたマップ
     * @throws IOException 解析に失敗した場合
     */
    protected Map<String, ?> parseData(String data, String charset) throws IOException {
        if (charset == null) {
            charset = "UTF-8";
        }

        return parser.parseData(new ByteArrayInputStream(data.getBytes(charset)), getLayoutDefinition());
    }

    /**
     * マップの検証を行います。
     * @param expected 期待結果
     * @param actual 実行結果
     */
    protected void assertMap(Map<String, ?> expected, Map<String, ?> actual) {
        if (expected != null) {
            for(String key : expected.keySet()) {
                if (expected.get(key) instanceof String[]) {
                    assertArrayEquals("Error Key:[" + key + "]", (String[])expected.get(key), (String[])actual.get(key));
                } else {
                    assertEquals("Error Key:[" + key + "]", expected.get(key), actual.get(key));
                }
            }
        }
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap) throws Exception {
        normalParseTest(target, expectedMap);
    }

    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String target, String expectedErrorMessage) throws Exception {
        // 期待結果Mapはnull
        Map<String, ?> expectedMap = null;
        // 期待結果String
        abnormalParseTest(target, expectedMap, expectedErrorMessage);
    }
    
    /**
     * 正常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void normalParseTest(String target, Map<String, ?> expected) throws Exception {
        normalParseTest(target, expected, null);
    }
    
    /**
     * 正常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     * @param charset 文字セット
     */
    private void normalParseTest(String target, Map<String, ?> expected, String charset) throws Exception {
        // 解析テスト
        Map<String, ?> resultMap = parseData(target, charset);
        assertNotNull(resultMap);
        assertMap(expected, resultMap);
    }
    
    /**
     * 異常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void abnormalParseTest(String target, Map<String, ?> expected, String expectedErrorMessage) throws Exception {
        try {
            // 解析テスト
            normalParseTest(target, expected, null);
          
            fail("例外InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            String actualMsg = e.getMessage();
            assertThat(actualMsg, containsString(expectedErrorMessage));
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
          <key2>value2</key2>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key2>value2</key2>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field key1 is required");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1</key1>
          </child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1</key1>
            <key2>value2</key2>
          </child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", "value1");
                    put("child.key2", "value2");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key2>value2</key2>
          </child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field key1 is required");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
          </child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child1>
            <key>value1</key>
          </child1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        
        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child1.key", "value1");
                    put("child2.key", "value2");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child2>
            <key>value2</key>
          </child2>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field child1 is required");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data attr1="value1">
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("attr1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data attr1="value1" attr2="value2">
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("attr1", "value1");
                    put("attr2", "value2");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data
          attr2="value2">
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field attr1 is required");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child attr1="value1"></child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.attr1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child attr1="value1" attr2="value2"></child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.attr1", "value1");
                    put("child.attr2", "value2");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child attr2="value2"></child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field attr1 is required");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 @child OB
        [child]
        1 key1 X
        2 key2 [0..1] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data child="hoge">
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field child is Object but specified by Attribute");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1"});
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
          <key1>value1-2</key1>
          <key1>value1-3</key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", new String[]{"value1-1","value1-2","value1-3"});
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1-1</key1>
          <key1>value1-2</key1>
          <key1>value1-3</key1>
          <key1>value1-4</key1>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = ," +
                "FieldName=key1:MinCount=1:MaxCount=3:Actual=4");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        2 key2 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = ,FieldName=key1:MinCount=1:MaxCount=3:Actual=0");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1-1</key1>
          </child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", new String[]{"value1-1"});
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child.key1", new String[]{"value1-1","value1-2","value1-3"});
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <key1>value1-1</key1>
            <key1>value1-2</key1>
            <key1>value1-3</key1>
            <key1>value1-4</key1>
          </child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = child,FieldName=key1:MinCount=1:MaxCount=3:Actual=4");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        2 key2 [1..3] X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child></child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = child,FieldName=key1:MinCount=1:MaxCount=3:Actual=0");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child><key>value2</key></child>
          <child><key>value3</key></child>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("child[0].key", "value1");
                    put("child[1].key", "value2");
                    put("child[2].key", "value3");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child><key>value1</key></child>
          <child><key>value2</key></child>
          <child><key>value3</key></child>
          <child><key>value4</key></child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=4");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = ,FieldName=child:MinCount=1:MaxCount=3:Actual=0");
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        <child>hoge</child>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field key is required");
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
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
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
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
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
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
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
    public void testCase3_2_2() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [1..1] X
        2 widestring  [1..1] N
        3 mixstring   [1..1] XN
        4 unsignedint [1..1] X9
        5 signedint   [1..1] SX9
        6 unsignedflt [1..1] X9
        7 signedflt   [1..1] SX9
        8 bool        [1..1] BL
         *******/

        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
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
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：データ型<br>
     * 小項目：null設定<br>
     * 
     * 【期待結果】<br>
     * 解析が失敗する<br>
     */
    @Test
    public void testCase3_2_3() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [1..1] X
         *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field halfstring is required");
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：不正データ<br>
     * 小項目：構文エラー<br>
     * 
     * 【期待結果】<br>
     * 解析が失敗する<br>
     */
    @Test
    public void testCase3_3() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
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

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <halfstring>abcde1234</halfstring>
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "invalid data found");
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：不正データ<br>
     * 小項目：構文エラー<br>
     * 
     * 【期待結果】<br>
     * 解析が失敗する<br>
     */
    @Test
    public void testCase3_4() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
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

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <halfstr
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "invalid data found");
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data></data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = null;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X "def"
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..1] X "def"
         *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X error
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <key1>value1</key1>
        </data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "DummyException field name=[key1].");
        
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <unsignedint>100</unsignedint>
          <signedint>-200</signedint>
          <unsignedflt>1.23</unsignedflt>
          <signedflt>-2.45</signedflt>
          <unsignedexp>1.23E+5</unsignedexp>
          <signedexp>-2.45E+5</signedexp>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("unsignedint", new BigDecimal("100"));
                    put("signedint", new BigDecimal("-200"));
                    put("unsignedflt", new BigDecimal("1.23"));
                    put("signedflt", new BigDecimal("-2.45"));
                    put("unsignedexp", new BigDecimal("1.23E+5"));
                    put("signedexp", new BigDecimal("-2.45E+5"));
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }

    /**
     * PJ要件XMLの解析テスト
     */
    @Test
    public void testCase4_0() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [xxx:tag01]
        1 ?@xmlns:xxx X(10) "http://xxx.yyy.zzz/tag02/xmlns"
        2 xxx:tag02 OB
        [xxx:tag02]
        1 xxx:tag03 OB
        [xxx:tag03]
        1 xxx:tag04 X
        2 xxx:tag05 X
        3 xxx:tag06 X
        4 xxx:tag07 X
        5 xxx:tag08 X
        6 xxx:tag09 X
        7 xxx:tag10 X
        8 xxx:tag11 X
        9 xxx:tag12 X
        10 xxx:tag13 X
        11 xxx:tag14 X
        12 xxx:tag15 X
        13 xxx:tag16 X
         *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <xxx:tag01 xmlns:xxx="http://xxx.yyy.zzz/tag02/xmlns">
            <xxx:tag02>
                <xxx:tag03>
                    <xxx:tag04>20131231121230</xxx:tag04>
                    <xxx:tag05>1</xxx:tag05>
                    <xxx:tag06>90</xxx:tag06>
                    <xxx:tag07>abcdef1234567890abcdef1234567890</xxx:tag07>
                    <xxx:tag08>1234567890abcdef</xxx:tag08>
                    <xxx:tag09>1234</xxx:tag09>
                    <xxx:tag10>0123456789ABCDEF</xxx:tag10>
                    <xxx:tag11>英雄　太郎</xxx:tag11>
                    <xxx:tag12>ｴｲﾕｳ ﾀﾛｳ</xxx:tag12>
                    <xxx:tag13>19990101</xxx:tag13>
                    <xxx:tag14>東京都渋谷区　代々木１－１－２　Ｋ１００１</xxx:tag14>
                    <xxx:tag15>1010011</xxx:tag15>
                    <xxx:tag16>09010011001</xxx:tag16>
                </xxx:tag03>
            </xxx:tag02>
        </xxx:tag01>
        *********************************************/
        
        // 期待結果
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("xxxTag02.xxxTag03.xxxTag04", "20131231121230");
                    put("xxxTag02.xxxTag03.xxxTag05", "1");
                    put("xxxTag02.xxxTag03.xxxTag06", "90");
                    put("xxxTag02.xxxTag03.xxxTag07", "abcdef1234567890abcdef1234567890");
                    put("xxxTag02.xxxTag03.xxxTag08", "1234567890abcdef");
                    put("xxxTag02.xxxTag03.xxxTag09", "1234");
                    put("xxxTag02.xxxTag03.xxxTag10", "0123456789ABCDEF");
                    put("xxxTag02.xxxTag03.xxxTag11", "英雄　太郎");
                    put("xxxTag02.xxxTag03.xxxTag12", "ｴｲﾕｳ ﾀﾛｳ");
                    put("xxxTag02.xxxTag03.xxxTag13", "19990101");
                    put("xxxTag02.xxxTag03.xxxTag14", "東京都渋谷区　代々木１－１－２　Ｋ１００１");
                    put("xxxTag02.xxxTag03.xxxTag15", "1010011");
                    put("xxxTag02.xxxTag03.xxxTag16", "09010011001");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 testns:key1 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply">
          <testns:key1>value1</testns:key1>
        </testns:data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 ?@xmlns:testns2 X "http://testns2.hoge.jp/apply"
        3 testns:key1 X
        4 testns2:key1 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply" xmlns:testns2="http://testns2.hoge.jp/apply">
          <testns:key1>value1-1</testns:key1>
          <testns2:key1>value2-1</testns2:key1>
        </testns:data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1-1");
                    put("testns2Key1", "value2-1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
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
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("testnsKey1", "value1-1");
                    put("testnsChild.testnsKey1", "value1-1");
                    put("testnsChild.testns2Key1", "value2-1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns X "http://defns.hoge.jp/apply"
        2 key1 X
         *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply">
          <key1>value1</key1>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns X "http://defns.hoge.jp/apply"
        2 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        3 key1 X
        4 testns:key1 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply" xmlns:testns="http://testns.hoge.jp/apply">
          <key1>value-def-1</key1>
          <testns:key1>value1</testns:key1>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value-def-1");
                    put("testnsKey1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
        formatFile = Hereis.file(getFormatFileName());
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

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns="http://defns.hoge.jp/apply">
          <key1>value-def-1</key1>
          <child xmlns:testns="http://testns.hoge.jp/apply">
            <testns:key1>value1</testns:key1>
          </child>
        </data>
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value-def-1");
                    put("child.testnsKey1", "value1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }
 
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：未定義のネームスペース使用<br>
     * 
     * 【期待結果】<br>
     * 解析に失敗する<br>
     */
    @Test
    public void testCase4_7() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [testns:data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 testns:key1 X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <testns:data xmlns:testns="http://testns.hoge.jp/apply">
          <testns2:key1>value1</testns2:key1>
        </testns:data>
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "\"testns2:key1\"");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：XMLネームスペース<br>
     * 小項目：デフォルトネームスペース未定義でデフォルトネームスペース使用<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase4_8() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 ?@xmlns:testns X "http://testns.hoge.jp/apply"
        2 key1 X
        3 testns:key1 X
        *******/

        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data xmlns:testns="http://testns.hoge.jp/apply">
          <key1>value1-1</key1>
          <testns:key1>value2-1</testns:key1>
        </data>
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1-1");
                    put("testnsKey1", "value2-1");
                }}
        ;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }

    /**
     * test9116。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void test9116() throws Exception{
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
         file-type:        "XML"
         text-encoding:    "UTF-8"
         [user]
         1 fw OB
         [fw]
         1 user X
         *******/
        formatFile.deleteOnExit();

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <user>
        <fw>
        <user>value1</user>
        </fw>
        </user>
        *********************************************/

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("fw.user", "value1");
                }};

        // 正常系テスト実施
        testNormalCase(target, expectedMap);

    }

    /**
     * リーフ名がノード名と重複する場合は、
     * 正しくデータが読み取れること。
     */
    @Test
    public void testCaseDuplicatedName() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 data X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
          <child>
            <data>name</data>
          </child>
        </data>
        *********************************************/

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("child.data", "name");
                }};

        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }


    /**
     * リーフ同士で、項目名が重複する場合でも
     * 正しくデータが読み取れること。
     */
    @Test
    public void testDuplicatedLeafNames() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [root]
        1 node1 OB
        2 node2 OB
        [node1]
        1 data1 X            #これと
        [node2]
        1 data1 X            #これ
        2 data2 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
          <node1>
            <data1>1_1</data1>
          </node1>
          <node2>
            <data1>2_1</data1>
            <data2>2_2</data2>
          </node2>
        </root>
        *********************************************/

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("node1.data1", "1_1");
                    put("node2.data1", "2_1");
                    put("node2.data2", "2_2");
                }};

        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }

    /**
     * 現状、親ノード名が重複する場合は、正しくデータが読み取れない。
     *
     */
    @Ignore // これは通らないケース
    @Test
    public void testCaseDuplicatedParentName() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
         file-type:        "XML"
         text-encoding:    "UTF-8"
         [a]
         1 b OB
         [b]
         1 a OB
         [a]
         1 d X
         *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <a>
          <b>
            <a>
              <d>val1</d>
            </a>
          </b>
        </a>
        *********************************************/

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("b.a.d", "val1");
                }};

        // 正常系テスト実施
        testNormalCase(target, expectedMap);
    }


    /**
     * ルートノードが発見できない場合、例外{@link InvalidDataFormatException}が発生すること。
     * @throws Exception 予期しない例外
     */
    @Test
    public void testRootNodeNotFound() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        <?xml version="1.0" encoding="UTF-8"?>

        <child attr2="value2"></child>

        *********************************************/

        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "expected node [data] not found.");
    }


}