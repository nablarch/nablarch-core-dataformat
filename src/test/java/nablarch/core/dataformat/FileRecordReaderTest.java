package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.core.ThreadContext;
import nablarch.core.dataformat.FormatterFactoryStub.DataRecordFormatterStub;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.launcher.CommandLine;
import nablarch.fw.launcher.Main;
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
     * 5多重でマルチスレッド実行した場合に、それぞれのスレッドが正しいレコード番号を取得できることを確認する。
     * （レコード番号と物理的なレコード番号が一致する）
     */
    @Test
    public void testGetRecordNumber() throws Exception { 
        
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        

        # レコードタイプ定義
        [classifier]
        1 type X

        # データレコード定義
        [header]
        type = "H"
        1  type   X "H"
        2  result   X
        
        [data]
        type = "D"
        1    type   X  "D"
        2    result X  

        [trailer]
        type = "T"
        1    type        X  "T"
        2    result     X   # データレコード件数
        
        
        [terminate]
        type = "Terminate"
        1    type       X  "Terminate"
        2    result     X   # データレコード件数
        ***************************************************/
        formatFile.deleteOnExit();
        
        // FileRecordWriterDisposeHandlerを使用するので、FileRecordWriterのインスタンスはスレッドローカルから削除される。
        File diConfig = Hereis.file("./batch-config.xml");
        /***********************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
          xmlns = "http://tis.co.jp/nablarch/component-configuration">
          
          <!-- データベース接続構成 -->
          <import file="db-default.xml"/>
          
          <!-- ハンドラーキュー構成 -->
          <list name="handlerQueue">
          
            <!-- 共通エラーハンドラー -->
            <component class="nablarch.fw.handler.GlobalErrorHandler" />
              
            <!-- スレッドコンテキスト管理ハンドラ-->
            <component class="nablarch.common.handler.threadcontext.ThreadContextHandler">
              <property name="attributes">
                <list>
                <!-- ユーザID -->
                <component class="nablarch.common.handler.threadcontext.UserIdAttribute">
                  <property name="sessionKey" value="user.id" />
                  <property name="anonymousId" value="9999999999" />
                </component>
                <!-- リクエストID -->
                <component class="nablarch.common.handler.threadcontext.RequestIdAttribute" />
                <!-- 言語 -->
                <component class="nablarch.common.handler.threadcontext.LanguageAttribute">
                  <property name="defaultLanguage" value="ja" />
                </component>
                <!-- 実行時ID -->
                <component class="nablarch.common.handler.threadcontext.ExecutionIdAttribute" />
                </list>
              </property>
            </component>
            
            <!-- データベース接続管理ハンドラ -->
            <component
                name="dbConnectionManagementHandler" 
                class="nablarch.common.handler.DbConnectionManagementHandler">
            </component>

            <!-- 業務アクションディスパッチハンドラ -->
            <component class="nablarch.fw.handler.RequestPathJavaPackageMapping">
              <property name="basePackage" value="nablarch.core.dataformat"/>
              <property name="immediate" value="false" />
            </component>
            
            <!-- FileRecordWriterの後処理を行うハンドラ -->
            <component class="nablarch.core.dataformat.FileRecordWriterDisposeHandlerStub" />
            
            
            <!-- マルチスレッド実行制御ハンドラ -->
            <component class="nablarch.fw.handler.MultiThreadExecutionHandler">
              <property name="concurrentNumber" value="5" />
              <property name="terminationTimeout" value="600" />
            </component>
            
            <!-- データベース接続管理ハンドラ -->
            <component class="nablarch.common.handler.DbConnectionManagementHandler">
            </component>
            
            <!-- ループハンドラ -->
            <component class="nablarch.fw.handler.LoopHandler" />
            
            <!-- データリードハンドラ -->
            <component class="nablarch.fw.handler.DataReadHandler">
            </component>
          </list>
          <!-- ハンドラーキュー構成(END) -->
        </component-configuration>
        ************************************************************************/      
        diConfig.deleteOnExit();
        
        String data  = "H,inoue" + "\n";
        data += "D,10" + "\n";
        data += "D,20" + "\n";
        data += "D,30" + "\n";
        data += "D,40" + "\n";
        data += "D,50" + "\n";
        data += "T,2" + "\n";
       
        FilePathSetting.getInstance()
        .addBasePathSetting("input", "file:" + new File("./").getPath())
        .addBasePathSetting("output", "file:" + new File("./").getPath())
        .addBasePathSetting("format", "file:" + new File("./").getPath())
        .addFileExtensions("input", "dat")
        .addFileExtensions("format", "fmt");
        
        FileOutputStream dest = new FileOutputStream(new File("./test.dat"));
        dest.write(data.getBytes("sjis"));
        dest.close();
        
        CatchingHandler.clear();
        
        CommandLine commandline = new CommandLine(
          "-diConfig",    "file:./batch-config.xml"
        , "-requestPath", "FileRecordReaderRecordNumberTestAction/req00001"
        , "-userId",      "wetherreport"
        );
        
        int exitCode = Main.execute(commandline);
        
        assertEquals(0, exitCode);

       // ファイルが正常に書きだされていることの確認
       BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File("./result.dat")), "sjis"));
       
       
       // ヘッダ、データ、トレイラ
       for(int i = 0; i < 7; i++) {
           String readLine = reader.readLine();
           if(readLine.startsWith("H,レコード番号=[1")){
               assertEquals("H,レコード番号=[1], 物理的に読み込んだレコード番号=[1]", readLine);
           } else if(readLine.startsWith("D,レコード番号=[2")){
               assertEquals("D,レコード番号=[2], 物理的に読み込んだレコード番号=[2]", readLine);
           } else if(readLine.startsWith("D,レコード番号=[3")){
               assertEquals("D,レコード番号=[3], 物理的に読み込んだレコード番号=[3]", readLine);
           } else if(readLine.startsWith("D,レコード番号=[4")){
               assertEquals("D,レコード番号=[4], 物理的に読み込んだレコード番号=[4]", readLine);
           } else if(readLine.startsWith("D,レコード番号=[5")){
               assertEquals("D,レコード番号=[5], 物理的に読み込んだレコード番号=[5]", readLine);
           } else if(readLine.startsWith("D,レコード番号=[6")){
               assertEquals("D,レコード番号=[6], 物理的に読み込んだレコード番号=[6]", readLine);
           } else if(readLine.startsWith("T,レコード番号=[7")){
               assertEquals("T,レコード番号=[7], 物理的に読み込んだレコード番号=[7]", readLine);
           } else {
               fail(readLine);
           }
       }

       // terminate
       assertEquals("Terminate,物理的に読み込んだレコード番号=[7]", reader.readLine());
       
       reader.close();
       
       new File("result.dat").delete();
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
