package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * エンコーディングの設定テスト。
 * 
 * 観点：
 * 特定のフィールドが正しくエンコーディングできることの確認。
 * 
 * @author Masato Inoue
 */
public class UseEncodingTest {

    private DataRecordFormatter formatter = null;
    
    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }
    
    /**
     * ms932とeuc-jpのフィールドが混在する場合に正常に読めることのテスト。
     */
    @Test
    public void testEncoding() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 40

        # データレコード定義
        [Default]
        1    doubleByteString   N(20)   # 全角文字
        21   doubleByteString2  N(20) encoding("euc-jp")  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        byte[] bytes = new byte[40];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        
        buffer.put("０１２３４５６７８９".getBytes("ms932")); 
        buffer.put("あいうおえかきくけこ".getBytes("euc-jp")); 
        
        InputStream source = new ByteArrayInputStream(buffer.array());
        
        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setInputStream(source).initialize();
        
        DataRecord readRecord = formatter.readRecord();
        assertEquals("０１２３４５６７８９", readRecord.get("doubleByteString"));
        assertEquals("あいうおえかきくけこ", readRecord.get("doubleByteString2"));


        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 40
        
        [Classifier] # レコードタイプ識別フィールド定義              
        21 doubleByteString2   N(20) encoding("euc-jp")  

        # データレコード定義
        [Default]
        doubleByteString2 = "あいうえおかきくけこ"
        1    doubleByteString   N(20)   # 全角文字
        21   doubleByteString2  N(20) encoding("euc-jp")  # 全角文字
        ***************************************************/

        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        bytes = new byte[40];
        buffer = ByteBuffer.wrap(bytes);
        
        buffer.put("０１２３４５６７８９".getBytes("ms932")); 
        buffer.put("あいうえおかきくけこ".getBytes("euc-jp")); 
        
        
        source = new ByteArrayInputStream(buffer.array());

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setInputStream(source).initialize();
        
        readRecord = formatter.readRecord();
        assertEquals("０１２３４５６７８９", readRecord.get("doubleByteString"));
        assertEquals("あいうえおかきくけこ", readRecord.get("doubleByteString2"));

    }
        


    /**
     * 引数が不正なことのテスト。
     */
    @Test
    public void testIllegalParameter() throws Exception {
        
        /**
         * 引数が空文字
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 40

        # データレコード定義
        [Default]
        1    doubleByteString   N(20)   # 全角文字
        21   doubleByteString2  N(20) encoding()  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        byte[] bytes = new byte[40];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        
        buffer.put("０１２３４５６７８９".getBytes("ms932")); 
        buffer.put("あいうおえかきくけこ".getBytes("euc-jp")); 
        
        InputStream source = new ByteArrayInputStream(buffer.array());
        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter was not specified. parameter must be specified. convertor=[UseEncoding]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }
        
        formatter.close();

        /**
         * 引数が文字列でない
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 40

        # データレコード定義
        [Default]
        1    doubleByteString   N(20)   # 全角文字
        21   doubleByteString2  N(20) encoding(2)  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        bytes = new byte[40];
        buffer = ByteBuffer.wrap(bytes);
        
        buffer.put("０１２３４５６７８９".getBytes("ms932")); 
        buffer.put("あいうおえかきくけこ".getBytes("euc-jp")); 
        
        source = new ByteArrayInputStream(buffer.array());
        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. " +
                            "parameter type must be 'String' but was: 'java.lang.Integer'. " +
                            "parameter=[2]. convertor=[UseEncoding]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }

    }

    /**
     * サポートしてないメソッドを実行した場合に例外がスローされるテスト。
     */
    @Test
    public void testUnsupported(){
        UseEncoding useEncoding = new UseEncoding();
        try {
            useEncoding.convertOnRead(null);
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
        try {
            useEncoding.convertOnWrite(null);
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }
    
    
    /**
     * 初期化時のパラメータ不正テスト。
     */
    @Test
    public void initializeArgError(){
        
        /**
         * 引数がnull。
         */
        UseEncoding encoding = new UseEncoding();
        try {
            encoding.initialize(new FieldDefinition(), null, "hoge");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("1st parameter was null. parameter=[null, hoge]. convertor=[UseEncoding].", e.getMessage());
        }
        
    }
    
}
