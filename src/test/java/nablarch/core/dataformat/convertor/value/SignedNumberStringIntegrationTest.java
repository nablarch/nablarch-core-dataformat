package nablarch.core.dataformat.convertor.value;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link SignedNumberString}の機能結合テスト。
 * 
 * @author Masato Inoue
 */
public class SignedNumberStringIntegrationTest {

    private DataRecordFormatter formatter = null;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(File file) {
        return FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(file);
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

    @Before
    public void setUp() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile,
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "",
                "[TestDataRecord]",
                "1  amount1 X  signed_number",
                "2  amount2 X  signed_number",
                "3  amount3 X  signed_number"
        );

        formatter = createFormatter(formatFile);

    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
    }

    /**
     * 入力された文字列をBigDecimal型としてDataRecordに格納できることの確認。
     */
    @Test
    public void read() throws Exception {
        InputStream inputStream = createInputStreamFrom("12.345,-234.56,-345.67");
        formatter.setInputStream(inputStream)
                 .initialize();
        
        DataRecord record = formatter.readRecord();

        assertThat((BigDecimal) record.get("amount1"), is(new BigDecimal("12.345")));
        assertThat((BigDecimal) record.get("amount2"), is(new BigDecimal("-234.56")));
        assertThat((BigDecimal) record.get("amount3"), is(new BigDecimal("-345.67")));
    }
    
    /**
     * NumberStringConvertorを使用し、
     * DataRecordに設定された数値、Integer型の数値、BigDecimal型の数値が正常にファイルに出力されることの確認。
     * (DBから取得したデータを書き出す場合の想定）
     */
    @Test
    public void write() throws Exception {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        
        formatter.writeRecord(new DataRecord(){{
            put("amount1", "+12.345"); // String Number
            put("amount2", -234.56); // Integer
            put("amount3", new BigDecimal(-34567)); // BigDecimal
        }});

        assertThat(outputStream.toString("ms932"), is("+12.345,-234.56,-34567\n"));
    }
    
    /**
     * 入力時に形式が符号付き数値でない場合に例外がスローされるテスト。
     */
    @Test
    public void testFormatCheckOnRead() throws Exception {

        SignedNumberString convertor = new SignedNumberString();
        
        String param = "a";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "1";
        BigDecimal paramDecimal = new BigDecimal(1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));

        param = "+1";
        paramDecimal = new BigDecimal(1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));
        
        param = "-1";
        paramDecimal = new BigDecimal(-1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));
        
        param = "+1.23";
        paramDecimal = new BigDecimal(+1.23);
        assertEquals(paramDecimal.intValue(), convertor.convertOnRead(param).intValue());

        param = "/1.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        param = ".23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "+.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "-.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        

        param = "1.2.3";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = ".9";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "9.";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        

        param = "1..3";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
    }

    /**
     * 出力時に形式が符号付き数値でない場合に例外がスローされるテスト。
     */
    @Test
    public void testFormatCheckOnWrite() throws Exception {

        SignedNumberString convertor = new SignedNumberString();

        String param = "";
        assertEquals(param, convertor.convertOnWrite(param));
        
        param = "a";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "1";
        BigDecimal paramDecimal = new BigDecimal(1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));

        param = "+1";
        paramDecimal = new BigDecimal(1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));
        
        param = "-1";
        paramDecimal = new BigDecimal(-1);
        assertEquals(paramDecimal, convertor.convertOnRead(param));
        
        param = "+1.23";
        paramDecimal = new BigDecimal(+1.23);
        assertEquals(paramDecimal.intValue(), convertor.convertOnRead(param).intValue());

        param = "/1.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        param = ".23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "+.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "-.23";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        

        param = "1.2.3";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = ".9";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "9.";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        

        param = "1..3";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
    }
    
    /**
     * 出力時に符号が付いている場合にエラーが発生しないことの確認。
     */
    @Test
    public void testWriteSign(){

        SignedNumberString convertor = new SignedNumberString();

        Number numberParam = new BigDecimal(-25.123455).setScale(5, RoundingMode.UP);
        String convertedString = convertor.convertOnWrite(numberParam);
        assertEquals("-25.12346", convertedString);
        
    }
    
    
}
