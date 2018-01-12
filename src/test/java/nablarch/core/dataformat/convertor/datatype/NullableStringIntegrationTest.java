package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

/**
 * {@link NullableString}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class NullableStringIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DataRecordFormatter formatter = null;

    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance()
                .setCacheLayoutFileDefinition(false)
                .createFormatter(file);
    }

    private void createFile(File formatFile, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(formatFile), "utf-8"));
        try {
            for (final String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }

    /**
     * 正常系の読込テスト。
     */
    @Test
    public void testRead() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"XML\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    string     X  "
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("<Default><string>ABC\uD840\uDC0B</string></Default>".getBytes("UTF-8"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getString("string"), is("ABC\uD840\uDC0B"));
    }

    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {
        // Java 1.8.0 Update 77以前では以下のバグがあるため、Java6, 7環境ではテストをスキップする。
        // https://bugs.openjdk.java.net/browse/JDK-8145969
        Assume.assumeThat(System.getProperty("java.specification.version"), allOf(not("1.6"), not("1.7")));

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"XML\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    string     X  "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("string", "ABC\uD840\uDC0B");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), isIdenticalTo("<Default><string>ABC&#x2000b;</string></Default>").ignoreWhitespace());
    }

    /**
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteNull() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"XML\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    string     X  "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("string", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is(containsString("<string></string>")));
    }

    /**
     * 出力時にnullが渡された場合、
     * レイアウト定義に指定されたデフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {
        // Java 1.8.0 Update 77以前では以下のバグがあるため、Java6, 7環境ではテストをスキップする。
        // https://bugs.openjdk.java.net/browse/JDK-8145969
        Assume.assumeThat(System.getProperty("java.specification.version"), allOf(not("1.6"), not("1.7")));


        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"XML\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    string     X  \"abc\uD840\uDC0B\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("string", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), isIdenticalTo("<Default><string>abc&#x2000b;</string></Default>").ignoreWhitespace());
    }
}
