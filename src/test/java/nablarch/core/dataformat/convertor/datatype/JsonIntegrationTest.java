package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Json用のデータタイプの機能結合テストクラス。
 * 対象クラス：{@link JsonBoolean}, {@link JsonNumber}, {@link JsonNumber}
 *
 * @author  TIS
 */
public class JsonIntegrationTest {

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
     * {@link JsonBoolean}:
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefaultBoolean() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    bool     BL  \"true\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("bool", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is("{\"bool\":true}"));
    }

    /**
     * {@link JsonNumber}:
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefaultNumber() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    number     X9  \"123\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("number", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is("{\"number\":123}"));
    }

    /**
     * {@link JsonString}:
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefaultString() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    string     X  \"abc\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("string", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is("{\"string\":\"abc\"}"));
    }
}
