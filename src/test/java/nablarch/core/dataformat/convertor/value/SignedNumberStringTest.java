package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 符号付き数値コンバータのテストケース。
 * 
 * 観点：
 * 入力時、出力時に符号付き数値データの型変換が正常に行われることの確認および、
 * 形式チェックの細かい動作確認（異常系）を行う。
 * 
 * @author Masato Inoue
 */
public class SignedNumberStringTest {

    private DataRecordFormatter formatter = null;
    
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(File file) {
        return FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(file);
    }

    /**
     * 入力された文字列をBigDecimal型としてDataRecordに格納できることの確認。
     */
    @Test
    public void testReadNumberStringConvertor() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  amount1        X  signed_number    # 数値1          
        2  amount2        X  signed_number    # 数値2           
        3  amount3        X  signed_number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        formatter = createFormatter(formatFile);
        
        InputStream inputStream = createInputStreamFrom("\"12.345\",\"-234.56\",\"-345.67\"");
        formatter.setInputStream(inputStream).initialize();
        
        DataRecord readRecord = formatter.readRecord();
        assertEquals(new BigDecimal("+12.345"), readRecord.get("amount1"));
        assertEquals(new BigDecimal("-234.56"), readRecord.get("amount2"));
        assertEquals(new BigDecimal("-345.67"), readRecord.get("amount3"));
        formatter.close();

        new File("record.dat").deleteOnExit();
    }
    
    /**
     * NumberStringConvertorを使用し、
     * DataRecordに設定された数値、Integer型の数値、BigDecimal型の数値が正常にファイルに出力されることの確認。
     * (DBから取得したデータを書き出す場合の想定）
     */
    @Test
    public void testWriteNumberStringConvertor() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  amount1        X  signed_number    # 数値1          
        2  amount2        X  signed_number    # 数値2           
        3  amount3        X  signed_number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        formatter = createFormatter(formatFile);
        
        FileOutputStream outputStream = new FileOutputStream("record.dat");
        formatter.setOutputStream(outputStream).initialize();
        
        formatter.writeRecord(new DataRecord(){{
            put("amount1", "+12.345"); // String Number
            put("amount2", -234.56); // Integer
            put("amount3", new BigDecimal(-34567)); // BigDecimal
        }});
        
        formatter.close();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("record.dat"), "ms932"
           ));
        
        assertEquals("\"+12.345\",\"-234.56\",\"-34567\"", reader.readLine());
        

        outputStream.close();
        new File("record.dat").deleteOnExit();
    }

    /**
     * 出力時に形式が符号付き数値でない場合に例外がスローされるテスト。
     */
    @Test
    public void testInvalidWriterNumberFormat() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  amount1        X  signed_number    # 数値1          
        2  amount2        X  signed_number    # 数値2           
        3  amount3        X  signed_number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        formatter = createFormatter(formatFile);
        
        FileOutputStream outputStream = new FileOutputStream("record.dat");
        formatter.setOutputStream(outputStream).initialize();
        
        try {
            formatter.writeRecord(new DataRecord() {
                {
                    put("amount1", ".12345"); // String Number
                    put("amount2", 234.56); // Integer
                    put("amount3", new BigDecimal(34567)); // BigDecimal
                }
            });
            fail();
        } catch (InvalidDataFormatException e) {
            assertEquals(
                    "invalid parameter format was specified. parameter format must be [^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[.12345]. field name=[amount1].",
                    e.getMessage());
        }
        
        formatter.close();
        
    }

    /**
     * 入力時の引数がnullおよび空文字の場合にnullが、
     * 出力時の引数がnullおよび空文字の場合にはそのままnullおよび空文字が返却されることのテスト。
     */
    @Test
    public void testArgNullOrEmpty() throws Exception {
        SignedNumberString numberString = new SignedNumberString();
        assertNull(numberString.convertOnRead(null));
        assertNull(numberString.convertOnRead(""));
        assertNull(numberString.convertOnWrite(null));
        assertEquals("", numberString.convertOnWrite(""));
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

        // 空文字はそのまま出力される
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
