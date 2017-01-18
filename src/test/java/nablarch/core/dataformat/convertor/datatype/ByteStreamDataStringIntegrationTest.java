package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * {@link ByteStreamDataString}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class ByteStreamDataStringIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
                "1    byteStreamString     XN(10)   "
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("abc       01αあ名            ".getBytes("sjis"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getString("byteStreamString"), is("abc"));
        record = formatter.readRecord();
        assertThat(record.getString("byteStreamString"), is("01αあ名"));
        record = formatter.readRecord();
        assertThat(record.getString("byteStreamString"), is(nullValue()));
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
                "1    byteStreamString     XN(10)   "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("byteStreamString", "0123456789");
        formatter.writeRecord(record);
        record.put("byteStreamString", "01αあ名");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("sjis"), is("012345678901αあ名  "));
    }

    /**
     * 正常系の書き込みテスト。
     * デフォルト値を設定しない null 出力のケース。
     */
    @Test
    public void testWriteNull() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteStreamString     XN(10)   "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("byteStreamString", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("sjis"), is("          "));
    }

    /**
     * 出力対象がnullの場合に
     * シングルバイト・ダブルバイト・３バイト文字が混在したデフォルト値を書き込めることのテスト。
     */
    @Test
    public void testWriteMultiByteWithDefaultValue() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"utf8\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteStreamString     XN(10)  \"123А名\" "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("byteStreamString", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("utf8"), is("123А名  "));
    }

    /**
     * バイト長が数値型でない場合に、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout_ByteIsNotNumber() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteStreamString     XN(\"a\")   "
        );
        createFormatter(formatFile);

        exception.expect(allOf(
                instanceOf(SyntaxErrorException.class),
                hasProperty("message", is(startsWith("invalid parameter type was specified. 1st parameter must be an integer. " +
                        "parameter=[a]. convertor=[ByteStreamDataString]."))),
                hasProperty("filePath", is(endsWith("format.fmt")))
        ));

        formatter.initialize();
    }

    /**
     * シングルバイトのパラメータが存在しない場合に、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout_ByteIsEmpty() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteStreamString     XN()   "
        );
        createFormatter(formatFile);

        exception.expect(allOf(
                instanceOf(SyntaxErrorException.class),
                hasProperty("message", is(startsWith("parameter was not specified. parameter must be specified. " +
                        "convertor=[ByteStreamDataString]."))),
                hasProperty("filePath", is(endsWith("format.fmt")))
        ));

        formatter.initialize();
    }

}
