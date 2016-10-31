package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link DefaultValue}の結合テスト。
 */
public class DefaultValueIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DataRecordFormatter formatter;

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
        if (formatter != null) {
            formatter.close();
        }
    }


    /**
     * 読み込み時にDefaultValueが無視されることのテスト。
     *
     * @throws Exception
     */
    @Test
    public void testDefaultValueOnRead() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile,
                "file-type: \"Variable\"",
                "text-encoding: \"utf-8\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[record]",
                "1 field1 X \"a0\"",
                "2 field2 X9 \"0\""
        );
        createFormatter(formatFile);

        final File inputFile = temporaryFolder.newFile();
        createFile(inputFile, "aaa,12345");

        DataRecord record = formatter.setInputStream(new FileInputStream(inputFile))
                                     .initialize()
                                     .readRecord();

        assertThat(record.getRecordType(), is("record"));
        assertThat(record.getString("field1"), is("aaa"));
        assertThat(record.getBigDecimal("field2"), is(new BigDecimal("12345")));
    }

    /**
     * 書き込み時に、デフォルト値が正しく使用されることのテスト。
     */
    @Test
    public void testDefaultValueOnWrite() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");

        createFile(formatFile, "",
                "file-type:\"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "",
                "[default]",
                "1 single X(4) \"1234\"",
                "5 double N(4) \"ああ\"",
                "9 number X9(2) 12"
        );

        createFormatter(formatFile);
        final File outputFile = temporaryFolder.newFile();
        formatter.setOutputStream(new FileOutputStream(outputFile))
                 .initialize();

        DataRecord dataRecord = new DataRecord();
        dataRecord.put("single", null);
        dataRecord.put("double", null);
        dataRecord.put("number", null);
        formatter.writeRecord(dataRecord);
        formatter.close();

        FileInputStream inputStream = new FileInputStream(outputFile);
        final String result;
        try {
            byte[] buffer = new byte[10];
            inputStream.read(buffer);
            result = new String(buffer, "ms932");
        } finally {
            inputStream.close();
        }
        assertThat(result, is("1234ああ12"));
    }

}
