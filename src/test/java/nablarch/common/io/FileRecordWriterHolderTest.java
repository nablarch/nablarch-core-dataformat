package nablarch.common.io;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FileRecordWriter;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import mockit.Mocked;
import mockit.Verifications;

/**
 * FileRecordWriterをスレッド上で保持するクラスのテスト。
 * 
 * 観点：
 * 正常系、異常系の網羅。
 * 
 * @author Masato Inoue
 */
public class FileRecordWriterHolderTest {

    @Before
    @SuppressWarnings("serial")
    public void setUp() throws Exception {
        FileRecordWriterHolder.closeAll();
        SystemRepository.clear();
        FilePathSetting.getInstance().setBasePathSettings(
                new HashMap<String, String>() {{
                    put("input",  "file:./");
                    put("format", "file:./");
                    put("output", "file:./");
                }}
        ).addFileExtensions("format", "fmt");

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
         2  name   X

         [data]
         type = "D"
         1    type   X  "D"
         2    amount X

         [trailer]
         type = "T"
         1    type        X  "T"
         2    records     X   # データレコード件数
         3    totalAmount X   # 合算値
         ***************************************************/
        formatFile.deleteOnExit();
    }

    /**
     * クローズが正常に行われることの確認。
     */
    @Test
    public void testClose() throws Exception {

        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }
        
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        [data]
        1    type   X  "D"
        2    amount X  
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance()
        .addBasePathSetting("output", "file:./")
        .addBasePathSetting("format", "file:./");
        
        /**
         * closeメソッドで削除する場合。
         */
        FileRecordWriterHolder.open("test.dat", "test2");
        FileRecordWriterHolder.open("test2.dat", "test2");
        FileRecordWriter writer = FileRecordWriterHolder.get("test.dat");
        writer.write(new HashMap<String, Object>() {{
            put("type", "test1");
            put("amount", "test100");
        }});
        FileRecordWriter writer2 = FileRecordWriterHolder.get("test2.dat");
        writer2.write(new HashMap<String, Object>() {{
            put("type", "test2");
            put("amount", "test200");
        }});
        
        File outputFile = new File("./test.dat");
        File outputFile2 = new File("./test2.dat");
        assertFalse(outputFile.delete()); // クローズされていないのでファイルの削除に失敗する
        assertFalse(outputFile2.delete()); // クローズされていないのでファイルの削除に失敗する

        FileRecordWriterHolder.close("test.dat");
        FileRecordWriterHolder.close("test2.dat");
        
        assertTrue(outputFile.delete()); // クローズされているのでファイルの削除に成功する
        assertTrue(outputFile2.delete()); // クローズされているのでファイルの削除に成功する
           
        
        /**
         * closeAllメソッドで削除するパターン。
         */
        FileRecordWriterHolder.open("test.dat", "test2");
        FileRecordWriterHolder.open("test2.dat", "test2");
        writer = FileRecordWriterHolder.get("test.dat");
        writer.write(new HashMap<String, Object>(){{
            put("type", "test1");
            put("amount", "test100");
        }});
        writer2 = FileRecordWriterHolder.get("test2.dat");
        writer2.write(new HashMap<String, Object>(){{
            put("type", "test2");
            put("amount", "test200");
        }});
        
        outputFile = new File("./test.dat");
        outputFile2 = new File("./test2.dat");
        assertFalse(outputFile.delete()); // クローズされていないのでファイルの削除に失敗する
        assertFalse(outputFile2.delete()); // クローズされていないのでファイルの削除に失敗する

        FileRecordWriterHolder.closeAll();
        
        assertTrue(outputFile.delete()); // クローズされているのでファイルの削除に成功する
        assertTrue(outputFile2.delete()); // クローズされているのでファイルの削除に成功する
    }
    

    /**
     * マルチレイアウトのwriteメソッド実行確認。
     */
    @Test
    public void testWriteMultiLayout() throws Exception {

        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test2.fmt");
        /**********************************************
        
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        [Classifier] # レコードタイプ識別フィールド定義              
        1 type   X
 
        [data]
        1    type   X  
        2    amount X  
        
        [header]
        1    abc   X  
        2    def   X  
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance()
        .addBasePathSetting("output", "file:./")
        .addBasePathSetting("format", "file:./");

        FileRecordWriterHolder.open("test.dat", "test2", 8000);
        DataRecord record = new DataRecord(){{
            put("type", "abc");
            put("amount", "123");
        }};
        FileRecordWriterHolder.write("data", record, "test.dat");
        FileRecordWriterHolder.close("test.dat");

        // result.datファイルが正常に書きだされていることの確認
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("./test.dat"), "sjis"));
        assertEquals("abc,123", reader.readLine());
        reader.close();
        
    }
    
    /**
     * バッファサイズが設定できることの確認。
     */
    @Test
    public void testBuffer() throws Exception {

        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        [data]
        1    type   X  "D"
        2    amount X  
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance()
        .addBasePathSetting("output", "file:./")
        .addBasePathSetting("outputTest", "file:./")
        .addBasePathSetting("format", "file:./");

        // バッファサイズを設定
        FileRecordWriterHolder.open("test.dat", "test2", 8000);
        DataRecord record = new DataRecord(){{
            put("type", "abc");
            put("amount", "123");
        }};
        FileRecordWriterHolder.write(record, "test.dat");
        FileRecordWriterHolder.close("test.dat");

        // result.datファイルが正常に書きだされていることの確認
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("./test.dat"), "sjis"));
        assertEquals("abc,123", reader.readLine());
        reader.close();
        

        // 不正なバッファサイズを設定
        try{
            FileRecordWriterHolder.open("test2.dat", "test2", -1);
            fail();
        }catch(IllegalArgumentException e){
            assertTrue(true);
        }
    }
    
    
    /**
     * basePathNameに&が設定された場合のテスト。
     */
    @Test
    public void testInvalidKey() throws Exception {

        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        [data]
        1    type   X  "D"
        2    amount X  
        ***************************************************/
        formatFile.deleteOnExit();

        // basePathNameに&を設定
        try{
            FileRecordWriterHolder.open("hog&e", "test2.dat", "test", "test2", -1);
            fail();
        }catch(IllegalArgumentException e){
            assertTrue(true);
        }
    }

    /**
     * 存在しないレイアウトファイルが指定された場合のテスト。
     */
    @Test
    public void testInvalidLayoutFile() throws Exception {

        // basePathNameに&を設定
        try{
            FileRecordWriterHolder.open("test.dat", "nonExist");
            fail();
        }catch(IllegalArgumentException e){
            String message = e.getMessage();
            assertThat(message, containsString("invalid layout file path was specified."));
            assertThat(message, containsString("file path=["));
            assertThat(message, containsString(new File("./nonExist.fmt").getAbsolutePath()));
        }
    }
    
    /**
     * 引数が空のテスト。
     */
    @Test
    public void testBlank() {

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
        
        [data]
        1    type   X  "D"
        2    amount X  
        ***************************************************/
        formatFile.deleteOnExit();
        
        
        /**
         * レイアウトファイルのパスを設定しない場合、例外がスローされる。
         */
        try {
            FileRecordWriterHolder.open("record.dat", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("layout file name was blank. layout file name must not be blank.", e.getMessage());
        }
        
        /**
         * データファイルのパスを設定しない場合、例外がスローされる。
         */
        try {
            FileRecordWriterHolder.open(null, "test");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("data file name was blank. data file name must not be blank.", e.getMessage());
        }
            
        
        /**
         * レイアウトファイルのベースパスを設定しない場合、例外がスローされる。
         */
        try {
            FileRecordWriterHolder.open("input", "record.dat", null, "test");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("layout file base path name was blank. layout file base path name must not be blank.", e.getMessage());
        }
        
        
        /**
         * データファイルのベースパスを設定しない場合、例外がスローされる。
         */
        try {
            FileRecordWriterHolder.open(null, "record.dat", "format", "test");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("data file base path name was blank. data file base path name must not be blank.", e.getMessage());
        }
    }

    public void createDataFile() throws Exception {
        String data;
        data  = "H,inoue" + "\n";
        data += "D,10" + "\n";
        data += "D,20" + "\n";
        data += "D,50" + "\n";
        data += "T,2,80" + "\n";

        new FileOutputStream("./test.dat").write(data.getBytes("sjis"));
    }

    /**
     * リポジトリからFileRecordWriterHolderのサブクラスのインスタンスを取得するテスト。
     */
    @Test
    public void testHolderGetFromRepository() throws Exception {

        // FileRecordWriterDisposeHandlerを使用するので、FileRecordWriterのインスタンスはスレッドローカルから削除される。
        File diConfig = Hereis.file("./batch-config.xml");
        /***********************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
          xmlns = "http://tis.co.jp/nablarch/component-configuration">

          <!-- FormatterFactoryの設定 -->
          <component name="fileRecordWriterHolder"
              class="nablarch.common.io.FileRecordWriterHolderStub">
          </component>

        </component-configuration>
        ************************************************************************/
        diConfig.deleteOnExit();
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                diConfig.toURI().toString());
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        FileRecordWriterHolder.open("test", "./test.fmt");
        FileRecordWriter writer = FileRecordWriterHolder.get("test");
        
        // FileRecordWriterのスタブを生成できている
        assertSame(writer.getClass(), FileRecordWriterHolderStub.FileRecordWriterStub.class);
    }

    
    /**
     * 不正なFileRecordWriterHolderの使い方をした場合、例外がスローされる。
     * @throws Exception
     */
    @Test
    public void testCloseBeforeOpened() throws Exception {

        /**
         * クローズしている状態でライタのインスタンスを取得しようとした場合、例外がスローされる。
         */
        try {
            FileRecordWriterHolder.get("result.dat");
            fail();
        } catch(IllegalStateException e) {
            // FileRecordWriterのインスタンスがスレッドから削除されているので、例外がスローされる
        }        
        try {
            FileRecordWriterHolder.get("result2.dat");
            fail();
        } catch(IllegalStateException e) {
            // FileRecordWriterのインスタンスがスレッドから削除されているので、例外がスローされる
        }


        /**
         * ２回オープンする場合、例外がスローされる。
         */
        FileRecordWriterHolder.open("test.dat", "test");

        try {
            FileRecordWriterHolder.open("test.dat", "test");
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    /**
     * クローズを２回実行しても例外が発生しないことの確認。
     */
    @Test
    public void testCloseDouble() throws Exception {

        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./test2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type: "Variable"
        
        # 文字列型フィールドの文字エンコーディング
        record-separator: "\n"
        
        # 文字列型フィールドの文字エンコーディング
        field-separator: ","
        
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        [data]
        1    type   X  "D"
        2    amount X  
        ***************************************************/
        formatFile.deleteOnExit();
        
        FilePathSetting.getInstance()
        .addBasePathSetting("output", "file:./")
        .addBasePathSetting("format", "file:./");
        
        FileRecordWriterHolder.open("test.dat", "test2");
        FileRecordWriterHolder.close("test.dat");
        FileRecordWriterHolder.close("test.dat");
        
    }

    /**
     * 子スレッドで開いたファイルを親スレッドで閉じることができること
     */
    @Test
    @Ignore("jacoco と jmockit が競合してエラーになるため")
    public void testMultiThread(@Mocked final FileRecordWriter writer) throws Exception {
        FileRecordWriterHolder.init();
        FilePathSetting.getInstance()
                .addBasePathSetting("output","file:./")
                .addBasePathSetting("format", "file:./");

        // 子スレッド内でファイルを開く
        ExecutorService service = Executors.newFixedThreadPool(2);
        Future future1 = service.submit(new Runnable() {
            @Override
            public void run() {
                FileRecordWriterHolder.open("test1.dat", "test");
            }
        });
        Future future2 = service.submit(new Runnable() {
            @Override
            public void run() {
                FileRecordWriterHolder.open("test2.dat", "test");
            }
        });

        // 子スレッドの処理が完了するまで待機
        future1.get();
        future2.get();

        FileRecordWriterHolder.close("test1.dat");
        FileRecordWriterHolder.close("test2.dat");

        // Writerのクローズ処理が2回呼ばれていること
        new Verifications() {{
            writer.close();
            times = 2;
        }};

        service.shutdown();
    }

    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }
}
