package nablarch.core.dataformat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.dataformat.convertor.ConvertorFactorySupport;
import nablarch.core.dataformat.convertor.ConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * {@link StructuredDataEditorSupport}のテストを行います。<br>
 * 本クラスでは電文の解析および構築に依らない定義ファイルの確認やレアケース異常系の分岐確認などを行います。
 * 
 * @author TIS
 */
public class StructuredDataParserSupportTest {
    @Rule
    public TestName testNameRule = new TestName();
    
    private StructuredDataParser parser = createParser();
    private StructuredDataBuilder builder = createBuilder();

    /**
     * 本クラスでは空のパーサーを作成します。
     * @return 空のパーサ－
     */
    protected StructuredDataParser createParser() {
        return new StructuredDataParser() {
            @Override
            public Map<String, ?> parseData(InputStream in,
                    LayoutDefinition layoutDef) throws IOException,
                    InvalidDataFormatException {
                return null;
            }
        };
    }
    
    /**
     * 本クラスでは空のビルダーを作成します。
     * @return 空のビルダー
     */
    protected StructuredDataBuilder createBuilder() {
        return new StructuredDataBuilder() {
            @Override
            public void buildData(Map<String, ?> map, LayoutDefinition layoutDef,
                    OutputStream out) throws IOException,
                    InvalidDataFormatException {
            }
        };
    }
    
    /**
     * 本クラスでは空のフォーマッターを作成します。
     * @return 空のフォーマッター
     */
    protected DataRecordFormatter createFormatter() {
        return new StructuredDataRecordFormatterSupport() {
            @Override
            public ConvertorSetting getConvertorSetting() {
                return new ConvertorSetting() {
                    @Override
                    public ConvertorFactorySupport getConvertorFactory() {
                        return new ConvertorFactorySupport() {
                            @Override
                            protected Map<String, Class<?>> getDefaultConvertorTable() {
                                return new HashMap<String, Class<?>>(){{
                                    put("X",         CharacterStreamDataString.class);
                                    put("N",         CharacterStreamDataString.class);
                                    put("XN",        CharacterStreamDataString.class);
                                    put("Z",         CharacterStreamDataString.class);
                                    put("X9",        CharacterStreamDataString.class);
                                    put("SX9",       CharacterStreamDataString.class);
                                    put("error",     ErrorConverter.class);
                                }};
                            }
                        };
                    }
                };
            }
        };
    }
    
    /**
     * テスト用に変換時にエラーを発生するコンバータ
     */
    public static class ErrorConverter implements ValueConvertor<Object, Object> {
        @Override
        public ValueConvertor<Object, Object> initialize(FieldDefinition field, Object... args) {
            return this;
        }
        @Override
        public Object convertOnRead(Object data) {
            throw new InvalidDataFormatException("DummyException");
        }
        @Override
        public Object convertOnWrite(Object data) {
            throw new InvalidDataFormatException("DummyException");
        }
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
     * レコード定義を取得します。
     * @param name レコード定義名
     * @param ld フォーマット定義
     * @return レコード定義
     */
    protected RecordDefinition getRecordDefinition(String name, LayoutDefinition ld) {
        if (ld != null) {
            for (RecordDefinition rd : ld.getRecords()) {
                if (rd.getTypeName().equals(name)) {
                    return rd;
                }
            }
        }
        return null;
    }
    
    /**
     * フィールド定義を取得します。
     * @param name フィールド定義名
     * @param rd レコード定義
     * @return フィールド定義
     */
    protected FieldDefinition getFieldDefinition(String name, RecordDefinition rd) {
        if (rd != null) {
            for (FieldDefinition fd : rd.getFields()) {
                if (fd.getName().equals(name)) {
                    return fd;
                }
            }
        }
        return null;
    }
    
    /**
     * フィールド定義を取得します。
     * @param rdName フレコード定義名
     * @param fdName フィールド定義名
     * @param ld フォーマット定義
     * @return フィールド定義
     */
    protected FieldDefinition getFieldDefinition(String rdName, String fdName, LayoutDefinition ld) {
        RecordDefinition rd = getRecordDefinition(rdName, ld);
        FieldDefinition fd = getFieldDefinition(fdName, rd);
        return fd;
    }

    /**
     * 文字セットにUTF-8を使用してデータの解析を行います。
     * @param data 解析対象データ
     * @return 解析されたマップ
     * @throws IOException 解析に失敗した場合
     */
    protected Map<String, ?> parseData(String data) throws IOException {
        return parseData(data, "UTF-8");
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
     * 文字セットにUTF-8を使用してデータの構築を行います。
     * @param data 構築対象データ
     * @return 構築された文字列
     * @throws IOException 解析に失敗した場合
     */
    protected String buildData(Map<String, ?> data) throws IOException {
        return buildData(data, "UTF-8");
    }
    
    /**
     * 文字セットにUTF-8を使用してデータの構築を行います。
     * @param data 構築対象データ
     * @param charset 文字セット
     * @return 構築された文字列
     * @throws IOException 解析に失敗した場合
     */
    protected String buildData(Map<String, ?> data, String charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        builder.buildData(data, getLayoutDefinition(), baos);
        return baos.toString(charset);
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
     * @param expectedMap 期待結果マップ
     * @param charset 文字コード
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, String charset) throws Exception {
        testNormalCase(target, expectedMap, expectedMap, true, charset);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap) throws Exception {
        testNormalCase(target, expectedMap, expectedMap, true);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     * @param isBuild 構築テスト実施有無
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, boolean isBuild) throws Exception {
        testNormalCase(target, expectedMap, expectedMap, isBuild);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, Map<String, ?> targetMap) throws Exception {
        testNormalCase(target, expectedMap, targetMap, true);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     * @param expectedString 期待結果データ
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, Map<String, ?> targetMap, String expectedString) throws Exception {
        expectedString = removeLineSeparatorAndWhiteSpace(expectedString);
        normalParseTest(target, expectedMap);
        normalBuildTest(targetMap, expectedString);
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     * @param isBuild 構築テスト実施有無
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, Map<String, ?> targetMap, boolean isBuild) throws Exception {
        // 期待結果String
        String expectedString = removeLineSeparatorAndWhiteSpace(target);
        normalParseTest(target, expectedMap);
        if (isBuild) {
            normalBuildTest(targetMap, expectedString);
        }
    }
    
    /**
     * 正常系のテストを行います。
     * @param target 対象データ
     * @param expectedMap 期待結果マップ
     * @param isBuild 構築テスト実施有無
     * @param charset 文字セット
     */
    protected void testNormalCase(String target, Map<String, ?> expectedMap, Map<String, ?> targetMap, boolean isBuild, String charset) throws Exception {
        // 期待結果String
        String expectedString = removeLineSeparatorAndWhiteSpace(target);
        normalParseTest(target, expectedMap, charset);
        if (isBuild) {
            normalBuildTest(targetMap, expectedString, charset);
        }
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String target, String expectedErrorMessage) throws Exception {
        testInvalidDataFormatCase(target, expectedErrorMessage, expectedErrorMessage, true);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     * @param isBuild 構築テスト実施有無
     */
    protected void testInvalidDataFormatCase(String target, String expectedErrorMessage, boolean isBuild) throws Exception {
        testInvalidDataFormatCase(target, expectedErrorMessage, expectedErrorMessage, isBuild);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 解析対象データ
     * @param target 構築対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String target, Map<String, ?> buildTarget, String expectedErrorMessage) throws Exception {
        testInvalidDataFormatCase(target, buildTarget, expectedErrorMessage, expectedErrorMessage, true);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 解析対象データ
     * @param target 構築対象データ
     * @param expectedErrorMessage 想定エラーメッセージ
     * @param isBuild 構築テスト実施有無
     */
    protected void testInvalidDataFormatCase(String target, Map<String, ?> buildTarget, String expectedErrorMessage, boolean isBuild) throws Exception {
        testInvalidDataFormatCase(target, buildTarget, expectedErrorMessage, expectedErrorMessage, isBuild);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedParseErrorMessage 解析時想定エラーメッセージ
     * @param expectedBuildErrorMessage 構築時想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String target, String expectedParseErrorMessage, String expectedBuildErrorMessage) throws Exception {
        testInvalidDataFormatCase(target, null, expectedParseErrorMessage, expectedBuildErrorMessage, true);
    }

    /**
     * データ異常系のテストを行います。
     * @param target 対象データ
     * @param expectedParseErrorMessage 解析時想定エラーメッセージ
     * @param expectedBuildErrorMessage 構築時想定エラーメッセージ
     * @param isBuild 構築テスト実施有無
     */
    protected void testInvalidDataFormatCase(String target, String expectedParseErrorMessage, String expectedBuildErrorMessage, boolean isBuild) throws Exception {
        testInvalidDataFormatCase(target, null, expectedParseErrorMessage, expectedBuildErrorMessage, isBuild);
    }
    
    /**
     * データ異常系のテストを行います。
     * @param parseTarget 解析対象データ
     * @param buildTarget 構築対象データ
     * @param expectedParseErrorMessage 解析時想定エラーメッセージ
     * @param expectedBuildErrorMessage 構築時想定エラーメッセージ
     */
    protected void testInvalidDataFormatCase(String parseTarget, Map<String, ?> buildTarget, String expectedParseErrorMessage, String expectedBuildErrorMessage) throws Exception {
        testInvalidDataFormatCase(parseTarget, buildTarget, expectedParseErrorMessage, expectedBuildErrorMessage, true);
    }

    /**
     * データ異常系のテストを行います。
     * @param parseTarget 解析対象データ
     * @param buildTarget 構築対象データ
     * @param expectedParseErrorMessage 解析時想定エラーメッセージ
     * @param expectedBuildErrorMessage 構築時想定エラーメッセージ
     * @param isBuild 構築テスト実施有無
     */
    protected void testInvalidDataFormatCase(String parseTarget, Map<String, ?> buildTarget, String expectedParseErrorMessage, String expectedBuildErrorMessage, boolean isBuild) throws Exception {
        // 期待結果Mapはnull
        Map<String, ?> expectedMap = null;
        
        // 期待結果String
        String expectedString = removeLineSeparatorAndWhiteSpace(parseTarget);
        abnormalParseTest(parseTarget, expectedMap, expectedParseErrorMessage);
        if (isBuild) {
            abnormalBuildTest(buildTarget, expectedString, expectedBuildErrorMessage);
        }
    }

    /**
     * 正常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void normalParseTest(String target, Map<String, ?> expected) throws Exception {
        // 解析テスト
        Map<String, ?> resultMap = parseData(target);
        assertNotNull(resultMap);
        assertMap(expected, resultMap);
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
     * 正常系構築テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void normalBuildTest(Map<String, ?> target, String expected) throws Exception {
        // 構築テスト
        String resultString = buildData(target);
        assertNotNull(resultString);
        assertEquals(expected, resultString);
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
     * 異常系解析テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void abnormalParseTest(String target, Map<String, ?> expected, String expectedErrorMessage) throws Exception {
        try {
            // 解析テスト
            Map<String, ?> resultMap = parseData(target);
            assertNotNull(resultMap);
            assertMap(expected, resultMap);
          
            fail("例外InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedErrorMessage));
        }
    }
    
    /**
     * 異常系構築テストを行います。
     * @param target 対象データ
     * @param expected 期待結果
     */
    private void abnormalBuildTest(Map<String, ?> target, String expected, String expectedErrorMessage) throws Exception {
        try {
            // 構築テスト
            String resultString = buildData(target);
            assertNotNull(resultString);
            assertEquals(expected, resultString);
            
            fail("例外InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedErrorMessage));
        }
    }
    

    /**
     * 必須チェックの確認を行います。
     */
    @Test
    public void testCheckRequired() {
        StructuredDataEditorSupport parser = new StructuredDataEditorSupport() {};
        FieldDefinition fd = new FieldDefinition();
        try {
            parser.checkRequired("basekey", fd, null, true);
            fail("InvalidDataFormatExceptionが発生する");
        } catch (InvalidDataFormatException e) {
        }
        
        parser.checkRequired("basekey", fd, "", true);
        
        parser.checkRequired("basekey", fd, "a", true);

        parser.checkRequired("basekey", fd, null, false);
        
        fd.markAsNotRequired();
        parser.checkRequired("basekey", fd, null, true);
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：必須項目指定<br>
     * 小項目：要素数指定なし（必須）<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_1() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = -1;
        int maxArraySize = -1;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
        } catch (SyntaxErrorException e) {
            fail("例外SyntaxErrorExceptionは発生しない");
        }
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：必須項目指定<br>
     * 小項目：必須指定<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_2() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = 1;
        int maxArraySize = 1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：必須項目指定<br>
     * 小項目：任意指定<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_3() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = 0;
        int maxArraySize = 1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性指定<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_4() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = true;
        boolean isArray = false;
        int minArraySize = -1;
        int maxArraySize = -1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性必須指定<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_5() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 [1..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = true;
        boolean isArray = false;
        int minArraySize = 1;
        int maxArraySize = 1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性任意指定<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_6() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 [0..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = true;
        boolean isArray = false;
        int minArraySize = 0;
        int maxArraySize = 1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性必須配列指定1<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_7() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 [1..2] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = true;
        boolean isArray = true;
        int minArraySize = 1;
        int maxArraySize = 2;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("attribute field can not be array."));
        }
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性必須配列指定2<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_8() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 [2] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = true;
        boolean isArray = true;
        int minArraySize = 2;
        int maxArraySize = 2;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("attribute field can not be array."));
        }
    }
    
    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：属性項目指定<br>
     * 小項目：属性任意配列指定<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_9() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 @key1 [0..2] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = true;
        boolean isArray = true;
        int minArraySize = 0;
        int maxArraySize = 2;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("attribute field can not be array."));
        }
    }
    
    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数<最大数1<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_10() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 1;
        int maxArraySize = 3;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数<最大数2<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_11() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 0;
        int maxArraySize = 3;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数>最大数<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_12() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [3..1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = 3;
        int maxArraySize = 1;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("max array size must be greater than min array size."));
        }
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数=最大数<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_13() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [3..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 3;
        int maxArraySize = 3;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数..*<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_14() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0..*] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 0;
        int maxArraySize = Integer.MAX_VALUE;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：*..最大数<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_15() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [*..3] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = Integer.MAX_VALUE;
        int maxArraySize = 3;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("encountered unexpected token."));
        }
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最大数のみ1<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_16() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [0] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = 0;
        int maxArraySize = 0;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("when not required, max count must be greater than 0"));
        }
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最大数のみ2<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_17() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = false;
        int minArraySize = 1;
        int maxArraySize = 1;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最大数のみ3<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_18() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [2] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 2;
        int maxArraySize = 2;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 正常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：*のみ<br>
     * 
     * 【期待結果】<br>
     * 正常に定義ファイルが読み込まれる<br>
     */
    @Test
    public void testCase1_19() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [*] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 0;
        int maxArraySize = Integer.MAX_VALUE;

        // テスト実施
        LayoutDefinition ld = getLayoutDefinition();
        FieldDefinition fd = getFieldDefinition("data", "key1", ld);
        
        assertEquals(isAttribute, fd.isAttribute());
        assertEquals(isRequired, fd.isRequired());
        assertEquals(isArray, fd.isArray());
        assertEquals(minArraySize, fd.getMinArraySize());
        assertEquals(maxArraySize, fd.getMaxArraySize());
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最大数>Integer.MAX_VALUE<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_20() {
        // フォーマット定義ファイル
        long maxval = Integer.MAX_VALUE + 1L;
        File formatFile = Hereis.file(getFormatFileName(), maxval );
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [1..${maxval}] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 1;
        long maxArraySize = Integer.MAX_VALUE + 1L;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("bad max array size"));
        }
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数>Integer.MAX_VALUE<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_21() {
        // フォーマット定義ファイル
        long maxval = Integer.MAX_VALUE + 1L;
        File formatFile = Hereis.file(getFormatFileName(), maxval );
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [${maxval}..${maxval}] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = true;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = 1;
        long maxArraySize = Integer.MAX_VALUE + 1L;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("bad min array size"));
        }
    }

    /**
     * 【分類】<br>
     * 異常系<br>
     * 
     * 【観点】<br>
     * 大項目：定義確認<br>
     * 中項目：配列項目指定<br>
     * 小項目：最小数<0<br>
     * 
     * 【期待結果】<br>
     * 定義ファイルの読み込みに失敗する<br>
     */
    @Test
    public void testCase1_22() {
        // フォーマット定義ファイル
        File formatFile = Hereis.file(getFormatFileName());
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [data]
        1 key1 [-1..*] X
        *******/
        formatFile.deleteOnExit();
        
        // 期待結果
        boolean isRequired = false;
        boolean isAttribute = false;
        boolean isArray = true;
        int minArraySize = -1;
        int maxArraySize = Integer.MAX_VALUE;

        // テスト実施
        try {
            LayoutDefinition ld = getLayoutDefinition();
            FieldDefinition fd = getFieldDefinition("data", "key1", ld);
            
            assertEquals(isAttribute, fd.isAttribute());
            assertEquals(isRequired, fd.isRequired());
            assertEquals(isArray, fd.isArray());
            assertEquals(minArraySize, fd.getMinArraySize());
            assertEquals(maxArraySize, fd.getMaxArraySize());
            
            fail("例外SyntaxErrorExceptionが発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("encountered unexpected token."));
        }
    }

}
