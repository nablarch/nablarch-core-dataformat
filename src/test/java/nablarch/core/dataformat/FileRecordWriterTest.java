package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;

import nablarch.core.dataformat.FormatterFactoryStub.DataRecordFormatterStub;
import nablarch.core.util.FileUtil;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * ファイルレコードライタのテスト
 * <p>
 * 観点：
 * 正常系のテストおよび、writeメソッドで書き出せること、closeメソッドが正常に動作すること、異常系のテストを行う。
 * バッファサイズが設定できることの確認も行う。
 *
 * @author Masato Inoue
 */
public class FileRecordWriterTest {

    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource(
            "nablarch/core/dataformat/FormatterFactory.xml");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testInvalidWrite_dataFileNull() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testInvalidWrite.fmt");

        expectedException.expect(IllegalArgumentException.class);
        new FileRecordWriter(null, new File(url.toURI()));
    }

    @Test
    public void testInvalidWrite_layoutFileNull() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new FileRecordWriter(temporaryFolder.newFile(), (File) null);
    }

    @Test
    public void testInvalidWrite_dataTypeNull() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testInvalidWrite.fmt");
        FileRecordWriter writer = new FileRecordWriter(temporaryFolder.newFile(), new File(url.toURI()));

        expectedException.expect(IllegalArgumentException.class);
        writer.write(null, new DataRecord());
    }

    @Test
    public void testInvalidWrite_DataTypeEmpty() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testInvalidWrite.fmt");
        FileRecordWriter writer = new FileRecordWriter(temporaryFolder.newFile(), new File(url.toURI()));

        expectedException.expect(IllegalArgumentException.class);
        writer.write("", new DataRecord());
    }

    /**
     * シングルレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedSingleLayout1() throws Exception {

        Map<String, Object> record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedSingleLayout1.fmt");
        File dataFile = temporaryFolder.newFile();
        new FileRecordWriter(dataFile, new File(url.toURI()))
                .write(record)
                .close();

        InputStream source = new FileInputStream(dataFile);
        byte[] byteString = new byte[10];
        source.read(byteString);
        assertThat(new String(byteString, "sjis"), is("0123456789"));

        byte[] wordString = new byte[10];
        source.read(wordString);
        assertThat(new String(wordString, "sjis"), is("あいうえお"));

        assertThat(source.read(), is(0x31));
        assertThat(source.read(), is(0x32));
        assertThat(source.read(), is(0x33));
        assertThat(source.read(), is(0x34));
        assertThat(source.read(), is(0x35));
        assertThat(source.read(), is(0x36));
        assertThat(source.read(), is(0x37));
        assertThat(source.read(), is(0x38));
        assertThat(source.read(), is(0x39));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(-1));
        source.close();
    }

    @Test
    public void testFixedSingleLayout2() throws Exception {
        Map<String, Object> record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};
        Map<String, Object> record2 = new DataRecord() {{
            put("byteString", "012345");
            put("wordString", "あいう");
            put("zoneDigits", 123);
        }};

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedSingleLayout2.fmt");
        final File dataFile = temporaryFolder.newFile();
        new FileRecordWriter(dataFile, new File(url.toURI()))
                .write(record)
                .write(record2)
                .close();

        assertThat(dataFile.exists(), is(true));

        final InputStream source = new FileInputStream(dataFile);

        // 1件目
        byte[] byteString = new byte[10];
        source.read(byteString);
        assertThat(new String(byteString, "sjis"), is("0123456789"));

        byte[] wordString = new byte[10];
        source.read(wordString);
        assertThat(new String(wordString, "sjis"), is("あいうえお"));

        assertThat(source.read(), is(0x31));
        assertThat(source.read(), is(0x32));
        assertThat(source.read(), is(0x33));
        assertThat(source.read(), is(0x34));
        assertThat(source.read(), is(0x35));
        assertThat(source.read(), is(0x36));
        assertThat(source.read(), is(0x37));
        assertThat(source.read(), is(0x38));
        assertThat(source.read(), is(0x39));
        assertThat(source.read(), is(0x30));

        // 2件目
        byteString = new byte[10];
        source.read(byteString);
        assertThat(new String(byteString, "sjis"), is("012345    "));

        wordString = new byte[10];
        source.read(wordString);
        assertThat(new String(wordString, "sjis"), is("あいう　　"));

        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x30));
        assertThat(source.read(), is(0x31));
        assertThat(source.read(), is(0x32));
        assertThat(source.read(), is(0x33));
        assertThat(source.read(), is(-1));

        source.close();
    }

    /**
     * マルチレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedMultiLayoutFile() throws Exception {
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

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedMultiLayoutFile.fmt");

        final File dataFile = temporaryFolder.newFile();
        new FileRecordWriter(dataFile, new File(url.toURI()))
                .write(recordA)            //フィールドの値でレコードタイプを自動判定
                .write("layoutB", recordB) //レコードタイプを明示的に指定して書き込む
                .close();

        InputStream source = new FileInputStream(dataFile);

        // 1件目
        byte[] byte10 = new byte[10];
        assertThat((char) source.read(), is('A'));
        source.read(byte10);
        assertThat(new String(byte10, "sjis"), is("0123456789"));
        source.read(byte10);
        assertThat(new String(byte10, "sjis"), is("あいうえお"));

        source.read(byte10);

        // 2件目
        byte10 = new byte[10];
        assertThat((char) source.read(), is('B'));
        source.read(byte10);
        assertThat(new String(byte10, "sjis"), is("0123456789"));
        source.read(byte10);
        assertThat(new String(byte10, "sjis"), is("0123456789"));

        source.read(byte10);

        assertThat(source.read(), is(-1));
    }

    @Test(expected = InvalidDataFormatException.class)
    public void testFixedLayout_specifyInvalidTypeName() throws Exception {

        Map<String, Object> recordBInvalid = new HashMap<String, Object>() {{
            put("layout", "B");
            put("longString", "01234567890123456789");
            put("zoneDigits", 1234567890);
        }};


        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedMultiLayoutFile.fmt");

        //本来とは異なるレコードタイプを指定して出力
        new FileRecordWriter(temporaryFolder.newFile(), new File(url.toURI()))
                .write("layoutA", recordBInvalid);
    }

    /**
     * マルチレイアウトファイルの書き出し。
     */
    @Test
    public void testFixedMultiLayoutFileClassifierBigDecimal() throws Exception {

        Map<String, Object> record1 = new HashMap<String, Object>();
        record1.put("layout", new BigDecimal("0.0000000001"));
        record1.put("data", "123");

        Map<String, Object> record2 = new HashMap<String, Object>();
        record2.put("layout", new BigDecimal("0.0000000002"));
        record2.put("data1", "1");
        record2.put("data2", "12");

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedMultiLayoutFileClassifierBigDecimal.fmt");

        final File dataFile = temporaryFolder.newFile();
        new FileRecordWriter(dataFile, new File(url.toURI()))
                .write(record1)            //フィールドの値でレコードタイプを自動判定
                .write(record2)
                .close();

        InputStream source = new FileInputStream(dataFile);

        // 1件目
        byte[] record = new byte[15];
        source.read(record);
        assertThat(new String(record, "sjis"), is("0.0000000001123"));
        record = new byte[15];
        source.read(record);
        assertThat(new String(record, "sjis"), is("0.0000000002112"));

        assertThat(source.read(), is(-1));
    }

    /**
     * コンストラクタの引数がファイルオブジェクトのパターン。
     */
    @Test
    public void testFixedConstructorArgFile() throws Exception {
        Map<String, Object> record = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "あいうえお");
            put("zoneDigits", 1234567890);
        }};


        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testFixedConstructorArgFile.fmt");
        File dataFile = temporaryFolder.newFile();

        new FileRecordWriter(dataFile, new File(url.toURI()))
                .write(record)
                .write(record)
                .write(record)
                .close();      // 3件出力

        assertThat(dataFile.exists(), is(true));

        InputStream source = new FileInputStream(dataFile);

        byte[] binaryRecord = new byte[30];
        source.read(binaryRecord);
        source.read(binaryRecord);
        source.read(binaryRecord);
        assertThat(source.read(), is(-1));

        source.close();
    }

    /**
     * バッファサイズが設定されることの確認。
     * メソッドを呼んで読み込みが正常にできることと、不正な値を設定した場合に例外がスローされることを確認する。
     */
    @Test
    public void testSetBufferSize() throws Exception {

        final File dataFile = temporaryFolder.newFile();
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testSetBufferSize.fmt");

        // バッファサイズを50にして正しく読めること。
        FileRecordWriter writer = new FileRecordWriter(dataFile, new File(url.toURI()), 50);
        DataRecord record1 = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456781"));
        }};
        record1.setRecordType("Default");
        writer.write(record1);

        DataRecord record2 = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456782"));
        }};
        record2.setRecordType("Default");
        writer.write(record2);

        DataRecord record3 = new DataRecord() {{
            put("byteString", "0123456789");
            put("wordString", "abcdefghij");
            put("zoneDigits", new BigDecimal("0123456783"));
        }};
        record3.setRecordType("Default");
        writer.write(record3);
        writer.close();

        InputStream in = new FileInputStream(dataFile);

        byte[] recordBytes = new byte[90];
        in.read(recordBytes);

        assertThat(in.read(), is(-1));
        ByteBuffer recordBuff = ByteBuffer.wrap(recordBytes);

        byte[] fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456789"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("abcdefghij"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456781"));
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456789"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("abcdefghij"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456782"));
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456789"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("abcdefghij"));
        fieldBytes = new byte[10];
        recordBuff.get(fieldBytes);
        assertThat(new String(fieldBytes, "sjis"), is("0123456783"));
    }

    /**
     * バッファサイズを0にすると例外が発生すること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSize() throws Exception {
        new FileRecordWriter(temporaryFolder.newFile(), temporaryFolder.newFile(), 0);
    }

    /**
     * IOExceptionがスローされるパターン。
     */
    @Test
    public void testException() throws Exception {

        systemRepositoryResource.addComponent("formatterFactory", new FormatterFactoryStub02());

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testException.fmt");

        // readメソッドでIOExceptionがスローされる場合のテスト
        FileRecordWriter writer = new FileRecordWriter(temporaryFolder.newFile(), new File(url.toURI()));
        try {
            writer.write(new DataRecord());
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getCause()
                        .getClass(), CoreMatchers.<Class<?>>is(IOException.class));
        }
    }

    /**
     * 引数が不正なテスト。
     */
    @Test
    public void testIllegalArgs() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordWriterTest/testIllegalArgs.fmt");

        // 引数が空文字
        final File dataFile = temporaryFolder.newFile();
        FileRecordWriter writer = new FileRecordWriter(dataFile, new File(url.toURI()));
        try {
            writer.write("", new DataRecord());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("record type was blank. record type must not be blank."));
        }

        // 引数がnull
        try {
            writer.write(null, new DataRecord());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("record type was blank. record type must not be blank."));
        }
    }

    /**
     * 本クラスのクローズメソッド実行時に、DataRecordFormatterのクローズメソッドが呼ばれることの確認。
     */
    @Test
    public void testClose() throws Exception {

        // Windows環境でない場合は終了する
        if (!getOsName().contains("windows")) {
            return;
        }

        systemRepositoryResource.addComponent("formatterFactory", new FormatterFactoryStub());

        final File dataFile = temporaryFolder.newFile();
        FileRecordWriter writer = new FileRecordWriter(dataFile, temporaryFolder.newFile());
        writer.write(new HashMap<String, String>());
        assertThat(dataFile.exists(), is(true));
        dataFile.delete();
        assertThat(dataFile.exists(), is(true)); // クローズされていないので削除できない

        assertThat(DataRecordFormatterStub.isCallClose, is(false));

        writer.close();

        assertThat(DataRecordFormatterStub.isCallClose, is(true)); // フォーマッタのクローズメソッドが呼ばれたことの確認

        dataFile.delete();
        assertThat(dataFile.exists(), is(false)); // クローズされているので削除できることの確認
    }

    /**
     * OS名を取得する。
     *
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name")
                     .toLowerCase();
    }

}
