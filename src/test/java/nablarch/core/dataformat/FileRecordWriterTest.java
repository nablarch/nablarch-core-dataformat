package nablarch.core.dataformat;

import nablarch.core.dataformat.FormatterFactoryStub.DataRecordFormatterStub;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ファイルレコードライタのテスト
 * 
 * 観点：
 * 正常系のテストおよび、writeメソッドで書き出せること、closeメソッドが正常に動作すること、異常系のテストを行う。
 * バッファサイズが設定できることの確認も行う。
 * 
 * @author Masato Inoue
 */
public class FileRecordWriterTest {
    
    @After
    public void setUp() throws Exception {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/FormatterFactory.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    @Test
    public void testInvalidWrite(){
        
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./tmp/format/format1.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [miniData]
        1    byteString   X(10)  # 1. シングルバイト文字列
        11   wordString   N(10)  # 2. ダブルバイト文字列
        21   zoneDigits   Z(10)  # 3. ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        
        File dataFile = new File("test.dat");
        dataFile.deleteOnExit();
        
        try {
            new FileRecordWriter(null, formatFile);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new FileRecordWriter(dataFile, (File) null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        /**
         * データタイプがnull
         */
        FileRecordWriter writer = new FileRecordWriter(dataFile, formatFile);
        try {
            writer.write(null, new DataRecord());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        /**
         * データタイプが空文字
         */
        try {
            writer.write(null, new DataRecord().setRecordType(""));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }
    
    /**
     * シングルレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedSingleLayout() throws Exception {
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./tmp/format/format1.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [miniData]
        1    byteString   X(10)  # 1. シングルバイト文字列
        11   wordString   N(10)  # 2. ダブルバイト文字列
        21   zoneDigits   Z(10)  # 3. ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance().setBasePathSettings(
            new HashMap<String, String>() {{
                put("input",  "file:./");
                put("format", "file:./tmp/format/");
                put("output", "file:./tmp/testdata/");
            }}
        );
        
        Map<String, Object> record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};

        File dataFile = new File("./tmp/testdata/test1.dat");
        new FileRecordWriter(dataFile, new File("./tmp/format/format1.fmt"))
                        .write(record)
                        .close();

        InputStream source = new FileInputStream(dataFile);
            
        
        byte[] byteString = new byte[10];
        source.read(byteString);
        assertEquals("0123456789", new String(byteString, "sjis"));
        
        byte[] wordString = new byte[10];
        source.read(wordString);
        assertEquals("あいうえお", new String(wordString, "sjis"));
        
        assertEquals((byte) 0x31, source.read());
        assertEquals((byte) 0x32, source.read());
        assertEquals((byte) 0x33, source.read());
        assertEquals((byte) 0x34, source.read());
        assertEquals((byte) 0x35, source.read());
        assertEquals((byte) 0x36, source.read());
        assertEquals((byte) 0x37, source.read());
        assertEquals((byte) 0x38, source.read());
        assertEquals((byte) 0x39, source.read());
        assertEquals((byte) 0x30, source.read());
        
        assertEquals(-1, source.read());

        source.close();
        
        formatFile = Hereis.file("./tmp/format2.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

        # データレコード定義
        [miniData]
        1    byteString     X(10)  # 1. シングルバイト文字列
        11   wordString     N(10)  # 2. ダブルバイト文字列
        21   zoneDigits     Z(10)  # 3. ゾーン10進
        ***************************************************/
        
        // デフォルトベースパスを使用する場合
        FilePathSetting.getInstance().addBasePathSetting("output", "file:./tmp")
                                 .addBasePathSetting("format", "file:./tmp");
        dataFile = FilePathSetting.getInstance().getFile("output", "file:test2.dat");
        dataFile.delete();
        
        assertFalse(dataFile.exists());
        
        Map<String, Object> record2 = new DataRecord() {{
            put("byteString", "012345");
            put("wordString", "あいう");
            put("zoneDigits", 123);
        }};
        

        new FileRecordWriter(dataFile, new File("./tmp/format2.fmt"))
                        .write(record)
                        .write(record2)
                        .close();
        
        assertTrue(dataFile.exists());
        
        source = new FileInputStream(dataFile);
        
        // 1件目
        byteString = new byte[10];
        source.read(byteString);
        assertEquals("0123456789", new String(byteString, "sjis"));
        
        wordString = new byte[10];
        source.read(wordString);
        assertEquals("あいうえお", new String(wordString, "sjis"));
        
        assertEquals((byte) 0x31, source.read());
        assertEquals((byte) 0x32, source.read());
        assertEquals((byte) 0x33, source.read());
        assertEquals((byte) 0x34, source.read());
        assertEquals((byte) 0x35, source.read());
        assertEquals((byte) 0x36, source.read());
        assertEquals((byte) 0x37, source.read());
        assertEquals((byte) 0x38, source.read());
        assertEquals((byte) 0x39, source.read());
        assertEquals((byte) 0x30, source.read());
        
        // 2件目
        byteString = new byte[10];
        source.read(byteString);
        assertEquals("012345    ", new String(byteString, "sjis"));
        
        wordString = new byte[10];
        source.read(wordString);
        assertEquals("あいう　　", new String(wordString, "sjis"));
        
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x30, source.read());
        assertEquals((byte) 0x31, source.read());
        assertEquals((byte) 0x32, source.read());
        assertEquals((byte) 0x33, source.read());

        assertEquals(-1, source.read());
        
        source.close();
    }
    
    /**
     * マルチレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedMultiLayoutFile() throws Exception {
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 31
        
        # レコードタイプ識別フィールド定義
        [Classifier] 
        1   layout  X(1)   # データレイアウト(A or B)

        # データレコード定義
        [layoutA]
        layout = "A"
        1    layout       X(1)   "A"
        2    byteString   X(10)  # 1. シングルバイト文字列
        12   wordString   N(10)  # 2. ダブルバイト文字列
        22   zoneDigits   Z(10)  # 3. ゾーン10進
        
        [layoutB]
        layout = "B"
        1    layout      X(1)   "B"
        2    longString  X(20)  # 1. シングルバイト文字列
        22   zoneDigits  Z(10)  # 3. ゾーン10進
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance().addBasePathSetting("output", "file:./")
                                 .addBasePathSetting("format",  "file:./");
            
        Map<String, Object> recordA = new HashMap<String, Object>() {{
            put("layout", "A");
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};
        
        Map<String, Object> recordB = new HashMap<String, Object>() {{
            put("longString", "01234567890123456789");
            put("zoneDigits", 1234567890);
        }};

        new FileRecordWriter(new File("./test.dat"), new File("./format.fmt"))
                        .write(recordA)            //フィールドの値でレコードタイプを自動判定
                        .write("layoutB", recordB) //レコードタイプを明示的に指定して書き込む
                        .close();
        
        InputStream source = new FileInputStream("./test.dat");
        
        // 1件目
        byte[] byte10 = new byte[10];
        assertEquals('A', (char)source.read());
        source.read(byte10);
        assertEquals("0123456789", new String(byte10, "sjis"));
        source.read(byte10);
        assertEquals("あいうえお", new String(byte10, "sjis"));
        
        source.read(byte10);
        
        // 2件目
        byte10 = new byte[10];
        assertEquals('B', (char)source.read());
        source.read(byte10);
        assertEquals("0123456789", new String(byte10, "sjis"));
        source.read(byte10);
        assertEquals("0123456789", new String(byte10, "sjis"));
        
        source.read(byte10);
        
        assertEquals(-1, source.read());
        

        Map<String, Object> recordBInvalid = new HashMap<String, Object>() {{
            put("layout", "B");
            put("longString", "01234567890123456789");
            put("zoneDigits", 1234567890);
        }};

        try {
            //本来とは異なるレコードタイプを指定して出力
            new FileRecordWriter(new File("./test.dat"), new File("format.fmt"))
            .write("layoutA", recordBInvalid);;
                            
            fail();
        } catch (Exception e) {
            assertEquals(InvalidDataFormatException.class, e.getClass());
        }
    }

    /**
     * マルチレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedMultiLayoutFileClassifierBigDecimal() throws Exception {
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type: "Fixed"

         # 文字列型フィールドの文字エンコーディング
         text-encoding: "sjis"

         # 各レコードの長さ
         record-length: 15

         # レコードタイプ識別フィールド定義
         [Classifier] 
         1   layout  X(12)   # データレイアウト

         # データレコード定義
         [layoutA]
         layout = "0.0000000001"
         1    layout       X(12)
         13    data         X(3)

         [layoutB]
         layout = "0.0000000002"
         1    layout      X(12)  
         13    data1       X(1)
         14    data2       X(2)
         ***************************************************/
        formatFile.deleteOnExit();

        FilePathSetting.getInstance()
                       .addBasePathSetting("output", "file:./")
                       .addBasePathSetting("format", "file:./");

        Map<String, Object> record1 = new HashMap<String, Object>();
        record1.put("layout", new BigDecimal("0.0000000001"));
        record1.put("data", "123");
        
        Map<String, Object> record2 = new HashMap<String, Object>();
        record2.put("layout", new BigDecimal("0.0000000002"));
        record2.put("data1", "1");
        record2.put("data2", "12");


        new FileRecordWriter(new File("./test.dat"), new File("./format.fmt"))
                .write(record1)            //フィールドの値でレコードタイプを自動判定
                .write(record2)
                .close();

        InputStream source = new FileInputStream("./test.dat");

        // 1件目
        byte[] record = new byte[15];
        source.read(record);
        assertEquals("0.0000000001123", new String(record, "sjis"));
        record = new byte[15];
        source.read(record);
        assertEquals("0.0000000002112", new String(record, "sjis"));

        assertEquals(-1, source.read());
    }

    /**
     * コンストラクタの引数がファイルオブジェクトのパターン。
     */
    @Test
    public void testFixedConstructorArgFile() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Fixed"

        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 30

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

        new FileRecordWriter(dataFile, new File("format.fmt"))
                        .write(record)
                        .write(record)
                        .write(record)
                        .close();      // 3件出力
        
        assertTrue(dataFile.exists());
        
        InputStream source = new FileInputStream(dataFile);
        
        byte[] binaryRecord = new byte[30];
        source.read(binaryRecord);
        source.read(binaryRecord);
        source.read(binaryRecord);
        assertEquals(-1, source.read());
        
        source.close();
    }
    

    /**
     * バッファサイズが設定されることの確認。
     * メソッドを呼んで読み込みが正常にできることと、不正な値を設定した場合に例外がスローされることを確認する。
     */
    @Test
    public void testSetBufferSize() throws Exception {
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
        FilePathSetting.getInstance().addBasePathSetting("output",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        
        /**
         * バッファサイズを50にして正しく読めること。
         */
        FileRecordWriter writer = new FileRecordWriter(new File("./buffer.dat"), new File("test.fmt"), 50);
        writer.write(new DataRecord(){{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456781"));
            }});
        writer.write(new DataRecord(){{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456782"));
            }});
        writer.write(new DataRecord(){{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456783"));
            }});
        writer.close();
        
        InputStream in = new FileInputStream("./buffer.dat");
        
        byte[] recordBytes = new byte[90];
        in.read(recordBytes);
        
        assertTrue(in.read() == -1);
        ByteBuffer recordBuff = ByteBuffer.wrap(recordBytes);
        
        byte[] fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("0123456789", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("abcdefghij", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("0123456781", new String(fieldBytes, "sjis"));
        recordBuff.get(fieldBytes);
        assertEquals("0123456789", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("abcdefghij", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("0123456782", new String(fieldBytes, "sjis"));
        recordBuff.get(fieldBytes);
        assertEquals("0123456789", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("abcdefghij", new String(fieldBytes, "sjis"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertEquals("0123456783", new String(fieldBytes, "sjis"));
        
        new File("buffer.dat").deleteOnExit();
        
        /**
         * バッファサイズを0にすると例外が発生すること
         */
        try {
            writer = new FileRecordWriter(new File("./buffer.dat"), new File("test.fmt"), 0);
            fail();
        } catch (IllegalArgumentException e) {

        }
        
    }
    

    /**
     * IOExceptionがスローされるパターン。
     */
    @Test
    public void testException() throws Exception {
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
        FileRecordWriter writer = new FileRecordWriter(new File("record.dat"), formatFile);
        try{
            writer.write(new DataRecord());
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }
        
        SystemRepository.clear();
    }

    /**
     * 引数が不正なテスト。
     */
    @Test
    public void testIllegalArgs() throws Exception {
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
        
        // 引数が空文字
        FileRecordWriter writer = new FileRecordWriter(new File("record.dat"), formatFile);
        try{
            writer.write("", new DataRecord());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("record type was blank. record type must not be blank.", e.getMessage());
        }

        // 引数がnull
        try{
            writer.write(null, new DataRecord());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("record type was blank. record type must not be blank.", e.getMessage());
        }
        
        
        SystemRepository.clear();
    }
    
    
    /**
     * 本クラスのクローズメソッド実行時に、DataRecordFormatterのクローズメソッドが呼ばれることの確認。
     */
    @Test
    public void testClose() throws Exception{

        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        FilePathSetting.getInstance().addBasePathSetting("output",  "file:./")
        .addBasePathSetting("format", "file:./");

        File formatFile = Hereis.file("./close.fmt");
        /**********************************************
        aa
        ***************************************************/
        formatFile.deleteOnExit();

        FileRecordWriter writer = new FileRecordWriter(new File("./close.dat"), new File("close.fmt"));
        writer.write(new HashMap<String, String>());
        
        File dataFile = new File("./close.dat");
        assertTrue(dataFile.exists()); 
        dataFile.delete();
        assertTrue(dataFile.exists()); // クローズされていないので削除できない
        
        assertFalse(DataRecordFormatterStub.isCallClose);
        
        writer.close();

        assertTrue(DataRecordFormatterStub.isCallClose); // フォーマッタのクローズメソッドが呼ばれたことの確認

        dataFile.delete();
        assertFalse(dataFile.exists()); // クローズされているので削除できることの確認
        
        SystemRepository.clear();
        
        new File("input/record.dat").deleteOnExit();

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
        SystemRepository.clear();
    }
}
