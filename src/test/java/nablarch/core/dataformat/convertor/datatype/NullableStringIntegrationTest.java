package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

        final InputStream inputStream = new ByteArrayInputStream("<Default><string>ABC</string></Default>".getBytes("UTF-8"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getString("string"), is("ABC"));
    }

    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {

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
        record.put("string", "ABC");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is(containsString("<Default><string>ABC</string></Default>")));
    }

    /**
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"XML\"",
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

        assertThat(outputStream.toString("UTF-8"), is(containsString("<string>abc</string>")));
    }
}
