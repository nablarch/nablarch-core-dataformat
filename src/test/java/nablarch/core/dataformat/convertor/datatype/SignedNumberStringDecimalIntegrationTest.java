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
 * {@link SignedNumberStringDecimal}の機能結合テストクラス。
 *
 * @author  TIS
 */
public class SignedNumberStringDecimalIntegrationTest {

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
     * ディレクティブを省略した読み込みテスト。
     */
    @Test
    public void testRead() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedNumber     SX9(10, 2)"
        );
        createFormatter(formatFile);

        InputStream inputStream = new ByteArrayInputStream("0000012345".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedNumber"), is(new BigDecimal("123.45")));
    }

    /**
     * ディレクティブを指定した読み込みテスト。
     */
    @Test
    public void testReadDirective() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "  #ディレクティブの指定  ",
                "required-decimal-point: false",
                "fixed-sign-position: false",
                "required-plus-sign: true",
                "",
                "[Default]",
                "1   signedNumber     SX9(10, 2)"
        );
        createFormatter(formatFile);

        InputStream inputStream = new ByteArrayInputStream("0000+12345".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                .initialize();

        DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("signedNumber"), is(new BigDecimal("123.45")));
    }

    /**
     * 出力時のパラメータがnullのときデフォルト値が出力されるテスト。
     */
    @Test
    public void testWriteNull() throws Exception{

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedNumber SX9(10, 3) "
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedNumber", null);
        }};
        formatter.writeRecord(record);

        assertThat(outputStream.toByteArray(), is("000000.000".getBytes("ms932")));
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
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedNumber SX9(10, 3)   123"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        DataRecord record = new DataRecord(){{
            put("signedNumber", null);
        }};
        formatter.writeRecord(record);

        assertThat(outputStream.toByteArray(), is("000123.000".getBytes("ms932")));
    }

    /**
     * ディレクティブを省略した書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "",
                "[Default]",
                "1   signedNumber     SX9(10, 2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedNumber", new BigDecimal("-1234"));
        }};
        formatter.writeRecord(record);

        assertThat(outputStream.toByteArray(), is("-001234.00".getBytes("ms932")));
    }

    /**
     * ディレクティブを指定した書き込みテスト。
     */
    @Test
    public void testWriteDirective() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "  #ディレクティブの指定  ",
                "required-decimal-point: false",
                "fixed-sign-position: false",
                "required-plus-sign: true",
                "",
                "[Default]",
                "1   signedNumber     SX9(10, 2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                .initialize();

        DataRecord record = new DataRecord(){{
            put("signedNumber", new BigDecimal("-12.34"));
        }};
        formatter.writeRecord(record);

        assertThat(outputStream.toByteArray(), is("00000-1234".getBytes("ms932")));
    }
}
