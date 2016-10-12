package nablarch.core.dataformat;

import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
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

    private DataRecordFormatter formatter;
   
    private static final String LS = Builder.LS;
  
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(String filePath) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File(filePath));
        return formatter;
    }
    /** フォーマッタ(write)を生成する。 */
    private DataRecordFormatter createWriteFormatter(File filePath, OutputStream dest) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        formatter.setOutputStream(dest).initialize();
        return formatter;
    }
    /** フォーマッタ(read)を生成する。 */
    private DataRecordFormatter createReadFormatter(File filePath, InputStream source) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath).setInputStream(source).initialize();
        formatter.setInputStream(source);
        return formatter;
    }
    
    @Before
    public void setUp() {

        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        #
        # 共通定義部分 
        #
        file-type:    "Fixed"
        text-encoding:    "ms932" # 文字列型フィールドの文字エンコーディング
        record-length:     120    # 各レコードの長さ
       
        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X(1)   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード                    
        113 withEdi   X(1)   # EDI情報使用フラグ
                             #    Y: EDIあり、N: なし

        [Header]  # ヘッダーレコード
          dataKbn = "1"
        1   dataKbn     X(1)  "1"      # データ区分
        2   processCode X(2)           # 業務種別コード
        4   codeKbn     X(1)           # コード区分
        5   itakuCode   X(10)          # 振込元の委託者コード
        15  itakuName   X(40)          # 振込元の委託者名
        55  date        X(4)           # 振込指定日
        59 ?unused      X(62) pad("0") # (未使用領域)


        [DataWithEDI] # データレコード (EDI情報あり)
          dataKbn  = "2"
          withEdi  = "Y"
        1    dataKbn       X(1)  "2"     # データ区分
        2    FIcode        X(4)          # 振込先金融機関コード
        6    FIname        X(15)         # 振込先金融機関名称
        21   officeCode    X(3)          # 振込先営業所コード
        24   officeName    X(15)         # 振込先営業所名
        39  ?tegataNum     X(4)  "9999"  # (手形交換所番号:未使用)
        43   syumoku       X(1)          # 預金種目
        44   accountNum    X(7)          # 口座番号
        51   recipientName X(30)         # 受取人名
        81   amount        X(10)         # 振込金額
        91   isNew         X(1)          # 新規コード
        92   ediInfo       X(20)         # EDI情報
        112  transferType  X(1)          # 振込区分
        113  withEdi       X(1)  "Y"     # EDI情報使用フラグ
        114 ?unused        X(7)  pad("0")# (未使用領域)            


        [DataWithoutEDI] < [DataWithEDI]  # データレコード (EDI情報なし)
          dataKbn = "2"                   #   EDI情報なしの場合、振込人情報を
          withEdi = "N"                   #   EDI情報の代わりに付記する。
        92   userCode1     X(10)      # ユーザコード1
        102  userCode2     X(10)      # ユーザコード2
        113  withEdi       X(1)  "N"  # EDI情報使用フラグ


        [Trailer] # トレーラーレコード
          dataKbn =  "8"
        1   dataKbn      X(1)   "8"      # データ区分
        2   totalRecords X(6)            # 総レコード件数
        8  ?unused       X(113) pad("0") # 未使用領域
        
        
        [Ending] # エンドレコード
          dataKbn = "9"
        1  dataKbn  X(1)   "9"      # データ区分
        2 ?unused   X(119) pad("0") # 未使用領域
                                       
        ************************************************************/
        formatFile.deleteOnExit();

        formatter = createFormatter("./format.dat");
    }
    
    
    /**
     * マルチフォーマットレコード読み込みのテスト
     */
    @Test
    public void testReadFrom() throws Exception {

        // #1 ヘッダレコード
        String testdata = Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          1660FSE302929 ﾀｼﾞﾏｺﾝﾂｪﾙﾝ ﾛﾝﾄﾞﾝｼﾃﾝ                 
              0831000000000000000000000000000000000000000000
          00000000000000000000*/
        //12345678901234567890123456789012345678901234567890
        
        //         1         2         3         4         5
        // #2 データレコード(EDIあり)
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          21234FSEｷﾞﾝｺｳ       ﾏｺ1ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  999917778888
          ﾀﾞｲｱﾅ ﾛｽ                      3020      Nﾀｸｼｰﾀﾞｲｷﾝ
          ﾃﾞｽ        4Y0000000*/                            
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #3 データレコード(EDIなし)
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          21234FSEｷﾞﾝｺｳ       ﾏｺ1ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  999917778888
          ﾀﾞｲｱﾅ ﾛｽ                      3020      Nﾀｼﾞﾏｲﾜｳｵ 
           302929    4N0000000*/                            
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #4 トレーラーレコード
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          85     0000000000000000000000000000000000000000000
          00000000000000000000000000000000000000000000000000
          00000000000000000000*/                           
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #5 エンドレコード
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          90000000000000000000000000000000000000000000000000
          00000000000000000000000000000000000000000000000000
          00000000000000000000*/                           
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        testdata = testdata.replace(LS, "");
        
        File dataFile = new File("./test.dat");
        dataFile.deleteOnExit();
        new FileOutputStream(dataFile, false).write(testdata.getBytes("ms932"));
        
        InputStream source = new FileInputStream("./test.dat");
        
        // ヘッダーレコードの読み込み
        DataRecord record = formatter.setInputStream(source).initialize().readRecord();
        
        assertEquals("Header", record.getRecordType()); 
        assertEquals(6, record.size());
        assertEquals("1",                   record.get("dataKbn"));     // 1. データ区分
        assertEquals("66",                  record.get("processCode")); // 2. 業務種別コード
        assertEquals("0",                   record.get("codeKbn"));     // 3. コード区分
        assertEquals("FSE302929",           record.get("itakuCode"));   // 4. 振込元の委託者コード
        assertEquals("ﾀｼﾞﾏｺﾝﾂｪﾙﾝ ﾛﾝﾄﾞﾝｼﾃﾝ", record.get("itakuName"));   // 5. 振込元の委託者名
        assertEquals("0831",                record.get("date"));        // 6. 振込指定日
        assertTrue  (!record.containsKey("unused"));                    // (未使用領域)
        
        
        // データレコードの読み込み
        record = formatter.setInputStream(source).readRecord();
        assertEquals("DataWithEDI", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ﾀﾞｲｱﾅ ﾛｽ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        // データレコードの読み込み
        record = formatter.setInputStream(source).readRecord();
        assertEquals("DataWithoutEDI", record.getRecordType()); 
//        assertEquals(14, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ﾀﾞｲｱﾅ ﾛｽ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード

        assertEquals("ﾀｼﾞﾏｲﾜｳｵ",     record.get("userCode1"));     // 11_a.ユーザコード1
        assertEquals("302929",       record.get("userCode2"));     // 11_b.ユーザコード2
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("N",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        
        // トレーラーレコードの読み込み
        record = formatter.setInputStream(source).readRecord();
        assertEquals("Trailer", record.getRecordType()); 
        assertEquals(2, record.size());

        assertEquals("8", record.get("dataKbn"));      // 1. データ区分
        assertEquals("5", record.get("totalRecords")); // 2. 総レコード件数
        
        // エンドレコードの読み込み
        record = formatter.setInputStream(source).readRecord();
        assertEquals("Ending", record.getRecordType()); 
        assertEquals(1, record.size());

        assertEquals("9", record.get("dataKbn"));      // 1. データ区分
        
        assertNull(formatter.setInputStream(source).readRecord());
    }
    
    
    /**
     * マルチフォーマットデータの書き出し
     */
    @Test
    public void testWriteTo() throws Exception {
        Map<String, Object> headerMap = new HashMap<String, Object>() {{
            put("dataKbn",     "1");
            put("processCode", "66");
            put("codeKbn",     "0");
            put("itakuCode",   "FSE302929");
            put("itakuName",   "ﾀｼﾞﾏｺﾝﾂｪﾙﾝ ﾛﾝﾄﾞﾝｼﾃﾝ");
            put("date",        "0831");
        }};

        Map<String, Object> recordMap1 = new HashMap<String, Object>() {{
            put("dataKbn",       "2");
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
            put("dataKbn",       "2");
            put("FIcode",        "1234");
            put("FIname",        "FSEｷﾞﾝｺｳ");
            put("officeCode",    "ﾏｺ1");
            put("officeName",    "ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ");
            put("syumoku",       "1");
            put("accountNum",    "7778888");
            put("recipientName", "ﾀﾞｲｱﾅ ﾛｽ");
            put("amount",        "3020");
            put("isNew",         "N");            
            put("userCode1",     "ﾀｼﾞﾏｲﾜｳｵ");     // 11_a.ユーザコード1
            put("userCode2",     "302929");       // 11_b.ユーザコード2
            put("transferType",  "4");
            put("withEdi",       "N");            // EDI使用フラグ
        }};
        
        // レコードタイプを明示的に指定する場合
        // この場合、レコードタイプの判定処理がスキップされるため、
        // その分性能が向上する。
        DataRecord trailerRecord = new DataRecord() {{
            put("dataKbn",      "8");
            put("totalRecords", "5");
        }}.setRecordType("Trailer");
        
        Map<String, Object> endRecordMap = new HashMap<String, Object>() {{
            put("dataKbn",      "9");
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        formatter.writeRecord(headerMap);
        formatter.writeRecord(recordMap1);
        formatter.writeRecord(recordMap2);
        formatter.writeRecord(trailerRecord);
        formatter.writeRecord(endRecordMap);
        
        dest.close();
        
        InputStream in = new FileInputStream("./output.dat");
        
        byte[] recordBytes = new byte[120 * 5];
        in.read(recordBytes);
        
        // #1 ヘッダレコード(EDIあり)
        String testdata = Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          1660FSE302929 ﾀｼﾞﾏｺﾝﾂｪﾙﾝ ﾛﾝﾄﾞﾝｼﾃﾝ                 
              0831000000000000000000000000000000000000000000
          00000000000000000000*/
        //12345678901234567890123456789012345678901234567890
        
        //         1         2         3         4         5
        // #2 データレコード(EDIあり)
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          21234FSEｷﾞﾝｺｳ       ﾏｺ1ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  999917778888
          ﾀﾞｲｱﾅ ﾛｽ                      3020      Nﾀｸｼｰﾀﾞｲｷﾝ
          ﾃﾞｽ        4Y0000000*/                            
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #3 データレコード(EDIなし)
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          21234FSEｷﾞﾝｺｳ       ﾏｺ1ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  999917778888
          ﾀﾞｲｱﾅ ﾛｽ                      3020      Nﾀｼﾞﾏｲﾜｳｵ 
           302929    4N0000000*/                            
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #4 トレーラーレコード
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          85     0000000000000000000000000000000000000000000
          00000000000000000000000000000000000000000000000000
          00000000000000000000*/                           
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        // #5 エンドレコード
        testdata += Hereis.string();
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          90000000000000000000000000000000000000000000000000
          00000000000000000000000000000000000000000000000000
          00000000000000000000*/                           
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        testdata = testdata.replace(LS, "");
        
        assertEquals(testdata , new String(recordBytes, "ms932"));
        assertEquals(-1, in.read());
    }
    

    /**
     * Writeの際に、引数で指定したレコードタイプが、レイアウト定義ファイルのレコードタイプに存在しない場合。
     */
    @Test
    public void testWriteNotExistDataType() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /***********************************************************
        #
        # 共通定義部分 
        #
        file-type:    "Fixed"
        text-encoding:    "ms932" # 文字列型フィールドの文字エンコーディング
        record-length:     11    # 各レコードの長さ
       
        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X(1)   # データ区分                  

        [Header]  # ヘッダーレコード
          dataKbn = "1"
        1   dataKbn     X(1)  "1"      # データ区分
        2   processCode N(10)           # 業務種別コード

        [DataWithEDI] # データレコード (EDI情報あり)
          dataKbn  = "2"
        1    dataKbn       X(1)  "2"     # データ区分
        2    FIcode        N(10)          # 振込先金融機関コード        

        [Trailer] # トレーラーレコード
          dataKbn =  "8"
        1   dataKbn      X(1)            # データ区分
        2   totalRecords N(10)            # 総レコード件数
                                       
        ************************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("output",  "file:./")
        .addBasePathSetting("format", "file:./");
        
        Map<String, Object> record1 = new DataRecord() {{
            put("dataKbn", "1"); // DataRecordのデータ区分は正しくても、不正な引数のデータ区分が優先される
            put("processCode", "あいうえお");
        }};
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        DataRecordFormatter formatter = createWriteFormatter(new File("format.fmt"), outputStream);
    
        try {
            formatter.writeRecord("error", record1);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString(
                    "an applicable layout definition was not found. specified record type=[error]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.fmt"));
        }
    }   
    
    
    /**
     * 入力データのデータ区分が、レイアウト定義ファイルで指定された識別子に合致しない場合に、例外がスローされること。
     */
    @Test
    public void testReadInvalidDataType() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /***********************************************************
        #
        # 共通定義部分 
        #
        file-type:    "Fixed"
        text-encoding:    "ms932" # 文字列型フィールドの文字エンコーディング
        record-length:     10    # 各レコードの長さ
       
        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X(1)   # データ区分                  

        [Header]  # ヘッダーレコード
          dataKbn = "1"
        1   dataKbn     X(1)  "1"      # データ区分
        2   processCode X(9)           # 業務種別コード

        [DataWithEDI] # データレコード (EDI情報あり)
          dataKbn  = "2"
        1    dataKbn       X(1)  "2"     # データ区分
        2    FIcode        X(9)          # 振込先金融機関コード        

        [Trailer] # トレーラーレコード
          dataKbn =  "8"
        1   dataKbn      X(1)            # データ区分
        2   totalRecords X(9)            # 総レコード件数
                                       
        ************************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
        .addBasePathSetting("format", "file:./");
        
        // 
        String data = 
            "112345678901234567890123456783"; // ２行目が合致しない
        
        InputStream source = createInputStreamFrom(data);
        DataRecordFormatter formatter = createReadFormatter(new File("format.fmt"), source);

        formatter.readRecord(); // １行目はデータ区分が「1」なので正常に読める
        
        try {
            formatter.readRecord(); // ２行目はデータ区分が「0」なので例外
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString(
                    "an applicable layout definition was not found in the record. record=[{dataKbn=0}]. "));
            assertThat(e.getRecordNumber(), is(2));
            assertThat(e.getFormatFilePath(), containsString("format.fmt"));
        }
    }

    /**
     * 出力データのデータ区分が、レイアウト定義ファイルで指定された識別子に合致しない場合に、例外がスローされること。
     */
    @Test
    public void testWriteInvalidDataType() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /***********************************************************
        #
        # 共通定義部分 
        #
        file-type:    "Fixed"
        text-encoding:    "ms932" # 文字列型フィールドの文字エンコーディング
        record-length:     11    # 各レコードの長さ
       
        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X(1)   # データ区分                  

        [Header]  # ヘッダーレコード
          dataKbn = "1"
        1   dataKbn     X(1)  "1"      # データ区分
        2   processCode N(10)           # 業務種別コード

        [DataWithEDI] # データレコード (EDI情報あり)
          dataKbn  = "2"
        1    dataKbn       X(1)  "2"     # データ区分
        2    FIcode        N(10)          # 振込先金融機関コード        

        [Trailer] # トレーラーレコード
          dataKbn =  "8"
        1   dataKbn      X(1)            # データ区分
        2   totalRecords N(10)            # 総レコード件数
                                       
        ************************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("output",  "file:./")
        .addBasePathSetting("format", "file:./");
        
        Map<String, Object> record1 = new DataRecord() {{
            put("dataKbn", "1");
            put("processCode", "あいうえお");
        }};
        Map<String, Object> record2 = new DataRecord() {{
            put("dataKbn", "0");
            put("FIcode", "かきくけこ");
        }};
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        formatter = createWriteFormatter(new File("format.fmt"), outputStream);
        formatter.writeRecord(record1);
        try {
            formatter.writeRecord(record2);
            fail();
        } catch (InvalidDataFormatException e) {
            // ２行目でエラーが発生する
            assertThat(e.getMessage(), startsWith("an applicable layout definition was not found in the record."));
            assertThat(e.getMessage(), containsString("dataKbn=0"));
            assertThat(e.getMessage(), containsString("FIcode=かきくけこ"));
            assertThat(e.getFormatFilePath(), endsWith("format.fmt"));
        }
    }
    
    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }
    
}
