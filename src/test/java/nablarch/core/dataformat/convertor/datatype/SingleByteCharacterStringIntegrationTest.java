package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link SingleByteCharacterString}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class SingleByteCharacterStringIntegrationTest {

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
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    singleByteString     X(10)   "
        );
        createFormatter(formatFile);


        final InputStream inputStream = new ByteArrayInputStream("abc       0123456789".getBytes("sjis"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getString("singleByteString"), is("abc"));
        record = formatter.readRecord();
        assertThat(record.getString("singleByteString"), is("0123456789"));
    }

    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    singleByteString     X(10)   "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("singleByteString", "0123456789");
        formatter.writeRecord(record);
        record.put("singleByteString", "abc");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("sjis"), is("0123456789abc       "));
    }

    /**
     * 出力時にパラメータがnullの場合にデフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    singleByteString     X(10)  \"abc\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("singleByteString", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("sjis"), is("abc       "));
    }
}
