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
 * {@link NumberStringDecimal}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class NumberStringDecimalIntegrationTest {

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
     * 正常系の読み込みテスト。
     */
    @Test
    public void testRead() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   number    X9(10, \"\")   ",
                "11  number2  SX9(10, 3)"
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("0000123.45000001234500000000000000000000".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.size(), is(2));
        assertThat(record.getBigDecimal("number"), is(new BigDecimal("123.45")));
        assertThat(record.getBigDecimal("number2"), is(new BigDecimal("12.345")));
        record = formatter.readRecord();
        assertThat(record.getBigDecimal("number"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("number2"), is(new BigDecimal("0.000")));
    }



    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWriteFormatFile() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   number    X9(10, 2)",
                "11  number2  SX9(10, 4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("number", "123.45");
        record.put("number2", String.valueOf(BigDecimal.valueOf(12345, 3)));
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("0000123.4500012.3450"));
    }

    /**
     * 書き込み時にパラメータがnullの場合にデフォルト値が出力されるテスト。
     */
    @Test
    public void testWriteNull() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   number    X9(10, 2)  ",
                "11  number2  SX9(10, 4)  "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("number", null);
        record.put("number2", null);
        formatter.writeRecord(record);

        // スケール違いの10ケタの0 が二つ
        // 0000000.00 と　00000.0000
        assertThat(outputStream.toString("ms932"), is("0000000.0000000.0000"));
    }

    /**
     * 書き込み時にパラメータがnullの場合に、
     * レイアウト定義に指定されたデフォルト値が出力されるテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   number    X9(10, 2)   123",
                "11  number2  SX9(10, 4)   321"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord();
        record.put("number", null);
        record.put("number2", null);
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("0000123.0000321.0000"));
    }
}
