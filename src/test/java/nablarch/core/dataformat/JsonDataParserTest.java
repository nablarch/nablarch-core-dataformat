package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * {@link JsonDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class JsonDataParserTest {




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
     * 本クラスではJSONのパーサーを作成します。
     * @return JSONのパーサ－
     */
    protected StructuredDataParser createParser() {
        return new JsonDataParser();
    }

    /**
     * 本クラスではJSONのフォーマッターを作成します。
     * @return JSONのフォーマッター
     */
    protected DataRecordFormatter createFormatter() {
        return new JsonDataRecordFormatter();
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
     * 指定された文字セットを使用してデータの解析を行います。
     * @param data 解析対象データ
     * @param charset 文字セット
     * @return 解析されたマップ
     * @throws IOException 解析に失敗した場合
     */
    protected Map<String, ?> parseData(String data, String charset) throws IOException {
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
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, String charset) throws Exception {
        normalParseTest(target, expectedMap, charset);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String target, String expectedErrorMessage) throws Exception {

        // 期待結果String
        abnormalParseTest(target, expectedErrorMessage);
    }
    
    /**
     * 正常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void normalParseTest(String target, Map<String, ?> expected) throws Exception {
        normalParseTest(target, expected, "UTF-8");
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
     */
    private void abnormalParseTest(String target, String expectedErrorMessage) throws Exception {
        try {
            // 解析テスト
            normalParseTest(target, null);
          
            fail("例外InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString(expectedErrorMessage));
        }
    }
    
    /**
     * 階層パターンを網羅したJSONデータのテストです。
     */
    @Test
    public void testParseAllPatternOfStratumJson() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 key1 X
        2 key2 X
        3 key3 [1..*] X
        4 Class1 OB
        5 Class2 OB
        6 Class3 OB
        7 Class4 OB
        [Class1]
        1 ClassKey1 X
        2 ClassKey2 X
        3 ClassKey3 X
        [Class2]
        1 Class2Key1 X
        2 Class2Key2 X
        [Class3]
        1 Class3Key1 X
        2 Class3Key2 X
        3 Class3Key3 [1..*] X
        [Class4]
        1 Class4Key1 X
        2 Class41 OB
        3 Class42 OB
        [Class41]
        1 Class41Key1 X
        2 Class41Key2 X
        3 Class41Key3 X
        [Class42]
        1 Class42Key1 X
        2 Class42Key2 X
        3 Class42Key3 X
        4 Class421 [1..*] OB
        [Class421]
        1 Class421Key1 X
        2 Class421Key2 X
        *******/
        
        
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "key1":"value1",
            "key2":"value2",
            "key3":["value3-1","value3-2"],
            "Class1":{
                "ClassKey1":"Class1Value1",
                "ClassKey2":"Class1Value2",
                "ClassKey3":"3"
            },
            "Class2":{
                "Class2Key1":"Class2Value1",
                "Class2Key2":"Class2Value2"
            },
            "Class3":{
                "Class3Key1":"Class3Value1",
                "Class3Key2":"Class3Value2",
                "Class3Key3":["Class3Value3-1","Class3Value3-2","Class3Value3-3","Class3Value3-4"]
            },
            "Class4":{
                "Class4Key1":"Class4Value1",
                "Class41":{
                    "Class41Key1":"Class4-1Value1",
                    "Class41Key2":"Class4-1Value2",
                    "Class41Key3":"Class4-1Value3"
                },
                "Class42":{
                    "Class42Key1":"Class4-2Value1",
                    "Class42Key2":"Class4-2Value2",
                    "Class42Key3":"Class4-2Value3",
                    "Class421":[{
                        "Class421Key1":"Class4-2-1[0]Value1",
                        "Class421Key2":"Class4-2-1[0]Value2"
                        },{
                        "Class421Key1":"Class4-2-1[1]Value1",
                        "Class421Key2":"Class4-2-1[1]Value2"
                        }]
                }
            }
        }
        *********************************************/
        
        // 内部で生成されるmapはキーの先頭が小文字になる
        Map<String, ?> result = parseData(json, "UTF-8");
        
        // 期待結果Map(失敗する)
        Map<String, Object> expectedFaildMap = new HashMap<String, Object>() {{
            put( "key1", "value1" );
            put( "key2", "value2" );
            put( "key3", new String[]{"value3-1", "value3-2"});
            
            put( "Class1.ClassKey1", "Class1Value1" );
            put( "Class1.ClassKey2", "Class1Value2" );
            put( "Class1.ClassKey3", "3" );
            
            put( "Class2.Class2Key1", "Class2Value1" );
            put( "Class2.Class2Key2", "Class2Value2" );
            
            put( "Class3.Class3Key1", "Class3Value1" );
            put( "Class3.Class3Key2", "Class3Value2" );
            put( "Class3.Class3Key3", new String[]{"Class3Value3-1","Class3Value3-2","Class3Value3-3","Class3Value3-4"});
            
            put( "Class4.Class4Key1", "Class4Value1" );
            put( "Class4.Class41.Class41Key1", "Class4-1Value1" );
            put( "Class4.Class41.Class41Key2", "Class4-1Value2" );
            put( "Class4.Class41.Class41Key3", "Class4-1Value3" );
            put( "Class4.Class42.Class42Key1", "Class4-2Value1" );
            put( "Class4.Class42.Class42Key2", "Class4-2Value2" );
            put( "Class4.Class42.Class42Key3", "Class4-2Value3" );
            put( "Class4.Class42.Class421Size", "2" );
            put( "Class4.Class42.Class421[0].Class421Key1", "Class4-2-1[0]Value1" );
            put( "Class4.Class42.Class421[0].Class421Key2", "Class4-2-1[0]Value2" );
            put( "Class4.Class42.Class421[1].Class421Key1", "Class4-2-1[1]Value1" );
            put( "Class4.Class42.Class421[1].Class421Key2", "Class4-2-1[1]Value2" );
        }};

        try {
            assertMap(expectedFaildMap, result);
            fail(); // AssertionErrorが発生する
        } catch( AssertionError e) {
        }
        
        // 期待結果Map(失敗する)
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put( "key1", "value1" );
            put( "key2", "value2" );
            put( "key3", new String[]{"value3-1", "value3-2"});
            
            put( "class1.classKey1", "Class1Value1" );
            put( "class1.classKey2", "Class1Value2" );
            put( "class1.classKey3", "3" );
            
            put( "class2.class2Key1", "Class2Value1" );
            put( "class2.class2Key2", "Class2Value2" );
            
            put( "class3.class3Key1", "Class3Value1" );
            put( "class3.class3Key2", "Class3Value2" );
            put( "class3.class3Key3", new String[]{"Class3Value3-1","Class3Value3-2","Class3Value3-3","Class3Value3-4"});
            
            put( "class4.class4Key1", "Class4Value1" );
            put( "class4.class41.class41Key1", "Class4-1Value1" );
            put( "class4.class41.class41Key2", "Class4-1Value2" );
            put( "class4.class41.class41Key3", "Class4-1Value3" );
            put( "class4.class42.class42Key1", "Class4-2Value1" );
            put( "class4.class42.class42Key2", "Class4-2Value2" );
            put( "class4.class42.class42Key3", "Class4-2Value3" );
            put( "class4.class42.class421Size", "2" );
            put( "class4.class42.class421[0].class421Key1", "Class4-2-1[0]Value1" );
            put( "class4.class42.class421[0].class421Key2", "Class4-2-1[0]Value2" );
            put( "class4.class42.class421[1].class421Key1", "Class4-2-1[1]Value1" );
            put( "class4.class42.class421[1].class421Key2", "Class4-2-1[1]Value2" );
        }};
        
        assertMap(expectedMap, result);
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"value1"
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"value1",
          "key2":"value2"
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        2 key2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key2":"value2"
        }
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
     * 小項目：ルート要素<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase2_3_2() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "key1":"value1"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "key1":"value1",
            "key2":"value2"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "key2":"value2"
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":{
          }
        }
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
        file-type:        "JSON"
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
        {
          "child1":{
            "key":"value1"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child1":{
            "key":"value1"
          },
          "child2":{
            "key":"value2"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child2":{
            "key":"value2"
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "attr1":"value1"
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "attr1":"value1",
          "attr2":"value2"
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @attr1 X
        2 @attr2 [0..1] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "attr2":"value2"
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "attr1":"value1"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "attr1":"value1",
            "attr2":"value2"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "attr2":"value2"
          }
        }
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
        file-type:        "JSON"
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
        {
          "child":{
            "key1":"value1"
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":["value1-1"]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":["value1-1",
                    "value1-2",
                    "value1-3"]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":["value1-1",
                    "value1-2",
                    "value1-3",
                    "value1-4"]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        2 key2 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":{
            "key1":["value1-1"]
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":{
            "key1":["value1-1",
                      "value1-2",
                      "value1-3"]
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child OB
        [child]
        1 key1 [1..3] X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":{
            "key1":["value1-1",
                      "value1-2",
                      "value1-3",
                      "value1-4"]
          }
        }
        *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Out of range array length BaseKey = child," +
                "FieldName=key1:MinCount=1:MaxCount=3:Actual=4");
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
        file-type:        "JSON"
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
        {
          "child":{}
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":[{"key":"value1"}]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":[
            {"key":"value1"},
            {"key":"value2"},
            {"key":"value3"}
          ]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":[
            {"key":"value1"},
            {"key":"value2"},
            {"key":"value3"},
            {"key":"value4"}
          ]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":[]
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 child [1..3] OB
        [child]
        1 key X
         *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child":["hoge"]
        }
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "Field child is Object Array but other item detected");
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
        file-type:        "JSON"
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
        {
          "halfstring":"abcde1234",
          "widestring":"あいうえお１２３４",
          "mixstring":"abcdeあいうえお1234",
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":100.23,
          "signedflt":-200.45,
          "unsignedexp":100.23e5,
          "signedexp1":-200.45e+5,
          "signedexp2":-200.45e-5,
          "bool":true
        }
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
        file-type:        "JSON"
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
        {
          "halfstring":null,
          "widestring":null,
          "mixstring":null,
          "unsignedint":null,
          "signedint":null,
          "unsignedflt":null,
          "signedflt":null,
          "bool":null
        }
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
    public void testCase3_2_1() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {
        }
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
        file-type:        "JSON"
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
        {
          "halfstring":"",
          "widestring":"",
          "mixstring":"",
          "unsignedint":"",
          "signedint":"",
          "unsignedflt":"",
          "signedflt":"",
          "bool":""
        }
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
    public void testCase3_2_3() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 halfstring  [1..1] X
         *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
        }
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
        file-type:        "JSON"
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
        {
          "halfstring":null,
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "JSON data must ends with '}'");
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
        file-type:        "JSON"
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
        {
          "halfstr
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "JSON data must ends with '}'");
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
    public void testCase3_5() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        "value"
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "JSON data must starts with '{'");
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
    public void testCase3_6() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        { ["value1","value2"] }
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "array is need start after :");
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
    public void testCase3_7() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {
          :"value"
        }
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "key is not string");
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
    public void testCase3_8() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {
          "bool":value
        }
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "found invalid token");
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
        file-type:        "JSON"
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
        {}
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = null;
        
        // 正常系テスト実施
        testNormalCase(target, expectedMap);
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
    public void testCase3_10() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {"key":,}
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "value is requires");
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
    public void testCase3_11() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {"key":"a"{}}
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "incorrect object starting position");
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
    public void testCase3_12() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {"key":]}
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "array end detected, but not started");
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
    public void testCase3_13() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
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
        {,"value"}
         *********************************************/
        
        // データ異常系テスト実施
        testInvalidDataFormatCase(target, "value is requires");
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X "def"
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"value1"
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..1] X "def"
         *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"def"
        }
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "def");
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
        File diConfigFile = Hereis.file("temp/JsonParserTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- FormatterFactoryの設定 -->
            <component name="jsonDataConvertorSetting"
                class="nablarch.core.dataformat.convertor.JsonDataConvertorSetting">
                <property name="convertorTable">
                    <map>
                        <entry key="X" value="nablarch.core.dataformat.convertor.datatype.NullableString"/>
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X error
        *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"value1"
        }
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
        file-type:        "JSON"
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
        {
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":1.23,
          "signedflt":-2.45,
          "unsignedexp":1.23E+5,
          "signedexp":-2.45E+5
        }
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
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：エスケープ<br>
     * 小項目：一般的なエスケープ対象文字<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_17() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
         *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"\"\\\b\f\n\r\t"
        }   
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "\"\\\b\f\n\r\t");
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
     * 中項目：エスケープ<br>
     * 小項目：コードポイントによるエスケープ対象文字(解析のみ)<br>
     * 
     * 【期待結果】<br>
     * 正常に解析が行われる<br>
     */
    @Test
    public void testCase3_18() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
         *******/
        
        
        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "key1":"\u3042い\u3046え\u304a"
        }   
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "あいうえお");
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
     * 中項目：エンコーディング<br>
     * 小項目：UTF16LE<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_19() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-16LE"
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
        {
          "halfstring":"abcde1234",
          "widestring":"あいうえお１２３４",
          "mixstring":"abcdeあいうえお1234",
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":100.23,
          "signedflt":-200.45,
          "unsignedexp":100.23e5,
          "signedexp1":-200.45e+5,
          "signedexp2":-200.45e-5,
          "bool":true
        }
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
        testNormalCase(target, expectedMap, "UTF-16LE");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：エンコーディング<br>
     * 小項目：UTF16BE<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_20() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-16BE"
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
        {
          "halfstring":"abcde1234",
          "widestring":"あいうえお１２３４",
          "mixstring":"abcdeあいうえお1234",
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":100.23,
          "signedflt":-200.45,
          "unsignedexp":100.23e5,
          "signedexp1":-200.45e+5,
          "signedexp2":-200.45e-5,
          "bool":true
        }
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
        testNormalCase(target, expectedMap, "UTF-16BE");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：エンコーディング<br>
     * 小項目：UTF32LE<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_21() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-32LE"
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
        {
          "halfstring":"abcde1234",
          "widestring":"あいうえお１２３４",
          "mixstring":"abcdeあいうえお1234",
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":100.23,
          "signedflt":-200.45,
          "unsignedexp":100.23e5,
          "signedexp1":-200.45e+5,
          "signedexp2":-200.45e-5,
          "bool":true
        }
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
        testNormalCase(target, expectedMap, "UTF-32LE");
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：バリエーション<br>
     * 中項目：エンコーディング<br>
     * 小項目：UTF32BE<br>
     * 
     * 【期待結果】<br>
     * 正常に解析および構築が行われる<br>
     */
    @Test
    public void testCase3_22() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-32BE"
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
        {
          "halfstring":"abcde1234",
          "widestring":"あいうえお１２３４",
          "mixstring":"abcdeあいうえお1234",
          "unsignedint":100,
          "signedint":-200,
          "unsignedflt":100.23,
          "signedflt":-200.45,
          "unsignedexp":100.23e5,
          "signedexp1":-200.45e+5,
          "signedexp2":-200.45e-5,
          "bool":true
        }
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
        testNormalCase(target, expectedMap, "UTF-32BE");
    }

    /**
     * 不正なデータタイプを指定した場合のテストを行います。<br>
     * 
     * 条件：<br>
     *   FormatterFactoryの設定でJsonデータ用のマーカークラス以外を指定する。<br>
     *   
     * 期待結果：<br>
     *   例外InvalidDataFormatExceptionが発生すること。<br>
     */
    @Test
    public void testInvalidDataType() throws Exception {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/ConvertorSetting.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        
        // テスト用フォーマット
        FilePathSetting fps = FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("format", "fmt");
        String formatFileName = 
                Builder.concat(
                        fps.getBasePathSettings().get("format").getPath(),
                        "/", "JsonDataRecordFormatterTest", ".", 
                        fps.getFileExtensions().get("format"));
        
        formatFile = Hereis.file(formatFileName);
        /****************************
        file-type:      "JSON"
        text-encoding:  "UTF-8"
        [request]
        1 id            Test
         ****************************/

        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        LayoutDefinition def = new LayoutFileParser(formatFile.getAbsolutePath()).parse();
        formatter.setDefinition(def).initialize();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.setOutputStream(baos);
        
        Map<String, Object> record = new HashMap<String, Object>();
        record.put("id", "value");
        try {
            formatter.writeRecord(record);
            fail("例外が発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue(e.getMessage().contains("Invalid data type definition. type=nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString" ));
        }

        formatter.close();
        
        // デフォルトのリポジトリに戻す
        loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml");
        container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);

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
         file-type:        "JSON"
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
        {
          "fw":{
            "user" : "value1"
          }
        }
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
         file-type:        "JSON"
         text-encoding:    "UTF-8"
         [data]
         1 child OB
         [child]
         1 data X
         *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {
          "child" : {
            "data" : "name"
          }
        }
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
         file-type:        "JSON"
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
        {
          "node1" : {
            "data1" : "1_1"
          },
          "node2" : {
            "data1" : "2_1",
            "data2" : "2_2"
          }
        }
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
    @Ignore // これは現状通らないケースです。
    @Test
    public void testCaseDuplicatedParentName() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
         file-type:        "JSON"
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
        {
          "b" : {
            "a" : {
              "d" : "val1"
            }
          }
        }
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
        file-type:        "JSON"
        text-encoding:    "UTF-8"
         [data]
         1 key1 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        {

        }
        *********************************************/

        testInvalidDataFormatCase(target, "BaseKey = ,Field key1 is required");
    }

    /**
     * ルートノードが発見できない(カーリーブラケットが無い)場合、
     * 例外{@link InvalidDataFormatException}が発生すること。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testNoCurlyBracket() throws Exception {
        // フォーマット定義ファイル
        formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [root]
        1 node1 OB
        2 node2 OB
        [node1]
        1 data1 X
        [node2]
        1 data2 X
        *******/

        // 変換対象データ
        String target = Hereis.string();
        /*******************************************
        "node1" : {
            "data1" : "1_1"
        },
          "node2" : {
          "data2" : "2_2"
        }
        *********************************************/

        // 異常系テスト実施
        testInvalidDataFormatCase(target, "JSON Parse Error. JSON data must starts with '{");
    }
}
