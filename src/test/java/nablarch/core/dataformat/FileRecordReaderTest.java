package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.core.ThreadContext;
import nablarch.core.dataformat.FormatterFactoryStub.DataRecordFormatterStub;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.handler.CatchingHandler;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ファイルレコードリーダのテスト
 * 
 * 観点：
 * 正常系のテスト、readメソッド、hasNextメソッド、closeメソッドが正常に動作し、ファイルの読み込みができること、
 * また、異常系のテストを行う。
 *  
 * @author Masato Inoue
 */
public class FileRecordReaderTest {
    
    private FileRecordReader reader = null;

    @BeforeClass
    public static void setUpClass() {
        // 強制的にキャッシュをオフに。
        // これで、このクラスで使用したいフォーマット定義が必ず使用される。
        FormatterFactory.getInstance().setCacheLayoutFileDefinition(false);
    }

    @Before
    public void setup() {
        FileRecordWriterHolder.closeAll();
        SystemRepository.clear();
    }
    
    /**
     * FileRecordReader実行時に、DataRecordFormatterのクローズメソッドが呼ばれることの確認。
     */
    @Test
    public void testClose() throws Exception{

        ThreadContext.setRequestId("test");
        
        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
        .addBasePathSetting("format", "file:./");

        File dataFile = new File("./record10.dat");
        dataFile.createNewFile();
        dataFile.deleteOnExit();
        
        reader = new FileRecordReader(
                dataFile, (File) null);
        reader.read();
        
        assertTrue(dataFile.exists()); 
        dataFile.delete();
        assertTrue(dataFile.exists()); // クローズされていないので削除できない
        
        assertFalse(DataRecordFormatterStub.isCallClose);
        
        reader.close();

        assertTrue(DataRecordFormatterStub.isCallClose); // フォーマッタのクローズメソッドが呼ばれたことの確認

        dataFile.delete();
        assertFalse(dataFile.exists()); // クローズされているので削除できることの確認
        
        SystemRepository.clear();
        
        new File("./record.dat").deleteOnExit();
    }

    
    /**
     * hasNextメソッドおよびreadメソッドの動作テスト。
     */
    @Test
    public void testReadHasNextMethodFirst() throws Exception{

        ThreadContext.setRequestId("test");
        
        SystemRepository.clear();
        
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format10.fmt");
        /**********************************************
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
        
        
        byte[] bytes = new byte[80];
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

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();
        

        reader = new FileRecordReader(new File("./record.dat"), new File("./format10.fmt"));
        
        assertTrue(reader.hasNext());
        DataRecord record = reader.read();
        
        assertEquals(9, record.size());
        assertEquals("ｱｲｳｴｵｶｷｸｹｺ",                           record.get("byteString"));
        assertEquals("あいうえお",                         record.get("wordString"));
        assertEquals(new BigDecimal("1234567890"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("1234567890123456789"),  record.get("packedDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"), record.get("signedPDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"), record.get("signedPDigits"));
        assertEquals(new BigDecimal("12.345"),               record.get("zDecimalPoint"));
        assertEquals(new BigDecimal("123.45"),               record.get("pDecimalPoint"));
        
        assertTrue(record.containsKey("nativeBytes"));
        
        byte[] nativeBytes = record.getValue("nativeBytes");
        assertEquals((byte)0xFF, nativeBytes[0]);
        assertEquals((byte)0xEE, nativeBytes[1]);
        assertEquals((byte)0x66, nativeBytes[9]);

        assertTrue(reader.hasNext());
        reader.read(); //2件め
        assertTrue(reader.hasNext());
        reader.read(); //3件め
        assertFalse(reader.hasNext());
        assertNull(reader.read());
    }

    /**
     * hasNextを呼び出さずに、先にreadを呼び出しても処理に影響がないことのテスト。
     */
    @Test
    public void testReadReadMethodFirst() throws Exception{

        ThreadContext.setRequestId("test");
        
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format11.fmt");
        /**********************************************
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
        

        byte[] bytes = new byte[80];
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

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        
        reader = new FileRecordReader(new File("./record.dat"), new File("./format11.fmt"));
        
        DataRecord record = reader.read();
        assertTrue(reader.hasNext());
        
        assertEquals(9, record.size());
        assertEquals("ｱｲｳｴｵｶｷｸｹｺ",                           record.get("byteString"));
        assertEquals("あいうえお",                         record.get("wordString"));
        assertEquals(new BigDecimal("1234567890"),           record.get("zoneDigits"));
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("1234567890123456789"),  record.get("packedDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"), record.get("signedPDigits"));
        assertEquals(new BigDecimal("-1234567890123456789"), record.get("signedPDigits"));
        assertEquals(new BigDecimal("12.345"),               record.get("zDecimalPoint"));
        assertEquals(new BigDecimal("123.45"),               record.get("pDecimalPoint"));
        
        assertTrue(record.containsKey("nativeBytes"));
        
        byte[] nativeBytes = record.getValue("nativeBytes");
        assertEquals((byte)0xFF, nativeBytes[0]);
        assertEquals((byte)0xEE, nativeBytes[1]);
        assertEquals((byte)0x66, nativeBytes[9]);

        assertTrue(reader.hasNext());
        reader.read(); //2件め
        assertTrue(reader.hasNext());
        reader.read(); //3件め
        assertFalse(reader.hasNext());
        assertNull(reader.read());
    }
    
    
    /**
     * IOExceptionがスローされるパターン。
     */
    @Test
    public void testException() throws Exception {

        ThreadContext.setRequestId("test");
        
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
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
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory02.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        // readメソッドでIOExceptionがスローされる場合のテスト
        FileRecordReader reader = new FileRecordReader(new File("record.dat"), formatFile);
        try{
            reader.read();
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }

        // hasNextメソッドでIOExceptionがスローされる場合のテスト
        try{
            reader.hasNext();
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }
        
    }

    /**
     * {@link InvalidDataFormatException}発生時に、
     * 入力ファイルのパス情報が付与されること。
     */
    @Test
    public void testAddingInputSourceToException() {
        ThreadContext.setRequestId("test");
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./temp")
                                 .addBasePathSetting("format", "file:./temp");
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./temp/format11.fmt");
        /*
        file-type:    "Fixed"
        text-encoding: "UTF-8"
        record-length: 10
        [Default]
        1    dataKbn       X(1)
        2    number        Z(9)
        */

        File dataFile = Hereis.file("./temp/record11.dat");
        //         1         2         3         4         5
        //12345678901234567890123456789012345678901234567890
          /*************************************************
          112345678902NOTNUMBER*/
        //12345678901234567890123456789012345678901234567890
        //

        reader = new FileRecordReader(dataFile, formatFile);
        try {
            while(reader.hasNext()) {
                reader.read();
            }
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid zone bits was specified."));
            assertThat(e.getFieldName(), is("number"));
            assertThat(e.getRecordNumber(), is(2));
            assertThat(e.getFormatFilePath(), endsWith("format11.fmt"));
            assertThat(e.getInputSourcePath(), endsWith("record11.dat"));
        }
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
        if (reader != null) {
            reader.close();
        }
        SystemRepository.clear();
    }
}
