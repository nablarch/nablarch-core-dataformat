package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.JsonDataConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.datatype.JsonString;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.dataformat.convertor.value.ValueConvertorSupport;
import nablarch.test.support.SystemRepositoryResource;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;

/**
 * {@link JsonDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class JsonDataBuilderTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /** テスト対象 */
    private JsonDataBuilder sut = new JsonDataBuilder();

    @Test
    public void 必須項目に値が設定されたJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"value\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須項目に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("Field key is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 任意項目に値が設定されたJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"value\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 任意項目に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の必須項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "    \"child\":\"value\"" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の必須項目に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("parent,Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("Field parent is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 子要素の任意項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "    \"child\":\"value\"" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の任意項目に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{}" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("array", new String[]{"value1", "value2", "value3"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"array\":[" +
                "    \"value1\"," +
                "    \"value2\"," +
                "    \"value3\"" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=array:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 任意配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("array", new String[]{"value1", "value2", "value3"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"array\":[" +
                "    \"value1\"," +
                "    \"value2\"," +
                "    \"value3\"" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 任意配列に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Test
    public void 配列内の必須配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", new String[]{"value1", "value2", "value3"});
            put("parent[1].child", new String[]{"value4", "value5", "value6"});
            put("parent[2].child", new String[]{"value7", "value8", "value9"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":[" +
                "        \"value1\"," +
                "        \"value2\"," +
                "        \"value3\"" +
                "      ]" +
                "    }," +
                "    {" +
                "      \"child\":[" +
                "        \"value4\"," +
                "        \"value5\"," +
                "        \"value6\"" +
                "      ]" +
                "    }," +
                "    {" +
                "      \"child\":[" +
                "        \"value7\"," +
                "        \"value8\"," +
                "        \"value9\"" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 配列内の必須配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 配列内の任意配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", new String[]{"value1", "value2", "value3"});
            put("parent[1].child", new String[]{"value4", "value5", "value6"});
            put("parent[2].child", new String[]{"value7", "value8", "value9"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":[" +
                "        \"value1\"," +
                "        \"value2\"," +
                "        \"value3\"" +
                "      ]" +
                "    }," +
                "    {" +
                "      \"child\":[" +
                "        \"value4\"," +
                "        \"value5\"," +
                "        \"value6\"" +
                "      ]" +
                "    }," +
                "    {" +
                "      \"child\":[" +
                "        \"value7\"," +
                "        \"value8\"," +
                "        \"value9\"" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 配列内の任意配列に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Test
    public void 必須配列と必須項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [1..10] X",
                "2 key2 X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key1", new String[]{"value1", "value2"});
            put("key2", "value3");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key1\":[" +
                "    \"value1\"," +
                "    \"value2\"" +
                "  ]," +
                "  \"key2\":\"value3\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須配列と必須項目に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=key1:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [1..10] X",
                "2 key2 X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key1", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 任意配列と任意項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [0..10] X",
                "2 key2 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key1", new String[]{"value1", "value2"});
            put("key2", "value3");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key1\":[" +
                "    \"value1\"," +
                "    \"value2\"" +
                "  ]," +
                "  \"key2\":\"value3\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 任意配列と任意項目に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [0..10] X",
                "2 key2 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key1", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の必須配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", new String[]{"value1", "value2", "value3"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "    \"child\":[" +
                "      \"value1\"," +
                "      \"value2\"," +
                "      \"value3\"" +
                "    ]" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の必須配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("parent,FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 子要素の任意配列に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", new String[]{"value1", "value2", "value3"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "    \"child\":[" +
                "      \"value1\"," +
                "      \"value2\"," +
                "      \"value3\"" +
                "    ]" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素の任意配列に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", new String[]{});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{}" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void オブジェクト配列の必須項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child1 X",
                "2 child2 X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child1", "value1");
            put("parent[1].child1", "value2");
            put("parent[2].child1", "value3");
            put("parent[0].child2", "value4");
            put("parent[1].child2", "value5");
            put("parent[2].child2", "value6");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child1\":\"value1\"," +
                "      \"child2\":\"value4\"" +
                "    }," +
                "    {" +
                "      \"child1\":\"value2\"," +
                "      \"child2\":\"value5\"" +
                "    }," +
                "    {" +
                "      \"child1\":\"value3\"," +
                "      \"child2\":\"value6\"" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Ignore("オブジェクト配列内の項目に対する必須チェックが実施されない不具合により、このテストは落ちる")
    @Test
    public void オブジェクト配列の必須項目に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("parent[0],Field child1 is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child1 X",
                "2 child2 X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child2", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void オブジェクト配列の任意項目に値が設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child1 [0..1] X",
                "2 child2 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child1", "value1");
            put("parent[1].child1", "value2");
            put("parent[2].child1", "value3");
            put("parent[0].child2", "value4");
            put("parent[1].child2", "value5");
            put("parent[2].child2", "value6");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child1\":\"value1\"," +
                "      \"child2\":\"value4\"" +
                "    }," +
                "    {" +
                "      \"child1\":\"value2\"," +
                "      \"child2\":\"value5\"" +
                "    }," +
                "    {" +
                "      \"child1\":\"value3\"," +
                "      \"child2\":\"value6\"" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void オブジェクト配列の任意項目に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child1 [0..1] X",
                "2 child2 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child2", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child2\":\"value\"" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void オブジェクト配列に値が設定されていないJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child1 [0..1] X",
                "2 child2 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>();

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Test
    public void フォーマット定義されていない項目はJSONに出力されないこと() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value1");
            put("undefined", "value2");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"value1\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 配列の要素数が超過しているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=array:MinCount=1:MaxCount=3:Actual=4"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [1..3] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("array", new String[]{"value1", "value2", "value3", "value4"});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 項目名が重複しているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent1 OB",
                "2 parent2 OB",
                "3 child X",
                "",
                "[parent1]",
                "1 child X",
                "",
                "[parent2]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("child", "value1");
            put("parent1.child", "value2");
            put("parent2.child", "value3");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"child\":\"value1\"," +
                "  \"parent1\":{" +
                "    \"child\":\"value2\"" +
                "  }," +
                "  \"parent2\":{" +
                "    \"child\":\"value3\"" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 各フィールドタイプを全て使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 type1 X",
                "2 type2 N",
                "3 type3 XN",
                "4 type4 X9",
                "5 type5 SX9",
                "6 type6 BL",
                "7 type7 OB",
                "",
                "[type7]",
                "1 type1 X",
                "2 type2 N",
                "3 type3 XN",
                "4 type4 X9",
                "5 type5 SX9",
                "6 type6 BL"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("type1", "value1");
            put("type2", "value2");
            put("type3", "value3");
            put("type4", "value4");
            put("type5", "value5");
            put("type6", "value6");
            put("type7.type1", "value1");
            put("type7.type2", "value2");
            put("type7.type3", "value3");
            put("type7.type4", "value4");
            put("type7.type5", "value5");
            put("type7.type6", "value6");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"type1\":\"value1\"," +
                "  \"type2\":\"value2\"," +
                "  \"type3\":\"value3\"," +
                "  \"type4\":\"value4\"," +
                "  \"type5\":\"value5\"," +
                "  \"type6\":\"value6\"," +
                "  \"type7\":{" +
                "    \"type1\":\"value1\"," +
                "    \"type2\":\"value2\"," +
                "    \"type3\":\"value3\"," +
                "    \"type4\":\"value4\"," +
                "    \"type5\":\"value5\"," +
                "    \"type6\":\"value6\"" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void numberコンバータを使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", 123456);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"number\":\"123456\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void numberコンバータでnullを使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"number\":\"\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void numberコンバータで符号付き数値の場合に型変換に失敗すること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("invalid parameter format was specified."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", -123456);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void signed_numberコンバータを使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X signed_number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", -123456);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"number\":\"-123456\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void signed_numberコンバータでnullを使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X signed_number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"number\":\"\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void signed_numberコンバータで文字列の場合にエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("invalid parameter format was specified."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number SX9 signed_number"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("number", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void replacementコンバータで置換されること() throws Exception {

        // 寄せ字用のコンポーネント定義
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName("type");
        config.setFilePath("classpath:nablarch/core/dataformat/replacement.properties");
        config.setEncoding("UTF-8");
        CharacterReplacementManager characterReplacementManager = new CharacterReplacementManager();
        characterReplacementManager.setConfigList(Arrays.asList(config));
        characterReplacementManager.initialize();
        repositoryResource.addComponent("characterReplacementManager", characterReplacementManager);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X replacement(\"type\")"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "髙﨑唖");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"高崎■\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void replacementコンバータでnullが使用できること() throws Exception {

        // 寄せ字用のコンポーネント定義
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName("type");
        config.setFilePath("classpath:nablarch/core/dataformat/replacement.properties");
        config.setEncoding("UTF-8");
        CharacterReplacementManager characterReplacementManager = new CharacterReplacementManager();
        characterReplacementManager.setConfigList(Arrays.asList(config));
        characterReplacementManager.initialize();
        repositoryResource.addComponent("characterReplacementManager", characterReplacementManager);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X replacement(\"type\")"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":null" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 独自コンバータが適用されること() throws Exception {

        JsonDataConvertorSetting setting = repositoryResource.getComponent("jsonDataConvertorSetting");
        setting.getConvertorFactory().getConvertorTable().put("custom", CustomValueConvertor.class);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X custom"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"custom\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 独自フィールドタイプが適用されること() throws Exception {

        JsonDataConvertorSetting setting = repositoryResource.getComponent("jsonDataConvertorSetting");
        setting.getConvertorFactory().getConvertorTable().put("CM", CustomDataType.class);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key CM"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":\"custom\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 不正なフィールドタイプを指定した場合にエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("Invalid data type definition."));

        JsonDataConvertorSetting setting = repositoryResource.getComponent("jsonDataConvertorSetting");
        setting.getConvertorFactory().getConvertorTable().put("CM", InvalidDataType.class);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key CM"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 必須項目にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("key is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 任意項目にnullが設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":null" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須配列にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=key:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 必須配列の要素にnullが設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", new String[]{null});
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\": [null]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 任意配列にnullが設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key\":null" +
                "}";
        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void オブジェクトの必須項目にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("BaseKey = parent,Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..1] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void オブジェクトの任意項目にnullが設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..1] OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "    \"child\":null" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void オブジェクトの必須配列にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("BaseKey = parent,FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..1] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void オブジェクトの任意配列にnullが設定されているJSONを出力できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..1] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent.child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":{" +
                "  \"child\":null" +
                "  }" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 必須オブジェクト配列にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=parent:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void 任意オブジェクト配列にnullが設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        JSONAssert.assertEquals("{}", actual.toString("utf-8"), true);
    }

    @Ignore("オブジェクト配列内の項目に対する必須チェックが実施されない不具合により、このテストは落ちる")
    @Test
    public void オブジェクト配列内の必須項目にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("BaseKey = parent[0],Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);
    }

    @Test
    public void オブジェクト配列内の任意項目にnullが設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":null" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void  子要素がオブジェクトで孫要素にのみ値が設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..1] OB",
                "",
                "[child]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child.key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":{" +
                "        \"key\":\"value\"" +
                "      }" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素がオブジェクトで孫要素にのみnullが設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..1] OB",
                "",
                "[child]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child.key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":{" +
                "        \"key\":null" +
                "      }" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素がオブジェクト配列で孫要素にのみ値が設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..10] OB",
                "",
                "[child]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child[0].key", "value");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":[" +
                "        {" +
                "          \"key\":\"value\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 子要素がオブジェクト配列で孫要素にのみnullが設定されているJSONを出力できること() throws Exception {
        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child [0..10] OB",
                "",
                "[child]",
                "1 key [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("parent[0].child[0].key", null);
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"parent\":[" +
                "    {" +
                "      \"child\":[" +
                "        {" +
                "          \"key\":null" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    @Test
    public void 任意項目が出力されない場合に余計なカンマがJSONに出力されないこと() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [0..1] X",
                "2 key2 [0..1] X",
                "3 key3 [0..1] X"
        );

        // MAP
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", "value2");
        }};

        // テスト実行
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(map, definition, actual);

        // 検証
        String expected = "{" +
                "  \"key1\":\"value1\"," +
                "  \"key2\":\"value2\"" +
                "}";

        JSONAssert.assertEquals(expected, actual.toString("utf-8"), true);
    }

    private LayoutDefinition createLayoutDefinition(String... records) throws Exception {
        File file = folder.newFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String record : records) {
            writer.write(record);
            writer.newLine();
        }
        writer.close();

        LayoutDefinition definition = new LayoutFileParser(file.getPath()).parse();
        DataRecordFormatter formatter = new JsonDataRecordFormatter();
        formatter.setDefinition(definition);
        formatter.initialize();
        return definition;
    }

    /**
     * カスタムの{@link ValueConvertor}実装クラス。
     */
    public static class CustomValueConvertor extends ValueConvertorSupport {

        @Override
        public Object convertOnRead(Object data) {
            return null;
        }

        @Override
        public Object convertOnWrite(Object data) {
            return "custom";
        }
    }

    /**
     * カスタムの{@link DataType}実装クラス。
     */
    public static class CustomDataType extends JsonString {

        @Override
        public String convertOnWrite(Object data) {
            return "custom";
        }
    }

    public static class InvalidDataType extends CharacterStreamDataString {
    }
}
