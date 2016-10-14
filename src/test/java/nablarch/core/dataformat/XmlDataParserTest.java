package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.XmlDataConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.dataformat.convertor.value.ValueConvertorSupport;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Ignore;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;

/**
 * {@link XmlDataParser}のテストを行います。
 * 
 * @author TIS
 */
public class XmlDataParserTest  {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /** テスト対象 */
    private XmlDataParser sut = new XmlDataParser();
    
    @Test
    public void ルート要素の必須属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X"
        );

        // インプット
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value\"></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value"));
    }

    @Test
    public void ルート要素の必須属性に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void ルート要素の任意属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value\"></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value"));
    }

    @Test
    public void ルート要素の任意属性に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", null));
    }

    @Ignore
    @Test
    public void ルート要素の必須コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>value</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("body", (Object) "value"));
    }

    @Ignore
    @Test
    public void ルート要素の必須コンテンツに値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field body is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Ignore
    @Test
    public void ルート要素の任意コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>value</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("body", (Object) "value"));
    }

    @Ignore
    @Test
    public void ルート要素の任意コンテンツに値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("body", null));
    }

    @Ignore
    @Test
    public void ルート要素の必須属性と必須コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">value2</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        assertThat(result, hasEntry("body", (Object) "value2"));
    }

    @Ignore
    @Test
    public void ルート要素の必須属性と必須コンテンツに値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Ignore
    @Test
    public void ルート要素の任意属性と任意コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">value2</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        assertThat(result, hasEntry("body", (Object) "value2"));
    }

    @Ignore
    @Test
    public void ルート要素の任意属性と任意コンテンツに値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", null));
    }

    @Test
    public void ルート要素の必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child", (Object) "value"));
    }

    @Test
    public void ルート要素の必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void ルート要素の任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child", (Object) "value"));
    }

    @Test
    public void ルート要素の任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child", null));
    }

    @Test
    public void ルート要素の必須属性と必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">",
                "  <child>value2</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        assertThat(result, hasEntry("child", (Object) "value2"));
    }

    @Test
    public void ルート要素の必須属性と必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void ルート要素の任意属性と任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">",
                "  <child>value2</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        assertThat(result, hasEntry("child", (Object) "value2"));
    }

    @Test
    public void ルート要素の任意属性と任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root></root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", null));
        assertThat(result, hasEntry("child", null));
    }

    @Test
    public void 子要素の必須属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value\"></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", (Object) "value"));
    }

    @Test
    public void 子要素の必須属性に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("child,Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value\"></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", (Object) "value"));
    }

    @Test
    public void 子要素の任意属性に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", null));
    }

    @Ignore
    @Test
    public void 子要素の必須属性と必須コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\">value2</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", (Object) "value1"));
        assertThat(result, hasEntry("child.body", (Object) "value2"));
    }

    @Ignore
    @Test
    public void 子要素の必須属性と必須コンテンツに値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("child,Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Ignore
    @Test
    public void 子要素の任意属性と任意コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\">value2</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", (Object) "value1"));
        assertThat(result, hasEntry("child.body", (Object) "value2"));
    }

    @Ignore
    @Test
    public void 子要素の任意属性と任意コンテンツに値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", null));
        assertThat(result, hasEntry("child.body", null));
    }

    @Test
    public void 子要素の必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", (Object) "value"));
    }

    @Test
    public void 子要素の必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("parent,Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", (Object) "value"));
    }

    @Test
    public void 子要素の任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.child", null));
    }

    @Test
    public void 子要素の必須属性と必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent attr=\"value1\">",
                "    <child>value2</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.attr", (Object) "value1"));
        assertThat(result, hasEntry("parent.child", (Object) "value2"));
    }

    @Test
    public void 子要素の必須属性と必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("parent,Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意属性と任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 @attr [0..1] X",
                "2 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent attr=\"value1\">",
                "    <child>value2</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.attr", (Object) "value1"));
        assertThat(result, hasEntry("parent.child", (Object) "value2"));
    }

    @Test
    public void 子要素の任意属性と任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 @attr [0..1] X",
                "2 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent.attr", null));
        assertThat(result, hasEntry("parent.child", null));
    }

    @Test
    public void 子要素の配列の必須コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value1</child>",
                "  <child>value2</child>",
                "  <child>value3</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
    }

    @Test
    public void 子要素の配列の必須コンテンツに値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の配列の任意コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value1</child>",
                "  <child>value2</child>",
                "  <child>value3</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("child");
        assertThat(child, hasItemInArray("value1"));
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
    }

    @Test
    public void 子要素の配列の任意コンテンツに値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 子要素の配列の必須属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\"></child>",
                "  <child attr=\"value2\"></child>",
                "  <child attr=\"value3\"></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", (Object) "value1"));
        assertThat(result, hasEntry("child[1].attr", (Object) "value2"));
        assertThat(result, hasEntry("child[2].attr", (Object) "value3"));
    }

    @Test
    public void 子要素の配列の必須属性に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("child[0],Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "  <child></child>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の配列の任意属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\"></child>",
                "  <child attr=\"value2\"></child>",
                "  <child attr=\"value3\"></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", (Object) "value1"));
        assertThat(result, hasEntry("child[1].attr", (Object) "value2"));
        assertThat(result, hasEntry("child[2].attr", (Object) "value3"));
    }

    @Test
    public void 子要素の配列の任意属性に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "  <child></child>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", null));
        assertThat(result, hasEntry("child[1].attr", null));
        assertThat(result, hasEntry("child[2].attr", null));
    }

    @Ignore
    @Test
    public void 子要素の配列の必須属性と必須コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\">value4</child>",
                "  <child attr=\"value2\">value5</child>",
                "  <child attr=\"value3\">value6</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", (Object) "value1"));
        assertThat(result, hasEntry("child[1].attr", (Object) "value2"));
        assertThat(result, hasEntry("child[2].attr", (Object) "value3"));
        assertThat(result, hasEntry("child[0].body", (Object) "value4"));
        assertThat(result, hasEntry("child[1].body", (Object) "value5"));
        assertThat(result, hasEntry("child[2].body", (Object) "value6"));
    }

    @Ignore
    @Test
    public void 子要素の配列の必須属性と必須コンテンツに値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("child[0],Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "  <child></child>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Ignore
    @Test
    public void 子要素の配列の任意属性と任意コンテンツに値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\">value4</child>",
                "  <child attr=\"value2\">value5</child>",
                "  <child attr=\"value3\">value6</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", (Object) "value1"));
        assertThat(result, hasEntry("child[1].attr", (Object) "value2"));
        assertThat(result, hasEntry("child[2].attr", (Object) "value3"));
        assertThat(result, hasEntry("child[0].body", (Object) "value4"));
        assertThat(result, hasEntry("child[1].body", (Object) "value5"));
        assertThat(result, hasEntry("child[2].body", (Object) "value6"));
    }

    @Ignore
    @Test
    public void 子要素の配列の任意属性と任意コンテンツに値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..10] OB",
                "",
                "[child]",
                "1 @attr [0..1] X",
                "2 body [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child></child>",
                "  <child></child>",
                "  <child></child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child[0].attr", null));
        assertThat(result, hasEntry("child[1].attr", null));
        assertThat(result, hasEntry("child[2].attr", null));
        assertThat(result, hasEntry("child[0].body", null));
        assertThat(result, hasEntry("child[1].body", null));
        assertThat(result, hasEntry("child[2].body", null));
    }

    @Test
    public void 子要素の配列の必須子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value4</child>",
                "    <child>value5</child>",
                "    <child>value6</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value7</child>",
                "    <child>value8</child>",
                "    <child>value9</child>",
                "  </parent>",
                "</root>"
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
    public void 子要素の配列の必須子要素の配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の配列の任意子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value4</child>",
                "    <child>value5</child>",
                "    <child>value6</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value7</child>",
                "    <child>value8</child>",
                "    <child>value9</child>",
                "  </parent>",
                "</root>"
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
    public void 子要素の配列の任意子要素の配列に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, is(emptyArray()));

        child = (String[]) result.get("parent[1].child");
        assertThat(child, is(emptyArray()));

        child = (String[]) result.get("parent[2].child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 子要素の配列の必須属性と必須子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent attr=\"value1\">",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "    <child>value4</child>",
                "  </parent>",
                "  <parent attr=\"value5\">",
                "    <child>value6</child>",
                "    <child>value7</child>",
                "    <child>value8</child>",
                "  </parent>",
                "  <parent attr=\"value9\">",
                "    <child>value10</child>",
                "    <child>value11</child>",
                "    <child>value12</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].attr", (Object) "value1"));
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
        assertThat(child, hasItemInArray("value4"));

        assertThat(result, hasEntry("parent[1].attr", (Object) "value5"));
        child = (String[]) result.get("parent[1].child");
        assertThat(child, hasItemInArray("value6"));
        assertThat(child, hasItemInArray("value7"));
        assertThat(child, hasItemInArray("value8"));

        assertThat(result, hasEntry("parent[2].attr", (Object) "value9"));
        child = (String[]) result.get("parent[2].child");
        assertThat(child, hasItemInArray("value10"));
        assertThat(child, hasItemInArray("value11"));
        assertThat(child, hasItemInArray("value12"));

    }

    @Test
    public void 子要素の配列の必須属性と必須子要素の配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("parent[0],Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の配列の任意属性と任意子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 @attr [0..1] X",
                "2 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent attr=\"value1\">",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "    <child>value4</child>",
                "  </parent>",
                "  <parent attr=\"value5\">",
                "    <child>value6</child>",
                "    <child>value7</child>",
                "    <child>value8</child>",
                "  </parent>",
                "  <parent attr=\"value9\">",
                "    <child>value10</child>",
                "    <child>value11</child>",
                "    <child>value12</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].attr", (Object) "value1"));
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
        assertThat(child, hasItemInArray("value4"));

        assertThat(result, hasEntry("parent[1].attr", (Object) "value5"));
        child = (String[]) result.get("parent[1].child");
        assertThat(child, hasItemInArray("value6"));
        assertThat(child, hasItemInArray("value7"));
        assertThat(child, hasItemInArray("value8"));

        assertThat(result, hasEntry("parent[2].attr", (Object) "value9"));
        child = (String[]) result.get("parent[2].child");
        assertThat(child, hasItemInArray("value10"));
        assertThat(child, hasItemInArray("value11"));
        assertThat(child, hasItemInArray("value12"));
    }

    @Test
    public void 子要素の配列の任意属性と任意子要素の配列に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 @attr [0..1] X",
                "2 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].attr", null));
        String[] child = (String[]) result.get("parent[0].child");
        assertThat(child, is(emptyArray()));

        assertThat(result, hasEntry("parent[1].attr", null));
        child = (String[]) result.get("parent[1].child");
        assertThat(child, is(emptyArray()));

        assertThat(result, hasEntry("parent[2].attr", null));
        child = (String[]) result.get("parent[2].child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void ルート要素の必須属性と必須子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">",
                "  <child>value2</child>",
                "  <child>value3</child>",
                "  <child>value4</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        String[] child = (String[]) result.get("child");
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
        assertThat(child, hasItemInArray("value4"));

    }

    @Test
    public void ルート要素の必須属性と必須子要素の配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void ルート要素の任意属性と任意子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">",
                "  <child>value2</child>",
                "  <child>value3</child>",
                "  <child>value4</child>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        String[] child = (String[]) result.get("child");
        assertThat(child, hasItemInArray("value2"));
        assertThat(child, hasItemInArray("value3"));
        assertThat(child, hasItemInArray("value4"));
    }

    @Test
    public void ルート要素の任意属性と任意子要素の配列に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr [0..1] X",
                "2 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", null));
        String[] child = (String[]) result.get("child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 必須子要素の配列と必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child1 [1..10] X",
                "2 child2 X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child1>value1</child1>",
                "  <child1>value2</child1>",
                "  <child2>value3</child2>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("child1");
        assertThat(child1, hasItemInArray("value1"));
        assertThat(child1, hasItemInArray("value2"));
        assertThat(result, hasEntry("child2", (Object) "value3"));
    }

    @Test
    public void 必須子要素の配列と必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("FieldName=child1:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child1 [1..10] X",
                "2 child2 X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 任意子要素の配列と任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child1 [0..10] X",
                "2 child2 [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child1>value1</child1>",
                "  <child1>value2</child1>",
                "  <child2>value3</child2>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("child1");
        assertThat(child1, hasItemInArray("value1"));
        assertThat(child1, hasItemInArray("value2"));
        assertThat(result, hasEntry("child2", (Object) "value3"));
    }

    @Test
    public void 任意子要素の配列と任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child1 [0..10] X",
                "2 child2 [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child1 = (String[]) result.get("child1");
        assertThat(child1, is(emptyArray()));
        assertThat(result, hasEntry("child2", null));
    }

    @Test
    public void 子要素の必須子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "  </parent>",
                "</root>"
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
    public void 子要素の必須子要素の配列に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("parent,FieldName=child:MinCount=1:MaxCount=10:Actual=0"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [1..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素の任意子要素の配列に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "    <child>value2</child>",
                "    <child>value3</child>",
                "  </parent>",
                "</root>"
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
    public void 子要素の任意子要素の配列に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent OB",
                "",
                "[parent]",
                "1 child [0..10] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        String[] child = (String[]) result.get("parent.child");
        assertThat(child, is(emptyArray()));
    }

    @Test
    public void 子要素配列の必須子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value2</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value3</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", (Object) "value1"));
        assertThat(result, hasEntry("parent[1].child", (Object) "value2"));
        assertThat(result, hasEntry("parent[2].child", (Object) "value3"));

    }

    @Test
    public void 子要素配列の必須子要素に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("parent[0],Field child is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 子要素配列の任意子要素に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "    <child>value1</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value2</child>",
                "  </parent>",
                "  <parent>",
                "    <child>value3</child>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", (Object) "value1"));
        assertThat(result, hasEntry("parent[1].child", (Object) "value2"));
        assertThat(result, hasEntry("parent[2].child", (Object) "value3"));
    }

    @Test
    public void 子要素配列の任意子要素に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent [1..10] OB",
                "",
                "[parent]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <parent>",
                "  </parent>",
                "  <parent>",
                "  </parent>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("parent[0].child", null));
        assertThat(result, hasEntry("parent[1].child", null));
    }

    @Test
    public void 複数の必須属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr1 X",
                "2 @attr2 X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr1=\"value1\" attr2=\"value2\">",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr1", (Object) "value1"));
        assertThat(result, hasEntry("attr2", (Object) "value2"));
    }

    @Test
    public void 複数の必須属性に値が設定されていないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr1 is required"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr1 X",
                "2 @attr2 X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 複数の任意属性に値が設定されているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr1 [0..1] X",
                "2 @attr2 [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr1=\"value1\" attr2=\"value2\">",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr1", (Object) "value1"));
        assertThat(result, hasEntry("attr2", (Object) "value2"));
    }

    @Test
    public void 複数の任意属性に値が設定されていないXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr1 [0..1] X",
                "2 @attr2 [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr1", null));
        assertThat(result, hasEntry("attr2", null));
    }

    @Test
    public void フォーマット定義されていない項目が存在するXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [0..1] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value1</child>",
                "  <undefined>value2</undefined>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child", (Object) "value1"));
        assertThat(result, not(hasKey("undefined")));
    }

    @Test
    public void 配列の要素数が超過しているためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("FieldName=child:MinCount=1:MaxCount=3:Actual=4"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child [1..3] X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child>value1</child>",
                "  <child>value2</child>",
                "  <child>value3</child>",
                "  <child>value4</child>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void タグが自己完結しているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <child attr=\"value1\" />",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("child.attr", (Object) "value1"));
    }

    @Test
    public void タグ名が重複しているXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 parent1 OB",
                "2 parent2 OB",
                "3 @attr X",
                "4 child X",
                "",
                "[parent1]",
                "1 @attr X",
                "2 child X",
                "",
                "[parent2]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value1\">",
                "  <child>value2</child>",
                "  <parent1 attr=\"value3\">",
                "    <child>value4</child>",
                "  </parent1>",
                "  <parent2 attr=\"value5\">",
                "    <child>value6</child>",
                "  </parent2>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("attr", (Object) "value1"));
        assertThat(result, hasEntry("child", (Object) "value2"));
        assertThat(result, hasEntry("parent1.attr", (Object) "value3"));
        assertThat(result, hasEntry("parent1.child", (Object) "value4"));
        assertThat(result, hasEntry("parent2.attr", (Object) "value5"));
        assertThat(result, hasEntry("parent2.child", (Object) "value6"));
    }

    @Test
    public void ルートのタグ名が異なるためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("expected node [root] not found."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X",
                "2 child X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<data attr=\"value1\">",
                "  <child>value2</child>",
                "</data>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void ネームスペースを設定したXMLを読み込めること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[testns:root]",
                "1 ?@xmlns:testns X \"http://testns.hoge.jp/apply\"",
                "2 testns:key X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<testns:root xmlns:testns=\"http://testns.hoge.jp/apply\">",
                "  <testns:key>value</testns:key>",
                "</testns:root>");

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, not(hasEntry("xmlns:testns", (Object) "http://testns.hoge.jp/apply")));
        assertThat(result, hasEntry("testnsKey", (Object) "value"));
    }

    @Test
    public void 属性にフィールドタイプにOBを設定してXMLを読み込めること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("Field attr is Object but specified by Attribute"));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr OB"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value\">",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void 各フィールドタイプが全て文字列型に変換されること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
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

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <type1>value1</type1>",
                "  <type2>value2</type2>",
                "  <type3>value3</type3>",
                "  <type4>value4</type4>",
                "  <type5>value5</type5>",
                "  <type6>value6</type6>",
                "  <type7>",
                "    <type1>value1</type1>",
                "    <type2>value2</type2>",
                "    <type3>value3</type3>",
                "    <type4>value4</type4>",
                "    <type5>value5</type5>",
                "    <type6>value6</type6>",
                "  </type7>",
                "</root>"
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
    public void numberコンバータで数値型に変換できること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X9 number"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <number>123456</number>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", (Object) new BigDecimal(123456)));
    }

    @Test
    public void numberコンバータで符号付き数値の場合に型変換に失敗すること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("invalid parameter format was specified."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number X9 number"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <number>-123456</number>",
                "</root>"
        );

        // テスト実行
        sut.parseData(input, definition);
    }

    @Test
    public void signed_numberコンバータで符号付き数値の場合に型変換に成功すること() throws Exception {

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number SX9 signed_number"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <number>-123456</number>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("number", (Object) new BigDecimal(-123456)));
    }

    @Test
    public void signed_numberコンバータで文字列型の場合に型変換に失敗すること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("invalid parameter format was specified."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 number SX9 signed_number"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <number>value</number>",
                "</root>"
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
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X replacement(\"type\")"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <key>髙﨑唖</key>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "高崎■"));
    }

    @Test
    public void 独自コンバータが適用されること() throws Exception {

        XmlDataConvertorSetting setting = repositoryResource.getComponent("xmlDataConvertorSetting");
        setting.getConvertorFactory().getConvertorTable().put("custom", CustomValueConvertor.class);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key X custom"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <key>value</key>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "custom"));
    }

    @Test
    public void 独自フィールドタイプが適用されること() throws Exception {

        XmlDataConvertorSetting setting = repositoryResource.getComponent("xmlDataConvertorSetting");
        setting.getConvertorFactory().getConvertorTable().put("CM", CustomDataType.class);

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 key CM"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <key>value</key>",
                "</root>"
        );

        // テスト実行
        Map<String, ?> result = sut.parseData(input, definition);

        // 検証
        assertThat(result, hasEntry("key", (Object) "custom"));
    }

    @Test
    public void 閉じタグが存在しないためエラーとなること() throws Exception {
        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage(containsString("invalid data found."));

        // フォーマット定義
        LayoutDefinition definition = createLayoutDefinition(
                "file-type:        \"XML\"",
                "text-encoding:    \"UTF-8\"",
                "[root]",
                "1 @attr X"
        );

        // XML
        InputStream input = createInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root attr=\"value\">"
        );

        // テスト実行
        sut.parseData(input, definition);
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
        DataRecordFormatter formatter = new XmlDataRecordFormatter();
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
    public static class CustomDataType extends CharacterStreamDataString {

        @Override
        public String convertOnRead(String data) {
            return "custom";
        }
    }
}