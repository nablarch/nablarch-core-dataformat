package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link ZonedDecimal}の機能結合テストクラス。
 *
 * @author TIS
 */
public class ZonedDecimalIntegrationTest {

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
                "text-encoding: \"sjis\"",
                "record-length: 20",
                "",
                "[Default]",
                "1    signedZDigit     SZ(10)",
                "11   unsignedZDigit    Z(10)"
        );
        createFormatter(formatFile);

        byte[] bytes = new byte[20];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x70); // -1234567890
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x30); // 1234567890
        final InputStream inputStream = new ByteArrayInputStream(bytes);

        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedZDigit"), is(new BigDecimal("-1234567890")));
        assertThat(record.getBigDecimal("unsignedZDigit"), is(new BigDecimal("1234567890")));
    }

    /**
     * 正常系の書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 20",
                "",
                "[Default]",
                "1    signedZDigit     SZ(10)",
                "11   unsignedZDigit    Z(10)"
        );
        createFormatter(formatFile);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord() {{
            put("signedZDigit", new BigDecimal("-1234567890"));
            put("unsignedZDigit", new BigDecimal("1234567890"));
        }};
        formatter.writeRecord(record);

        byte[] expected = new byte[20];
        ByteBuffer buff = ByteBuffer.wrap(expected);
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x70); // -1234567890
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x30); // 1234567890

        assertThat(outputStream.toByteArray(), is(expected));
    }

    /**
     * フィールドごとのパラメータでゾーンビット（正/負）の値を渡して、正常に動作することの確認。
     */
    @Test
    public void testNibble() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    signedZDigit     SZ(10, \"\", \"4\", \"6\")"
        );
        createFormatter(formatFile);

        byte[] bytes = new byte[20];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x60); // -1234567890
        buff.put("123456789".getBytes("sjis"))
                .put((byte) 0x40); // 1234567890
        final InputStream inputStream = new ByteArrayInputStream(bytes);

        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedZDigit"), is(new BigDecimal("-1234567890")));
        record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedZDigit"), is(new BigDecimal("1234567890")));
    }

    /**
     * 出力時のパラメータがnullの場合にデフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    signedZDigit     SZ(10, \"\", \"3\", \"7\") 123"
        );
        createFormatter(formatFile);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        DataRecord record = new DataRecord() {{
            put("signedZDigit", null);
        }};
        formatter.writeRecord(record);

        assertThat(outputStream.toByteArray(), is("0000000123".getBytes("sjis")));
    }
}
