package nablarch.common.io;

import nablarch.core.dataformat.DataFormatConfigFinder;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link FileRecordWriterHolder}のバッファリングのテスト。
 *
 * @link Kiyohito Itoh
 */
public class FileRecordWriterHolderBufferTest {

    @Before
    public void setUp() {

        DataFormatConfigFinder.getDataFormatConfig().setFlushEachRecordInWriting(true);
        FormatterFactory.getInstance().setCacheLayoutFileDefinition(false);
        FileRecordWriterHolder.closeAll();

        SystemRepository.clear();

        FilePathSetting.getInstance().setBasePathSettings(
                new HashMap<String, String>() {{
                    put("input",  "file:./");
                    put("format", "file:./");
                    put("output", "file:./");
                }}
        ).addFileExtensions("format", "fmt");

        new File("./test.fmt").delete();
        new File("./test.out").delete();
    }

    @After
    public void tearDown() {
        DataFormatConfigFinder.getDataFormatConfig().setFlushEachRecordInWriting(true);
        FormatterFactory.getInstance().setCacheLayoutFileDefinition(true);
        FileRecordWriterHolder.closeAll();
        SystemRepository.clear();
    }

    @Test
    public void testFixedLengthDataFormatWithoutBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "Fixed"
         text-encoding: "UTF-8"
         record-separator: "\n"
         record-length: 10
         [data]
         1 test X(5)
         6 test1 X(5)
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        testWriteWithoutBuffer(formatFile, data, 10 + 1); // +1: line feed
    }

    @Test
    public void testFixedLengthDataFormatWithBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "Fixed"
         text-encoding: "UTF-8"
         record-separator: "\n"
         record-length: 10
         [data]
         1 test X(5)
         6 test1 X(5)
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        /*
        20>220
        40>440
        60>660
        80>880
         */
        testWriteWithBuffer(formatFile, data, 10 + 1, 396);
    }

    @Test
    public void testVariableLengthDataFormatWithoutBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "Variable"
         text-encoding: "UTF-8"
         record-separator: "\n"
         field-separator: ","
         [data]
         1 test X
         2 test1 X
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        testWriteWithoutBuffer(formatFile, data, 10 + 2); // +2: separator + line feed
    }

    @Test
    public void testVariableLengthDataFormatWithBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "Variable"
         text-encoding: "UTF-8"
         record-separator: "\n"
         field-separator: ","
         [data]
         1 test X
         2 test1 X
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 500));
            put("test1", str('s', 500));
        }};

        /*
        n:回数
        w  :アプリから書き込むサイズ
        BW :BufferedWriter(default buffer size:8,192)が書き込むサイズ
        OSW:OutputStreamWriter(default buffer size:8,192)が書き込むサイズ
        BOS:BufferedOutputStream(specified buffer size: 24576)
         n      w       BW        OSW     BOS
        ---------------------------------------
        20 >  20,040   16,384   16,384        0
        40 >  40,080   32,768   32,768        0
        60 >  60,120   49,152   32,768   24,576
        80 >  80,160   65,536‬   65,536   49,152
        end> 100,200  100,200  100,200  100,200
        ---------------------------------------
        wに対してBW+OSWのバッファ16,384を差し引いたサイズがBOSに渡され、
        BOSのバッファを超えるとファイルに書き込まれる。
         */
        testWriteWithBuffer(
                formatFile, data, 500 + 500 + 2, 24576,
                new int[] { 0, 0, 24576, 49152, 100200 });
    }

    @Test
    public void testStructuredDataFormatWithoutBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "JSON"
         text-encoding: "UTF-8"
         [data]
         1 test X(5)
         2 test1 X(5)
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        testWriteWithoutBuffer(formatFile, data, 32); // 32: {"test":"kkkkk","test1":"kkkkk"}
    }

    @Test
    public void testStructuredDataFormatWithBuffer() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "JSON"
         text-encoding: "UTF-8"
         [data]
         1 test X(5)
         2 test1 X(5)
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        /*
        20>640
        40>1280
        60>1920
        80>2560
         */
        testWriteWithBuffer(formatFile, data, 32, 1184);
    }

    /**
     * 100回レコードを書き込み、1レコード毎にflushする場合はデータ件数に応じたサイズで書き込まれることを確認。
     *
     * @param formatFile フォーマットファイル
     * @param data フォーマットに応じた1レコードのデータ
     * @param outRecordLength 1レコードのデータ長
     */
    private void testWriteWithoutBuffer(File formatFile, Map<String, String> data, int outRecordLength) {

        int numberOfRecord = 100;
        int fileSize = numberOfRecord * outRecordLength;

        // The file size before testing is 0
        assertThat(new File("./test.out").length(), is(0L));

        // Write the data
        FileRecordWriterHolder.open("test.out", "test");
        for (int i = 0; i < numberOfRecord; i++) {
            if (i == 20) {
                int size = 20 * outRecordLength;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("20>" + new File("./test.out").length());
            }
            if (i == 40) {
                int size = 40 * outRecordLength;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("40>" + new File("./test.out").length());
            }
            if (i == 60) {
                int size = 60 * outRecordLength;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("60>" + new File("./test.out").length());
            }
            if (i == 80) {
                int size = 80 * outRecordLength;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("80>" + new File("./test.out").length());
            }
            FileRecordWriterHolder.write(data, "test.out");
        }
        FileRecordWriterHolder.closeAll();

        // Checking the file size after writing
        assertThat(new File("./test.out").length(), is(Long.valueOf(fileSize)));
        System.out.println("end>" + new File("./test.out").length());
    }

    /**
     * 100回レコードを書き込み、1レコード毎にflushしない場合はバッファサイズに応じたサイズで書き込まれることを確認。
     *
     * @param formatFile フォーマットファイル
     * @param data フォーマットに応じた1レコードのデータ
     * @param outRecordLength 1レコードのデータ長
     */
    private void testWriteWithBuffer(File formatFile, Map<String, String> data, int outRecordLength, int bufferSize) {

        DataFormatConfigFinder.getDataFormatConfig().setFlushEachRecordInWriting(false);

        int numberOfRecord = 100;

        // The file size before testing is 0
        assertThat(new File("./test.out").length(), is(0L));

        // Write the data
        FileRecordWriterHolder.open("test.out", "test", bufferSize);
        for (int i = 0; i < numberOfRecord; i++) {
            if (i == 20) {
                int size = ((20 * outRecordLength) / bufferSize) * bufferSize;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("20>" + new File("./test.out").length());
            }
            if (i == 40) {
                int size = ((40 * outRecordLength) / bufferSize) * bufferSize;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("40>" + new File("./test.out").length());
            }
            if (i == 60) {
                int size = ((60 * outRecordLength) / bufferSize) * bufferSize;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("60>" + new File("./test.out").length());
            }
            if (i == 80) {
                int size = ((80 * outRecordLength) / bufferSize) * bufferSize;
                assertThat(new File("./test.out").length(), is(Long.valueOf(size)));
                System.out.println("80>" + new File("./test.out").length());
            }
            FileRecordWriterHolder.write(data, "test.out");
        }
        FileRecordWriterHolder.closeAll();

        // Checking the file size after writing
        assertThat(new File("./test.out").length(), is(Long.valueOf(numberOfRecord * outRecordLength)));
        System.out.println("end>" + new File("./test.out").length());
    }

    /**
     * 100回レコードを書き込み、1レコード毎にflushしない場合はバッファサイズに応じたサイズで書き込まれることを確認。
     *
     * @param formatFile フォーマットファイル
     * @param data フォーマットに応じた1レコードのデータ
     * @param outRecordLength 1レコードのデータ長
     */
    private void testWriteWithBuffer(
            File formatFile, Map<String, String> data, int outRecordLength, int bufferSize, int[] expectedBufferSizes) {

        DataFormatConfigFinder.getDataFormatConfig().setFlushEachRecordInWriting(false);

        int numberOfRecord = 100;

        // The file size before testing is 0
        assertThat(new File("./test.out").length(), is(0L));

        // Write the data
        FileRecordWriterHolder.open("test.out", "test", bufferSize);
        for (int i = 0; i < numberOfRecord; i++) {
            if (i == 20) {
                assertThat(new File("./test.out").length(), is(Long.valueOf(expectedBufferSizes[0])));
                System.out.println("20>" + new File("./test.out").length());
            }
            if (i == 40) {
                assertThat(new File("./test.out").length(), is(Long.valueOf(expectedBufferSizes[1])));
                System.out.println("40>" + new File("./test.out").length());
            }
            if (i == 60) {
                assertThat(new File("./test.out").length(), is(Long.valueOf(expectedBufferSizes[2])));
                System.out.println("60>" + new File("./test.out").length());
            }
            if (i == 80) {
                assertThat(new File("./test.out").length(), is(Long.valueOf(expectedBufferSizes[3])));
                System.out.println("80>" + new File("./test.out").length());
            }
            FileRecordWriterHolder.write(data, "test.out");
        }
        FileRecordWriterHolder.closeAll();

        // Checking the file size after writing
        assertThat(new File("./test.out").length(), is(Long.valueOf(numberOfRecord * outRecordLength)));
        System.out.println("end>" + new File("./test.out").length());
    }

    private String str(char c, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
