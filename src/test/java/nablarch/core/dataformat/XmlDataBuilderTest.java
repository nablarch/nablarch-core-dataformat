package nablarch.core.dataformat;

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.HashMap;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * {@link XmlDataParser}のテストを行います。
 *
 * @author TIS
 */
public class XmlDataBuilderTest {

    @Rule
    public TestName testNameRule = new TestName();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private XmlDataBuilder sut = new XmlDataBuilder();

    /**
     * テストケースごとにデフォルト設定でリポジトリを再構築します。
     */
    @Before
    public void setUp() {
        SystemRepository.clear();
        SystemRepository.load(
                new DiContainer(
                        new XmlComponentDefinitionLoader(
                                "nablarch/core/dataformat/XmlBuilder.xml")));
    }

    /**
     * フォーマット定義ファイル名を取得します
     *
     * @return フォーマット定義ファイル名
     */
    private String getFormatFileName() {
        FilePathSetting fps = FilePathSetting.getInstance()
                                             .addBasePathSetting("format", temporaryFolder.getRoot()
                                                                                          .toURI()
                                                                                          .toString())
                                             .addFileExtensions("fortmat", "fmt");
        return fps.getBasePathSettings()
                  .get("format")
                  .getPath() + '/' + testNameRule.getMethodName() + ".fmt";
    }

    /**
     * フォーマット定義情報を取得します。
     *
     * @return フォーマット定義情報
     */
    private LayoutDefinition getLayoutDefinition() {
        LayoutDefinition ld = new LayoutFileParser(getFormatFileName()).parse();
        DataRecordFormatter formatter = new XmlDataRecordFormatter();
        formatter.setDefinition(ld);
        formatter.initialize();
        return ld;
    }

    private void createFormatFile(String charset, String... lines) throws Exception {
        final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(getFormatFileName()), "utf-8"));
        writer.append("file-type: \"XML\"");
        writer.newLine();
        writer.append("text-encoding: \"")
              .append(charset)
              .append("\"");
        writer.newLine();

        for (final String line : lines) {
            writer.append(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    @Test
    public void ルートタグのみのXMLが生成できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[data]",
                "1 child [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><data></data>"));
    }

    @Test
    public void ルートタグに属性が出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[data]",
                "1 @name X",
                "2 @age  [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("name", "属性");
        input.put("age", 50);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><data name=\"属性\" age=\"50\"></data>"));
    }

    @Test
    public void ルートタグの必須の属性を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[data]",
                "1 @name X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("name is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void ルートタグの任意の属性を指定しなくてもエラーとならないこと() throws Exception {
        createFormatFile(
                "UTF-8",
                "[data]",
                "1 @name X",
                "2 @age  [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("name", "属性");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><data name=\"属性\"></data>"));
    }

    @Test
    public void ルートタグにコンテンツが出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("body", "データ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<root>データ</root>"));
    }
    
    @Test
    public void ルートタグのコンテンツが任意の場合に出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("body", "任意項目でも問題ない");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<root>任意項目でも問題ない</root>"));
    }

    @Test
    public void ルートタグのコンテンツが任意の場合で値を指定しなかった場合空で出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<root></root>"));
    }

    @Test
    public void コンテンツ名を変更した場合その名前の要素がコンテンツ部に出力されること() throws Exception {
        sut.setContentName("content");

        createFormatFile(
                "UTF-8",
                "[root]",
                "1 content [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("content", "この値がコンテンツ部に出力される");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<root>この値がコンテンツ部に出力される</root>"));
    }

    @Test
    public void ルートのコンテンツが必須で指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("body is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void ルートに属性とコンテンツの組み合わせが使えること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @attr X",
                "2 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性");
        input.put("body", "コンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        sut.buildData(input, getLayoutDefinition(), actual);
        
        System.out.println(actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root attr=\"属性\">コンテンツ</root>"));
    }
    
    @Test
    public void ルートに属性とコンテンツの組み合わせがあってコンテンツがnullの場合コンテンツは空となること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @attr X",
                "2 body [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性");
        input.put("body", null);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root attr=\"属性\"></root>"));
    }

    @Test
    public void ルートに属性とコンテンツの組み合わせがあって必須のコンテンツの値を指定しない場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @attr X",
                "2 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("body is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }
    
    

    @Test
    public void ネストしたタグが出力できること() throws Exception {

        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child1 X",
                "2 child2 [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child1", "ネスト要素");
        input.put("child2", 100);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child1>ネスト要素</child1>\n"
                        + "  <child2>100</child2>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void ネストした任意のタグを指定しなくても出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child1 X",
                "2 child2 [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child1", "ネスト要素");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child1>ネスト要素</child1>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void ネストした必須のタグを指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child1 X",
                "2 child2 [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child2", 99);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("child1 is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 属性と子要素の組み合わせが出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @name X",
                "2 child X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("name", "なまえ");
        input.put("child", 99);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root name=\"なまえ\">\n"
                        + "  <child>99</child>\n"
                        + "</root>\n").ignoreWhitespace());
    }

    @Test
    public void 属性と子要素が任意の場合指定しなくても出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @name [0..1] X",
                "2 child [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "</root>\n").ignoreWhitespace());
    }

    @Test
    public void 子要素に属性が使用できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child [0..1] OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性の値");
        input.put("child.any", "任意の属性");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr=\"属性の値\" any='任意の属性'></child>\n"
                        + "</root>\n").ignoreWhitespace());
    }

    @Test
    public void 子要素の任意属性を指定しない場合でも出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child [0..1] OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性の値");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr=\"属性の値\"></child>\n"
                        + "</root>\n").ignoreWhitespace());
    }

    @Test
    public void 子要素の必須属性を指定しない場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child [0..1] OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.any", "任意のみ指定");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("attr is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void タグを複数ネスト出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 content1 X",
                "2 content2 [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.content1", "データ");
        input.put("child.content2", "データ２");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child>\n"
                        + "    <content1>データ</content1>\n"
                        + "    <content2>データ２</content2>\n"
                        + "  </child>\n"
                        + "</root>\n").ignoreWhitespace());
    }

    @Test
    public void 複数ネストした場合で任意の要素を指定しなくても出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 content1 X",
                "2 content2 [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.content1", "データ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child>\n"
                        + "    <content1>データ</content1>\n"
                        + "  </child>\n"
                        + "</root>\n").ignoreWhitespace());
    }
    
    @Test
    public void 複数ネストした場合でネスト要素から始まるキーが存在しない場合エラーになること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 content1 [0..1] X",
                "2 content2 [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("dummy.data", "データ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("child is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 複数ネストした場合で必須要素を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 content1 X",
                "2 content2 [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.content2", "データ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("content1 is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 子要素の属性が出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X",
                "3 content X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性値");
        input.put("child.any", "任意属性");
        input.put("child.content", "子供のコンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr='属性値' any='任意属性'>\n"
                        + "    <content>子供のコンテンツ</content>\n"
                        + "  </child>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 子要素の任意属性を指定しなかった場合でも出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X",
                "3 content X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性値");
        input.put("child.content", "子供のコンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr='属性値'>\n"
                        + "    <content>子供のコンテンツ</content>\n"
                        + "  </child>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 子要素の必須属性を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "",
                "[child]",
                "1 @attr X",
                "2 @any  [0..1] X",
                "3 content X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.any", "任意の属性");
        input.put("child.content", "子供のコンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("attr is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 子要素に属性とコンテンツが出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性値");
        input.put("child.body", "属性値とセットで出力されるコンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr='属性値'>\n"
                        + "    属性値とセットで出力されるコンテンツ\n"
                        + "  </child>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 子要素の属性ありのコンテンツを省略できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "[child]",
                "1 @attr X",
                "2 body [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性値");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <child attr='属性値'>\n"
                        + "  </child>\n"
                        + "</root>").ignoreWhitespace());
    }
    
    @Test
    public void 子要素の属性ありの必須コンテンツ指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "[child]",
                "1 @attr X",
                "2 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child.attr", "属性値");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("body is required");
        sut.buildData(input, getLayoutDefinition(), actual);

    }

    @Test
    public void 配列の子要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children", new String[] {"子供１", "子供２"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>子供１</children>\n"
                        + "  <children>子供２</children>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列の任意子要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [0..2] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children", new String[] {"子供１", "子供２"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>子供１</children>\n"
                        + "  <children>子供２</children>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列の任意子要素に空の配列を指定しても出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [0..2] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children", new String[] {});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列の任意子要素を指定しなくても出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [0..2] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 必須の配列子要素を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..2] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("children is required");
        sut.buildData(input, getLayoutDefinition(), actual);

    }

    @Test
    public void 配列子要素に属性が使えること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [0..2] OB",
                "[children]",
                "1 @name X",
                "2 @age [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].name", "子供１");
        input.put("children[0].age", "20");
        input.put("children[1].name", "子供２");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children name='子供１' age='20'></children>\n"
                        + "  <children name='子供２'></children>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列子要素の必須属性を指定しなかった場合エラーになること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [0..2] OB",
                "[children]",
                "1 @name X",
                "2 @age [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].age", "20");
        input.put("children[1].name", "子供２");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("name is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 子要素を持つ配列を出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 child [1..10]OB",
                "[child]",
                "1 name X",
                "2 age [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child[0].name", "子供１");
        input.put("child[0].age", 30);
        input.put("child[1].name", "子供２");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <child>\n"
                        + "    <name>子供１</name>\n"
                        + "    <age>30</age>\n"
                        + "  </child>\n"
                        + "  <child>\n"
                        + "    <name>子供２</name>\n"
                        + "  </child>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列の必須子要素を指定しなかった場合エラーになること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 child [1..10]OB",
                "[child]",
                "1 name X",
                "2 age [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child[0].age", 30);
        input.put("child[1].name", "子供２");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("name is required");
        sut.buildData(input, getLayoutDefinition(), actual);

    }

    @Test
    public void 配列に属性とコンテンツを出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 child [1..10]OB",
                "[child]",
                "1 @name X",
                "2 body [0..1] X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child[0].name", "子供1");
        input.put("child[0].body", "コンテンツ");
        input.put("child[1].name", "子供2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <child name='子供1'>\n"
                        + "    コンテンツ\n"
                        + "  </child>\n"
                        + "  <child name='子供2'>\n"
                        + "  </child>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列に属性とコンテンツがある場合で必須のコンテンツを指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 child [1..10]OB",
                "[child]",
                "1 @name X",
                "2 body X9"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("child[0].name", "子供1");
        input.put("child[1].name", "子供2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("body is required");
        sut.buildData(input, getLayoutDefinition(), actual);

    }
    
    @Test
    public void 属性を持つ要素に配列子要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性値");
        input.put("child", new String[] {"子供１", "子供２"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent attr='属性値'>\n"
                        + "  <child>子供１</child>\n"
                        + "  <child>子供２</child>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 属性を持つ要素の任意配列子要素を指定しなくても出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 @attr X",
                "2 child [0..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性値");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent attr='属性値'>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 属性を持つ要素の必須配列子要素を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 @attr X",
                "2 child [1..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("attr", "属性値");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("child is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 配列要素と非配列要素を並列に出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 size X9",
                "2 child [1..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("size", 3);
        input.put("child", new String[] {"1", "2", "3"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <size>3</size>\n"
                        + "  <child>1</child>\n"
                        + "  <child>2</child>\n"
                        + "  <child>3</child>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 任意の配列要素と非配列要素を並列に出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 size X9",
                "2 child [0..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("size", 3);
        input.put("child", new String[] {"1", "2", "3"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <size>3</size>\n"
                        + "  <child>1</child>\n"
                        + "  <child>2</child>\n"
                        + "  <child>3</child>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 任意の配列要素を指定しない場合と非配列要素のみが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 size X9",
                "2 child [0..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("size", 0);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <size>0</size>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 必須の配列要素と非配列要素がある場合で配列要素を指定しない場合エラーが発生すること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 size X9",
                "2 child [1..10] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("size", 0);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("child is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 子要素を持つ配列要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 data X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].data", "1");
        input.put("children[1].data", "2");
        input.put("children[2].data", "3");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children><data>1</data></children>\n"
                        + "  <children><data>2</data></children>\n"
                        + "  <children><data>3</data></children>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 任意子要素を持つ配列を出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 data [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].data", 1);
        input.put("children[1].data", 2);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children><data>1</data></children>\n"
                        + "  <children><data>2</data></children>\n"
                        + "</parent>").ignoreWhitespace());
    }

    @Test
    public void 配列の子要素として配列要素を出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 @name X",
                "2 mail [1..*] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].name", "child1");
        input.put("children[0].mail", new String[] {"mail1@child1.com", "mail2@child1.com"});
        input.put("children[1].name", "child2");
        input.put("children[1].mail", new String[] {"mail1@child2.com"});
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children name=\"child1\">\n"
                        + "    <mail>mail1@child1.com</mail>\n"
                        + "    <mail>mail2@child1.com</mail>\n"
                        + "  </children>\n"
                        + "  <children name=\"child2\">\n"
                        + "    <mail>mail1@child2.com</mail>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }

    @Test
    public void 配列の子要素の任意配列を省略できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 @name X",
                "2 mail [0..*] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].name", "child1");
        input.put("children[0].mail", new String[] {"mail1@child1.com", "mail2@child1.com"});
        input.put("children[1].name", "child2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children name=\"child1\">\n"
                        + "    <mail>mail1@child1.com</mail>\n"
                        + "    <mail>mail2@child1.com</mail>\n"
                        + "  </children>\n"
                        + "  <children name=\"child2\">\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }

    @Test
    public void 配列の子要素の必須配列を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 @name X",
                "2 mail [1..*] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].name", "child1");
        input.put("children[0].mail", new String[] {"mail1@child1.com", "mail2@child1.com"});
        input.put("children[1].name", "child2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("mail is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void オブジェクトの孫要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children.child.name", "子供");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child><name>子供</name></child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }

    @Test
    public void オブジェクトの任意の孫要素を省略できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children.child", null);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child></child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }
    
    @Test
    public void オブジェクトの必須の孫要素を指定しなかった場合エラーになること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children.child", null);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("name is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void オブジェクトの孫要素に属性とコンテンツを出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 @name  X",
                "2 body   X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children.child.name", "属性");
        input.put("children.child.body", "コンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child name='属性'>コンテンツ</child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }
    
    @Test
    public void 配列の孫要素を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].child.name", "子供1");
        input.put("children[1].child.name", "子供2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child><name>子供1</name></child>\n"
                        + "  </children>\n"
                        + "  <children>\n"
                        + "    <child><name>子供2</name></child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }

    @Test
    public void 配列の任意の孫要素を省略できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name [0..1] X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].child", null);
        input.put("children[1].child.name", null);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child></child>\n"
                        + "  </children>\n"
                        + "  <children>\n"
                        + "    <child><name></name></child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }
    

    @Test
    public void 配列の必須の孫要素を指定しなかった場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 name  X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].child", null);
        input.put("children[1].child.name", "子供2");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("name is required");
        sut.buildData(input, getLayoutDefinition(), actual);

    }

    @Test
    public void 配列の子要素に属性が出力できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[parent]",
                "1 children [1..*] OB",
                "[children]",
                "1 child OB",
                "[child]",
                "1 @name [0..1] X",
                "2 body X"
        );

        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("children[0].child.name", "子供1");
        input.put("children[0].child.body", "子供1のコンテンツ");
        input.put("children[1].child.name", "子供2");
        input.put("children[1].child.body", "子供2のコンテンツ");
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println(actual.toString("utf-8"));

        assertThat(actual.toString("utf-8"), isIdenticalTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<parent>\n"
                        + "  <children>\n"
                        + "    <child name='子供1'>子供1のコンテンツ</child>\n"
                        + "  </children>\n"
                        + "  <children>\n"
                        + "    <child name='子供2'>子供2のコンテンツ</child>\n"
                        + "  </children>\n"
                        + "</parent>"
        ).ignoreWhitespace());
    }

    @Test
    public void 出力対象にnullを指定した場合で子要素を持つ場合ルート要素だけが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data [0..1] OB",
                "[data]",
                "1 name X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(null, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root></root>").ignoreWhitespace());
    }
    
    @Test
    public void 出力対象にnullを指定した場合で非配列要素を保つ場合ルート要素だけが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data [0..1] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(null, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root></root>").ignoreWhitespace());
    }

    @Test
    public void 出力対象にnullを指定した場合で配列要素を保つ場合ルート要素だけが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..*] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(null, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root></root>").ignoreWhitespace());
    }

    @Test
    public void 出力対象にnullを指定した場合で階層構造の場合ルート要素だけが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..*] X",
                "[data]",
                "1 name X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(null, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root></root>").ignoreWhitespace());
    }

    @Test
    public void 出力対象に空のMapを指定した場合ルート要素のみが出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..1] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        sut.buildData(new HashMap<String, Object>(), getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root></root>").ignoreWhitespace());
    }

    @Test
    public void 出力対象が空文字列の場合コンテンツは空となること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("data", "");
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root><data /></root>").ignoreWhitespace());

    }
    
    @Test
    public void 出力対象がnullで必須の場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("data", null);

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("data is required");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 出力対象がnullで任意項目の場合その要素のコンテンツは空になること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..1] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("data", null);
        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root><data /></root>").ignoreWhitespace());
    }

    @Test
    public void 配列要素で指定要素より大きい場合はエラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..2] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("data", new String[] {"1" ,"2", "3"});

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("FieldName=data:MinCount=0:MaxCount=2:Actual=3");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 配列オブジェクトで指定要素より大きい場合はエラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data  [0..2] OB",
                "[data]",
                "1 name X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final HashMap<String, Object> input = new HashMap<String, Object>();
        input.put("data[0].name", "1");
        input.put("data[1].name", "2");
        input.put("data[2].name", "3");
        input.put("data[3].name", "4");
        
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("FieldName=data:MinCount=0:MaxCount=2:Actual=4");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 文字列_X_N_XNタイプが使用できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 X X",
                "2 N N",
                "3 XN XN"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("X", "あいうえお");
        input.put("N", 100);
        input.put("XN", BigDecimal.ONE);

        sut.buildData(input, getLayoutDefinition(), actual);
        
        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <X>あいうえお</X>\n"
                        + "  <N>100</N>\n"
                        + "  <XN>1</XN>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 数字系_X9_XS9タイプが使用できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 X9 X9",
                "2 SX9 SX9"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("X9", "aaaa");
        input.put("SX9", true);

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <X9>aaaa</X9>\n"
                        + "  <SX9>true</SX9>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 真偽値_BLタイプが使用できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 BL1 BL",
                "2 BL2 BL"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("BL1", true);
        input.put("BL2", "aaaa");

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <BL1>true</BL1>\n"
                        + "  <BL2>aaaa</BL2>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void コンテンツ要素にデータタイプが使用できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body BL"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("body", true);

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>"
                        + "  true"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void コンテンツ要素にコンバータが指定できること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 body X9 number"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("body", new BigDecimal("1.0003"));

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>"
                        + "  1.0003"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void OBタイプで項目名を属性とした場合エラーになること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 @child OB",
                "[child]",
                "1 name X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("child.name", "name");

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("child is Object but specified by Attribute");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void コンテンツを配列としてフォーマット定義した場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 child OB",
                "[child]",
                "1 body [0..2] X"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("child.body", new String[] {"値"});

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("Array type can not be specified in the content. parent name: child,field name: body");
        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void nullを指定した場合デフォルト値が出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data X \"デフォルト値\"",
                "2 num X9 999"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("data", null);

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <data>デフォルト値</data>\n"
                        + "  <num>999</num>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 値を指定した場合デフォルト値ではなく指定した値が出力されること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 data X \"デフォルト値\""
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("data", "あいうえお");

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <data>あいうえお</data>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void 数値コンバータを使って値を出力出来ること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 X9 X9 number",
                "2 SX9 SX9 signed_number"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("X9", "100");
        input.put("SX9", "-1.123");

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <X9>100</X9>\n"
                        + "  <SX9>-1.123</SX9>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void numberコンバータに符号付きを指定した場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 X9 X9 number",
                "2 SX9 SX9 signed_number"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("X9", new BigDecimal("-100"));
        input.put("SX9", "-1.123");

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("value=[-100]. field name=[X9]");

        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void signed_numberに非数字を指定した場合エラーとなること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 X9 X9 number",
                "2 SX9 SX9 signed_number"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("X9", new BigDecimal("100"));
        input.put("SX9", "一");

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("value=[一]. field name=[SX9]");

        sut.buildData(input, getLayoutDefinition(), actual);
    }

    @Test
    public void 文字の変換ができること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 text X replacement(\"charconverter\")"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("text", "アイウエオ");

        sut.buildData(input, getLayoutDefinition(), actual);
        
        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root>\n"
                        + "  <text>あいうえお</text>\n"
                        + "</root>").ignoreWhitespace());
    }

    @Test
    public void ネームスペースが使えること() throws Exception {
        createFormatFile(
                "UTF-8",
                "[root]",
                "1 ?@xmlns:ns X \"http://test.com/apply\"",
                "2 ns:data OB",
                "[ns:data]",
                "1 ns:name X",
                "2 ns:age  X9"
        );

        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final DataRecord input = new DataRecord();
        input.put("nsData.nsname", "あいうえお");
        input.put("nsData.nsage", 100);

        sut.buildData(input, getLayoutDefinition(), actual);

        System.out.println("actual.toString(\"utf-8\") = " + actual.toString("utf-8"));
        assertThat(actual.toString("utf-8"),
                isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<root xmlns:ns='http://test.com/apply'>\n"
                        + "  <ns:data>\n"
                        + "    <ns:name>あいうえお</ns:name>\n"
                        + "    <ns:age>100</ns:age>\n"
                        + "  </ns:data>\n"
                        + "</root>").ignoreWhitespace());
    }
}