package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * 固定長レコードフォーマッタのシングルレイアウトを使用した場合のテスト。
 * 
 * 観点：
 * 固定長ファイルが、レイアウト定義ファイルの内容に伴って正しく読み書きできるかのテストを行う。
 * シングルレイアウファイルの読み書き、固定長ファイル関連のディレクティブの妥当性検証、
 * データタイプ（X、Nなど）が正常に使用されること、また、このクラスが担う異常系のテストを網羅する。
 * また、ゾーンビット（正/負）について、リポジトリのシステム共通定義を使用するパターンと、ディレクティブを使用する場合で動作テストを行う。
 *  
 * @author Masato Inoue
 */
public class FixedLengthDataRecordFormatterSingleLayoutTest {

    private DataRecordFormatter formatter;

    /** フォーマッタ(read)を生成する。 */
    private DataRecordFormatter createReadFormatter(File filePath, InputStream source) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        formatter.setInputStream(source).initialize();
        return formatter;
    }
    /** フォーマッタ(write)を生成する。 */
    private DataRecordFormatter createReadFormatterWrite(File filePath, OutputStream source) {
        formatter = new FormatterFactory().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        formatter.setOutputStream(source).initialize();
        return formatter;
    }
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(String filePath) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File(filePath));
        formatter.initialize();
        return formatter;
    }
    
    @Before
    public void setUp() {
        
        // レイアウト定義ファイル
        // (シングルフォーマット)
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 120

        # データレコード定義
        [Default]
        1    dataKbn       X(1)  "2"      # 1. データ区分
        2    FIcode        X(4)           # 2. 振込先金融機関コード
        6    FIname        X(15)          # 3. 振込先金融機関名称
        21   officeCode    X(3)           # 4. 振込先営業所コード
        24   officeName    X(15)          # 5. 振込先営業所名
        39  ?tegataNum     X(4)  "9999"   # (手形交換所番号)
        43   syumoku       X(1)           # 6. 預金種目
        44   accountNum    X(7)           # 7. 口座番号
        51   recipientName X(30)          # 8. 受取人名
        81   amount        X(10)          # 9. 振込金額
        91   isNew         X(1)           # 10.新規コード
        92   ediInfo       X(20)          # 11.EDI情報
        112  transferType  X(1)           # 12.振込区分
        113  withEdi       X(1)  "Y"      # 13.EDI情報使用フラグ
        114 ?unused        X(7)  pad("0") # (未使用領域)
        ***************************************************/
        formatFile.deleteOnExit();
        createFormatter("./format.fmt");
    }
    
    /**
     * 典型的なデータレコードの読み込み
     */
    @Test
    public void testReadFrom() throws Exception {
        String LS = Builder.LS;
        String testdata = Hereis.string().replaceAll(LS, "");
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          21234FSEｷﾞﾝｺｳ       ﾏｺ1ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  999917778888
          ﾀﾞｲｱﾅ ﾛｽ                      3020      Nﾀｸｼｰﾀﾞｲｷﾝ
          ﾃﾞｽ        4Y0000000*/                            
        //12345678901234567890123456789012345678901234567890     
        //         1         2         3         4         5
        
        File dataFile = new File("./test.dat");
        dataFile.deleteOnExit();
        new FileOutputStream(dataFile, false).write(testdata.getBytes("ms932"));
        
        InputStream source = new FileInputStream("./test.dat");
        
        // レコードの読み込み
        formatter = createFormatter("./format.fmt");
        DataRecord record = formatter.setInputStream(source).initialize().readRecord();
        
        assertEquals("Default", record.getRecordType()); 
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
        
        record = formatter.readRecord();
        assertNull(record);
    }
    
    /**
     * 典型的なデータレコードの書き出し
     */
    @Test
    public void testWriteTo() throws Exception {
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
        
        OutputStream dest = new FileOutputStream(outputData, false);
        
        formatter = createFormatter("./format.fmt");
        formatter.setOutputStream(dest);
        formatter.writeRecord(recordMap);
        
        dest.close();
        
        InputStream in = new FileInputStream("./output.dat");
        
        byte[] recordBytes = new byte[120];
        in.read(recordBytes);
        
        assertTrue(in.read() == -1);
        ByteBuffer recordBuff = ByteBuffer.wrap(recordBytes);
        byte[] fieldBuff = null;
        
        // 1    dataKbn       X(1)  2       # 1. データ区分
        byte[] fieldBytes = new byte[1];
        recordBuff.get(fieldBytes);
        assertEquals("2", new String(fieldBytes, "ms932"));
        
        // 2    FIcode        X(4)          # 2. 振込先金融機関コード
        fieldBytes = new byte[4];
        recordBuff.get(fieldBytes);
        assertEquals("1234", new String(fieldBytes, "ms932"));
        
        // 6    FIname        X(15)         # 3. 振込先金融機関名称
        fieldBytes = new byte[15];
        recordBuff.get(fieldBytes);
        assertEquals("FSEｷﾞﾝｺｳ       ", new String(fieldBytes, "ms932"));
        
        // 21   officeCode    X(3)          # 4. 振込先営業所コード
        fieldBytes = new byte[3];
        recordBuff.get(fieldBytes);
        assertEquals("ﾏｺ1", new String(fieldBytes, "ms932"));
        
        // 24   officeName    X(15)         # 5. 振込先営業所名
        fieldBytes = new byte[15];
        recordBuff.get(fieldBytes);
        assertEquals("ﾏﾂﾄﾞｺｶﾞﾈﾊﾗｼﾃﾝ  ", new String(fieldBytes, "ms932"));
        
        // 39  ?tegataNum     X(4)  9999    # (手形交換所番号)
        fieldBytes = new byte[4];
        recordBuff.get(fieldBytes);
        assertEquals("9999", new String(fieldBytes, "ms932"));
        
        // 43   syumoku       X(1)          # 6. 預金種目
        fieldBytes = new byte[1];
        recordBuff.get(fieldBytes);
        assertEquals("1", new String(fieldBytes, "ms932"));
        
        // 44   accountNum    X(7)          # 7. 口座番号
        fieldBytes = new byte[7];
        recordBuff.get(fieldBytes);
        assertEquals("7778888", new String(fieldBytes, "ms932"));
        
        // 51   recipientName X(30)         # 8. 受取人名
        fieldBytes = new byte[30];
        recordBuff.get(fieldBytes);
        assertEquals("ﾀﾞｲｱﾅ ﾛｽ                      ", new String(fieldBytes, "ms932"));
        
        // 81   amount        X(10)         # 9. 振込金額
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("3020      ", new String(fieldBytes, "ms932"));
        
        //  91   isNew         X(1)          # 10.新規コード
        fieldBytes = new byte[1];
        recordBuff.get(fieldBytes);
        assertEquals("N", new String(fieldBytes, "ms932"));
        
        //  92   ediInfo       X(20)         # 11.EDI情報
        fieldBytes = new byte[20];
        recordBuff.get(fieldBytes);
        assertEquals("ﾀｸｼｰﾀﾞｲｷﾝﾃﾞｽ        ", new String(fieldBytes, "ms932"));
        
        // 112  transferType  X(1)          # 12.振込区分
        fieldBytes = new byte[1];
        recordBuff.get(fieldBytes);
        assertEquals("4", new String(fieldBytes, "ms932"));
        
        // 113  withEdi       X(1)  "Y"     # 13.EDI情報使用フラグ
        fieldBytes = new byte[1];
        recordBuff.get(fieldBytes);
        assertEquals("Y", new String(fieldBytes, "ms932"));
        
        // 114 ?unused        X(7)  ALL "0" # (未使用領域)
        fieldBytes = new byte[7];
        recordBuff.get(fieldBytes);
        assertEquals("0000000", new String(fieldBytes, "ms932"));
    }
    
    
    /**
     * データ型の網羅テスト。改行コードのテストも同時に行う。
     */
    @Test
    public void testAllDataTypeAndRecordSeparator() throws Exception {
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"

        # 各レコードの長さ
        record-length: 110
        
        #レコード区切り文字列
        record-separator: "\n"

        # データレコード定義
        [Default]
        1    byteString     X (10)  # 1. シングルバイト文字列
        11   wordString     N (10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z (10)  # 3. ゾーン10進
        31   signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        41   packedDigits   P (10)  # 5. パック10進
        51   signedPDigits  SP(10)  # 6. 符号付パック10進
        61   nativeBytes    B (10)  # 7. バイト列
        71   zDecimalPoint  Z(5, 3) # 8. 仮想小数点付きゾーン10進(5byte)
        76   pDecimalPoint  P(3, 2) # 9. 仮想小数点付きパック10進(3byte)
        79  ?endMark        X(2)   "00"
        81   X9             X9(10) pad("X") # 符号なし数値（パディング文字は、「X」）
        91   X92            X9(5)           # 符号なし数値（パディング文字はデフォルト）
        96   SX9            SX9(10) pad("X")# 符号あり数値（パディング文字は、「X」）
        106  SX92           SX9(5)          # 符号あり数値（パディング文字はデフォルト）
        ***************************************************/
        formatFile.deleteOnExit();

        byte[] bytes = new byte[110];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        
        buff.put("ｱｲｳｴｵｶｷｸｹｺ".getBytes("sjis")); //X(10)
        buff.put("あいうえお".getBytes("sjis"));  //N(10)
        buff.put("1234567890".getBytes("sjis")); //9(10)
        buff.put("123456789".getBytes("sjis"))   //S9(10)
            .put((byte) 0x70); // -1234567890
        buff.put(new byte[] {                    //P(10)
            0x12, 0x34, 0x56, 0x78, (byte) 0x90,
            0x12, 0x34, 0x56, 0x78, (byte) 0x93 
        }); // 1234567890123456789
        buff.put(new byte[] {                    //SP(10)
            0x12, 0x34, 0x56, 0x78, (byte) 0x90,
            0x12, 0x34, 0x56, 0x78, (byte) 0x97
        }); // -1234567890123456789
        buff.put(new byte[] {                    // B(10)
            (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB,
            (byte) 0xAA, (byte) 0x99, (byte) 0x88, (byte) 0x77, (byte) 0x66,
        });
        buff.put("12345".getBytes("sjis"));      //99.999
        // = 12.345
        buff.put(new byte[] {                    //PPP.PP
            0x12, 0x34, 0x53
        }); // = 123.45
        buff.put("  ".getBytes());
        buff.put("XXXXX12345".getBytes());      // X9:12345
        buff.put("00005".getBytes());           // X9:5
        buff.put("-XXXX54321".getBytes());      // SX9:-54321
        buff.put("+0055".getBytes());           // SX9:55

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write("\n".getBytes("sjis"));
        dest.write(bytes);
        dest.write("\n".getBytes("sjis"));
        dest.write(bytes);
        dest.write("\n".getBytes("sjis"));
        dest.close();
        

        formatter = createReadFormatter(new File("format.fmt"), new BufferedInputStream(new FileInputStream("record.dat")));
        
        assertTrue(formatter.hasNext());
        DataRecord record = formatter.readRecord(); // #1
        assertNotNull(record);
        record = formatter.readRecord();            // #2
        assertThat("type:X9->指定したパディング文字がトリムされていること", record.getBigDecimal("X9"), is(new BigDecimal("12345")));
        assertThat("type:X9->デフォルトのパディング文字(0)でトリムされていること", record.getBigDecimal("X92"), is(new BigDecimal("5")));
        assertThat("type:SX9->指定したパディング文字がトリムされていること", record.getBigDecimal("SX9"), is(new BigDecimal("-54321")));
        assertThat("type:X9->デフォルトのパディング文字(0)でトリムされていること", record.getBigDecimal("SX92"), is(new BigDecimal("55")));
        assertNotNull(record);
        record = formatter.readRecord();            // #3
        assertNotNull(record);
        record = formatter.readRecord();            // #4
        assertNull("レコードを全て読み込んだためnullとなる", record);
        assertFalse(formatter.hasNext());
    }
    
    /**
     * ディレクティブの検証で例外が発生するテスト。
     */
    @Test
    public void testReportingSystaxErrorOfFormatFile() throws Exception {

        // 必須ディレクティブがない。
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).initialize().readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains(
                "directive 'text-encoding' was not specified. directive 'text-encoding' must be specified."
            ));
        }
        
        // レイアウト定義ファイルにrecord-lengthディレクティブの設定がない。
        File layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        text-encoding: "sjis"  
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        InputStream source = createInputStreamFrom("test");

        try {
            formatter = createReadFormatter(new File("test.fmt"), source);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "directive 'record-length' was not specified. " +
                            "directive 'record-length' must be specified."));
            assertThat(e.getFilePath(), containsString("test.fmt"));
        }
        
        // ディレクティブの型が不正
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: 1  # ディレクティブの型が不正
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "the value of the directive 'text-encoding' must be java.lang.String " +
                            "but was java.lang.Integer."));
            assertThat(e.getFilePath(), containsString("erroneous.fmt"));
        }
        
        // レコード長が不正。
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"
        [Default]
        1    byteString     X(10)
        11   wordString     N(10)
        21   zoneDigits     Z(11) # 計31バイト!!
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid record length was specified by 'record-length' directive. " +
                            "sum of length of fields must be '30' byte but was '31'."));
            assertThat(e.getFilePath(), containsString("erroneous.fmt"));
        }
        
        // マルチバイト文字のバイト長が奇数。
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 31
        text-encoding: "sjis"
        [Default]
        1    byteString  X(1)
        11   wordString  N(11) # 奇数！！
        21   zoneDigits  Z(10) 
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).readRecord();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof SyntaxErrorException);
            assertTrue(e.getMessage().contains(
                "the length of DoubleByteCharacter data "
              + "field must be a even number"
            ));
        }
        
        // レコードタイプ名がない。
        // (シングルフォーマットでも必要。)
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"
        
        1    byteString     X(10)
        10   wordString     N(10)
        20   zoneDigits     Z(10)
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).readRecord();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof SyntaxErrorException);
            assertTrue(e.getMessage().contains(
            "encountered unexpected token. allowed token types are: RECORD_TYPE_HEADER"
            ));
        }
        
        
        // 文字列リテラル内のエスケープが不正
        Hereis.file("./erroneous.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 80
        text-encoding: "sjis"
        [strings]
        1    string1 X(10) pad("\t")
        11   string2 X(10) pad("\f")
        21   string3 X(10) pad("\"") 
        31   string4 X(10) pad("\'") 
        41   string5 X(10) pad("\r") 
        51   string6 X(10) pad("\n") 
        61   string7 X(10) pad("\\")
        71   string8 X(10) pad("\o") # "\o"はNG!
        ***************************************************/
        try {
            formatter = createFormatter("./erroneous.fmt");
            formatter.setInputStream(createInputStreamFrom("hoge")).readRecord();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof SyntaxErrorException);
            assertTrue(e.getMessage().contains(
                "invalid escape sequence was specified. value=[\\o]"
            ));
        }
    }
    
    /**
     * 初期化前にhasNextを読んだ場合に、falseが返却されること。
     */
    @Test
    public void testHasNext() throws Exception{
        assertFalse(new FixedLengthDataRecordFormatter().hasNext());
    }

    /**
     * 引数で渡したストリームの長さがディレクティブで定義されたrecord-lengthより短い場合に例外がスローされることの確認。
     */
    @Test
    public void testDataShort() throws Exception {
        File layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        // 例外がスローされることの確認
        InputStream source = new ByteArrayInputStream(
                "01234567890123456789012345678".getBytes("sjis")); // 29バイトしかない場合
        DataRecordFormatter fewer = createReadFormatter(new File("test.fmt"), source);
        try {
            fewer.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid data record found. " +
                    "the length of a record must be 30 byte " +
                    "but read data was only 29 byte."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("test.fmt"));
        }

        // 例外がスローされないことの確認
        source = createInputStreamFrom("012345678901234567890123456789"); // 30バイト（正しい）の場合
        DataRecordFormatter just = createReadFormatter(new File("test.fmt"), source);
        just.readRecord();
    }
    

    /**
     * 固定長ファイルの改行コードのテスト。
     */
    @Test
    public void testUseSeparator() throws Exception {

        /**
         * 正常系：改行コード「\n」
         */
        File layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        record-separator: "\n"
        
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        String data = 
            "0123456789abcdefghij0123456781" + "\n"
            + "0123456789abcdefghij0123456782" + "\n"
            + "0123456789abcdefghij0123456783" + "\n"; // \n
        
        InputStream source = createInputStreamFrom(data);
        DataRecordFormatter formatter = createReadFormatter(new File("test.fmt"), source);
        

        assertUseSeparatorTest(formatter);

        /**
         * 正常系：改行コード「\r」
         */
        layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        record-separator: "\r"
        
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        data = 
            "0123456789abcdefghij0123456781" + "\r"
            + "0123456789abcdefghij0123456782" + "\r"
            + "0123456789abcdefghij0123456783" + "\r";
        
        source = createInputStreamFrom(data);
        formatter = createReadFormatter(new File("test.fmt"), source); // \r
        

        assertUseSeparatorTest(formatter);

        
        /**
         * 正常系：改行コード「\r\n」
         */
        layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        record-separator: "\r\n"
        
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        data = 
            "0123456789abcdefghij0123456781" + "\r\n"
            + "0123456789abcdefghij0123456782" + "\r\n"
            + "0123456789abcdefghij0123456783" + "\r\n"; // \r\n
        
        source = createInputStreamFrom(data);
        formatter = createReadFormatter(new File("test.fmt"), source);
        
        assertUseSeparatorTest(formatter);

        
        /**
         * 異常系：改行コード「改行コードがあるべき場所にない」
         */
        layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        record-separator: "\r\n"
        
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        data = 
            "0123456789abcdefghij012345678" + "\r\n"
            + "0123456789abcdefghij0123456782" + "\r\n"
            + "0123456789abcdefghij0123456783" + "\r\n"; // 改行コードの場所が1バイトずれている
        
        source = createInputStreamFrom(data);
        final DataRecordFormatter illegal = createReadFormatter(new File("test.fmt"), source);

        try {
            illegal.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid record separator was specified by 'record-separator' directive. "
                  + "value=[[10, 48]]. "));
                    
            assertThat(e.getRecordNumber(), is(1));
        }

    }
    
    /**
     * アサート
     */
    private void assertUseSeparatorTest(DataRecordFormatter formatter) throws Exception {
        DataRecord readRecord = formatter.readRecord();
        assertEquals("0123456789", readRecord.get("byteString"));
        assertEquals("abcdefghij", readRecord.get("wordString"));
        assertEquals(new BigDecimal("0123456781"), readRecord.get("zoneDigits"));
        readRecord = formatter.readRecord();
        assertEquals("0123456789", readRecord.get("byteString"));
        assertEquals("abcdefghij", readRecord.get("wordString"));
        assertEquals(new BigDecimal("0123456782"), readRecord.get("zoneDigits"));
        readRecord = formatter.readRecord();
        assertEquals("0123456789", readRecord.get("byteString"));
        assertEquals("abcdefghij", readRecord.get("wordString"));
        assertEquals(new BigDecimal("0123456783"), readRecord.get("zoneDigits"));
        assertTrue(true);        
    }


    /**
     * データレコードの書き出し（レコードタイプが空文字）
     */
    @Test
    public void testRecordTypeNullOrEmpty() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"

        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 70
        
        # レコード区切り文字列
        record-separator: "\r\n"

        # データレコード定義
        [miniData]
        1    byteString     X(10)           # 1. シングルバイト文字列
        11   wordString     N(10)           # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)           # 3. ゾーン10進
        31   x9             X9(10) pad("x") # 4.符号なし数値（パディング指定有り）
        41   x92            X9(10)          # 5.符号なし数値（パディング指定なし）
        51   sx9           SX9(10) pad("x") # 6.符号あり数値（パディング指定有り）
        61   sx92          SX9(10)          # 7.符号あり数値（パディング指定なし）
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance().addBasePathSetting("output", "file:./")
                                 .addBasePathSetting("format",  "file:./");
        
        DataRecord record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
            put("x9", 100);
            put("x92", 10);
            put("sx9", 1);
            put("sx92", -1);
        }};
        
        record.setRecordType("");
        
        File dataFile = FilePathSetting.getInstance().getFile("output", "test.dat");
        dataFile.delete();

        FileOutputStream outputStream = new FileOutputStream("test.dat");
        formatter = createReadFormatterWrite(new File("format.fmt"), outputStream);
        formatter.writeRecord(record);
        formatter.writeRecord(record);
        formatter.writeRecord(record);
        formatter.close();      // 3件出力
        
        assertTrue(dataFile.exists());
        
        InputStream source = new FileInputStream(dataFile);

        byte[] binaryRecord = new byte[70];
        source.read(binaryRecord);
        byte[] x9 = new byte[10];
        byte[] x92 = new byte[10];
        byte[] sx9 = new byte[10];
        byte[] sx92 = new byte[10];
        System.arraycopy(binaryRecord, 30, x9, 0, 10);
        System.arraycopy(binaryRecord, 40, x92, 0, 10);
        System.arraycopy(binaryRecord, 50, sx9, 0, 10);
        System.arraycopy(binaryRecord, 60, sx92, 0, 10);
        assertThat("type:X9->指定したパディング文字でパディングされていること", new String(x9), is("xxxxxxx100"));
        assertThat("type:X9->デフォルトのパディング文字でパディングされていること", new String(x92), is("0000000010"));
        assertThat("type:SX9->指定したパディング文字でパディングされていること", new String(sx9), is("xxxxxxxxx1"));
        assertThat("type:SX9->デフォルトのパディング文字でパディングされていること", new String(sx92), is("-000000001"));
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        source.read(binaryRecord);
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        source.read(binaryRecord);
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        assertEquals(-1, source.read());

        source.close();
    }
    
    
    /**
     * ゾーンビット（正/負）ディレクティブに不正な値を設定した場合に例外がスローされることの確認。
     */
    @Test
    public void testInvalidNibble() throws Exception {
        
        /**
         * negative-zone-sign-nibble（ゾーンビット（正））ディレクティブに不正な値を設定
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ゾーンビット（正）
        positive-zone-sign-nibble: "11"
        # ゾーンビット（負）
        negative-zone-sign-nibble: "2"
        
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 80

        # データレコード定義
        [Default]
        1    byteString     X(10)   # 1. シングルバイト文字列
        11   wordString     N(10)   # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)   # 3. ゾーン10進
        31   signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        41   packedDigits   P(10)   # 5. パック10進
        51   signedPDigits  SP(10)  # 6. 符号付パック10進
        61   nativeBytes    B(10)   # 7. バイト列
        71   zDecimalPoint  Z(5, 3) # 8. 仮想小数点付きゾーン10進(5byte)
        76   pDecimalPoint  P(3, 2) # 9. 仮想小数点付きパック10進(3byte)
        79  ?endMark        X(2)   "00"    
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");


        InputStream source = createInputStreamFrom("test");
        
        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid sign nibble was specified by 'positive-zone-sign-nibble' directive. " +
                            "value=[11]. sign nibble format must be [[0-9a-fA-F]]."));
            assertThat(e.getFilePath(), containsString("format.fmt"));
        }
      
        /**
         * negative-zone-sign-nibble（ゾーンビット（正））ディレクティブに不正な値を設定
         */
       formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ゾーンビット（正）
        positive-zone-sign-nibble: "2"
        # ゾーンビット（負）
        negative-zone-sign-nibble: "ff"
        
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 80

        # データレコード定義
        [Default]
        1    byteString     X(10)   # 1. シングルバイト文字列
        11   wordString     N(10)   # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)   # 3. ゾーン10進
        31   signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        41   packedDigits   P(10)   # 5. パック10進
        51   signedPDigits  SP(10)  # 6. 符号付パック10進
        61   nativeBytes    B(10)   # 7. バイト列
        71   zDecimalPoint  Z(5, 3) # 8. 仮想小数点付きゾーン10進(5byte)
        76   pDecimalPoint  P(3, 2) # 9. 仮想小数点付きパック10進(3byte)
        79  ?endMark        X(2)   "00"    
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");


        source = createInputStreamFrom("test");
        
        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid sign nibble was specified by 'negative-zone-sign-nibble' directive. " +
                            "value=[ff]. sign nibble format must be [[0-9a-fA-F]]."));
            assertThat(e.getFilePath(), containsString("format.fmt"));
        }
    }
    
    /**
     * negative-zone-sign-nibble（ゾーンビット（正/負））ディレクティブに正常な値を設定　
     */
    @Test
    public void testNibble() throws Exception {
    {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ゾーンビット（正）
        positive-zone-sign-nibble: "4"
        # ゾーンビット（負）
        negative-zone-sign-nibble: "8" 
        
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     Z(10)   # 3. ゾーン10進
        11  signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        21  signedZDigits2  SZ(10)  # 4. 符号付ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        
        buff.put("1234567890".getBytes("sjis")); //9(10)
        buff.put("123456789".getBytes("sjis"))   //S9(10)
            .put((byte) 0x80); // -1234567890  // 0x80が負を表す
        buff.put("123456789".getBytes("sjis"))   //S9(10)
        .put((byte) 0x40); // 1234567890   // 0x40が正を表す

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("+1234567890"),          record.get("signedZDigits2"));
        
        
        source.close();
        new File("record.dat").deleteOnExit();
        }
    }
    

    
    /**
     * nibble（ゾーンビット（正/負））をリポジトリから設定（システム全体の設定となる）　
     */
    @Test
    public void testNibbleRepository() throws Exception {
    {
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/FixedLengthConvertorSetting.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        File formatFile = Hereis.file("./format2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     Z(10)   # 3. ゾーン10進
        11  signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        21  signedZDigits2  SZ(10)  # 4. 符号付ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        
        buff.put("1234567890".getBytes("sjis")); //9(10)
        buff.put("123456789".getBytes("sjis"))   //S9(10)
            .put((byte) 0x50); // -1234567890  // 0x20が負を表す（コンポーネント設定ファイルで設定）
        buff.put("123456789".getBytes("sjis"))   //S9(10)
        .put((byte) 0x60); // 1234567890   // 0x90が正を表す（コンポーネント設定ファイルで設定）

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        formatter = createReadFormatter(formatFile, source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("+1234567890"),          record.get("signedZDigits2"));
        
        
        source.close();
        new File("record.dat").deleteOnExit();
        SystemRepository.clear();
        }
    }
    
    /**
     * EBICIDICの場合にディレクティブで設定したnibble（ゾーンビット（正/負））が正しく動作するテスト。
     */
    @Test
    public void testNibbleEbcdic() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "IBM1047"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     Z(10)   # 3. ゾーン10進
        11  signedZDigits  SZ(10)  # 4. 符号付ゾーン10進
        21  signedZDigits2  SZ(10)  # 4. 符号付ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        
        buff.put("1234567890".getBytes("IBM1047")); //9(10)
        buff.put("123456789".getBytes("IBM1047"))   //S9(10)
            .put((byte) 0xD0); // -1234567890  // 0xD0が負を表す
        buff.put("123456789".getBytes("IBM1047"))   //S9(10)
        .put((byte) 0xC0); // 1234567890   // 0xC0が正を表す

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("+1234567890"),          record.get("signedZDigits2"));
        
        source.close();
        new File("record.dat").deleteOnExit();
    }

    

    /**
     * negative-pack-sign-nibble（パックビット（正/負））ディレクティブに正常な値を設定　
     */
    @Test
    public void testPackNibble() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # パックビット（正）
        positive-pack-sign-nibble: "4"
        # パックビット（負）
        negative-pack-sign-nibble: "8" 
        
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     P(10)   # 3. パック10進
        11  signedZDigits  SP(10)  # 4. 符号付パック10進
        21  signedZDigits2  SP(10)  # 4. 符号付パック10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.put(new byte[] {                    //P(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x93 
            });
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x94
            }); // 1234567890123456789
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x98
            }); // -1234567890123456789
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890123456789"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("1234567890123456789"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"),          record.get("signedZDigits2"));
        
        source.close();
        new File("record.dat").deleteOnExit();
    }
    

    
    /**
     * nibble（パックビット（正/負））をリポジトリから設定（システム全体の設定となる）　
     */
    @Test
    public void testPackNibbleRepository() throws Exception {

        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/FixedLengthConvertorSetting.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        File formatFile = Hereis.file("./format2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     P(10)   # 3. パック10進
        11  signedZDigits  SP(10)  # 4. 符号付パック10進
        21  signedZDigits2  SP(10)  # 4. 符号付パック10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.put(new byte[] {                    //P(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x93 
            });
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x98
            }); // 1234567890123456789
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x97
            }); // -1234567890123456789
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        formatter = createReadFormatter(formatFile, source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890123456789"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("1234567890123456789"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"),          record.get("signedZDigits2"));
        
        
        source.close();
        new File("record.dat").deleteOnExit();
        SystemRepository.clear();
    }
    
    /**
     * EBICIDICの場合にディレクティブで設定したnibble（パックビット（正/負））が正しく動作するテスト。
     */
    @Test
    public void testPackNibbleEbcdic() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "IBM1047"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [Default]
        1   zoneDigits     P(10)   # 3. パック10進
        11  signedZDigits  SP(10)  # 4. 符号付パック10進
        21  signedZDigits2  SP(10)  # 4. 符号付パック10進
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.put(new byte[] {                    //P(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x9F 
            });
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x9C
            }); // 1234567890123456789
        buff.put(new byte[] {                    //SP(10)
                0x12, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x9D
            }); // -1234567890123456789

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(3, record.size());
        assertEquals(new BigDecimal("1234567890123456789"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("1234567890123456789"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"),          record.get("signedZDigits2"));
        
        
        source.close();
        new File("record.dat").deleteOnExit();
    }


    /**
     * 改行コードありの固定長ファイル。
     */
    @Test
    public void testFixedRecordSeparator() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"

        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30
        
        # レコード区切り文字列
        record-separator: "\r\n"

        # データレコード定義
        [miniData]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance().addBasePathSetting("output", "file:./")
                                 .addBasePathSetting("format",  "file:./");
        
        Map<String, Object> record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};
        
        File dataFile = FilePathSetting.getInstance().getFile("output", "test.dat");
        dataFile.delete();

        FileOutputStream outputStream = new FileOutputStream("test.dat");
        formatter = createReadFormatterWrite(new File("format.fmt"), outputStream);
        formatter.writeRecord(record);
        formatter.writeRecord(record);
        formatter.writeRecord(record);
        formatter.close();      // 3件出力
        
        assertTrue(dataFile.exists());
        
        InputStream source = new FileInputStream(dataFile);
        
        byte[] binaryRecord = new byte[30];
        source.read(binaryRecord);
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        source.read(binaryRecord);
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        source.read(binaryRecord);
        assertEquals('\r', (char) source.read());
        assertEquals('\n', (char) source.read());
        assertEquals(-1, source.read());
        
        source.close();
    }

    
    /**
     * 引数のデータタイプがnull。
     */
    @Test
    public void testDataTypeNull() throws Exception {
        File layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis" 
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");


        FileOutputStream outputStream = new FileOutputStream("test.dat");
        formatter = createReadFormatterWrite(new File("test.fmt"), outputStream);
        
        try {
            formatter.writeRecord(null, new HashMap<String, Object>());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
    
    /**
     * 引数のデータタイプが空文字。
     */
    @Test
    public void testDataTypeBlank() throws Exception {
        File layoutFile = Hereis.file("./test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis"  
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        FileOutputStream outputStream = new FileOutputStream("test.dat");
        formatter = createReadFormatterWrite(new File("test.fmt"), outputStream);
        try {
            formatter.writeRecord("", new HashMap<String, Object>());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 初期化されていない状態で実行メソッドを呼んだ場合に例外がスローされるテスト。
     */
    @Test
    public void testDefinitionNull() throws Exception {

        FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();
        try{
            formatter.readRecord();
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        formatter = new FixedLengthDataRecordFormatter();
        try{
            formatter.writeRecord(new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        
        formatter = new FixedLengthDataRecordFormatter();
        try{
            formatter.writeRecord("", new DataRecord());
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

        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./format.fmt"));
        formatter.initialize();
        try{
            formatter.readRecord();
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        formatter.close();
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./format.fmt"));
        formatter.initialize();
        try{
            formatter.writeRecord(new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
        
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("./format.fmt"));
        formatter.initialize();
        try{
            formatter.writeRecord("", new DataRecord());
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
    }
    
    /**
     * クローズされることの確認
     */
    @Test
    public void testClose() throws Exception{
        
        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }
        
        // 本テストで作成するファイル名の接頭辞定義
        final String FILE_NAME_PREFIX = "FixedLengthDataRecordFormatterSingleLayoutTest#testClose#";
        
        File layoutFile = Hereis.file("./" + FILE_NAME_PREFIX + "test.fmt");
        /**********************************************
        file-type:    "Fixed"
        record-length: 30
        text-encoding: "sjis" 
        [Default]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進 
        ***************************************************/
        layoutFile.deleteOnExit();
        
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
        .addBasePathSetting("format", "file:./")
        .addBasePathSetting("output", "file:./");
        
        
        /**
         * readRecordの場合に、inputStreamがクローズされることの確認。
         */
        File inputDataFile = Hereis.file("./" + FILE_NAME_PREFIX + "record.dat");
        /**********************************************
        0123456789abcdefghij0123456789
        ***************************************************/
        inputDataFile.deleteOnExit();
        
        formatter = createReadFormatter(layoutFile, new FileInputStream(inputDataFile));

        DataRecord readRecord = formatter.readRecord();
        assertEquals("0123456789", readRecord.get("byteString"));
        assertEquals("abcdefghij", readRecord.get("wordString"));
        assertEquals(new BigDecimal("0123456789"), readRecord.get("zoneDigits"));
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
        FileOutputStream outputStream = new FileOutputStream("close.dat");
        formatter = FormatterFactory.getInstance().createFormatter(layoutFile).setOutputStream(outputStream);

        Map<String, Object> record = new DataRecord() {{
            put("dataKbn", "1");
            put("processCode", "あいうえお");
        }};
        
        dataFile = new File("./close.dat");
        assertTrue(dataFile.exists()); 
        dataFile.delete();
        assertTrue(dataFile.exists()); // クローズされていないので削除できない
        
        formatter.close();

        dataFile.delete();
        assertFalse(dataFile.exists()); // クローズされているので削除できることの確認
        
        SystemRepository.clear();
        
        new File("./close.dat").deleteOnExit();
    }


    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }
    
    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
        SystemRepository.clear();
    }
}
