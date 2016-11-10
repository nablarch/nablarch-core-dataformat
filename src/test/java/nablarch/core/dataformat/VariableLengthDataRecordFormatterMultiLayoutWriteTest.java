package nablarch.core.dataformat;

import nablarch.core.util.FileUtil;
import nablarch.test.IgnoringLS;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 可変長ファイルフォーマッタのマルチレイアウトのテストケース。
 * 
 * 観点：
 * 可変長ファイルをマルチレイアウトで書き出す際の、正常系テストおよび異常系テストを網羅する。
 * 
 * @author Masato Inoue
 */

public class VariableLengthDataRecordFormatterMultiLayoutWriteTest {
    
    private String LS = System.getProperty("line.separator");

    private DataRecordFormatter formatter;
    
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(String filePath) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File(filePath));
        return formatter;
    }
    
    /**
     * titleレコードの場合のテスト
     * 
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     * @throws Exception
     */
    @Test
    public void testTitle() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";

        /**
         *  正常系。setRecordTypeで設定したデータタイプが、DataRecordに設定されたデータ区分と一致しないパターン。
         */
        DataRecord titleRecord1 = new DataRecord();
        titleRecord1.setRecordType("TestTitleRecord");
        
        DataRecord dataRecord2 = new DataRecord() {{
            put("dataKbn",       "2");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("tegataNum",     "aaaa");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("unused",        "xxxxx");
        }};
        dataRecord2.setRecordType("TestDataRecord");
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createMultiLayoutFormatter(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(titleRecord1);
        formatter.writeRecord(dataRecord2);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "データ区分","振込先金融機関コード","振込先金融機関名称","振込先営業所コード","振込先営業所名","(手形交換所番号)","預金種目","口座番号","受取人名","振込金額","新規コード","EDI情報","振込区分","EDI情報使用フラグ","(未使用領域)"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","xxxxx"
        */
    }
    
    /**
     * レコードタイプが設定されていて、マルチレイアウトの場合のテスト
     * 
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testSettingDataType() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";

        /**
         *  正常系。setRecordTypeで設定したデータタイプが、DataRecordに設定されたデータ区分と一致するパターン。
         */
        DataRecord titleRecord1 = new DataRecord() {{
            put("dataKbn",       "1"); 
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};        
        titleRecord1.setRecordType("TestTitleRecord");
        
        DataRecord dataRecord2 = new DataRecord() {{
            put("dataKbn",       "2");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("tegataNum",     "aaaa");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("unused",        "xxxxx");
        }};
        dataRecord2.setRecordType("TestDataRecord");
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(titleRecord1);
        formatter.writeRecord(dataRecord2);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertThat(fileToString, IgnoringLS.equals(Hereis.string()));
        /*
        "1","振込先金融機関コード","振込先金融機関名称","振込先営業所コード","振込先営業所名","(手形交換所番号)","預金種目","口座番号","受取人名","振込金額","新規コード","EDI情報","振込区分","EDI情報使用フラグ","(未使用領域)"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","xxxxx"
        */

        /**
         *  異常系。setRecordTypeで設定したデータタイプが、DataRecordに設定されたデータ区分と一致しないパターン。
         */
        titleRecord1 = new DataRecord() {{
            put("dataKbn",       "2"); 
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};        
        titleRecord1.setRecordType("TestTitleRecord");
        
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try{
            formatter.writeRecord(titleRecord1);
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith("this record could not be applied to the record type"));
            assertThat(e.getMessage(), containsString("following conditions must be met: [dataKbn = [1]]."));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
            assertThat(e.getRecordNumber(), is(1));
        }



        /**
         *  正常系。setRecordTypeで設定したデータタイプが、DataRecordに設定されたデータ区分およびDEI情報使用フラグと一致するパターン。
         */
        titleRecord1 = new DataRecord() {{
                put("dataKbn",       "1");
                put("FIcode",        "1234");
                put("FIname",        "FSEｷﾞﾝｺｳ");
                put("officeCode",    "ﾏｺ1");
                put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
                put("tegataNum",     "aaaa");
                put("syumoku",       "1");
                put("accountNum",    "7778888");
                put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
                put("amount",        "3020");
                put("isNew",         "N");
                put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
                put("transferType",  "4");
                put("withEdi",       "Y");
                put("test1",         "a");
                put("test2",         "b");
                put("test3",         "c");
            }};        
        titleRecord1.setRecordType("TestHeaderRecord1");
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatter2HeaderAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(titleRecord1);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","a","b","c"
        */
        

        /**
         *  異常系。setRecordTypeで設定したデータタイプが、DataRecordに設定されたデータ区分およびDEI情報使用フラグと一致しないパターン。
         */
        titleRecord1 = new DataRecord() {{
                put("dataKbn",       "1");
                put("FIcode",        "1234");
                put("FIname",        "FSEｷﾞﾝｺｳ");
                put("officeCode",    "ﾏｺ1");
                put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
                put("tegataNum",     "aaaa");
                put("syumoku",       "1");
                put("accountNum",    "7778888");
                put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
                put("amount",        "3020");
                put("isNew",         "N");
                put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
                put("transferType",  "4");
                put("withEdi",       "Z");
                put("test1",         "a");
                put("test2",         "b");
                put("test3",         "c");
            }};        
        titleRecord1.setRecordType("TestHeaderRecord1");
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createMultiLayoutFormatter2HeaderAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try{
            formatter.writeRecord(titleRecord1);
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);

            assertTrue(e.getMessage().contains("this record could not be applied to the record type"));
            assertTrue(e.getMessage().contains("following conditions must be met:"));
            assertTrue(e.getMessage().contains("dataKbn = [1]"));
            assertTrue(e.getMessage().contains("withEdi = [Y]"));
        }
        dest.close();
        
        

        /**
         *  異常系。不正なデータタイプ。
         */
        titleRecord1 = new DataRecord() {{
                put("dataKbn",       "1");
                put("FIcode",        "1234");
                put("FIname",        "FSEｷﾞﾝｺｳ");
                put("officeCode",    "ﾏｺ1");
                put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
                put("tegataNum",     "aaaa");
                put("syumoku",       "1");
                put("accountNum",    "7778888");
                put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
                put("amount",        "3020");
                put("isNew",         "N");
                put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
                put("transferType",  "4");
                put("withEdi",       "Z");
                put("test1",         "a");
                put("test2",         "b");
                put("test3",         "c");
            }};        
        titleRecord1.setRecordType("IllegalRecordType");
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatter2HeaderAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try {
            formatter.writeRecord(titleRecord1);
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "an applicable record type was not found. specified record type=[IllegalRecordType]. "));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        } finally {
            dest.close();
        }


        /**
         *  正常系。引数で設定したデータタイプが、Mapに設定されたデータ区分と一致するパターン。
         */
        Map<String, Object> columnMap1 = new HashMap<String, Object>() {{
            put("dataKbn",       "1"); 
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};        

        Map<String, Object> columnMap2 = new HashMap<String, Object>() {{
            put("dataKbn",       "2");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("tegataNum",     "aaaa");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("unused",        "xxxxx");
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord("TestTitleRecord", columnMap1);
        formatter.writeRecord("TestDataRecord", columnMap2);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertThat(fileToString, IgnoringLS.equals(Hereis.string()));
        /*
        "1","振込先金融機関コード","振込先金融機関名称","振込先営業所コード","振込先営業所名","(手形交換所番号)","預金種目","口座番号","受取人名","振込金額","新規コード","EDI情報","振込区分","EDI情報使用フラグ","(未使用領域)"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","xxxxx"
        */
        
        

        /**
         *  異常系。引数で設定したデータタイプが、Mapに設定されたデータ区分と一致しないパターン。
         */
        columnMap1 = new HashMap<String, Object>() {{
            put("dataKbn",       "2"); 
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }}; 
        
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try {
            formatter.writeRecord("TitleRecord", titleRecord1);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "an applicable record type was not found. specified record type=[TitleRecord]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        } finally {
            dest.close();
        }

        /**
         *  異常系。引数で設定したデータタイプがnull。
         */
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try{
            formatter.writeRecord(null, new HashMap<String, Object>());
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "record type was blank. record type must not be blank."));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        } finally {
            dest.close();
        }

        /**
         *  異常系。引数で設定したデータタイプが空文字。
         */
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try{
            formatter.writeRecord("", new HashMap<String, Object>());
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "record type was blank. record type must not be blank."));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        } finally {
            dest.close();
        }

    }
    
        
    /**
     * マルチフォーマットのテスト
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testMultiFormat() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";

        /**
         *  データ区分が1（タイトル）と、データ区分が２（データ）のパターン 
         */
        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("dataKbn",       "1");
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("dataKbn",       "2");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("tegataNum",     "aaaa");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("unused",        "xxxxx");
        }};
        
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1","振込先金融機関コード","振込先金融機関名称","振込先営業所コード","振込先営業所名","(手形交換所番号)","預金種目","口座番号","受取人名","振込金額","新規コード","EDI情報","振込区分","EDI情報使用フラグ","(未使用領域)"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","xxxxx"
        */

        /**
         *  データ区分=1,EDI情報使用フラグ=Y（ヘッダ1）と、
         *  データ区分=1,EDI情報使用フラグ=Z（ヘッダ2）、
         *  データ区分=2（データ）のパターン。 
         */
        recordMap1 = new HashMap<String, Object>() {{
            put("dataKbn",       "1");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("tegataNum",     "aaaa");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("test1",         "a");
            put("test2",         "b");
            put("test3",         "c");
        }};        
        recordMap2 = new HashMap<String, Object>() {{
            put("dataKbn",       "1");
            put("FIcode",        "12342");
            put("FIname",        "FSEｷﾞﾝｺｳ2");
            put("officeCode",    "ﾏｺ12");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2");
            put("tegataNum",     "aaaa2");
            put("syumoku",       "1");
            put("accountNum",    "77788882");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ2");
            put("amount",        "30202");
            put("isNew",         "N2");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2");
            put("transferType",  "42");
            put("withEdi",       "Z");
            put("unused",        "xxxxx2");
        }};
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("dataKbn",       "2");
            put("FIcode",        "12343");
            put("FIname",        "FSEｷﾞﾝｺｳ3");
            put("officeCode",    "ﾏｺ13");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3");
            put("tegataNum",     "aaaa3");
            put("syumoku",       "1");
            put("accountNum",    "77788883");
            put("data", "データ部です");
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatter2HeaderAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord(recordMap3);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","aaaa","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","a","b","c"
        "1","12342","FSEｷﾞﾝｺｳ2","ﾏｺ12","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2","aaaa2","1","77788882","ﾀﾞｲｱﾅ ﾛｽ2","30202","N2","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2","42","Z","xxxxx2"
        "2","12343","FSEｷﾞﾝｺｳ3","ﾏｺ13","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3","aaaa3","1","77788883","データ部です"
        */

        
    }

    /**
     * 不正なマルチフォーマットのテスト
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testIllegalMultiFormat() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";

        /**
         *  データ区分のキーがMapに存在しないパターン 
         */
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        try {
            formatter.writeRecord(recordMap);
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
            assertTrue(e.getMessage().contains("an applicable record type was not found."));
        }
        dest.close();

        /**
         *  データ区分にマッチする値がMapに存在しないパターン 
         */
        recordMap = new HashMap<String, Object>() {{
            put("dataKbn",       "3");
            put("FIcode",        "振込先金融機関コード");
            put("FIname",        "振込先金融機関名称");
            put("officeCode",    "振込先営業所コード");
            put("officeName",    "振込先営業所名");
            put("tegataNum",     "(手形交換所番号)");
            put("syumoku",       "預金種目");
            put("accountNum",    "口座番号");
            put("recipientName", "受取人名");
            put("amount",        "振込金額");
            put("isNew",         "新規コード");
            put("ediInfo",       "EDI情報");
            put("transferType",  "振込区分");
            put("withEdi",       "EDI情報使用フラグ");
            put("unused",        "(未使用領域)");
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
            createMultiLayoutFormatterTitleAndData(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        try {
            formatter.writeRecord(recordMap);
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
            assertTrue(e.getMessage().contains("an applicable record type was not found."));
        }
        dest.close();
        

    }
    
    
    private DataRecordFormatter createMultiLayoutFormatter(String qt,
            String fs,
            String rs,
            String encoding) {

        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード       

        [TestTitleRecord]  # タイトルレコード
          dataKbn = "1"
        1   dataKbn       X    "データ区分"
        2   FIcode        X    "振込先金融機関コード"
        3   FIname        X    "振込先金融機関名称"
        4   officeCode    X    "振込先営業所コード"
        5   officeName    X    "振込先営業所名"
        6  ?tegataNum     X    "(手形交換所番号)"
        7   syumoku       X    "預金種目"
        8   accountNum    X    "口座番号"
        9   recipientName X    "受取人名"
        10  amount        X    "振込金額"
        11  isNew         X    "新規コード"
        12  ediInfo       N    "EDI情報"
        13  transferType  X    "振込区分"
        14  withEdi       X    "EDI情報使用フラグ"
        15 ?unused        X    "(未使用領域)"         
              
        
        [TestDataRecord]  # ヘッダーレコード
          dataKbn = "2"
        1   dataKbn       X   "2"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X             # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X  "Y"        # EDI情報使用フラグ
        15 ?unused        X             # (未使用領域)             
        ************************************************************/
        formatFile.deleteOnExit();
        return createFormatter("./format.dat");
    }
    
    private DataRecordFormatter createMultiLayoutFormatterTitleAndData(String qt,
            String fs,
            String rs,
            String encoding) {

        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード       

        [TestTitleRecord]  # ヘッダーレコード
          dataKbn = "1"
        1   dataKbn       X   "1"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X             # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X  "Y"        # EDI情報使用フラグ
        15 ?unused        X             # (未使用領域)         
        
        [TestDataRecord]  # ヘッダーレコード
          dataKbn = "2"
        1   dataKbn       X   "2"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X             # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X  "Y"        # EDI情報使用フラグ
        15 ?unused        X             # (未使用領域)             
        ************************************************************/

        formatFile.deleteOnExit();
        return createFormatter("./format.dat");
    }

    private DataRecordFormatter createMultiLayoutFormatter2HeaderAndData(String qt,
            String fs,
            String rs,
            String encoding) {

        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", qt, fs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード  
        2   withEdi   X   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード  

        [TestHeaderRecord1]  # ヘッダーレコード1
          dataKbn = "1"
          withEdi = "Y"
        1   dataKbn       X   "1"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X             # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X             # EDI情報使用フラグ     
        15  test1         X             # test1
        16  test2         X             # test2
        17  test3         X             # test3 
        
        [TestHeaderRecord2]  # ヘッダーレコード2
          dataKbn = "1"
          withEdi = "Z"
        1   dataKbn       X   "2"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X             # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X  "Y"        # EDI情報使用フラグ
        15 ?unused        X             # (未使用領域)            
                   
        [TestDataRecord]  # データレコード
          dataKbn = "2"
        1   dataKbn       X   "2"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   data          X             # データ部
        ************************************************************/

        formatFile.deleteOnExit();
        return createFormatter("./format.dat");
    }

    /**
     * ファイルの内容を文字列に変換する。
     * @param file ファイル
     * @return ファイルの内容を文字列化したもの
     */
    private String fileToString(File file, String charset) {

        StringBuilder builder = new StringBuilder();
        InputStreamReader inputStream = null;
        try {
            inputStream = new InputStreamReader(new FileInputStream(file), charset);
            char[] charBuffer = new char[1024];
            int length = 0;
            while ((length = inputStream.read(charBuffer)) >= 0) {
                builder.append(charBuffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(inputStream);
        }

        return builder.toString();
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
    }
    

    /**
     * マルチレイアウトかつrequires-titleディレクティブがtrueの場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するレコードタイプで1行目のタイトル行を書き込めること。
     * ②requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するレコードタイプで2行目以降のレコードが書き込まれないこと。
     * ③writeメソッドの引数でrequires-titleディレクティブに対応するレコードタイプを指定した場合に、そのレコードタイプで1行目のタイトル行を書き込めること。
     * ④requires-titleディレクティブをfalseに設定した場合に、requires-titleディレクティブに対応するレコードタイプでないレコードタイプを使用して1行目のタイトル行を書き込めること。
     */
    @Test
    public void testRequiresTitleDirective() throws Exception {
       
        /*
         * 以下、観点①、②
         */
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [Title]
        1   Kubun      N  "価格(税込)"
        2   Title      N  "タイトル"
        3   Publisher  N  "出版社"
        4   Authors    N  "著者"
        
        [DataRecord]
          Kubun = "1"
        1   Kubun      X  
        2   Title      N  
        3   Publisher  N  
        4   Authors    N  
        
        [TrailerRecord]
          Kubun = "2"
        1   Kubun      X  
        2   RecordNum  X  
        *****************************************/
        formatFile.deleteOnExit();

        String expected = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        区分,タイトル,出版社,著者
        1,Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb
        1,Programming with POSIX Threads,Addison-Wesley,David R. Butenhof
        2,2
        **********************************************************************/
        
        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("Kubun",    "区分");
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("Kubun",    "1");
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
        }};        
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("Kubun",    "1");
            put("Title",        "Programming with POSIX Threads");
            put("Publisher",        "Addison-Wesley");
            put("Authors",    "David R. Butenhof");
        }};        
        Map<String, Object> recordMap4 = new HashMap<String, Object>() {{
            put("Kubun",    "2");
            put("RecordNum",        "2");
        }};        
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        OutputStream dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord(recordMap3);
        formatter.writeRecord(recordMap4);
        
        String fileToString = fileToString(new File("./output.dat"), "ms932");
        
        assertEquals(expected, fileToString);
        

        /*
         * 以下、観点③
         */
        dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord("Title",recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord("DataRecord", recordMap3);
        formatter.writeRecord(recordMap4);
        
        fileToString = fileToString(new File("./output.dat"), "ms932");
        
        assertEquals(expected, fileToString);


        /*
         * 以下、観点④
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        requires-title: false  # 最初の行をタイトルとして読み書きしない!!
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [TitleRecord]
          Kubun = "区分"
        1   Kubun      N  "価格(税込)"
        2   Title      N  "タイトル"
        3   Publisher  N  "出版社"
        4   Authors    N  "著者"
        
        [DataRecord]
          Kubun = "1"
        1   Kubun      X  
        2   Title      N  
        3   Publisher  N  
        4   Authors    N  
        
        [TrailerRecord]
          Kubun = "2"
        1   Kubun      X  
        2   RecordNum  X  
        *****************************************/
        formatFile.deleteOnExit();
        
        dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord("TitleRecord",recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord("DataRecord", recordMap3);
        formatter.writeRecord(recordMap4);
        
        fileToString = fileToString(new File("./output.dat"), "ms932");
        
        assertEquals(expected, fileToString);
    }

    /**
     * マルチレイアウトかつrequires-titleディレクティブがtrueの場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するレコードタイプで1行目のタイトル行を書き込めること。
     * ②【異常系】2行目以降の行を書き出す場合に、requires-titleディレクティブに対応するレコードタイプの条件にマッチするレコードが指定された場合、例外がスローされること。
     */
    @Test
    public void testTitleHasCondition() throws Exception {
       
        /*
         * 以下、観点①
         */
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [Title]
          Kubun = "区分"
        1   Kubun      N  "価格(税込)"
        2   Title      N  "タイトル"
        3   Publisher  N  "出版社"
        4   Authors    N  "著者"
        
        [DataRecord]
          Kubun = "1"
        1   Kubun      X  
        2   Title      N  
        3   Publisher  N  
        4   Authors    N  
        
        [TrailerRecord]
          Kubun = "2"
        1   Kubun      X  
        2   RecordNum  X  
        *****************************************/
        formatFile.deleteOnExit();

        String expected = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        区分,タイトル,出版社,著者
        1,Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb
        1,Programming with POSIX Threads,Addison-Wesley,David R. Butenhof
        2,2
        **********************************************************************/
        
        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("Kubun",    "区分");
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("Kubun",    "1");
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
        }};        
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("Kubun",    "1");
            put("Title",        "Programming with POSIX Threads");
            put("Publisher",        "Addison-Wesley");
            put("Authors",    "David R. Butenhof");
        }};        
        Map<String, Object> recordMap4 = new HashMap<String, Object>() {{
            put("Kubun",    "2");
            put("RecordNum",        "2");
        }};        
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        OutputStream dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord(recordMap3);
        formatter.writeRecord(recordMap4);
        
        String fileToString = fileToString(new File("./output.dat"), "ms932");
        
        assertEquals(expected, fileToString);
        
        formatter.close();
        
        /*
         * 以下、観点②
         */
        dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        recordMap1 = new HashMap<String, Object>() {{
            put("Kubun",    "区分");
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
        }};
        recordMap2 = new HashMap<String, Object>() {{
            put("Kubun",    "1");
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
        }};        
        recordMap3 = new HashMap<String, Object>() {{
            put("Kubun",    "区分");
            put("Title",        "Programming with POSIX Threads");
            put("Publisher",        "Addison-Wesley");
            put("Authors",    "David R. Butenhof");
        }};        
        
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        try {
            formatter.writeRecord(recordMap3);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("title record occurred after the first line. When directive 'requires-title' is true, can not apply the record type 'Title' to after the first line. record type=[Title]."));
            assertThat(e.getMessage(), containsString("conditionToApply=[[Kubun = [区分]]]."));
            assertThat(e.getMessage(), containsString("record number=[3]."));
        }
        
        fileToString = fileToString(new File("./output.dat"), "ms932");
        
    }

    /**
     * Mapに設定された値がnullでもエラーとならずに出力されること
     */
    @Test
    public void testNullValue() throws Exception {

        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:     "UTF-8"
         record-separator:  "\n"
         field-separator:   ","
         requires-title: false

         [Classifier]
         1 type X

         [Type1]
         type = "1"
         1 type  X
         2 key1  X

         [Type2]
         type = "2"
         1 type  X
         2 key2  X
         *****************************************/
        formatFile.deleteOnExit();


        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("type", "1");
            put("key1", "value");
        }};

        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("type", "2");
            put("key2", null);
        }};

        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        OutputStream dest = new FileOutputStream(outputData, false);

        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();

        formatter.writeRecord("Type1", recordMap1);
        formatter.writeRecord("Type2", recordMap2);

        assertThat(fileToString(new File("./output.dat"), "ms932"), is(Hereis.string().replace(LS, "\n")));
        /**********************************************************************
        1,value
        2,
        **********************************************************************/
    }

    /**
     * 識別項目の値にnullを指定した場合でも出力できること。
     */
    @Test
    public void testClassifierNullValue() throws Exception {

        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:     "UTF-8"
         record-separator:  "\n"
         field-separator:   ","
         requires-title: false

         [Classifier]
         1 type X

         [Type1]
         type = "1"
         1 type  X "1"
         2 key1  X

         [Type2]
         type = "2"
         1 type  X "2"
         2 key2  X
         *****************************************/
        formatFile.deleteOnExit();


        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("type", null);
            put("key1", "value1");
        }};

        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        OutputStream dest = new FileOutputStream(outputData, false);

        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        formatter.writeRecord("Type1", recordMap);
        assertThat(fileToString(new File("./output.dat"), "ms932"), is(Hereis.string().replace(LS, "\n")));
        /**********************************************************************
        1,value1
        **********************************************************************/
    }
}
