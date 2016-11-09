package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 符号なし数値コンバータのテストケース。
 * 
 * 観点：
 * 入力時、出力時に数値データの型変換が正常に行われることの確認および、
 * 形式チェックの細かい動作確認（異常系）を行う。
 * 
 * @author Masato Inoue
 */
public class NumberStringTest {

    private DataRecordFormatter formatter = null;
    
    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(file);
    }

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
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
        1  amount1        X  number    # 数値1          
        2  amount2        X  number    # 数値2           
        3  amount3        X  number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        
        InputStream inputStream = createInputStreamFrom("\"12.345\",\"234.56\",\"34567\"");
        formatter.setInputStream(inputStream).initialize();
        
        DataRecord readRecord = formatter.readRecord();
        assertEquals(new BigDecimal("12.345"), readRecord.get("amount1"));
        assertEquals(new BigDecimal("234.56"), readRecord.get("amount2"));
        assertEquals(new BigDecimal("34567"), readRecord.get("amount3"));
        formatter.close();

        new File("record.dat").deleteOnExit();
    }
    
    /**
     * NumberStringConvertorを使用し、
     * DataRecordに設定された符号なし数値、Integer型の数値、BigDecimal型の数値が正常にファイルに出力されることの確認。
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
        1  amount1        X  number    # 数値1          
        2  amount2        X  number    # 数値2           
        3  amount3        X  number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        
        FileOutputStream outputStream = new FileOutputStream("record.dat");
        formatter.setOutputStream(outputStream).initialize();
        
        formatter.writeRecord(new DataRecord(){{
            put("amount1", "12.345"); // String Number
            put("amount2", 234.56); // Integer
            put("amount3", new BigDecimal(34567)); // BigDecimal
        }});
        
        formatter.close();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("record.dat"), "ms932"
           ));
        
        assertEquals("\"12.345\",\"234.56\",\"34567\"", reader.readLine());
        

        outputStream.close();
        new File("record.dat").deleteOnExit();
    }
    
    

    /**
     * 出力時に形式が符号付き符号なし数値でない場合に例外がスローされるテスト。
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
        1  amount1        X  number    # 数値1          
        2  amount2        X  number    # 数値2           
        3  amount3        X  number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        
        FileOutputStream outputStream = new FileOutputStream("record.dat");
        formatter.setOutputStream(outputStream).initialize();
        
        try {
            formatter.writeRecord(new DataRecord() {
                {
                    put("amount1", "12.3.45"); // String Number
                    put("amount2", 234.56); // Integer
                    put("amount3", new BigDecimal(34567)); // BigDecimal
                }
            });
            fail();
        } catch (InvalidDataFormatException e) {
            assertEquals(
                    "invalid parameter format was specified. parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[12.3.45]. field name=[amount1].",
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
        NumberString numberString = new NumberString();
        assertNull(numberString.convertOnRead(null));
        assertNull(numberString.convertOnRead(""));
        assertNull(numberString.convertOnWrite(null));
        assertEquals("", numberString.convertOnWrite(""));
    }
    
    /**
     * 入力時に形式が符号なし数値でない場合に例外がスローされるテスト。
     */
    @Test
    public void testFormatCheckOnRead() throws Exception {

        NumberString convertor = new NumberString();
        FieldDefinition field = new FieldDefinition().setName("testField");
        convertor.initialize(field);
        
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
        
        param = "123";
        paramDecimal = new BigDecimal(123);
        assertEquals(paramDecimal, convertor.convertOnRead(param));
        
        param = "1.23";
        paramDecimal = new BigDecimal(1.23);
        assertEquals(paramDecimal.intValue(), convertor.convertOnRead(param).intValue());

        param = "+1";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "-1";
        try {
            convertor.convertOnRead(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

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
     * 出力時に形式が符号なし数値でない場合に例外がスローされるテスト。
     */
    @Test
    public void testFormatCheckOnWrite() throws Exception {

        NumberString convertor = new NumberString();
        FieldDefinition field = new FieldDefinition().setName("testField");
        convertor.initialize(field);

        // 空文字はそのまま出力される
        String param = "";
        convertor.convertOnWrite(param);
        assertEquals(param, convertor.convertOnWrite(param));
        
        param = "a";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "1";
        assertEquals(param, convertor.convertOnWrite(param));
        
        param = "123";
        assertEquals(param, convertor.convertOnWrite(param));
        
        param = "1.23";
        assertEquals(param, convertor.convertOnWrite(param));

        param = "+1";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "-1";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "/1.23";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = ".23";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        

        param = "1.2.3";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = ".9";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }

        param = "9.";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
        param = "1..3";
        try {
            convertor.convertOnWrite(param);
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
    }
    

    /**
     * 出力時に数値型のオブジェクトである場合のテスト
     */
    @Test
    public void testWriteDataType(){

        NumberString convertor = new NumberString();

        // Integer
        Number numberParam = new Integer(5);
        String convertedString = convertor.convertOnWrite(numberParam);
        assertEquals("5", convertedString);

        // Double
        numberParam = new Double(25.55558);
        convertedString = convertor.convertOnWrite(numberParam);
        assertEquals("25.55558", convertedString);

        // BigDecimal
        numberParam = new BigDecimal(25.123455).setScale(5, RoundingMode.UP);
        convertedString = convertor.convertOnWrite(numberParam);
        assertEquals("25.12346", convertedString);

        // 指数表現になるBigDecimal
        final BigDecimal input = new BigDecimal("0.0000000001");
        assertThat(convertor.convertOnWrite(input), is("0.0000000001"));

    }
    
    /**
     * 出力時に符号が付いている場合にエラーが発生することの確認。
     */
    @Test
    public void testWriteSign(){

        NumberString convertor = new NumberString();

        // 符号が付いている場合、エラー
        Number numberParam = new BigDecimal(-25.123455).setScale(5, RoundingMode.UP);
        try{
            convertor.convertOnWrite(numberParam);            
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
    }
    
    
    /**
     * 出力時に不正なパラメータの型が設定された場合にエラーが発生することの確認。
     */
    @Test
    public void testInvalidDataType(){

        NumberString convertor = new NumberString();

        // 符号が付いている場合、エラー
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("int", "8");
        try{
            convertor.convertOnWrite(map);            
            fail();
        } catch (InvalidDataFormatException e) {
            assertTrue(true);
        }
        
    }
    
    

    /**
     * 入力時にエラーが発生し、例外メッセージにレコード番号およびフィールド名が付与されることのテスト。
     */
    @Test
    public void testExceptionConfirmMessage() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  amount1        X  number    # 数値1          
        2  amount2        X  number    # 数値2           
        3  amount3        X  number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        
        InputStream inputStream = createInputStreamFrom("\"a\",\"234.56\",\"34567\"");
        formatter.setInputStream(inputStream).initialize();
        
        try{
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter format was specified. parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[a]. "));
            assertThat(e.getFieldName(), is("amount1"));
            assertThat(e.getRecordNumber(), is(1));
        }
    }
    
    /**
     * 入力された空項目をnullとしてDataRecordに格納できることの確認。
     */
    @Test
    public void testReadEmptyField() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  amount1        X  number    # 数値1          
        2  amount2        X  number    # 数値2           
        3  amount3        X  number    # 数値3           
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        
        InputStream inputStream = createInputStreamFrom("\"12.345\",\"\",\"\"");
        formatter.setInputStream(inputStream).initialize();
        
        DataRecord readRecord = formatter.readRecord();
        assertEquals(new BigDecimal("12.345"), readRecord.get("amount1"));
        assertNull(readRecord.get("amount2")); // null!!
        assertNull(readRecord.get("amount3")); // null!!
        formatter.close();
    }
}
