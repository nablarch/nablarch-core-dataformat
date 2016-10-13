package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * ダブルバイト文字のコンバートテスト。
 * 
 * 観点：
 * 正常系はフォーマッタのテストで確認しているので、ここではオプション設定関連のテストを行う。
 *   ・全角文字のパディング、トリムのテスト。
 *   ・任意のパディング、トリム文字の設定テスト。
 * 
 * @author Masato Inoue
 */
public class DoubleByteCharacterStringTest {

    private DataRecordFormatter formatter = null;
    
    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }
    
    /**
     * 全角文字が正しくトリムされることのテスト。
     */
    @Test
    public void testTrim() throws Exception {
        
        /**
         * トリム文字を未指定
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1    doubleByteString     N(20)  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        InputStream source = new ByteArrayInputStream("０１２３４　　　　　".getBytes("ms932"));

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setInputStream(source).initialize();
        
        DataRecord readRecord = formatter.readRecord();
        assertEquals("０１２３４", readRecord.get("doubleByteString"));
        
        formatter.close();
        
        /**
         * トリム文字を「"０"」に指定
         */
        formatFile = Hereis.file("./format2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1    doubleByteString     N(20) pad("０") # "0"でトリム
        ***************************************************/
        formatFile.deleteOnExit();

        source = new ByteArrayInputStream("０１２３４０００００".getBytes("ms932"));

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setInputStream(source).initialize();
        
        readRecord = formatter.readRecord();
        assertEquals("０１２３４", readRecord.get("doubleByteString"));

    }

    /**
     * 全角文字が正しくパディングされることのテスト。
     */
    @Test
    public void testPading() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1    doubleByteString     N(20)  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setOutputStream(outputStream).initialize();
        
        DataRecord dataRecord = new DataRecord(){{
            put("doubleByteString", "あいうえお");
        }};
        
        formatter.writeRecord(dataRecord);
        formatter.close();
        
        FileInputStream inputStream = new FileInputStream("test.dat");
        byte[] buffer = new byte[20];
        inputStream.read(buffer);
       
        
        assertEquals("あいうえお　　　　　", new String(buffer, "ms932"));
        
        
        /**
         * パディング文字を「"０"」に指定
         */
        formatFile = Hereis.file("./format2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1    doubleByteString     N(20) pad("１") # "1"でパディング
        ***************************************************/
        formatFile.deleteOnExit();

        outputStream = new FileOutputStream("test.dat");

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setOutputStream(outputStream).initialize();
        
        dataRecord = new DataRecord(){{
            put("doubleByteString", "あいうえお");
        }};
        
        formatter.writeRecord(dataRecord);
        formatter.close();
        
        inputStream = new FileInputStream("test.dat");
        buffer = new byte[20];
        inputStream.read(buffer);
       
        
        assertEquals("あいうえお１１１１１", new String(buffer, "ms932"));
    }
    

    /**
     * 出力時にパラメータがnullまたは空白の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() throws Exception {
        DoubleByteCharacterString doubleByteCharacter = new DoubleByteCharacterString();
        doubleByteCharacter.init(new FieldDefinition().setEncoding(Charset.forName("MS932")), new Object[]{10});
        assertThat("　　　　　".getBytes("MS932"), is(doubleByteCharacter.convertOnWrite(null)));
        assertThat("　　　　　".getBytes("MS932"), is(doubleByteCharacter.convertOnWrite("")));
    }
    
    
}
