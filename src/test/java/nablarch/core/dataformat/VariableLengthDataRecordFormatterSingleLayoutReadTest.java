package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.FileUtil;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 可変長ファイルフォーマッタのシングルレイアウトのテストケース。
 * 
 * 観点：
 * シングルレイアウトの可変長ファイルが、レイアウト定義ファイルの内容に伴って正しく読み書きできるかのテストを行う。
 * 可変長ファイル関連のディレクティブの妥当性検証、ディレクティブの設定（囲み文字、改行コード、エンコード、区切り文字）に応じて
 * 正しくファイルが読み込めること、また、このクラスが担う異常系のテストを網羅する。
 * また、各種可変長ファイルのフォーマット（0バイト、改行のみ、空行のみ、末尾に改行なしなど）についての読み込みテストを行う。
 * 
 * @author Masato Inoue
 */

public class VariableLengthDataRecordFormatterSingleLayoutReadTest {
    
    private String LS = Builder.LS;

    private DataRecordFormatter formatter = null;
    
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
    
    private File formatFile = null;
    private InputStream source = null;
    private OutputStream dest = null;

    /**
     * 典型的なキャラクタストリームフォーマットの使用例。
     * 単純なカンマ区切り(CSV)ファイルフォーマットのテスト。
     * レコード番号がインクリメントされることも確認する。
     */
    @Test
    public void testSimpleCSVFFormat() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:    "ms932"
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        String data = Hereis.string().replace(LS, "\r\n");
        /*********************************************
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        
        // よみこみ
        
        DataRecord record = formatter.readRecord();
        assertEquals(1, formatter.getRecordNumber()); // レコード番号が「1」
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        assertTrue(formatter.hasNext());
        
        record =  formatter.readRecord();
        assertEquals(2, formatter.getRecordNumber()); // レコード番号が「2」
        assertEquals("Books", record.getRecordType());
        assertEquals("Programming with POSIX Threads", record.get("Title"));
        assertEquals("Addison-Wesley", record.get("Publisher"));
        assertEquals("David R. Butenhof", record.get("Authors"));
        assertEquals("29.00", record.getBigDecimal("Price").toString());
        assertTrue(formatter.hasNext());

        record = formatter.readRecord();
        assertEquals(3, formatter.getRecordNumber()); // レコード番号が「3」
        assertEquals("Books", record.getRecordType());
        assertEquals("HACKING (2nd ed)", record.get("Title"));
        assertEquals("no starch press", record.get("Publisher"));
        assertEquals("Jon Erickson", record.get("Authors"));
        assertEquals("35.20", record.getBigDecimal("Price").toString());

        assertFalse(formatter.hasNext());
        assertNull(formatter.readRecord());


        // かきこみ
        
        File destfile = new File("./test.out");
        destfile.createNewFile();
        destfile.deleteOnExit();
        
        dest = new FileOutputStream(destfile);
        formatter = createWriteFormatter(new File("./test.fmt"), dest);
        
        record = new DataRecord();
        record.put("Title",     "Programming Ruby 2nd Edition");
        record.put("Publisher", "Pragmatic Bookshelf");
        record.put("Authors",   "Dave Thomas");
        record.put("Price",     "44.95");
        
        formatter.writeRecord(record);
        
        record = new DataRecord();
        record.put("Title",     "Programming Ruby 3rd Edition");
        record.put("Publisher", "Pragmatic Bookshelf");
        record.put("Authors",   "Dave Thomas");
        record.put("Price",     "54.95");

        formatter.writeRecord(record);
        dest.flush();
        dest.close();
        
        
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
             new FileInputStream(destfile), "ms932"
        ));
        
        assertEquals(
            "Programming Ruby 2nd Edition,Pragmatic Bookshelf,Dave Thomas,44.95"
          , reader.readLine()
        );
        assertEquals(
            "Programming Ruby 3rd Edition,Pragmatic Bookshelf,Dave Thomas,54.95"
          , reader.readLine()
        );
        assertNull(reader.readLine());
    }
    
    
    /**
     * タブ区切り(TSV)でクォートあり。
     */
    @Test
    public void testTSVUsingQuotingFormat() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\r\n"  # CRLFで改行
        field-separator:   "\t"    # タブ区切り
        quoting-delimiter: "'"     # シングルクォートを使用
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X  Number  # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        String data = Hereis.string().replace(LS, "\r\n").replace("\\t", "\t");
        /*********************************************
        Learning the vi and vim Editors\tOReilly\t'Robbins\tHanneah\tLamb'\t38.50
        'Programming with
        POSIX Threads'\tAddison-Wesley\tDavid R. Butenhof\t29.00
        'HACKIN'' (2nd ed)'\tno' starch press\tJon Erickson\t35.20
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));

        formatter = createReadFormatter(new File("./test.fmt"), source);
        
        // よみこみ
        
        DataRecord record = formatter.readRecord();
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        // タブがあってもOK
        assertEquals("Robbins\tHanneah\tLamb", record.get("Authors"));

        
        record = formatter.readRecord();
        
        // 改行があってもOK
        assertEquals("Programming with" + "\r\n" + "POSIX Threads", record.get("Title"));
        
        record = formatter.readRecord();
        
        // クォート文字を重ねることでエスケープできる。
        assertEquals("HACKIN' (2nd ed)", record.get("Title"));
        // クォートされていないフィールドの中では、クォート文字は通常文字として扱われる。
        assertEquals("no' starch press", record.get("Publisher"));
        
        assertNull(formatter.readRecord());
        
        
        // かきこみ
        
        File destfile = new File("./test.out");
        destfile.createNewFile();
        destfile.deleteOnExit();
        
        dest = new FileOutputStream(destfile);

        formatter = createWriteFormatter(formatFile, dest);
        
        record = new DataRecord();
        record.put("Title",     "Programmin' Ruby 2nd Edition");
        record.put("Publisher", "Pragmatic\r\nBookshelf");
        record.put("Authors",   "Dave Thomas");
        record.put("Price",     "44.95");

        formatter.writeRecord(record);
        
        record = new DataRecord();
        record.put("Title",     "'Programmin'' Ruby 3rd Edition'");
        record.put("Publisher", "'Pragmatic\r\nBookshelf'");
        record.put("Authors",   "Dave Thomas");
        record.put("Price",     "54.95");

        formatter.writeRecord(record);
        dest.flush();
        dest.close();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
             new FileInputStream(destfile), "ms932"
        ));
        
        // 出力時には、全てのフィールドが強制的にクォートされる。
        assertEquals("'Programmin'' Ruby 2nd Edition'\t'Pragmatic", reader.readLine());
        assertEquals("Bookshelf'\t'Dave Thomas'\t'44.95'", reader.readLine());
        // というわけなので、レコードの値の中でクォートを意識した値を設定してしまうと、
        // まずいことになる。
        assertEquals("'''Programmin'''' Ruby 3rd Edition'''\t'''Pragmatic", reader.readLine());
        assertEquals("Bookshelf'''\t'Dave Thomas'\t'54.95'", reader.readLine());
        assertNull(reader.readLine());
    }
    
    /**
     * 囲み文字を使用せず、読み込む場合のテスト。（この場合、ダブルクォートも囲み文字と認識せずに読み込む）
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： 設定しない
     * 区切り文字 ： ,
     */
    @Test
    public void testNotSetEnclose() throws Exception {

        String charset = "MS932";
        String fieldSeparator = ",";
        String recordSeparator = "\n";

        formatter = createFormatterNotSetEnclose(
            fieldSeparator, recordSeparator, charset
        );
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */

        InputStream dataStream = createInputStream(charset, testdata);
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord(); // 1行目の読み込み
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("\"2\"",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("\"1234\"",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("\"FSEｷﾞﾝｺｳ\"",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("\"ﾏｺ1\"",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("\"ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ\"", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("\"1\"",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("\"7778888\"",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("\"ジャックスパロウ\"",     record.get("recipientName")); // 8. 受取人名
        assertEquals("\"3020\"",         record.get("amount"));        // 9. 振込金額
        assertEquals("\"N\"",            record.get("isNew"));         // 10.新規コード
        assertEquals("\"ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ\"", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("\"4\"",            record.get("transferType"));  // 12.振込区分
        assertEquals("\"Y\"",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
    }
    

    /**
     * 空行の存在を許すフォーマットのテスト。
     * また、空行についてもレコード番号がインクリメントされることを確認する。
     */
    @Test
    public void testFormatThatIgnoresBlankLine() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:   "\r\n" # CRLFで改行
        field-separator:    ","    # カンマ区切り
        ignore-blank-lines: true   # 空行よみとばし
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        String data = Hereis.string().replace(LS, "\r\n");
        /*********************************************
        
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        
        
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));

        formatter = createReadFormatter(formatFile, source);
        
        // よみこみ
        
        DataRecord record = formatter.setInputStream(source).readRecord();
        assertEquals(2, formatter.getRecordNumber()); // レコード番号が「2」
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        record =  formatter.readRecord();
        assertEquals(4, formatter.getRecordNumber()); // レコード番号が「4」
        assertEquals("Books", record.getRecordType());
        assertEquals("Programming with POSIX Threads", record.get("Title"));
        assertEquals("Addison-Wesley", record.get("Publisher"));
        assertEquals("David R. Butenhof", record.get("Authors"));
        assertEquals("29.00", record.getBigDecimal("Price").toString());
        record = formatter.readRecord();
        assertEquals(5, formatter.getRecordNumber()); // レコード番号が「5」
        assertEquals("Books", record.getRecordType());
        assertEquals("HACKING (2nd ed)", record.get("Title"));
        assertEquals("no starch press", record.get("Publisher"));
        assertEquals("Jon Erickson", record.get("Authors"));
        assertEquals("35.20", record.getBigDecimal("Price").toString());
        
        assertNull(formatter.setInputStream(source).readRecord());


        // 空行読み飛ばしオプションをはずして同じことをすると
        // フォーマットエラーになる。
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:      "ms932"
        record-separator:   "\r\n" # CRLFで改行
        field-separator:    ","    # カンマ区切り
        #ignore-blank-lines: true   # 空行よみとばし
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        data = Hereis.string().replace(LS, "\r\n");
        /*********************************************
        
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        
        
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));

        formatter = createReadFormatter(formatFile, source);
        
        try {
            record = formatter.setInputStream(source).readRecord();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    /**
     * フィールドが空の場合のテスト。
     */
    @Test
    public void testColumnEmpty() throws Exception {

        String encoding = "UTF-8";
        String qt = "\\\"";
        String fs = ",";
        String rs = "\n";
        
        
        File formatFile = Hereis.file("./format.dat", qt, fs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [TestDataRecord]
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
        15  used          X             # (未使用領域)               
        ************************************************************/
        formatFile.deleteOnExit();
        formatter = createFormatter("./format.dat");
        
        
        /**
         *  末尾がダブルクォート+改行コードのパターン 。
         */
        String testdata = Hereis.string().replaceAll(LS, rs);
        /*
        ,"",,"","","","",,,"","","","","",""
        */
        
        InputStream dataStream = createInputStream(encoding, testdata);
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord();
                
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

        assertEquals(null,             record.get("dataKbn"));      // 1. データ区分
        assertEquals(null,          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals(null,      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals(null,           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals(null, record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals(null,            record.get("syumoku"));       // 6. 預金種目
        assertEquals(null,      record.get("accountNum"));    // 7. 口座番号
        assertEquals(null,     record.get("recipientName")); // 8. 受取人名
        assertEquals(null,         record.get("amount"));        // 9. 振込金額
        assertEquals(null,            record.get("isNew"));         // 10.新規コード
        assertEquals(null, record.get("ediInfo"));       // 11.EDI情報
        assertEquals(null,            record.get("transferType"));  // 12.振込区分
        assertEquals(null,            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertEquals(null,            record.get("used"));       // 14.used
        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
        

        /**
         *  末尾がフィールドのみのパターン 。
         */
        formatter = createFormatter("./format.dat");
        testdata = Hereis.string().replaceAll(LS, rs);
        /*
        ,"",,"","","","",,,"","","","","",
        */
        
        dataStream = createInputStream(encoding, testdata);
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

        assertEquals(null,             record.get("dataKbn"));      // 1. データ区分
        assertEquals(null,          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals(null,      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals(null,           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals(null, record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals(null,            record.get("syumoku"));       // 6. 預金種目
        assertEquals(null,      record.get("accountNum"));    // 7. 口座番号
        assertEquals(null,     record.get("recipientName")); // 8. 受取人名
        assertEquals(null,         record.get("amount"));        // 9. 振込金額
        assertEquals(null,            record.get("isNew"));         // 10.新規コード
        assertEquals(null, record.get("ediInfo"));       // 11.EDI情報
        assertEquals(null,            record.get("transferType"));  // 12.振込区分
        assertEquals(null,            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertEquals(null,            record.get("used"));       // 14.used
        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
    }
    
    /**
     * フィールドに改行を含むパターン。
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testInnerRecordSeparator() throws Exception {

        String charset = "MS932";
        String enclose = "\\\"";
        String recordSeparator = "\n";
        String fieldSeparator = ",";

        /**
         *  ダブルクォートで囲まれたトークン内に改行があるパターン。
         */
        formatter = createFormatter(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","12
        34","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞ
        ｺｶﾞﾈ
        ﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */

        InputStream dataStream = createInputStream(charset, testdata);

        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord(); // 1行目の読み込み
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",                record.get("dataKbn"));      // 1. データ区分
        assertEquals("12" + recordSeparator + "34",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞ\nｺｶﾞﾈ\nﾊﾗｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
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
        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
        

        /**
         *  ダブルクォートで囲まれていないトークン内に改行があるパターン。
         */
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2",12
        34,FSEｷﾞﾝｺｳ,"ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        dataStream = createInputStream(charset, testdata);
        try {
            record = formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [2]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }
    }
    

    /**
     * フィールド内にダブルクォート。
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testInnerDoubleQuote() throws Exception {

        String charset = "MS932";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        String recordSeparator = "\n";

        /**
         *  フィールド内に正常な連続ダブルクォート。トークンの先頭、トークンの末尾、トークンの途中、トークンの途中に２つ、４つのダブルクォートが連続で続くパターン。
         */
        formatter = createFormatter(
            enclose, fieldSeparator, recordSeparator, charset
        );
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        """2",1234,"FSEｷﾞﾝｺｳ""","ﾏｺ""1","ﾏﾂﾄﾞ""ｺｶﾞﾈﾊﾗ""ｼﾃﾝ","xxxxxx","1""""","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */

        InputStream dataStream = createInputStream(charset, testdata);
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord(); // 1行目の読み込み
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("\"2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("1234",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ\"",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ\"1",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞ\"ｺｶﾞﾈﾊﾗ\"ｼﾃﾝ", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("1\"\"",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("7778888",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ",     record.get("recipientName")); // 8. 受取人名
        assertEquals("3020",         record.get("amount"));        // 9. 振込金額
        assertEquals("N",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("4",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
        
        

        /**
         *  トークンにダブルクォートが１つあるパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        ""2",1234,"FSEｷﾞﾝｺｳ""","ﾏｺ""1","ﾏﾂﾄﾞ""ｺｶﾞﾈﾊﾗ""ｼﾃﾝ","xxxxxx","1""""","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */        

        dataStream = createInputStream(charset, testdata);
        try {
            record = formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "the field value was delimited by a wrong separator. : 2."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }


        /**
         * トークンにダブルクォートが３つ続くパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        """"2",1234,"FSEｷﾞﾝｺｳ""","ﾏｺ""1","ﾏﾂﾄﾞ""ｺｶﾞﾈﾊﾗ""ｼﾃﾝ","xxxxxx","1""""","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */        

        dataStream = createInputStream(charset, testdata);
        try{
            record = formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        }catch(Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
            assertTrue(e.getMessage().contains(
                    "the field value was delimited by a wrong separator."
            ));
        }

    }
    
    /**
     * 様々なフォーマットのファイルの読み込み。（不正なファイルフォーマットも多数含む）
     * 改行コード   ： LF
     * 文字コード   ： UTF-8
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testIllegalFileFormat() throws Exception {

        String charset = "UTF-8";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        String recordSeparator = "\n";
        
        
        /**
         *  一つ目のフィールドが不正。ダブルクォートの次にフィールドがないパターン。
         */
        formatter = createFormatter(
            enclose, fieldSeparator, recordSeparator, charset
        );
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2,"1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */           
        InputStream dataStream = createInputStream(charset, testdata);
        try{
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        }catch(InvalidDataFormatException e) {
            // "2,"が第1フィールドとして認識されてしまう。
            assertThat(e.getMessage(), startsWith("the field value was delimited by a wrong separator. : 1. "));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        /**
         *  フィールドの数がレイアウト定義された項目数より１つ少ないパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y"
        */        

        dataStream = createInputStream(charset, testdata);
        try{
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        }catch(InvalidDataFormatException e) {
            
            assertThat(e.getMessage(), startsWith("number of input fields was invalid. number of fields must be [15], but number of input fields was [14]. "));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        /**
         *  フィールドの数がレイアウト定義された項目数より２つ少ないパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4"
        */        

        dataStream = createInputStream(charset, testdata);
        try{
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        }catch(InvalidDataFormatException e) {

            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [13]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        /**
         *  フィールドの数がレイアウト定義された項目数より１つ多いパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX","error"
        */        

        dataStream = createInputStream(charset, testdata);
        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [16]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        
        /**
         *  二つ目のフィールドが不正。ダブルクォートの次にフィールドがないパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234,"FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */        

        dataStream = createInputStream(charset, testdata);
        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "the field value was delimited by a wrong separator. : F."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        
        /**
         *  最後のフィールドが不正。ダブルクォートが閉じられてないパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX
        */        

        dataStream = createInputStream(charset, testdata);
        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith("Unclosed quotation."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }       
        
        /**
         *  二行目のフィールドが不正なパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2"error,"xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */        

        dataStream = createInputStream(charset, testdata);
        formatter.setInputStream(dataStream).initialize().readRecord();
        try {
            formatter.readRecord();
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "the field value was delimited by a wrong separator. : e."));
            assertThat(e.getRecordNumber(), is(2));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }     
        
        /**
         *  ダブルクォートで囲まれない文字の形式が不正なパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2",1234"FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        */        

        dataStream = createInputStream(charset, testdata);
        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [14]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }
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
    public void testReadLF() throws Exception {

        String charset = "UTF-8";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        String recordSeparator = "\n";
        
        doTest(charset, enclose, fieldSeparator, recordSeparator);
    }
    
    /**
     * 文字コードMS932のテスト。
     * 
     * 改行コード   ： LF
     * 文字コード   ： MS932
     * 囲み文字     ： "
     * 区切り文字 ： ,
     */
    @Test
    public void testReadMS932() throws Exception {

        String charset = "MS932";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        String recordSeparator = "\n";

        doTest(charset, enclose, fieldSeparator, recordSeparator);
    }
    
    
    /**
     * 囲み文字をシングルクォートにした場合のテスト。
     * 
     * 改行コード   ： LF
     * 文字コード   ： MS932
     * 囲み文字     ： '
     * 区切り文字 ： ,
     */
    @Test
    public void testReadEncloseSingleQuote() throws Exception {

        String charset = "MS932";
        String enclose = "'";
        String fieldSeparator = ",";
        String recordSeparator = "\n";

        /**
         *  3件、末尾に改行ありのパターン 。
         */
        formatter = createFormatter(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        '2',1234,'FSEｷﾞﾝｺｳ','ﾏｺ1','ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ','xxxxxx','1','7778888','ジャックスパロウ','3020','N','ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ','4','Y','XXXXXX'
        '2',12342,'FSEｷﾞﾝｺｳ2','ﾏｺ12','ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2','xxxxxx2','12','77788882','ジャックスパロウ2','30202','N2','ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2','42','Y2','XXXXXX2'
        '2',12343,'FSEｷﾞﾝｺｳ3','ﾏｺ13','ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3','xxxxxx3','13','77788883','ジャックスパロウ3','30203','N3','ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3','43','Y3','XXXXXX3'
        */
        
        InputStream dataStream = createInputStream(charset, testdata);
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord();
        
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

        record = formatter.setInputStream(dataStream).readRecord(); // 二行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12342",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ2",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ12",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("12",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788882",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ2",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30202",         record.get("amount"));        // 9. 振込金額
        assertEquals("N2",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("42",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y2",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        record = formatter.setInputStream(dataStream).readRecord(); // 三行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12343",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ3",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ13",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("13",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788883",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ3",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30203",         record.get("amount"));        // 9. 振込金額
        assertEquals("N3",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("43",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y3",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        
        
        record = formatter.setInputStream(dataStream).readRecord();
        assertNull(record);
    }

    
    
    /**
     * データレコードの読み込み。
     */
    public void doTest(String charset, String enclose, String fieldSeparator, String recordSeparator) throws Exception {


        /**
         *  １件、末尾に改行ありのパターン 。
         */
        formatter = createFormatterUseLastField(enclose, fieldSeparator, recordSeparator, charset);
        String testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y",xxxxx
        */
        
        InputStream dataStream = createInputStream(charset, testdata);
        DataRecord record = formatter.setInputStream(dataStream).initialize().readRecord();
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

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
        assertEquals("xxxxx",            record.get("used"));       // 13.EDI情報使用フラグ
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        

        /**
         *  １件、末尾に改行なし、かつ末尾が空文字列かつ、最後のフィールドが囲み文字で囲まれていないパターン 。
         */
        formatter = createFormatterUseLastField(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y",*/
        
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

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
        assertEquals(null,            record.get("used"));       // 14. 空文字列が正常に取得できることを確認!!!
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        

        /**
         *  １件、末尾に改行なし、かつ末尾が囲み文字で囲まれた空文字列のパターン。
         */
        formatter = createFormatterUseLastField(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y",""*/
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

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
        assertEquals(null,            record.get("used"));       // 14.空文字列が正常に取得できることを確認!!!
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        
        
        /**
         *  １件、末尾に改行なし、かつ最後のフィールドが囲み文字で囲まれていないパターン。
         */
        formatter = createFormatterUseLastField(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y",xxxxx*/
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        

        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(14, record.size());

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
        assertEquals("xxxxx",            record.get("used"));       // 13.EDI情報使用フラグ
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        
        
        
        
        /**
         *  １件、末尾に改行なし、かつ最後のフィールドが囲み文字で囲まれているパターン。
         */
        formatter = createFormatter(
                enclose, fieldSeparator, recordSeparator, charset
        );
        
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","xxxxx"*/
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        
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
        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);

        
        /**
         *  １件、最後が空行のパターン 。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2","1234","FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        
        */
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        try {
            record = formatter.readRecord();
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [1]."));
            assertThat(e.getRecordNumber(), is(2));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        assertNull(record);
        
        
        /**
         *  0件のパターン 。 
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        */
        
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord();
        
        assertNull(record);
        
        /**
         *  空白のみの行のパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
                        
        */
        
        dataStream = createInputStream(charset, testdata);
        try{
            record = formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [1]. "));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }
        

        /**
         *  フォーマットが不正なレコードのパターン。 
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
                   ああああああああああああ
        */

        dataStream = createInputStream(charset, testdata);
        try {
            record = formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [1]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }

        /**
         *  改行のみの行のパターン。
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        
        
        
        */
        
        dataStream = createInputStream(charset, testdata);
        try {
            formatter.setInputStream(dataStream).initialize().readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "number of input fields was invalid. number of fields must be [15], " +
                            "but number of input fields was [1]."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), endsWith("format.dat"));
        }
        
        
        /**
         *  3件、末尾に改行ありのパターン。（ダブルクォートで囲まれたフィールドとそうでないフィールドが混在）
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        "2",1234,"FSEｷﾞﾝｺｳ","ﾏｺ1","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ","xxxxxx","1","7778888","ジャックスパロウ","3020","N","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ","4","Y","XXXXXX"
        "2",12342,"FSEｷﾞﾝｺｳ2","ﾏｺ12","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2","xxxxxx2","12","77788882","ジャックスパロウ2","30202","N2","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2","42","Y2","XXXXXX2"
        "2",12343,"FSEｷﾞﾝｺｳ3","ﾏｺ13","ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3","xxxxxx3","13","77788883","ジャックスパロウ3","30203","N3","ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3","43","Y3","XXXXXX3"
        */
        
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord(); // 1行目の読み込み
        
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

        record = formatter.readRecord(); // 二行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12342",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ2",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ12",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("12",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788882",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ2",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30202",         record.get("amount"));        // 9. 振込金額
        assertEquals("N2",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("42",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y2",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        record = formatter.readRecord(); // 三行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12343",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ3",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ13",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("13",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788883",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ3",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30203",         record.get("amount"));        // 9. 振込金額
        assertEquals("N3",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("43",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y3",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        
        
        record = formatter.readRecord();
        assertNull(record);
        
        
        /**
         *  3件、末尾に改行あり、囲み文字なしのパターン
         */
        formatter = createFormatter(enclose, fieldSeparator, recordSeparator, charset);
        testdata = Hereis.string().replaceAll(LS, recordSeparator);
        /*
        2,1234,FSEｷﾞﾝｺｳ,ﾏｺ1,ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ,xxxxxx,1,7778888,ジャックスパロウ,3020,N,ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ,4,Y,XXXXXX
        2,12342,FSEｷﾞﾝｺｳ2,ﾏｺ12,ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2,xxxxxx2,12,77788882,ジャックスパロウ2,30202,N2,ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2,42,Y2,XXXXXX2
        2,12343,FSEｷﾞﾝｺｳ3,ﾏｺ13,ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3,xxxxxx3,13,77788883,ジャックスパロウ3,30203,N3,ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3,43,Y3,XXXXXX3
        */
        
        dataStream = createInputStream(charset, testdata);
        record = formatter.setInputStream(dataStream).initialize().readRecord(); // 1行目の読み込み
        
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

        record = formatter.readRecord(); // 二行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());

        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12342",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ2",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ12",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ2", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("12",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788882",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ2",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30202",         record.get("amount"));        // 9. 振込金額
        assertEquals("N2",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ2", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("42",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y2",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)

        record = formatter.readRecord(); // 三行目の読み込み
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(13, record.size());
        
        assertEquals("2",             record.get("dataKbn"));      // 1. データ区分
        assertEquals("12343",          record.get("FIcode"));       // 2. 振込先金融機関コード
        assertEquals("FSEｷﾞﾝｺｳ3",      record.get("FIname"));       // 3. 振込先金融機関名称
        assertEquals("ﾏｺ13",           record.get("officeCode"));   // 4. 振込先営業所コード
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ3", record.get("officeName"));   // 5. 振込先営業所名
        assertTrue  (!record.containsKey("tegataNum"));            // (手形交換所番号)
        assertEquals("13",            record.get("syumoku"));       // 6. 預金種目
        assertEquals("77788883",      record.get("accountNum"));    // 7. 口座番号
        assertEquals("ジャックスパロウ3",     record.get("recipientName")); // 8. 受取人名
        assertEquals("30203",         record.get("amount"));        // 9. 振込金額
        assertEquals("N3",            record.get("isNew"));         // 10.新規コード
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ3", record.get("ediInfo"));       // 11.EDI情報
        assertEquals("43",            record.get("transferType"));  // 12.振込区分
        assertEquals("Y3",            record.get("withEdi"));       // 13.EDI情報使用フラグ
        assertTrue  (!record.containsKey("unused"));               // (未使用領域)
        
        record = formatter.readRecord();
        assertNull(record);
    }

    /**
     * hasNextが正常動作することの確認。
     */
    @Test
    public void testHasNext() throws Exception{

        /**
         * ストリームが設定されていない場合にfalseが返却される。
         */
        assertFalse(new VariableLengthDataRecordFormatter().hasNext());
        
        /**
         * ストリームが設定されているが、初期化が行われていない場合にfalseが返却される。
         */
        assertFalse(new VariableLengthDataRecordFormatter().setInputStream(new BufferedInputStream(new ByteArrayInputStream("".getBytes()))).hasNext());

        /**
         * ストリームが設定されており、データが存在する場合はtrueが返却される。
         */
        assertTrue(new VariableLengthDataRecordFormatter().setInputStream(new BufferedInputStream(new ByteArrayInputStream(" ".getBytes()))).hasNext());

    }


    /**
     * 行末の改行コードが無視されることの確認テスト。
     */
    @Test
    public void testIgnoreBlank() throws Exception{

        /*
         * 行末の改行コードについて、hasNextがfalseを返却し、かつ行末の改行コードについてレコード番号がインクリメントされないことを確認するテスト。
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:    "ms932"
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        ignore-blank-lines: true        # 改行をスキップする
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        String data = Hereis.string().replace(LS, "\r\n");
        /*********************************************
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        
        
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());
        assertEquals(1, formatter.readRecord().getRecordNumber()); // 1行目を読み込む
        assertTrue(formatter.hasNext());
        assertEquals(3, formatter.readRecord().getRecordNumber()); // 3行目を読み込む
        assertFalse(formatter.hasNext());
        assertNull(formatter.readRecord());
        assertEquals(3, formatter.getRecordNumber()); // レコード番号が3であることを確認する
        
        
        /*
         * hasNextを使用せず、readRecordメソッドを使用した場合でも、行末の改行コードについてreadRecordメソッドがnullを返却し、かつ行末の改行コードについてレコード番号がインクリメントされないことを確認するテスト。
         */
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertEquals(1, formatter.readRecord().getRecordNumber()); // 1行目を読み込む
        assertEquals(3, formatter.readRecord().getRecordNumber()); // 3行目を読み込む
        assertNull(formatter.readRecord());
        assertEquals(3, formatter.getRecordNumber()); // レコード番号が3であることを確認する
        assertNull(formatter.readRecord());
        assertEquals(3, formatter.getRecordNumber()); // レコード番号が3であることを確認する
    }


    /**
     * Definitionがnullの場合のテスト。
     */
    @Test
    public void testDefinitionNull() throws Exception {

        VariableLengthDataRecordFormatter formatter = new VariableLengthDataRecordFormatter();
        try{
            formatter.readRecord();
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        

        formatter = new VariableLengthDataRecordFormatter();
        try{
            formatter.writeRecord(new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
    }
    /**
     * ストリームが設定されていない状態で実行メソッドを呼んだ場合に例外がスローされるテスト。
     */
    @Test
    public void testStreamNull() throws Exception {

        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [Books]
         1   Title      X          # タイトル
         2   Publisher  X          # 出版社
         3   Authors    X          # 著者
         4   Price      X Number   # 価格
         *****************************************/
        formatFile.deleteOnExit();

        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./test.fmt"));
        formatter.initialize();
        try{
            formatter.readRecord();
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        formatter.close();
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./test.fmt"));
        formatter.initialize();
        try{
            formatter.writeRecord(new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        formatter.close();
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./test.fmt"));
        formatter.initialize();
        try{
            formatter.writeRecord("", new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        formatter.close();
        
    }
    
    
    
    
    /**
     * クローズのテスト。
     */
    @Test
    public void testClose() throws Exception{

        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }
        
        // 本テストで作成するファイル名の接頭辞定義
        final String FILE_NAME_PREFIX = "VariableLengthDataRecordFormatterSingleLayoutReadTest#testClose#";
        
        File layoutFile = Hereis.file("./" + FILE_NAME_PREFIX + "variableTest.fmt");
        /**********************************************
        file-type:    "Variable"
        text-encoding:    "utf8"
        record-separator: "\n" # LFで改行
        field-separator:  ","    # カンマ区切り
        quoting-delimiter: "\""     # シングルクォートを使用
        
        [Default]
        1    dataKbn       X  # 1. シングルバイト文字列
        2    wordString    N  # 2. ダブルバイト文字列
        3    account       X  # 3. シングルバイト文字列
        ***************************************************/
        layoutFile.deleteOnExit();
        
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
        .addBasePathSetting("format", "file:./")
        .addBasePathSetting("output", "file:./");
        
        
        /**
         * readRecordの場合に、inputStreamがクローズされることの確認。
         */
        String data = Hereis.string().replaceAll(LS, "\n");
        /*
        "1","あいうえお","10000"
        */
        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./" + FILE_NAME_PREFIX + "record.dat")));
        writer.write(data);
        writer.close();
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(layoutFile).setInputStream(new FileInputStream("./" + FILE_NAME_PREFIX + "record.dat")).initialize();

        DataRecord readRecord = formatter.initialize().readRecord();
        assertEquals("1", readRecord.get("dataKbn"));
        assertEquals("あいうえお", readRecord.get("wordString"));
        assertEquals("10000", readRecord.get("account"));
        assertTrue(true);  
        
        File dataFile = new File("./" + FILE_NAME_PREFIX + "record.dat");
        assertTrue(dataFile.exists()); 
        dataFile.delete();
        assertTrue(dataFile.exists()); // クローズされていないので削除できない
        
        formatter.close();

        dataFile.delete();
        assertFalse(dataFile.exists()); // クローズされているので削除できることの確認
        
        SystemRepository.clear();
        
        new File("./" + FILE_NAME_PREFIX + "record.dat").deleteOnExit();

        
        
        /**
         * writeRecordの場合に、outputStreamがクローズされることの確認。
         */
        FileOutputStream outputStream = new FileOutputStream("./" + FILE_NAME_PREFIX + "close.dat");
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(layoutFile).setOutputStream(outputStream).initialize();

        Map<String, Object> record = new DataRecord() {{
            put("dataKbn", "1");
            put("wordString", "あいうえお");
            put("account", "10000");
        }};

        formatter.writeRecord(record);
        
        dataFile = new File("./" + FILE_NAME_PREFIX + "close.dat");
        assertTrue(dataFile.exists()); 
        dataFile.delete();
        assertTrue(dataFile.exists()); // クローズされていないので削除できない
        
        formatter.close();

        dataFile.delete();
        assertFalse(dataFile.exists()); // クローズされているので削除できることの確認
        
        SystemRepository.clear();
        
        new File("./" + FILE_NAME_PREFIX + "record.dat").deleteOnExit();
    }

    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }
    
    private InputStream createInputStream(String testFileCharset,
            String testdata) throws IOException, FileNotFoundException,
            UnsupportedEncodingException {
        return new ByteArrayInputStream(testdata.getBytes(testFileCharset));
    }
    
    private DataRecordFormatter
    createFormatterNotSetEnclose(String fs, String rs, String encoding){
        
        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", fs, rs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"        # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字

        [TestDataRecord]
        1   dataKbn       X   "2"       # データ区分
        2   FIcode        X             # 振込先金融機関コード
        3   FIname        X             # 振込先金融機関名称
        4   officeCode    X             # 振込先営業所コード
        5   officeName    X             # 振込先営業所名
        6  ?tegataNum     X  "9999"     # (手形交換所番号:未使用)
        7   syumoku       X             # 預金種目
        8   accountNum    X             # 口座番号
        9   recipientName X             # 受取人名
        10  amount        X  "0"        # 振込金額
        11  isNew         X             # 新規コード
        12  ediInfo       N             # EDI情報
        13  transferType  X             # 振込区分
        14  withEdi       X  "Y"        # EDI情報使用フラグ
        15 ?unused        X             # (未使用領域)               
        ************************************************************/
        formatFile.deleteOnExit();
        LayoutDefinition definition = new LayoutFileParser("./format.dat").parse();
        return new VariableLengthDataRecordFormatter().setDefinition(definition);
    }

    private DataRecordFormatter
    createFormatterUseLastField(String qt, String fs, String rs, String encoding) {

        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [TestDataRecord]
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
        15  used          X             # 使用するフィールド             
        ************************************************************/
        formatFile.deleteOnExit();
        return createFormatter("./format.dat");
    }
    
    private DataRecordFormatter
    createFormatter(String qt, String fs, String rs, String encoding) {

        rs = rs.replace("\r", "\\r").replace("\n", "\\n");
        
        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"       # レコード区切り文字
        field-separator:   "$fs"       # フィールド区切り文字
        quoting-delimiter: "$qt"       # クオート文字

        [TestDataRecord]
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
    
    /**
     * ディレクティブの異常系テスト。
     */
    @Test
    public void testInvalidDirective() throws Exception{
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:    "ms932"
        record-separator: "\r\n" # CRLFで改行
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        String data = Hereis.string().replace(LS, "\r\n");
        /*********************************************
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        
        try {
            formatter = createReadFormatter(formatFile, source);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "directive 'field-separator' was not specified. " +
                            "directive 'field-separator' must be specified."));
            assertThat(e.getFilePath(), endsWith("test.fmt"));
        }
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
        FileUtil.closeQuietly(dest);
        FileUtil.closeQuietly(source);
        SystemRepository.clear();
    }
    

    /**
     * requires-titleディレクティブがtrueの場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するデフォルトのレコードタイプ[Title]で最初の行が読み込まれること。
     * ②requires-titleディレクティブをtrueに設定した場合に、requires-titleディレクティブに対応するデフォルトのレコードタイプ[Title]で2行目以降のレコードが読み込まれないこと。
     * ③title-record-type-nameディレクティブに設定したレコードタイプ[Hoge]で最初の行が読み込まれること。
     * ④requires-titleディレクティブをfalseに設定した場合に、requires-titleディレクティブに対応するレコードタイプでないレコードタイプを使用してタイトル行を読み込めること。
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

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        DataRecord record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
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
        title-record-type-name: "Hoge"  # タイトルのレコードタイプ名
        
        [Hoge]
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

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        record = formatter.readRecord();
        assertEquals("Hoge", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
        // 終了
        assertNull(formatter.readRecord());
        
        formatter.close();
        formatFile.delete();
        
        /*
         * 以下、観点④
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
        4  Price X     # 第4カラムを見て判定する。
                
        [TitleRecord]
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

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // ヘッダー行
        record = formatter.readRecord();
        assertEquals("TitleRecord", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // データ行 #1
        record = formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        assertEquals("Learning the vi and vim Editors", record.get("Title"));
        assertEquals("OReilly", record.get("Publisher"));
        assertEquals("Robbins Hanneah and Lamb", record.get("Authors"));
        assertEquals("38.50", record.getBigDecimal("Price").toString());
        
        // データ行 #2
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
        // データ行 #3
        record =  formatter.readRecord();
        assertEquals("Books", record.getRecordType());
        
        // 終了
        assertNull(formatter.readRecord());
    }
   
    /**
     * requires-titleディレクティブがtrueかつignore-blank-linesがtrue/falseの場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①1行目が空文字かつignore-blank-linesがtrueの場合、空のタイトル行を読み込むことができること。（定義されているフィールドが1つのみならば空行を読み込むことは可能）
     * ②1行目が空文字かつignore-blank-linesがfalseの場合、空行の後の行のタイトル行を読み込むことができること。（定義されているフィールドが1つのみならば空行を読み込むことは可能）
     */
    @Test
    public void testTitleIfBlank() throws Exception {
       
        /*
         * 以下、観点①
         */
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true  # 最初の行をタイトルとして読み書きする
        ignore-blank-lines: false # 空行を無視するかどうか
        
        [Title]
        1   Title      N  "空のタイトルも読み込める"
        
        [Books]
        1   Title      X          # タイトル
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
        
        assertTrue(formatter.hasNext());

        // 1行目の空行をタイトルとして読める(ただし、空文字はnull)
        DataRecord readRecord = formatter.readRecord();
        assertThat((String)readRecord.get("Title"), is(nullValue()));
        
        formatter.close();
        formatFile.delete();
        
        
        /*
         * 以下、観点②
         */
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true  # 最初の行をタイトルとして読み書きする
        ignore-blank-lines: true  # 空行を無視するかどうか
        
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

        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // 2行目のタイトル行を読める
        readRecord = formatter.readRecord();
        assertThat((String)readRecord.get("Title"), is("タイトル"));
        assertThat((String)readRecord.get("Publisher"), is("出版社"));
        assertThat((String)readRecord.get("Authors"), is("著者"));
        assertThat((String)readRecord.get("Price"), is("価格(税込)"));
        
    }
    

    /**
     * requires-titleディレクティブがtrueの場合の異常系テスト。
     * 
     * 以下の観点でテストを行う。
     * ①【異常系】最初の行のタイトルに紐付くレコードタイプがレイアウト定義ファイルで定義されていない場合、例外がスローされること。
     */
    @Test
    public void testInvalidTitle() throws Exception {
       
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

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        
        try {        
            formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString("record type 'Title' was not found. When directive 'requires-title' is true, must be specified record type 'Title'."));
        }

    }
    

    
    /**
     * 一行で読み込める文字列数の上限を超えた場合に例外がスローされることの確認テスト。
     * 
     * 以下の観点でテストを行う。
     * 【異常系】max-record-lengthで設定された文字列数の上限を超えた場合に例外がスローされること。
     */
    @Test
    public void testReadRecordSizeLimit() throws Exception {
       
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:    "Variable"
        text-encoding:     "ms932"
        record-separator:  "\n"  # LFで改行
        field-separator:   ","   # CSVです。
        quoting-delimiter: "\""  # ダブルクォートを使用
        requires-title: true     # 最初の行をタイトルとして読み書きする
        max-record-length: 10000 # 読み込みを許容する1行の文字列数
        
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
        
        StringBuilder builder = new StringBuilder();

        /*
         * 10000文字までのデータは正常に読み込める
         */
        while(builder.length() < 9994) {
            builder.append("1");
        }
        builder.append(",2,3,4");
        
        source = new ByteArrayInputStream(builder.toString().getBytes("ms932"));
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setInputStream(source).initialize();

        formatter.readRecord(); // 10000文字はOK
        assertTrue(true);
        
        /*
         * 10001文字のデータの場合には例外が発生する
         */
        builder = new StringBuilder();
        while(builder.length() < 9995) {
            builder.append("1");
        }
        builder.append(",2,3,4");
        
        source = new ByteArrayInputStream(builder.toString().getBytes("ms932"));
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile).setInputStream(source).initialize();

        try {        
            formatter.readRecord(); // 10001文字はNG!!
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("the number of the read characters exceeded the upper limit. the reading upper limit for 1 record is '10000'. record number=[1]."));
        }
        
    }
        
    
    /**
     * タイトルしか存在しないフォーマットで読み込む場合のテスト。
     * 
     * 以下の観点でテストを行う。
     * ①【正常系】タイトルのみ存在するファイルを読み込むことができること。
     * ②【異常系】タイトル以外のレコードが存在するファイルを読み込んだ場合に例外がスローされること。
     */
    @Test
    public void testTitleOnly() throws Exception {

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
        
        [Title]
        1   Title      N  "タイトル"
        2   Publisher  N  "出版社"
        3   Authors    N  "著者"
        4   Price      N  "価格(税込)"
        
        *****************************************/
        formatFile.deleteOnExit();

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        DataRecord record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // 終了
        assertNull(formatter.readRecord());
        
        formatter.close();
        

        /*
         * 以下、観点②
         */
        data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        タイトル,出版社,著者,価格(税込)
        Learning the vi and vim Editors,OReilly,Robbins Hanneah and Lamb,38.50
        Programming with POSIX Threads,Addison-Wesley,David R. Butenhof,29.00
        HACKING (2nd ed),no starch press,Jon Erickson,35.20
        **********************************************************************/
        
        source = new ByteArrayInputStream(data.getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);
        
        assertTrue(formatter.hasNext());

        // タイトル行（最初の行）
        record = formatter.readRecord();
        assertEquals("Title", record.getRecordType());
        assertEquals("タイトル", record.get("Title"));
        assertEquals("出版社", record.get("Publisher"));
        assertEquals("著者", record.get("Authors"));
        assertEquals("価格(税込)", record.get("Price"));
        
        // データ行 ← 存在してはいけない!!
        try {
            record = formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith("an applicable record type was not found. This format must not have non-title records."));
            assertThat(e.getMessage(), containsString("record number=[2]."));
        }
    }

    /**
     * 空の項目が設定されたファイルを読み込んだ場合に、nullで読み込めること
     */
    @Test
    public void testBlankField() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [Books]
         1   Title      X          # タイトル
         2   Publisher  X          # 出版社
         3   Authors    X          # 著者
         4   Price      X Number   # 価格
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("タイトル,,著者,1000".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        DataRecord record = formatter.readRecord();
        assertThat(record.getString("Publisher"), is(nullValue()));
    }

    /**
     * Numberコンバータによって数値型に変換されていること
     */
    @Test
    public void testNumber() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value,123456".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        assertThat(formatter.hasNext(), is(true));
        assertThat(formatter.readRecord().get("number"), is((Object) new BigDecimal("123456")));
    }

    /**
     * Numberコンバータの値に空文字が設定されていてもエラーが発生しないこと
     */
    @Test
    public void testNumberBlank() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value,".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        assertThat(formatter.hasNext(), is(true));
        assertThat(formatter.readRecord().get("number"), is(nullValue()));
    }

    /**
     * Numberコンバータで数値に変換できない値の場合にエラーとなること
     */
    @Test
    public void testNumberFailed() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value1,value2".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        try {
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("invalid parameter format was specified."));
        }
    }

    /**
     * SignedNumberコンバータによって数値型に変換されていること
     */
    @Test
    public void testSignedNumber() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X signed_number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value,-123456".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        assertThat(formatter.hasNext(), is(true));
        assertThat(formatter.readRecord().get("number"), is((Object) new BigDecimal("-123456")));
    }

    /**
     * SignedNumberコンバータの値に空文字が設定されていてもエラーが発生しないこと
     */
    @Test
    public void testSignedNumberBlank() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X signed_number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value,".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        assertThat(formatter.hasNext(), is(true));
        assertThat(formatter.readRecord().get("number"), is(nullValue()));
    }

    /**
     * SignedNumberコンバータで数値に変換できない値の場合にエラーとなること
     */
    @Test
    public void testSignedNumberFailed() throws Exception {
        formatFile = Hereis.file("./test.fmt");
        /*****************************************
         file-type:    "Variable"
         text-encoding:    "ms932"
         record-separator: "\r\n" # CRLFで改行
         field-separator:  ","    # カンマ区切り

         [data]
         1 key X
         2 number X signed_number
         *****************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("value1,value2".getBytes("ms932"));
        formatter = createReadFormatter(formatFile, source);

        try {
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), containsString("invalid parameter format was specified."));
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * フォーマットファイルを生成する。
     * @param format フォーマットファイルの中身
     * @return フォーマットファイル
     * @throws Exception 発生する例外はすべて投げる
     */
    private File createFormatFile(String... format) throws Exception{
        File formatFile = folder.newFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(formatFile));
        for (String line : format) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
        return formatFile;
    }

    /**
     * 後方互換の設定のテスト。
     * convertEmptyToNull プロパティを{@code false}に設定することで、未入力（空文字列）を空文字列として取得できる。
     * ({@code null}に変換しない)
     */
    @Test
    public void testDoNotConvertEmptyToNull() throws Exception {
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/core/dataformat/convertor/ConvertorSettingCompatible.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        File formatFile = createFormatFile(
                "file-type:    \"Variable\"",
                "text-encoding:    \"ms932\"",
                "record-separator: \"\\r\\n\" # CRLFで改行",
                "field-separator:  \",\"    # カンマ区切り",
                "",
                "[Books]",
                "1   Title      X          # タイトル",
                "2   Publisher  X          # 出版社",
                "3   Authors    X          # 著者",
                "4   Price      X Number   # 価格"
        );
        source = new ByteArrayInputStream("タイトル,,著者,1000".getBytes("ms932"));

        formatter = createReadFormatter(formatFile, source);
        DataRecord actual = formatter.readRecord();
        assertThat(actual.getString("Publisher"), is(""));

        SystemRepository.clear();
    }
}
