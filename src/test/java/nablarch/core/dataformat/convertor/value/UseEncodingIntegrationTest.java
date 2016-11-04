package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link UseEncoding}の機能結合テスト。
 *
 * @author Masato Inoue
 */
public class UseEncodingIntegrationTest {

    private DataRecordFormatter formatter = null;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private void createFile(File formatFile, String encoding, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(formatFile), encoding));
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
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 40",
                "",
                "[Default]",
                "1 doubleByteString   N(20)",
                "21 doubleByteString2  N(20) encoding(\"euc-jp\")"
        );

        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(formatFile);
    }


    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
    }

    /**
     * ms932とeuc-jpのフィールドが混在する場合に正常に読めることのテスト。
     */
    @Test
    public void read() throws Exception {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("０１２３４５６７８９".getBytes("ms932"));
        stream.write("あいうおえかきくけこ".getBytes("euc-jp"));
        
        formatter.setInputStream(new ByteArrayInputStream(stream.toByteArray()))
                 .initialize();

        DataRecord readRecord = formatter.readRecord();
        assertThat(readRecord.getString("doubleByteString"), is("０１２３４５６７８９"));
        assertThat(readRecord.getString("doubleByteString2"), is("あいうおえかきくけこ"));
    }

    @Test
    public void write() throws Exception {
        final HashMap<String, Object> record = new HashMap<String, Object>();
        record.put("doubleByteString", "０１２３４５６７８９");
        record.put("doubleByteString2", "あいうえおかきくけこ");

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        formatter.writeRecord(record);

        final byte[] bytes = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(bytes, 0, 20), is("０１２３４５６７８９".getBytes("ms932")));
        assertThat(Arrays.copyOfRange(bytes, 20, 40), is("あいうえおかきくけこ".getBytes("euc-jp")));
    }
}
