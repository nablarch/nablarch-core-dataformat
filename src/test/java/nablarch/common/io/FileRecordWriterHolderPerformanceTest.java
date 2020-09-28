package nablarch.common.io;

import nablarch.core.dataformat.DataFormatConfigFinder;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * 汎用データフォーマットの出力処理の性能テスト。
 *
 * 汎用データフォーマットの出力処理は1レコード毎にflushしているため、複数レコードをまとめてバッファリングできない。
 * 1レコード毎のflushにより、レコード単位で書き込まれる。
 * ただし、書き込み件数が多い場合は1レコード毎にflushすると書き込み頻度が多くなるため処理時間が長くなる。
 * このクラスはこれらの問題を再現するためのテストである。
 *
 * @author Kiyohito Itoh
 */
@Ignore("汎用データフォーマットの出力処理の性能テスト用のクラスなのでCIでは無効とする")
public class FileRecordWriterHolderPerformanceTest {

    @Before
    public void setUp() {

        //コメントを外すと1レコード毎のflushをしない。
        //DataFormatConfigFinder.getDataFormatConfig().setFlushEachRecordInWriting(false);

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
    }

    @Test
    public void testFixedLengthDataFormat() {

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

        testDataFormat(formatFile, data, 10 + 1); // +1: line feed
    }

    @Test
    public void testVariableLengthDataFormat() {

        File formatFile = Hereis.file("./test.fmt");
        /**********************************************
         file-type: "Variable"
         text-encoding: "UTF-8"
         record-separator: "\n"
         field-separator: ","
         [data]
         1 test X(5)
         2 test1 X(5)
         ***************************************************/

        Map<String, String> data = new HashMap<String, String>() {{
            put("test", str('k', 5));
            put("test1", str('s', 5));
        }};

        testDataFormat(formatFile, data, 10 + 2); // +2: separator + line feed
    }

    @Test
    public void testStructuredDataFormat() {

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

        testDataFormat(formatFile, data, 32); // 32: {"test":"kkkkk","test1":"kkkkk"}
    }

    /**
     * バッファサイス1MBで大量のレコードを書き込む。
     *
     * 出力ファイルのテスト前後のサイズをアサートし、処理されていることを確認。
     * その上で、書き込み時間を標準出力に出力する。
     *
     * @param formatFile フォーマットファイル
     * @param data フォーマットに応じた1レコードのデータ
     * @param outRecordLength 1レコードのデータ長
     */
    private void testDataFormat(File formatFile, Map<String, String> data, int outRecordLength) {

        int numberOfRecord = 3000000;

        // The file size before testing is 0
        assertThat(new File("./test.out").length(), is(0L));

        // Write the data
        FileRecordWriterHolder.open("test.out", "test", 1048576);
        long start = System.currentTimeMillis();
        for (int i = 0; i < numberOfRecord; i++) {
            FileRecordWriterHolder.write(data, "test.out");
        }
        long stop = System.currentTimeMillis();
        FileRecordWriterHolder.closeAll();

        // Checking the file size after writing
        assertThat(new File("./test.out").length(), is(Long.valueOf(numberOfRecord * outRecordLength)));

        // Output the write time
        System.out.println("[" + ((stop - start) / 1000) + "] seconds");
    }

    private String str(char c, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
