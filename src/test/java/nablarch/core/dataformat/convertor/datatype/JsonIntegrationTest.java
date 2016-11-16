package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Json用のデータタイプの機能結合テストクラス。
 * 対象クラス：{@link JsonBoolean}, {@link JsonNumber}, {@link JsonNumber}, {@link JsonObject}
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
     * 正常系の読込テスト。
     */
    @Test
    public void testRead() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1  object     OB",
                "",
                "[object]",
                "1    bool     BL",
                "2  number     X9",
                "3  string     X "
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("{\"object\":{\"bool\":false,\"number\":321,\"string\":\"ABC\"}}".getBytes("UTF-8"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();

        assertThat(record.getString("object.bool"), is("false"));
        assertThat(record.getBigDecimal("object.number"), is(new BigDecimal("321")));
        assertThat(record.getString("object.string"), is("ABC"));
    }

    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1  object     OB",
                "",
                "[object]",
                "1    bool     BL",
                "2  number     X9",
                "3  string     X "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("object.bool", false);
        record.put("object.number", 321);
        record.put("object.string", "ABC");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is("{\"object\":{\"bool\":false,\"number\":321,\"string\":\"ABC\"}}"));
    }

    /**
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"JSON\"",
                "text-encoding: \"UTF-8\"",
                "",
                "[Default]",
                "1    bool     BL  \"true\" ",
                "2  number     X9  \"123\" ",
                "3  string     X   \"abc\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("bool", null);
        record.put("number", null);
        record.put("string", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("UTF-8"), is("{\"bool\":true,\"number\":123,\"string\":\"abc\"}"));
    }

//    /**
//     * {@link JsonNumber}:
//     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
//     */
//    @Test
//    public void testWriteDefaultNumber() throws Exception {
//
//        // レイアウト定義ファイル
//        final File formatFile = temporaryFolder.newFile("format.fmt");
//        createFile(formatFile,
//                "file-type:    \"JSON\"",
//                "text-encoding: \"UTF-8\"",
//                "",
//                "[Default]",
//                "1    number     X9  \"123\" "
//        );
//        createFormatter(formatFile);
//
//        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        formatter.setOutputStream(outputStream)
//                .initialize();
//
//        DataRecord record = new DataRecord();
//        record.put("number", null);
//        formatter.writeRecord(record);
//
//        assertThat(outputStream.toString("UTF-8"), is("{\"number\":123}"));
//    }
//
//    /**
//     * {@link JsonString}:
//     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
//     */
//    @Test
//    public void testWriteDefaultString() throws Exception {
//
//        // レイアウト定義ファイル
//        final File formatFile = temporaryFolder.newFile("format.fmt");
//        createFile(formatFile,
//                "file-type:    \"JSON\"",
//                "text-encoding: \"UTF-8\"",
//                "",
//                "[Default]",
//                "1    string     X  \"abc\" "
//        );
//        createFormatter(formatFile);
//
//        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        formatter.setOutputStream(outputStream)
//                .initialize();
//
//        DataRecord record = new DataRecord();
//        record.put("string", null);
//        formatter.writeRecord(record);
//
//        assertThat(outputStream.toString("UTF-8"), is("{\"string\":\"abc\"}"));
//    }
}
