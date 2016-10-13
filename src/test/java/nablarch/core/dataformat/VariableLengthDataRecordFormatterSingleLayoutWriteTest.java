package nablarch.core.dataformat;

import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * 可変長ファイルフォーマッタのシングルレイアウトのテストケース。
 * @author Masato Inoue
 */

public class VariableLengthDataRecordFormatterSingleLayoutWriteTest {
    
    private String LS = Builder.LS;
    
    private DataRecordFormatter formatter = null;
    
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(String filePath) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File(filePath));
        return formatter;
    }
    

    private String escapeRs(String recordSeparator) {
        return recordSeparator.replace("\r", "\\r").replace("\n", "\\n");
    }
    
    /**
     * レコードタイプが設定されていて、マルチレイアウトでない場合のテスト（シングルレイアウトの場合、レコードタイプを設定した場合、例外がスローされる）
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
         * 1行書き出すパターン。
         */
        DataRecord dataRecord = new DataRecord() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
        dataRecord.setRecordType("test");
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        DataRecordFormatter formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);
        
        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        try {
            formatter.writeRecord(dataRecord);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "an applicable record type was not found. specified record type=[test]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        }
    }
    
    /**
     *　囲み文字関連のテスト。
     */
    @Test
    public void testEnclose() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String fieldSeparator = ",";

        /**
         * 囲み文字を設定しないパターン。
         */
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("illegal1",       "error!!");
            put("illegal2",       "error!!");
            put("illegal3",       "error!!");
        }};

        DataRecordFormatter formatter = 
                createFormatterNotUseEnclose(fieldSeparator, recordSeparator, charset);
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        1234,FSEｷﾞﾝｺｳ,ﾏｺ1,ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ,1,7778888,ﾀﾞｲｱﾅ ﾛｽ,3020,N,ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ,4,Y
        */
        
        /**
         * 囲み文字がシングルクォートのパターン。
         */
        String enclose = "'";
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        '1234','FSEｷﾞﾝｺｳ','ﾏｺ1','ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ','1','7778888','ﾀﾞｲｱﾅ ﾛｽ','3020','N','ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ','4','Y'
        */
        

        /**
         * カラム内に囲み文字が含まれている場合に正しくエスケープされるパターン。
         */
        enclose = "\\\"";
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        recordMap = new HashMap<String, Object>() {{
            put("FIcode",        "\"1234");
            put("FIname",        "\"FSEｷﾞﾝｺｳ\"");
            put("officeCode",    "ﾏ\"\"ｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("illegal1",       "error!!");
            put("illegal2",       "error!!");
            put("illegal3",       "error!!");
        }};
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        """1234","""FSEｷﾞﾝｺｳ""","ﾏ""""ｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */
        
    }
    
    /**
     *　項目数の過不足。
     */
    @Test
    public void testMapSizeShortAndLong() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        /**
         * レイアウトにない項目をMapに設定するパターン。（レイアウトにない項目は無視され、ファイルに出力されない）
         */
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
            put("illegal1",       "error!!");
            put("illegal2",       "error!!");
            put("illegal3",       "error!!");
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        DataRecordFormatter formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        assertEquals(Hereis.string().replaceAll(LS, "\n"), fileToString);
        /*
        "1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */

        /**
         * レイアウトにある項目を設定しないパターン。（設定しない項目は空のカラムとして出力される）
         */
        recordMap = new HashMap<String, Object>() {{
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        
        try{
            formatter.writeRecord(recordMap);
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("field value was not set. field value must be set. field name=[FIcode].", e.getMessage());
        }
    }
    
    /**
     * 値を設定しないMapの項目について、デフォルト値が使用されるパターン。
     */
    @Test
    public void testDefaultValue() throws Exception {
       
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:        "Variable" 
        text-encoding:     "UTF-8"    # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字    

        [DataRecord]
        1   FIcode        X  ""           # 振込先金融機関コード
        2   FIname        X  "a1"           # 振込先金融機関名称
        3   officeCode    X  "a2"           # 振込先営業所コード
        4   officeName    X  "D"           # 振込先営業所名
        5   syumoku       X  "a4"           # 預金種目
        6   accountNum    X  "a5"           # 口座番号
        7   recipientName X  "0"           # 受取人名
        8   amount        X  "1"           # 振込金額
        9   isNew         X  "2"           # 新規コード
        10  ediInfo       N  "3"           # EDI情報
        11  transferType  X  "4"           # 振込区分
        12 ?withEdi       X  ""        # EDI情報使用フラグ
        ************************************************************/
        formatFile.deleteOnExit();
        DataRecordFormatter formatter = createFormatter("./format.dat");

        
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("officeName", "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
        }};

        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), "UTF-8");
        
        assertEquals(Hereis.string().replaceAll(LS, "\n"), fileToString);
        /*
        "","a1","a2","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","a4","a5","0","1","2","3","4",""
        */
    }
    
    /**
     * 改行コードLFのテスト。
     * 
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testWriteLF() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        doTestSimple(charset, recordSeparator, enclose, fieldSeparator);
    }
    
    /**
     * 改行コードCRのテスト。
     * 
     * 改行コード   ： CR
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testWriteCR() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\r";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        doTestSimple(charset, recordSeparator, enclose, fieldSeparator);
    }

    /**
     * 改行コードCRLFのテスト。
     * 
     * 改行コード   ： CRLF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testWriteCRLF() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\r\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        doTestSimple(charset, recordSeparator, enclose, fieldSeparator);
    }

    /**
     * 文字コードMS932のテスト。
     * 
     * 改行コード   ： CRLF
     * 文字コード   ： MS932
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testWriteMS932() throws Exception {

        String charset = "MS932";
        String recordSeparator = "\r\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        doTestSimple(charset, recordSeparator, enclose, fieldSeparator);
    }
    
    
    /**
     * データレコードの書き出し
     */
    public void doTestSimple(String charset, final String recordSeparator, String enclose, String fieldSeparator) throws Exception {
        
        /**
         * 1行書き出すパターン。
         */
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        DataRecordFormatter formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */


        /**
         * 三行書き出すパターン。
         */
        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("FIcode",        "12342");
            put("FIname",        "FSEｷﾞﾝｺｳ2");
            put("officeCode",    "ﾏｺ12");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2");
            put("syumoku",       "2");
            put("accountNum",    "77788882");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ2");
            put("amount",        "30202");
            put("isNew",         "O");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2");
            put("transferType",  "5");
            put("withEdi",       "Z");
        }};
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("FIcode",        "12343");
            put("FIname",        "FSEｷﾞﾝｺｳ3");
            put("officeCode",    "ﾏｺ13");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3");
            put("syumoku",       "3");
            put("accountNum",    "77788883");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ3");
            put("amount",        "30203");
            put("isNew",         "P");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3");
            put("transferType",  "5");
            put("withEdi",       "A");
        }};

        FileUtil.deleteFile(outputData);
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord(recordMap3);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
                
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        "12342","FSEｷﾞﾝｺｳ2","ﾏｺ12","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2","2","77788882","ﾀﾞｲｱﾅ ﾛｽ2","30202","O","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2","5","Z"
        "12343","FSEｷﾞﾝｺｳ3","ﾏｺ13","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3","3","77788883","ﾀﾞｲｱﾅ ﾛｽ3","30203","P","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3","5","A"
        */


        /**
         * 改行を出力するパターン。
         */
        recordMap = new HashMap<String, Object>() {{
            put("FIcode",        recordSeparator + "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ" + recordSeparator);
            put("officeCode",    "ﾏ" + recordSeparator + "ｺ1");
            put("officeName",    "ﾏﾂﾄﾞ" + recordSeparator + recordSeparator + "ｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       recordSeparator);
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
       
        FileUtil.deleteFile(outputData);
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(recordMap);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
                
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "
        1234","FSEｷﾞﾝｺｳ
        ","ﾏ
        ｺ1","ﾏﾂﾄﾞ
        
        ｺｶﾞﾈﾊﾗｼﾃﾝ","
        ","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */
        
        /**
         * 引数のMapがnullのパターン。
         */
        FileUtil.deleteFile(outputData);
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        try{
            formatter.writeRecord(null);
            fail();
        } catch(NullPointerException e){
            assertTrue(true);
        }
        dest.close();
        
        

        /**
         * Mapのキーを設定しない場合、それらが空白のカラムとして出力されるパターン。
         */
        recordMap = new HashMap<String, Object>() {{
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        try {
            formatter.writeRecord(recordMap);
            fail();
        } catch (IllegalArgumentException e){
            assertTrue(true);
        }
        dest.close();

        
        /**
         * Mapの値を設定しない場合、例外がスローされるパターン。
         */
        recordMap = new HashMap<String, Object>() {{
            put("FIcode",        null);
            put("FIname",        null);
            put("officeCode",    null);
            put("officeName",    null);
            put("syumoku",       null);
            put("accountNum",    null);
            put("recipientName", null);
            put("amount",        null);
            put("isNew",         null);
            put("ediInfo",       null);
            put("transferType",  null);
            put("withEdi",       null);
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);
        formatter.setOutputStream(dest).initialize();
        try {
            formatter.writeRecord(recordMap);
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("field value was not set. field value must be set. field name=[FIcode].", e.getMessage());
        }
        dest.close();


    }
    

    /**
     * データレコードの書き出し
     */
    @Test
    public void testRecordTypeNullOrEmpty() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        /**
         * 引数がDataRecordかつ、レコードタイプが設定されていない。
         */
        DataRecord datarecord = new DataRecord() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        DataRecordFormatter formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(datarecord);
        dest.close();
        
        String fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */

        
        
        /**
         * 引数がDataRecordかつ、レコードタイプが空文字。
         */
        datarecord = new DataRecord() {{
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");
            put("ediInfo",       "ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ");
            put("transferType",  "4");
            put("withEdi",       "Y");
        }};
        
        datarecord.setRecordType("");
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatter = 
                createFormatterSimple(enclose, fieldSeparator, recordSeparator, charset);

        dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(datarecord);
        dest.close();
        
        fileToString = fileToString(new File("./output.dat"), charset);
        
        
        assertEquals(Hereis.string().replaceAll(LS, recordSeparator), fileToString);
        /*
        "1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","1","7778888","ﾀﾞｲｱﾅ ﾛｽ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */

    }
    
    private DataRecordFormatter createFormatterSimple(String qt,
            String fs,
            String rs,
            String encoding) {

        rs = escapeRs(rs);
        
        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
        file-type:        "Variable" 
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字    

        [DataRecord]
        1   FIcode        X             # 振込先金融機関コード
        2   FIname        X             # 振込先金融機関名称
        3   officeCode    X             # 振込先営業所コード
        4   officeName    X             # 振込先営業所名
        5   syumoku       X             # 預金種目
        6   accountNum    X             # 口座番号
        7   recipientName X             # 受取人名
        8   amount        X             # 振込金額
        9   isNew         X             # 新規コード
        10  ediInfo       N             # EDI情報
        11  transferType  X             # 振込区分
        12 ?withEdi       X             # EDI情報使用フラグ
        ************************************************************/
        formatFile.deleteOnExit();
        return createFormatter("./format.dat");
    }
    
    private DataRecordFormatter createFormatterNotUseEnclose(
            String fs,
            String rs,
            String encoding) {

        rs = escapeRs(rs);
        
        File formatFile = Hereis.file("./format.dat", fs, rs, encoding);
        /***********************************************************
        file-type:        "Variable" 
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字

        [DataRecord]
        1   FIcode        X             # 振込先金融機関コード
        2   FIname        X             # 振込先金融機関名称
        3   officeCode    X             # 振込先営業所コード
        4   officeName    X             # 振込先営業所名
        5   syumoku       X             # 預金種目
        6   accountNum    X             # 口座番号
        7   recipientName X             # 受取人名
        8   amount        X             # 振込金額
        9   isNew         X             # 新規コード
        10  ediInfo       N             # EDI情報
        11  transferType  X             # 振込区分
        12 ?withEdi       X             # EDI情報使用フラグ
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
        if(formatter == null) {
            formatter.close();
        }
    }
    
    /**
     * requires-titleディレクティブがtrueの場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するデフォルトのレコードタイプ[Title]で最初の行が書き込めること。writeメソッドの引数にレコードタイプを指定しなくても、自動的に一行目が[Title]のレコードタイプで出力される。
     * ②requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するデフォルトのレコードタイプ[Title]で2行目以降のレコードが書き込まれないこと。
     * ③writeメソッドの引数でrequires-titleディレクティブに対応するレコードタイプを指定した場合に、そのレコードタイプで1行目のタイトル行を書き込めること。
     * ④requires-titleディレクティブをfalseに設定した場合に、requires-titleディレクティブに対応するレコードタイプでないレコードタイプを使用してタイトル行を書き込めること。
     */
    @Test
    public void testTitle() throws Exception {
       
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
        
        [Title]
        1   Title      N  "タイトル"
        2   Publisher  N  "出版社"
        3   Authors    N  "著者"
        4   Price      N  "価格(税込)"
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X  Number  # 価格
        *****************************************/
        formatFile.deleteOnExit();

        String expected = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/

        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
            put("Price",    "価格(税込)");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
            put("Price",    "38.50");
        }};        
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("Title",        "Programming with POSIX Threads");
            put("Publisher",        "Addison-Wesley");
            put("Authors",    "David R. Butenhof");
            put("Price",    "29.00");
        }};        
        Map<String, Object> recordMap4 = new HashMap<String, Object>() {{
            put("Title",        "HACKING (2nd ed)");
            put("Publisher",        "no starch press");
            put("Authors",    "Jon Erickson");
            put("Price",    "35.20");
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
        formatter.writeRecord("Books", recordMap3);
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
        
        [TitleRecord]
        1   Title      N  "タイトル"
        2   Publisher  N  "出版社"
        3   Authors    N  "著者"
        4   Price      N  "価格(税込)"
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X  Number  # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord("TitleRecord",recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord("Books", recordMap3);
        formatter.writeRecord(recordMap4);
        
        fileToString = fileToString(new File("./output.dat"), "ms932");
        
        assertEquals(expected, fileToString);

    }
    
    /**
     * requires-titleディレクティブがtrueの場合の異常系テスト。
     * 
     * 以下の観点でテストを行う。
     * ①【異常系】2行目以降の行を書き出す時のレコードタイプに、requires-titleディレクティブに対応するレコードタイプを指定した場合、例外がスローされること。
     * ②【異常系】1行目の行を書き出す時のレコードタイプに、requires-titleディレクティブに対応するレコードタイプ以外のレコードタイプを指定した場合、例外がスローされること。
     */
    @Test
    public void testInvalidTitle() throws Exception {
       
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Title]
        1   Title      N  "タイトル"
        2   Publisher  N  "出版社"
        3   Authors    N  "著者"
        4   Price      N  "価格(税込)"
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X  Number  # 価格
        *****************************************/
        formatFile.deleteOnExit();

        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
            put("Price",    "価格(税込)");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
            put("Price",    "38.50");
        }};        
        Map<String, Object> recordMap3 = new HashMap<String, Object>() {{
            put("Title",        "Programming with POSIX Threads");
            put("Publisher",        "Addison-Wesley");
            put("Authors",    "David R. Butenhof");
            put("Price",    "29.00");
        }};        
        Map<String, Object> recordMap4 = new HashMap<String, Object>() {{
            put("Title",        "HACKING (2nd ed)");
            put("Publisher",        "no starch press");
            put("Authors",    "Jon Erickson");
            put("Price",    "35.20");
        }};        
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();

        OutputStream dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        /*
         * 観点①
         */
        formatter.writeRecord("Title",recordMap1); // 1行目はOK
        formatter.writeRecord("Books",recordMap2);
        try {
            formatter.writeRecord("Title",recordMap3); // 3行目はNG!!
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("invalid record type was specified. When directive 'requires-title' is true, record type of after the first line must not be 'Title'. record type=[Title]."));
            assertThat(e.getMessage(), containsString("record number=[3]."));
        }

        /*
         * 観点②
         */
        dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        try {
            formatter.writeRecord("Books",recordMap1);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("invalid record type was specified. When directive 'requires-title' is true, record type of first line must be 'Title'. record type=[Books]."));
            assertThat(e.getMessage(), containsString("record number=[1]."));
        }

    }


    /**
     * タイトルのみ存在するフォーマット定義で書き込むテスト。
     * 
     * 以下の観点でテストを行う。
     * ①タイトルのみ存在するファイルにタイトルのみ（最初の行のみ）を書き込めること。
     * ②タイトルのみ存在するファイルにタイトル以降の行（最初の行以降の行）を書き込もうとした場合、例外がスローされること。
     */
    @Test
    public void testTitleOnly() throws Exception {
       
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Title]
        1   Title      N  "タイトル"
        2   Publisher  N  "出版社"
        3   Authors    N  "著者"
        4   Price      N  "価格(税込)"
        
        *****************************************/
        formatFile.deleteOnExit();

        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("Title",        "タイトル");
            put("Publisher",        "出版社");
            put("Authors",    "著者");
            put("Price",    "価格(税込)");
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("Title",        "Learning the vi and vim Editors");
            put("Publisher",        "OReilly");
            put("Authors",    "Robbins Hanneah and Lamb");
            put("Price",    "38.50");
        }};        
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();

        OutputStream dest = new FileOutputStream(outputData, false);
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setOutputStream(dest).initialize();
        
        /*
         * 観点①
         */
        formatter.writeRecord("Title", recordMap1); // 1行目はOK
        
        /*
         * 観点②
         */
        try {
            formatter.writeRecord(recordMap2); // 2行目はNG!!
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("an applicable record type was not found. This format must not have non-title records."));
            assertThat(e.getMessage(), containsString("record number=[2]."));
        }

    }
}
