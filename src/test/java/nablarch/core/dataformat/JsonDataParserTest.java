package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.JsonDataConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.datatype.JsonString;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.dataformat.convertor.value.ValueConvertorSupport;
import nablarch.test.support.SystemRepositoryResource;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class JsonDataParserTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /** テスト対象 */
    private JsonDataParser sut = new JsonDataParser();

    @Test
    public void 必須項目に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "value"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意項目に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "value"));
    }

    @Test
    public void 任意項目に値が設定されていないJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", null));
    }

    @Test
    public void 子要素の必須項目に値が設定されているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":\"value\"",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", (Object) "value"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{}",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意項目に値が設定されているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":\"value\"",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", (Object) "value"));
    }

    @Test
    public void 子要素の任意項目に値が設定されていないJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{}",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", null));
    }

    @Test
    public void 必須配列に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [1..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"array\":[",
                "    \"value1\",",
                "    \"value2\",",
                "    \"value3\"",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("array");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意配列に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [0..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"array\":[",
                "    \"value1\",",
                "    \"value2\",",
                "    \"value3\"",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("array");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
    }

    @Test
    public void 任意配列に値が設定されていないJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 array [0..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("array");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 配列内の必須配列に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":[",
                "        \"value1\",",
                "        \"value2\",",
                "        \"value3\"",
                "      ]",
                "    },",
                "    {",
                "      \"child\":[",
                "        \"value4\",",
                "        \"value5\",",
                "        \"value6\"",
                "      ]",
                "    },",
                "    {",
                "      \"child\":[",
                "        \"value7\",",
                "        \"value8\",",
                "        \"value9\"",
                "      ]",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));

        child = (String[]) result.get("parent[1].child");
        assertThat(child, hasItemInArray("value4"));
        assertThat(child, hasItemInArray("value5"));
        assertThat(child, hasItemInArray("value6"));

        child = (String[]) result.get("parent[2].child");
        assertThat(child, hasItemInArray("value7"));
        assertThat(child, hasItemInArray("value8"));
        assertThat(child, hasItemInArray("value9"));

    }

    @Test
    public void 配列内の必須配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":[]",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 配列内の任意配列に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":[",
                "        \"value1\",",
                "        \"value2\",",
                "        \"value3\"",
                "      ]",
                "    },",
                "    {",
                "      \"child\":[",
                "        \"value4\",",
                "        \"value5\",",
                "        \"value6\"",
                "      ]",
                "    },",
                "    {",
                "      \"child\":[",
                "        \"value7\",",
                "        \"value8\",",
                "        \"value9\"",
                "      ]",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));

        child = (String[]) result.get("parent[1].child");
        assertThat(child, hasItemInArray("value4"));
        assertThat(child, hasItemInArray("value5"));
        assertThat(child, hasItemInArray("value6"));

        child = (String[]) result.get("parent[2].child");
        assertThat(child, hasItemInArray("value7"));
        assertThat(child, hasItemInArray("value8"));
        assertThat(child, hasItemInArray("value9"));
    }

    @Test
    public void 配列内の任意配列に値が設定されていないJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":[]",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 必須配列と必須項目に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [1..10] X",
                "2 key2 X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key1\":[",
                "    \"value1\",",
                "    \"value2\"",
                "  ]",
                "  \"key2\":\"value3\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("key1");
        assertThat(child1, hasItemInArray("value1"));
        assertThat(child1, hasItemInArray("value2"));
        assertThat(result, hasEntry("key2", (Object) "value3"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key1\":[]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意配列と任意項目に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [0..10] X",
                "2 key2 [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key1\":[",
                "    \"value1\",",
                "    \"value2\"",
                "  ]",
                "  \"key2\":\"value3\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("key1");
        assertThat(child1, hasItemInArray("value1"));
        assertThat(child1, hasItemInArray("value2"));
        assertThat(result, hasEntry("key2", (Object) "value3"));
    }

    @Test
    public void 任意配列と任意項目に値が設定されていないJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key1 [0..10] X",
                "2 key2 [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key1\":[]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("key1");
        assertThat(child1, is(emptyArray()));
        assertThat(result, hasEntry("key2", null));
    }

    @Test
    public void 子要素の必須配列に値が設定されているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\": {",
                "    \"child\":[",
                "      \"value1\",",
                "      \"value2\",",
                "      \"value3\"",
                "    ]",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent.child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\": {",
                "    \"child\":[]",
                "  }",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意配列に値が設定されているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\": {",
                "    \"child\":[",
                "      \"value1\",",
                "      \"value2\",",
                "      \"value3\"",
                "    ]",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent.child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
    }

    @Test
    public void 子要素の任意配列に値が設定されていないJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\": {",
                "    \"child\":[]",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent.child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void オブジェクト配列の必須項目に値が設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":\"value1\"",
                "    },",
                "    {",
                "      \"child\":\"value2\"",
                "    },",
                "    {",
                "      \"child\":\"value3\"",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", (Object) "value1"));
        assertThat(result, hasEntry("parent[1].child", (Object) "value2"));
        assertThat(result, hasEntry("parent[2].child", (Object) "value3"));

    }

    @Test
    public void オブジェクト配列の必須項目に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("parent[0],Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {}",
                "  ]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void オブジェクト配列の任意項目に値が設定されているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":\"value1\"",
                "    },",
                "    {",
                "      \"child\":\"value2\"",
                "    },",
                "    {",
                "      \"child\":\"value3\"",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", (Object) "value1"));
        assertThat(result, hasEntry("parent[1].child", (Object) "value2"));
        assertThat(result, hasEntry("parent[2].child", (Object) "value3"));
    }

    @Test
    public void オブジェクト配列の任意項目に値が設定されていないJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {}",
                "  ]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", null));
    }

    @Test
    public void オブジェクト配列に値が設定されていないJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parentSize", (Object) "0"));
    }

    @Test
    public void フォーマット定義されていない項目が存在するJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value1\",",
                "  \"undefined\":\"value2\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "value1"));
        assertThat(result, not(hasKey("undefined")));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"array\":[",
                "    \"value1\",",
                "    \"value2\",",
                "    \"value3\",",
                "    \"value4\"",
                "  ]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 項目名が重複しているJSONを読み込めること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"child\":\"value1\",",
                "  \"parent1\":{",
                "    \"child\":\"value2\"",
                "  },",
                "  \"parent2\":{",
                "    \"child\":\"value3\"",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child", (Object) "value1"));
        assertThat(result, hasEntry("parent1.child", (Object) "value2"));
        assertThat(result, hasEntry("parent2.child", (Object) "value3"));
    }

    @Test
    public void 各フィールドタイプが全て文字列型に変換されること() throws Exception {

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"type1\":\"value1\",",
                "  \"type2\":\"value2\",",
                "  \"type3\":\"value3\",",
                "  \"type4\":\"value4\",",
                "  \"type5\":\"value5\",",
                "  \"type6\":\"value6\",",
                "  \"type7\":{",
                "    \"type1\":\"value1\",",
                "    \"type2\":\"value2\",",
                "    \"type3\":\"value3\",",
                "    \"type4\":\"value4\",",
                "    \"type5\":\"value5\",",
                "    \"type6\":\"value6\"",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("type1", (Object) "value1"));
        assertThat(result, hasEntry("type2", (Object) "value2"));
        assertThat(result, hasEntry("type3", (Object) "value3"));
        assertThat(result, hasEntry("type4", (Object) "value4"));
        assertThat(result, hasEntry("type5", (Object) "value5"));
        assertThat(result, hasEntry("type6", (Object) "value6"));
        assertThat(result, hasEntry("type7.type1", (Object) "value1"));
        assertThat(result, hasEntry("type7.type2", (Object) "value2"));
        assertThat(result, hasEntry("type7.type3", (Object) "value3"));
        assertThat(result, hasEntry("type7.type4", (Object) "value4"));
        assertThat(result, hasEntry("type7.type5", (Object) "value5"));
        assertThat(result, hasEntry("type7.type6", (Object) "value6"));

        assertThat(result.get("type1"), instanceOf(String.class));
        assertThat(result.get("type2"), instanceOf(String.class));
        assertThat(result.get("type3"), instanceOf(String.class));
        assertThat(result.get("type4"), instanceOf(String.class));
        assertThat(result.get("type5"), instanceOf(String.class));
        assertThat(result.get("type6"), instanceOf(String.class));
        assertThat(result.get("type7.type1"), instanceOf(String.class));
        assertThat(result.get("type7.type2"), instanceOf(String.class));
        assertThat(result.get("type7.type3"), instanceOf(String.class));
        assertThat(result.get("type7.type4"), instanceOf(String.class));
        assertThat(result.get("type7.type5"), instanceOf(String.class));
        assertThat(result.get("type7.type6"), instanceOf(String.class));

    }

    @Test
    public void 各フィールドタイプがnullの場合に全てnullで読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 type1 [0..1] X",
                "2 type2 [0..1] N",
                "3 type3 [0..1] XN",
                "4 type4 [0..1] X9",
                "5 type5 [0..1] SX9",
                "6 type6 [0..1] BL",
                "7 type7 [0..1] OB",
                "",
                "[type7]",
                "1 type1 [0..1] X",
                "2 type2 [0..1] N",
                "3 type3 [0..1] XN",
                "4 type4 [0..1] X9",
                "5 type5 [0..1] SX9",
                "6 type6 [0..1] BL"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"type1\":null,",
                "  \"type2\":null,",
                "  \"type3\":null,",
                "  \"type4\":null,",
                "  \"type5\":null,",
                "  \"type6\":null,",
                "  \"type7\":{",
                "    \"type1\":null,",
                "    \"type2\":null,",
                "    \"type3\":null,",
                "    \"type4\":null,",
                "    \"type5\":null,",
                "    \"type6\":null",
                "  }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("type1", null));
        assertThat(result, hasEntry("type2", null));
        assertThat(result, hasEntry("type3", null));
        assertThat(result, hasEntry("type4", null));
        assertThat(result, hasEntry("type5", null));
        assertThat(result, hasEntry("type6", null));
        assertThat(result, hasEntry("type7.type1", null));
        assertThat(result, hasEntry("type7.type2", null));
        assertThat(result, hasEntry("type7.type3", null));
        assertThat(result, hasEntry("type7.type4", null));
        assertThat(result, hasEntry("type7.type5", null));
        assertThat(result, hasEntry("type7.type6", null));
    }

    @Test
    public void numberコンバータで数値型に変換できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"123456\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", (Object) new BigDecimal(123456)));
    }

    @Test
    public void numberコンバータでnullを使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number [0..1] X number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", null));
    }

    @Test
    public void numberコンバータで空文字が指定されてもエラーとならないこと() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number [0..1] X number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", null));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"-123456\"",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void signed_numberコンバータで符号付き数値の場合に型変換に成功すること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X signed_number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"-123456\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", (Object) new BigDecimal(-123456)));
    }

    @Test
    public void signed_numberコンバータでnullが使用できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number [0..1] X signed_number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", null));
    }

    @Test
    public void signed_numberコンバータで空文字が指定されてもエラーとならないこと() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number [0..1] X signed_number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", null));
    }

    @Test
    public void signed_numberコンバータで文字列型の場合に型変換に失敗すること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("invalid parameter format was specified."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X signed_number"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"number\":\"value\"",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"髙﨑唖\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "高崎■"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", null));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "custom"));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value\"",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "custom"));
    }

    @Test
    public void 閉じタグが存在しないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("JSON Parse Error. JSON data must ends with '}'"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":\"value\""
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void オブジェクト配列の要素に配列が設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("Field parent is Object Array but other item detected"));

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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    \"value\"",
                "  ]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":null",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意項目にnullが設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..1] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", null));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":null",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 必須配列の要素にnullが設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [1..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":[null]",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) new String[]{null}));
    }

    @Test
    public void 任意配列にnullが設定されているJSONを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key [0..10] X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"key\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) new String[]{}));
    }

    @Test
    public void 必須オブジェクトにnullが設定されているためエラーとなること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":null",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意オブジェクトにnullが設定されているJSONを読み込めること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result.size(), is(0));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":null",
                "  }",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void オブジェクトの任意項目にnullが設定されているJSONを読み込めること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", null));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":null",
                "  }",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void オブジェクトの任意配列にnullが設定されているJSONを読み込めること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":{",
                "    \"child\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", (Object) new String[]{}));
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":null",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意オブジェクト配列にnullが設定されているJSONを読み込めること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":null",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parentSize", (Object) "0"));
    }

    @Test
    public void オブジェクト配列内の必須項目にnullが設定されているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(CoreMatchers.containsString("BaseKey = parent[0],Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"JSON\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [0..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":null",
                "    }",
                "  ]",
                "}"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void オブジェクト配列内の任意項目にnullが設定されているJSONを読み込めること() throws Exception {
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

        // JSON
        InputStream input = createInputStream(
                "{",
                "  \"parent\":[",
                "    {",
                "      \"child\":null",
                "    }",
                "}"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", null));
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

    private InputStream createInputStream(String... records) throws Exception {
        File file = folder.newFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String record : records) {
            writer.write(record);
            writer.newLine();
        }
        writer.close();
        return new FileInputStream(file);
    }

    /**
     * カスタムの{@link ValueConvertor}実装クラス。
     */
    public static class CustomValueConvertor extends ValueConvertorSupport {

        @Override
        public Object convertOnRead(Object data) {
            return "custom";
        }

        @Override
        public Object convertOnWrite(Object data) {
            return null;
        }
    }

    /**
     * カスタムの{@link DataType}実装クラス。
     */
    public static class CustomDataType extends JsonString {

        @Override
        public String convertOnRead(String data) {
            return "custom";
        }
    }
}
