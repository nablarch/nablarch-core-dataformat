package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;

import org.hamcrest.CoreMatchers;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.core.ThreadContext;
import nablarch.core.dataformat.FormatterFactoryStub.DataRecordFormatterStub;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.FileUtil;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * ファイルレコードリーダのテスト
 * <p>
 * 観点：
 * 正常系のテスト、readメソッド、hasNextメソッド、closeメソッドが正常に動作し、ファイルの読み込みができること、
 * また、異常系のテストを行う。
 *
 * @author Masato Inoue
 */
public class FileRecordReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource(null);

    private FileRecordReader reader;


    @BeforeClass
    public static void setUpClass() {
        // 強制的にキャッシュをオフに。
        // これで、このクラスで使用したいフォーマット定義が必ず使用される。
        FormatterFactory.getInstance()
                        .setCacheLayoutFileDefinition(false);
    }

    @Before
    public void setup() {
        FileRecordWriterHolder.closeAll();
    }
    
    @After
    public void tearDown() throws Exception {
        if (reader != null) {
            reader.close();
        }
        SystemRepository.clear();
    }


    /**
     * FileRecordReader実行時に、DataRecordFormatterのクローズメソッドが呼ばれることの確認。
     */
    @Test
    public void testClose() throws Exception {

        ThreadContext.setRequestId("test");

        // Windows環境でない場合は終了する
        if (!getOsName().contains("windows")) {
            return;
        }
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        FilePathSetting.getInstance()
                       .addBasePathSetting("input", "file:" + folder.newFolder("input").getAbsolutePath())
                       .addBasePathSetting("format", "file:" + folder.newFolder("output").getAbsolutePath());

        File dataFile = new File(folder.getRoot(), "input/record10.dat");
        dataFile.createNewFile();

        reader = new FileRecordReader(dataFile, (File) null);
        reader.read();
        assertThat(dataFile.exists(), is(true));
        dataFile.delete();
        assertThat(dataFile.exists(), is(true)); // クローズされていないので削除できない

        assertThat(DataRecordFormatterStub.isCallClose, is(false));

        reader.close();

        assertThat(DataRecordFormatterStub.isCallClose, is(true)); // フォーマッタのクローズメソッドが呼ばれたことの確認

        dataFile.delete();
        assertThat(dataFile.exists(), is(false)); // クローズされているので削除できることの確認
    }

    /**
     * hasNextメソッドおよびreadメソッドの動作テスト。
     */
    @Test
    public void testReadHasNextMethodFirst() throws Exception{

        ThreadContext.setRequestId("test");

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

        final File inputFile = new File(folder.getRoot(), "record.dat");
        OutputStream dest = new FileOutputStream(inputFile, false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordReaderTest/testReadHasNextMethodFirst.fmt");
        reader = new FileRecordReader(inputFile, new File(url.toURI()));

        assertThat(reader.hasNext(), is(true));
        DataRecord record = reader.read();

        assertThat(record.size(), is(9));
        assertThat(record.get("byteString"), CoreMatchers.<Object>is("ｱｲｳｴｵｶｷｸｹｺ"));
        assertThat(record.get("wordString"), CoreMatchers.<Object>is("あいうえお"));
        assertThat(record.get("zoneDigits"), CoreMatchers.<Object>is(new BigDecimal("1234567890")));
        assertThat(record.get("signedZDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890")));
        assertThat(record.get("packedDigits"), CoreMatchers.<Object>is(new BigDecimal("1234567890123456789")));
        assertThat(record.get("signedPDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890123456789")));
        assertThat(record.get("signedPDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890123456789")));
        assertThat(record.get("zDecimalPoint"), CoreMatchers.<Object>is(new BigDecimal("12.345")));
        assertThat(record.get("pDecimalPoint"), CoreMatchers.<Object>is(new BigDecimal("123.45")));

        assertThat(record.containsKey("nativeBytes"), is(true));

        byte[] nativeBytes = record.getValue("nativeBytes");
        assertThat(nativeBytes[0], is((byte) 0xFF));
        assertThat(nativeBytes[1], is((byte) 0xEE));
        assertThat(nativeBytes[9], is((byte) 0x66));

        assertThat(reader.hasNext(), is(true));
        reader.read(); //2件め
        assertThat(reader.hasNext(), is(true));
        reader.read(); //3件め
        assertThat(reader.hasNext(), is(false));
        assertThat(reader.read(), nullValue());
    }

    /**
     * hasNextを呼び出さずに、先にreadを呼び出しても処理に影響がないことのテスト。
     */
    @Test
    public void testReadReadMethodFirst() throws Exception{

        ThreadContext.setRequestId("test");
        
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

        final File inputFile = new File(folder.newFolder("input"), "record.dat");
        OutputStream dest = new FileOutputStream(inputFile, false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordReaderTest/testReadReadMethodFirst.fmt");

        reader = new FileRecordReader(inputFile, new File(url.toURI()));

        DataRecord record = reader.read();
        assertThat(reader.hasNext(), is(true));

        assertThat(record.size(), is(9));
        assertThat(record.get("byteString"), CoreMatchers.<Object>is("ｱｲｳｴｵｶｷｸｹｺ"));
        assertThat(record.get("wordString"), CoreMatchers.<Object>is("あいうえお"));
        assertThat(record.get("zoneDigits"), CoreMatchers.<Object>is(new BigDecimal("1234567890")));
        assertThat(record.get("signedZDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890")));
        assertThat(record.get("packedDigits"), CoreMatchers.<Object>is(new BigDecimal("1234567890123456789")));
        assertThat(record.get("signedPDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890123456789")));
        assertThat(record.get("signedPDigits"), CoreMatchers.<Object>is(new BigDecimal("-1234567890123456789")));
        assertThat(record.get("zDecimalPoint"), CoreMatchers.<Object>is(new BigDecimal("12.345")));
        assertThat(record.get("pDecimalPoint"), CoreMatchers.<Object>is(new BigDecimal("123.45")));

        assertThat(record.containsKey("nativeBytes"), is(true));

        byte[] nativeBytes = record.getValue("nativeBytes");
        assertThat(nativeBytes[0], is((byte) 0xFF));
        assertThat(nativeBytes[1], is((byte) 0xEE));
        assertThat(nativeBytes[9], is((byte) 0x66));

        assertThat(reader.hasNext(), is(true));
        reader.read(); //2件め
        assertThat(reader.hasNext(), is(true));
        reader.read(); //3件め
        assertThat(reader.hasNext(), is(false));
        assertThat(reader.read(), nullValue());
    }

    /**
     * IOExceptionがスローされるパターン。
     */
    @Test
    public void testException() throws Exception {

        ThreadContext.setRequestId("test");

        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory02.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        final URL url = FileUtil.getResourceURL("classpath:nablarch/core/dataformat/FileRecordReaderTest/testException.fmt");
        // readメソッドでIOExceptionがスローされる場合のテスト
        FileRecordReader reader = new FileRecordReader(folder.newFile(), new File(url.toURI()));
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
    public void testAddingInputSourceToException() throws Exception {
        ThreadContext.setRequestId("test");

        final File file = folder.newFile();
        final BufferedWriter stream = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        try {
            stream.write("112345678902NOTNUMBER");
        } finally {
            stream.close();
        }

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/FileRecordReaderTest/testAddingInputSourceToException.fmt");
        reader = new FileRecordReader(file, new File(url.toURI()));
        
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
            assertThat(e.getFormatFilePath(), endsWith("testAddingInputSourceToException.fmt"));
            assertThat(e.getInputSourcePath(), endsWith(file.getName()));
        }
    }

    /**
     * 現在読み込んでいるレコード番号を取得できることを確認する。
     */
    @Test
    public void testGetRecordNumber() throws Exception {

        // テスト用のデータファイルを作成
        String data  = "test1\n" +
                "test2\n" +
                "test3\n" +
                "test4\n" +
                "test5\n";
        File dataFile = folder.newFile("test.dat");
        writeFile(dataFile, data);

        // テスト用のフォーマット定義ファイルを作成
        String format = "file-type: \"Variable\"\n" +
                "record-separator: \"\\n\"\n" +
                "field-separator: \",\"\n" +
                "text-encoding: \"sjis\"\n" +
                "[data]\n" +
                "1 type X\n";
        File formatFile = folder.newFile("test.fmt");
        writeFile(formatFile, format);

        FileRecordReader reader = new FileRecordReader(dataFile, formatFile);

        assertThat("読み込み前なので0となること", reader.getRecordNumber(), is(0));

        for (int i = 0; i < 5; i++) {
            reader.read();
            assertThat("読み込みを行うたびに行数が加算されること", reader.getRecordNumber(), is(i + 1));
        }

        reader.read();
        assertThat("余分に読み込みを行っても、行数が加算されないこと", reader.getRecordNumber(), is(5));
    }

    /**
     * ファイルに書き込む
     * @param file ファイル
     * @param value 書き込む文字列
     * @throws Exception
     */
    private void writeFile(File file, String value) throws Exception {
        FileOutputStream dest = new FileOutputStream(file);
        dest.write(value.getBytes("sjis"));
        dest.close();
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
