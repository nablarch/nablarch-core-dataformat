package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;


/**
 * 固定長レコードフォーマッタのマルチレイアウトを使用した場合のテスト。
 * 
 * 観点：
 * 正常にマルチレイアウトファイルを読み書きできること、およびマルチレイアウト関連の異常系を網羅する。
 * 
 * @author Iwauo Tajima
 */
public class FixedLengthDataRecordFormatterMultiLayoutFormatTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DataRecordFormatter formatter;

    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(File formatFile) {
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(formatFile);
        formatter.initialize();
        return formatter;
    }

    private void createFile(File file, String encoding, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), encoding));
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

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
        SystemRepository.clear();
    }
    
    /**
     * マルチフォーマットレコード読み込みのテスト
     */
    @Test
    public void マルチレイアウトのファイルが読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "record-separator: \"\\n\"",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final File inputFile = temporaryFolder.newFile("inputFile");
        createFile(inputFile, "ms932",
                "1             ",
                "2あいう       ",
                "2かきく       ",
                "2さしす       ",
                "9000000003    ");

        final InputStream inputStream = new FileInputStream(inputFile);
        formatter.setInputStream(inputStream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getString("dataKbn"), is("1"));

        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getString("dataKbn"), is("2"));
        assertThat(record2.getString("name"), is("あいう"));

        assertThat(formatter.readRecord()
                            .getString("name"), is("かきく"));
        assertThat(formatter.readRecord()
                            .getString("name"), is("さしす"));

        final DataRecord record5 = formatter.readRecord();
        assertThat(record5.getString("dataKbn"), is("9"));
        assertThat(record5.getBigDecimal("count"), is(new BigDecimal("3")));
    }

    /**
     * マルチレイアウトの書き込みのテスト。
     * @throws Exception
     */
    @Test
    public void マルチレイアウトのファイルが書き込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        // write header
        formatter.writeRecord("header", new DataRecord());
        // write data
        final DataRecord data = new DataRecord();
        data.put("dataKbn", "2");
        data.put("name", "あ");
        formatter.writeRecord(data);
        data.put("name", "い");
        formatter.writeRecord(data);
        // write end
        final DataRecord end = new DataRecord();
        end.put("count", 2);
        formatter.writeRecord("end", end);

        final byte[] actual = outputStream.toByteArray();
        assertThat("header", new String(Arrays.copyOfRange(actual, 0, 14), "ms932"), is("1             "));
        assertThat("data1", new String(Arrays.copyOfRange(actual, 14, 28), "ms932"), is("2あ　　       "));
        assertThat("data2", new String(Arrays.copyOfRange(actual, 28, 42), "ms932"), is("2い　　       "));
        assertThat("end", new String(Arrays.copyOfRange(actual, 42, 56), "ms932"), is("9000000002    "));
    }

    @Test
    public void 識別値にnullを設定してもレコード名を指定して書き込んだ場合正常に出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        // write header
        final DataRecord header = new DataRecord();
        header.put("dataKbn", null);
        formatter.writeRecord("header", header);
        // write data
        final DataRecord data = new DataRecord();
        data.put("dataKbn", null);
        data.put("name", "あ");
        formatter.writeRecord("data", data);

        final byte[] actual = outputStream.toByteArray();
        assertThat("header", new String(Arrays.copyOfRange(actual, 0, 14), "ms932"), is("1             "));
        assertThat("data1", new String(Arrays.copyOfRange(actual, 14, 28), "ms932"), is("2あ　　       "));
    }

    @Test
    public void 複数の識別項目を定義した場合でも読み込みが出来ること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "[Classifier]",
                "1 dataKbn X(1)",
                "14 kbn   X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data1]",
                "dataKbn = \"2\"",
                "kbn = \"A\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(6)",
                "14   ?kbn         X(1)",
                "[data2]",
                "dataKbn = \"2\"",
                "kbn = \"B\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(6)",
                "14   ?kbn         X(1)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

    }
    
    @Test
    public void 複数の識別項目を定義した場合でも書き込みが出来ること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "record-separator: \"\\n\"",
                "[Classifier]",
                "1 dataKbn X(1)",
                "14 kbn   X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data1]",
                "dataKbn = \"2\"",
                "kbn = \"A\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(6)",
                "14   ?kbn         X(1)",
                "[data2]",
                "dataKbn = \"2\"",
                "kbn = \"B\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(6)",
                "14   ?kbn         X(1)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final File inputFile = temporaryFolder.newFile();
        createFile(inputFile, "ms932",
                "1             ",
                "2あいう      A",
                "2かきく      B",
                "2　　　      A",
                "2あ　う      B",
                "9000000004    "
                );

        formatter.setInputStream(new BufferedInputStream(new FileInputStream(inputFile)))
                 .initialize();

        final DataRecord header = formatter.readRecord();
        assertThat(header.getRecordType(), is("header"));
        assertThat(header.getString("dataKbn"), is("1"));

        final DataRecord data1 = formatter.readRecord();
        assertThat(data1.getRecordType(), is("data1"));
        assertThat(data1.getString("name"), is("あいう"));

        final DataRecord data2 = formatter.readRecord();
        assertThat(data2.getRecordType(), is("data2"));
        assertThat(data2.getString("name"), is("かきく"));

        final DataRecord data3 = formatter.readRecord();
        assertThat(data3.getRecordType(), is("data1"));
        assertThat(data3.getString("name"), isEmptyString());

        final DataRecord data4 = formatter.readRecord();
        assertThat(data4.getRecordType(), is("data2"));
        assertThat(data4.getString("name"), is("あ　う"));

        final DataRecord end = formatter.readRecord();
        assertThat(end.getRecordType(), is("end"));
        assertThat(end.getBigDecimal("count"), is(new BigDecimal("4")));

        assertThat(formatter.hasNext(), is(false));
        assertThat(formatter.readRecord(), is(nullValue()));
    }


    @Test
    public void フォーマット定義に定義されていない識別値を持ったレコードの書き込みは失敗すること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        // write header
        formatter.writeRecord("header", new DataRecord());
        // write data
        final DataRecord data = new DataRecord();
        data.put("dataKbn", "3");
        data.put("name", "あ");

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("an applicable layout definition was not found in the record. record=[{dataKbn=3, name=あ}]. ");
        formatter.writeRecord(data);
    }
    
    @Test
    public void フォーマット定義にないレコード名称を指定した場合は書き込みに失敗すること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        // write header
        formatter.writeRecord("header", new DataRecord());
        // write data
        final DataRecord data = new DataRecord();
        data.put("dataKbn", "2");
        data.put("name", "あ");

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("an applicable layout definition was not found. specified record type=[data1]. ");
        formatter.writeRecord("data1", data);
    }

    @Test
    public void 読み込んだレコードの識別値がフォーマット定義に定義されていない場合は読み込みに失敗すること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "record-separator: \"\\n\"",
                "[Classifier]",
                "1 dataKbn X(1)",
                "[header]",
                "dataKbn = \"1\"",
                "1    dataKbn       X(1)  \"1\"",
                "2    ?unused       X(13) ",
                "[data]",
                "dataKbn = \"2\"",
                "1    dataKbn      X(1) \"2\"",
                "2    name         N(6)",
                "8    ?unused      X(7)",
                "[end]",
                "dataKbn = \"9\"",
                "1    dataKbn      X(1) \"9\"",
                "2    count       X9(9)",
                "11   ?unused      X(4)"
        );
        createFormatter(formatFile);

        final File inputFile = temporaryFolder.newFile("inputFile");
        createFile(inputFile, "ms932",
                "1             ",
                "2あいう       ",
                "3かきく       ",
                "2さしす       ",
                "9000000003    ");
        formatter.setInputStream(new FileInputStream(inputFile))
                 .initialize();

        assertThat(formatter.readRecord(), is(notNullValue()));
        assertThat(formatter.readRecord(), is(notNullValue()));

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("an applicable layout definition was not found in the record. record=[{dataKbn=3}].");
        formatter.readRecord();
    }
}
