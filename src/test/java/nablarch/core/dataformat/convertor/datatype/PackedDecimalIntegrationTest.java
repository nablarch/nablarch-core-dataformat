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
import static org.junit.Assert.*;

/**
 * {@link PackedDecimal}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class PackedDecimalIntegrationTest {

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
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   signedPDigit       SP(10, \"\", \"7\", \"4\")",
                "11  unsignedPDigit     P(10)"
        );
        createFormatter(formatFile);

        byte[] bytes = new byte[] {
                // -87654321
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x14,
                // 87654321
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x13,
                // 0000000000
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x07,
                // 0000000000
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x03
        };
        final InputStream inputStream = new ByteArrayInputStream(bytes);
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.size(), is(2));
        assertThat(record.getBigDecimal("signedPDigit"), is(new BigDecimal("-87654321")));
        assertThat(record.getBigDecimal("unsignedPDigit"), is(new BigDecimal("87654321")));
        record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedPDigit"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("unsignedPDigit"), is(BigDecimal.ZERO));
    }

    /**
     * 出力時のパラメータがnullのときデフォルト値が出力されるテスト。
     */
    @Test
    public void testWrite() throws Exception{

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 20",
                "",
                "[Default]",
                "1   signedPDigit     SP(10, \"\", \"7\", \"4\")",
                "11  unsignedPDigit     P(10)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedPDigit", new BigDecimal("-87654321"));
            put("unsignedPDigit", new BigDecimal(("87654321")));
        }};
        formatter.writeRecord(record);

        byte[] expected = new byte[] {
                // -87654321
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x14,
                // 87654321
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x08, 0x76, 0x54, 0x32, 0x13
        };

        assertThat(outputStream.toByteArray(), is(expected));
    }

    /**
     * 出力時のパラメータがnullのときデフォルト値が出力されるテスト。
     */
    @Test
    public void testWriteNull() throws Exception{

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedPDigit     SP(10, \"\", \"7\", \"4\")"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedPDigit", null);
        }};
        formatter.writeRecord(record);

        byte[] expected = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x07
        };

        assertThat(outputStream.toByteArray(), is(expected));
    }

    /**
     * 出力時のパラメータがnullのとき
     * レイアウト定義に指定されたデフォルト値が出力されるテスト。
     */
    @Test
    public void testWriteDefault() throws Exception{

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedPDigit     SP(10, \"\", \"7\", \"4\") 123"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedPDigit", null);
        }};
        formatter.writeRecord(record);

        byte[] expected = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x12, 0x37
        };

        assertThat(outputStream.toByteArray(), is(expected));
    }
}
