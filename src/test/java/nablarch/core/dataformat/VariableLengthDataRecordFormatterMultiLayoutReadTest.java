package nablarch.core.dataformat;

import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * 可変長ファイルフォーマッタのマルチレイアウトのテストケース。
 * 
 * 観点：
 * 可変長ファイルをマルチレイアウトで読み込む際の、正常系テストおよび異常系テストを網羅する。
 * 
 * @author Masato Inoue
 */
public class VariableLengthDataRecordFormatterMultiLayoutReadTest {
    
    private String LS = System.getProperty("line.separator");

    private DataRecordFormatter formatter;

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

    /**
     * 典型的なマルチレイアウトの読み書きのテスト。
     */
    @Test
    public void testMultiLayoutFormat() throws Exception {
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        
        [Classifier]
        4  Price X     # 第4カラムを見て判定する。
        
        [Header]
          Price = "価格(税込)"
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
        
        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));

        formatter = createReadFormatter(formatFile, source);
        
        // よみこみ
        
        DataRecord record;
        
        // ヘッダー行
        record = formatter.setInputStream(source).readRecord();
        assertEquals("Header", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // データ行 #1
        record = formatter.setInputStream(source).readRecord();
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        
        // データ行 #2
        record =  formatter.setInputStream(source).readRecord();
        assertEquals("Books", record.getRecordType());
        
        // データ行 #3
        record =  formatter.setInputStream(source).readRecord();
        assertEquals("Books", record.getRecordType());
        
        // 終了
        assertNull(formatter.setInputStream(source).readRecord());
        
        
        
        
        // 書き込み
        
        File destfile = new File("./test.out");
        destfile.createNewFile();
        destfile.deleteOnExit();
        
        dest = new FileOutputStream(destfile);
        

        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
             new FileInputStream(destfile), "ms932"
        ));
        

        formatter = createWriteFormatter(new File("./test.fmt"), dest);
        formatter.writeRecord("Header", new HashMap<String, Object>());
        
        record = new DataRecord();
        record.put("Title",     "Programming Ruby 2nd Edition");
        record.put("Publisher", "Pragmatic Bookshelf");
        record.put("Authors",   "Dave Thomas");
        record.put("Price",     "44.95");
        
        formatter.writeRecord("Books", record);
        
        formatter.writeRecord(record); // 自動判定
        
        Map<String, Object> footer = new HashMap<String, Object>();
        footer.put("Price", "価格(税込)");
        formatter.writeRecord(footer); // 自動判定
        
        dest.flush();
        
        // 出力時には、全てのカラムが強制的にクォートされる。
        assertEquals("\"タイトル\",\"出版社\",\"著者\",\"価格(税込)\"", reader.readLine());

        // 出力時には、全てのカラムが強制的にクォートされる。
        assertEquals(
            "\"Programming Ruby 2nd Edition\",\"Pragmatic Bookshelf\",\"Dave Thomas\",\"44.95\""
          , reader.readLine()
        );
        assertEquals(
            "\"Programming Ruby 2nd Edition\",\"Pragmatic Bookshelf\",\"Dave Thomas\",\"44.95\""
          , reader.readLine()
        );
        
        assertEquals("\"タイトル\",\"出版社\",\"著者\",\"価格(税込)\"", reader.readLine());
    }
    
    /**
     * マルチフォーマットの読み込みテスト。
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
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "1","振込先金融機関コード","振込先金融機関名称","振込先営業所コード","振込先営業所名","(手形交換所番号)","預金種目","口座番号","受取人名","振込金額","新規コード","EDI情報","振込区分","EDI情報使用フラグ","(未使用領域)"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */
        InputStream dataStream = createInputStream(charset, testdata);


        formatter = createMultiLayoutFormatter(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        // タイトル行の読み込み
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertEquals("TestTitleRecord", record.getRecordType()); 
        assertEquals(13, record.size());
        assertEquals("1",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("振込先金融機関コード",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("振込先金融機関名称",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("振込先営業所コード",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("振込先営業所名", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("預金種目",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("口座番号",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("受取人名",     record.get("recipientName")); // 8. 受取人名
        assertEquals("振込金額",         record.get("amount"));        // 9. 振込金額
        assertEquals("新規コード",            record.get("isNew"));         // 10.新規コード
        assertEquals("EDI情報", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("振込区分",            record.get("transferType"));  // 12.振込区分
        assertEquals("EDI情報使用フラグ",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        // データ行の読み込み
        record = formatter.setInputStream(dataStream).readRecord();
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());
        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        

        /**
         *  データ区分が1かつEDI情報使用フラグがY（ヘッダ1）と、データ区分が1かつEDI情報使用フラグがZ（ヘッダ2）のパターン。 
         */
        formatter = createMultiLayoutFormatterWithEDI(enclose, fieldSeparator, recordSeparator, charset);
        
        testdata = Hereis.string().replaceAll(LS, "\n");
        /*
        "1","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","テスト1","テスト2","テスト3"
        "1","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Z","XXXXXX"
        "3","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","データ部です。"
        */
        dataStream = createInputStream(charset, testdata);

        // ヘッダ行（EDI情報使用フラグY）の読み込み
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertEquals("TestHeaderRecord1", record.getRecordType()); 
        assertEquals(16, record.size());
        assertEquals("1",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertEquals("テスト1",            record.get("test1"));       // 14.テスト１情報
        assertEquals("テスト2",            record.get("test2"));       // 15.テスト２情報
        assertEquals("テスト3",            record.get("test3"));       // 16.テスト３情報


        // ヘッダ行（EDI情報使用フラグN）の読み込み
        record = formatter.setInputStream(dataStream).readRecord();
        assertEquals("TestHeaderRecord2", record.getRecordType()); 
        assertEquals(13, record.size());
        assertEquals("1",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("Z",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        // データ行（データ区分2）の読み込み
        record = formatter.setInputStream(dataStream).readRecord();
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(8, record.size());
        assertEquals("3",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("データ部です。",     record.get("data")); // 8. 受取人名
        
        /**
         *  0バイトのデータを読み込むパターン。最初のカラムにデータ区分が存在する場合。
         */
        formatter = createMultiLayoutFormatter(enclose, fieldSeparator, recordSeparator, charset);
        
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /* 
        */
        
        dataStream = createInputStream(charset, testdata);

        assertNull(formatter.setInputStream(dataStream).initialize().readRecord());
        
        
        /**
         *  0バイトのデータを読み込むパターン。最初のカラムにデータ区分が存在しない場合。
         */
        formatter = createMultiLayoutFormatterWithEDI(enclose, fieldSeparator, recordSeparator, charset);
        
        testdata = "";
        
        dataStream = createInputStream(charset, testdata);

        assertNull(formatter.setInputStream(dataStream).initialize().readRecord());
        
    }
    
    /**
     * 不正なマルチレイアウトのテスト。
     */
    @Test
    public void testInvalidMultiFormat() throws Exception{

        String charset = "UTF-8";
        String recordSeparator = "\n";
        
        /**
         *  レコード区分の値が一致しないパターン。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "UTF-8" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X   # データ区分
                             #    1: ヘッダー、2: データレコード
                             #    8: トレーラー、9: エンドレコード  

        [TestHeaderRecord1]  # ヘッダーレコード1
          dataKbn = "1"
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
        16  test2         X             # test2
        17  test3         X             # test3             
                   
        ************************************************************/
        formatFile.deleteOnExit();
        formatter = createFormatter("./format.dat");
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "3","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","テスト1","テスト2","テスト3"
        */
        
        final InputStream dataStream = createInputStream(charset, testdata);


        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                  "an applicable record type was not found. record=[{dataKbn=3}]. "));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("format.dat"));
        } finally {
            formatter.close();
            formatFile.delete();
        }

        
        /**
         *  Classifierの開始位置が不正なパターン。
         */
        formatFile = Hereis.file("./format.dat");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        
        [Classifier]
        100  Price X     # 不正な開始位置
        
        [Header]
          Price = "価格(税込)"
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
        formatter = createFormatter("./format.dat");

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        
        // 開始位置に合致するフィールドのないレコードの読み込み
        try {
            formatter.setInputStream(source).initialize().readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("invalid field position was defined by Classifier. Field position must be less than [4].", e.getMessage());
        }
    }
    
    
    
    private InputStream createInputStream(String testFileCharset, String testdata)
            throws IOException, FileNotFoundException, UnsupportedEncodingException {
        File dataFile = new File("./input.csv");
        dataFile.deleteOnExit();
        new FileOutputStream(dataFile, false).write(testdata.getBytes(testFileCharset));
        InputStream source = new FileInputStream(dataFile);
        return source;
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

        [TestTitleRecord]  # ヘッダーレコード
          dataKbn = "1"
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
    
    private DataRecordFormatter createMultiLayoutFormatterWithEDI(String qt,
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
        14   withEdi   X   # データ区分
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
          dataKbn = "3"
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

    private InputStream source = null;
    private OutputStream dest = null;


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
     * ①requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するレコードタイプで1行目のレコードを読み込めること。
     * ②requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するレコードタイプで2行目以降のレコードが読み込まれないこと。
     * ③requires-titleディレクティブをfalseに設定した場合に、requires-titleディレクティブに対応するレコードタイプでないレコードタイプを使用してタイトル行を読み込めること。
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
        quoting-delimiter: "\""  # ダブルクォートを使用
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

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        区分,タイトル,出版社,著者
        1,Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb
        1,Programming with POSIX Threads,Addison-Wesley,David R. Butenhof
        2,2
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        DataRecord record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("区分", record.get("Kubun"));
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Programming with POSIX Threads", record.get("Title"));
        assertEquals("Addison-Wesley", record.get("Publisher"));
        assertEquals("David R. Butenhof", record.get("Authors"));
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("TrailerRecord", record.getRecordType());
        assertEquals("2", record.get("Kubun"));
        assertEquals("2", record.get("RecordNum"));
        
        // 終了
        assertNull(formatter.readRecord());

        formatter.close();
        formatFile.delete();
        
        
        /*
         * 以下、観点③
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: false  # 最初の行をタイトルとして読み書きしない!!
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [TitleRecord]
          Kubun = "区分"
        1   Kubun      N  "区分"
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

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        record = formatter.readRecord();
        assertEquals("TitleRecord", record.getRecordType());
        assertEquals("区分", record.get("Kubun"));
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Programming with POSIX Threads", record.get("Title"));
        assertEquals("Addison-Wesley", record.get("Publisher"));
        assertEquals("David R. Butenhof", record.get("Authors"));
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("TrailerRecord", record.getRecordType());
        assertEquals("2", record.get("Kubun"));
        assertEquals("2", record.get("RecordNum"));
        
        // 終了
        assertNull(formatter.readRecord());
    }
    
    /**
     * マルチレイアウトかつrequires-titleディレクティブがtrueかつTitleRecordが条件式を持つ場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①読み込んだタイトル行が条件式を満たさない場合、例外がスローされること。
     * ②読み込んだタイトル行が条件式を満たす場合、タイトル行を読み込めること。
     * ③最初の行以降にタイトル行の条件にマッチする行が現れた場合、例外がスローされること。
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
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [Title]
          Kubun = "0"    # マッチしない区分!!
        1   Kubun      N  
        2   Title      N 
        3   Publisher  N  
        4   Authors    N  
        
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

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        区分,タイトル,出版社,著者
        1,Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb
        1,Programming with POSIX Threads,Addison-Wesley,David R. Butenhof
        2,2
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        try {
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("this record could not be applied to the record type. record type=[Title]"));
            assertThat(e.getMessage(), containsString("following conditions must be met: [Kubun = [0]]."));
            assertThat(e.getMessage(), containsString("record number=[1]."));
        }
        
        formatter.close();
        formatFile.delete();
        
        
        /*
         * 以下、観点②
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [Title]
          Kubun = "区分"  # マッチする区分!!
        1   Kubun      N  
        2   Title      N 
        3   Publisher  N  
        4   Authors    N  
        
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

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        DataRecord record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("区分", record.get("Kubun"));
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Programming with POSIX Threads", record.get("Title"));
        assertEquals("Addison-Wesley", record.get("Publisher"));
        assertEquals("David R. Butenhof", record.get("Authors"));
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("TrailerRecord", record.getRecordType());
        assertEquals("2", record.get("Kubun"));
        assertEquals("2", record.get("RecordNum"));
        
        // 終了
        assertNull(formatter.readRecord());

        formatter.close();
        formatFile.delete();
        
        /*
         * 以下、観点③
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true  # 最初の行をタイトルとして読み書きする
        
        [Classifier]
        1  Kubun X     # 第1カラムを見て判定する。
        
        [Title]
          Kubun = "区分"  # マッチする区分!!
        1   Kubun      N  
        2   Title      N 
        3   Publisher  N  
        4   Authors    N  
        
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
        
        data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        区分,タイトル,出版社,著者
        1,Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb
        区分,タイトル2,出版社2,著者2
        2,2
        **********************************************************************/
        

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("区分", record.get("Kubun"));
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("DataRecord", record.getRecordType());
        assertEquals("1", record.get("Kubun"));
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        
        // データ行（最初の行以降にタイトル行の条件にマッチするレコードが存在!!）
        try {
            record =  formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("title record occurred after the first line. When directive 'requires-title' is true, can not apply the record type 'Title' to after the first line. record type=[Title]. record=[{Kubun=区分}], conditionToApply=[[Kubun = [区分]]]."));
            assertThat(e.getMessage(), containsString("record number=[3]."));
        }
    }
    
}
