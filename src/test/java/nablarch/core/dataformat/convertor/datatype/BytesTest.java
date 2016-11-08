package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * バイト型コンバータのテスト。
 * 
 * 観点：
 * 正常系はフォーマッタのテストで確認しているので、ここではBytesクラスで発生する例外系を網羅する。
 * 
 * @author Masato Inoue
 */
public class BytesTest {

    private DataRecordFormatter formatter = null;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }
    
    /**
     * 正常系の、バイト配列の出力テスト。
     * CI上でconvertOnWriteメソッドのカバレッジが通っていなかったので、念のため追加。
     */
    @Test
    public void testWrite() throws Exception {
        
        // レイアウト定義ファイルのbyte型に不正な値を設定する 
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 3

        # データレコード定義
        [Default]
        1    byteString     B(3)   # バイト列
        ***************************************************/
        formatFile.deleteOnExit();

        OutputStream stream = new BufferedOutputStream(new FileOutputStream("test.dat"));
        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        formatter.setOutputStream(stream).initialize();
        DataRecord record = new DataRecord();
        record.put("byteString", "abc".getBytes());

        formatter.writeRecord(record);

        FileInputStream inputStream = new FileInputStream("test.dat");
        byte[] buffer = new byte[3];
        inputStream.read(buffer);
       
        assertEquals("abc", new String(buffer, "sjis"));
    }

    /**
     * バイト長が違った場合のテスト。
     */
    @Test
    public void testWriteInvalidByteLength() throws Exception {
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type:    "Fixed"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "sjis"
         # 各レコードの長さ
         record-length: 2

         # データレコード定義
         [Default]
         1    byteString     B(2)   # バイト列
         ***************************************************/
        formatFile.deleteOnExit();

        OutputStream stream = new BufferedOutputStream(new FileOutputStream("test.dat"));
        DataRecordFormatter formatter =
                FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        formatter.setOutputStream(stream).initialize();
        DataRecord record = new DataRecord();
        record.put("byteString", "abc".getBytes());

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter length = [3], expected = [2].");

        formatter.writeRecord(record);

        // レイアウト定義ファイル
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type:    "Fixed"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "sjis"
         # 各レコードの長さ
         record-length: 4

         # データレコード定義
         [Default]
         1    byteString     B(4)   # バイト列
         ***************************************************/
        formatFile.deleteOnExit();

        stream = new BufferedOutputStream(new FileOutputStream("test.dat"));
        formatter =
                FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        formatter.setOutputStream(stream).initialize();
        record = new DataRecord();
        record.put("byteString", "abc".getBytes());

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter length = [3], expected = [4].");

        formatter.writeRecord(record);
    }
    
    /**
     * レイアウト定義ファイルに不正なバイト列のパラメータを設定した場合、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout() throws Exception {

        // argsが0
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    byteString     B()   # バイト列（不正な値）
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        InputStream source = new ByteArrayInputStream(
                "testtesttest".getBytes("sjis"));

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter was not specified. parameter must be specified. convertor=[Bytes]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }
        formatter.close();
        
        
        // レイアウト定義ファイルのbyte型に不正な値を設定する 
        // レイアウト定義ファイル
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    byteString     B("a")   # バイト列（不正な値）
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");
        
        source = createInputStreamFrom("testtesttest");

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. parameter type must be 'Integer' " +
                            "but was: 'java.lang.String'. parameter=[a]. convertor=[Bytes]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        } finally {
            formatter.close();
        }

        
        
        Bytes bytes = new Bytes();
        try {
            bytes.initialize(null, null);
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("1st parameter was null. parameter=[null, null]. convertor=[Bytes].", e.getMessage());
        }
    }
    

    /**
     * 出力時にバイト以外のデータを渡した場合。
     */
    @Test
    public void testInvalidDataType() throws Exception {
        
        // レイアウト定義ファイルのbyte型に不正な値を設定する 
        // レイアウト定義ファイル
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    byteString     B(10)   # バイト列
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./");

        OutputStream stream = new ByteArrayOutputStream();
        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        formatter.setOutputStream(stream).initialize();
        DataRecord record = new DataRecord();
        record.put("byteString", "abc");
        
        try {
            formatter.writeRecord(record);
            fail();
        } catch(InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                       "invalid parameter type was specified. parameter must be a byte array."));
            assertThat(e.getFieldName(), is("byteString"));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFormatFilePath(), containsString("format.fmt"));
        }
    }

    /**
     * 出力時にパラメータがnullまたは空白の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() {
        Bytes bytes = new Bytes();
        bytes.init(new FieldDefinition(), 10);
        byte[] expected = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00, 0x00};
        assertThat(bytes.convertOnWrite(null), is(expected));

        assertThat(bytes.convertOnWrite(""), is(expected));
    }

    /**
     * 入力時にパラメータが空白の場合のテスト。
     * 固定長を扱うため、nullがわたされることはないため考慮しない。
     */
    @Test
    public void testReadParameterEmpty() {
        Bytes bytes = new Bytes();
        bytes.init(new FieldDefinition(), 0);

        assertThat(bytes.convertOnRead("".getBytes()), is("".getBytes()));
    }

}
