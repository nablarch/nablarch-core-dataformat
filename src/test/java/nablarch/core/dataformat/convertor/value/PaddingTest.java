package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * パディングコンバータのテストケース。
 * 
 * 観点：
 * パディング処理は、実際の処理を委譲するDoubleByteCharacterTestクラス、SingleByteCharacterTestクラスのテストで確認するので、
 * ここでは異常系の網羅のみ行う。
 * 
 * @author Masato Inoue
 */
public class PaddingTest {

    /**
     * パディング文字のパラメータを２つ設定した場合に例外がスローされることの確認。
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
        1    doubleByteString     N(20) pad("0", "1")  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        
        DataRecord dataRecord = new DataRecord(){{
            put("doubleByteString", "あいうえお");
        }};
        
        try {
            formatter.setOutputStream(outputStream).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter size was invalid. parameter size must be one, " +
                            "but was [2]. parameter=[0, 1]. convertor=[Padding]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }
    }
    

    /**
     * パディング文字のパラメータが存在しない場合に例外がスローされることの確認。
     */
    @Test
    public void testInvalidParameter() throws Exception {
        
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
        1    doubleByteString     N(20) pad()  # 全角文字
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        
        DataRecord dataRecord = new DataRecord(){{
            put("doubleByteString", "あいうえお");
        }};
        
        try {
            formatter.setOutputStream(outputStream).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter size was invalid. parameter size must be one, " +
                            "but was [0]. parameter=[]. convertor=[Padding]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }

        /**
         * 引数がnull。
         */
        Padding padding = new Padding();
        try {
            padding.initialize(null, new Object[] { null });
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(true);
        }
    }
    
    /**
     * コンバートメソッドがサポートされていないことのテスト。
     */
    @Test
    public void testWriteDoubleByte() throws Exception {
        
        Padding padding = new Padding();
        
        try {
            padding.convertOnRead("abc");
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
        try {
            padding.convertOnWrite("abc");
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
 
    }
}
