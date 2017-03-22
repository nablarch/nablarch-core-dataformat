package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.activemq.util.ByteArrayInputStream;

import nablarch.core.dataformat.StructuredDataRecordFormatterSupport.StructuredDataDirective;
import nablarch.core.util.FileUtil;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link StructuredDataRecordFormatterSupport}のテスト<br>
 *
 * @author TIS
 */
public class StructuredDataRecordFormatterSupportTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private StructuredDataRecordFormatterSupport createFormatter() {
        return new StructuredDataRecordFormatterSupport() {
        };
    }

    /**
     * 内部クラスにおける、通常使用しないメソッドのテストです。
     */
    @Test
    public void testInnerClassEnumMethods() throws Exception {
        new StructuredDataDirective("hoge", String.class);
        assertEquals(StructuredDataDirective.TEXT_ENCODING, StructuredDataDirective.valueOf("text-encoding"));
    }

    /**
     * レコード読み取りテストを行います。<br>
     * <p>
     * 条件：<br>
     * レコード読み取り処理を実行する。<br>
     * <p>
     * 期待結果：<br>
     * 例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testReadRecord() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/StructuredDataRecordFormatterSupportTest/testReadRecord.fmt");

        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        LayoutDefinition def = new LayoutFileParser(new File(url.toURI()).getAbsolutePath()).parse();
        formatter.setDefinition(def)
                 .initialize();

        String json = "{\"id\":\"1\",\"name\":\"name1\"}";

        DataRecord expected = new DataRecord();
        expected.put("id", "1");
        expected.put("name", "name1");

        // ソース設定前は何も起こらない
        formatter.close();

        // ソース設定前はfalse
        assertThat(formatter.hasNext(), is(false));

        formatter.setInputStream(new ByteArrayInputStream(json.getBytes("UTF-8")));

        // 読み取り前はtrue
        assertThat(formatter.hasNext(), is(true));

        DataRecord record = formatter.readRecord();
        assertThat(record, is(expected));

        // 読み取り後はfalse
        assertThat(formatter.hasNext(), is(false));

        formatter.close();
    }

    /**
     * レコード書き込みテストを行います。<br>
     * <p>
     * 条件：<br>
     * レコード書き込み処理を実行する。<br>
     * <p>
     * 期待結果：<br>
     * 例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testWriteRecord() throws Exception {

        final URL formatFile = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/StructuredDataRecordFormatterSupportTest/testWriteRecord.fmt");

        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        LayoutDefinition def = new LayoutFileParser(new File(formatFile.toURI()).getAbsolutePath()).parse();
        formatter.setDefinition(def)
                 .initialize();

        // language=json
        String json = "{\"id\":\"1\",\"name\":\"name1\"}";

        DataRecord record = new DataRecord();
        record.put("id", "1");
        record.put("name", "name1");

        // ソース設定前は何も起こらない
        formatter.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.setOutputStream(baos);

        formatter.writeRecord(record);
        assertThat(baos.toByteArray(), is(json.getBytes("UTF-8")));

        formatter.close();
    }

    /**
     * 入力ストリームを指定しない状態でのレコード読み取りテストを行います。<br>
     * <p>
     * 条件：<br>
     * 入力ストリームを指定せず、レコード読み取り処理を実行する。<br>
     * <p>
     * 期待結果：<br>
     * 例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testReadRecordOnSourceNotSet() throws Exception {
        StructuredDataRecordFormatterSupport formatter = createFormatter();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("input stream was not set. input stream must be set before reading.");
        formatter.readRecord();
    }

    /**
     * 出力ストリームを指定しない状態でのレコード書き込みテストを行います。<br>
     * <p>
     * 条件：<br>
     * 出力ストリームを指定せず、レコード書き込み処理を実行する。<br>
     * <p>
     * 期待結果：<br>
     * 例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testWriteRecordOnDestNotSet() throws Exception {
        StructuredDataRecordFormatterSupport formatter = createFormatter();
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("output stream was not set. output stream must be set before writing.");
        formatter.writeRecord(new HashMap<String, Object>());
    }

    /**
     * クローズ時のエラーテストを行います。<br>
     * 
     * 条件：<br>
     *   エラーが発生するストリームを指定し、クローズ処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   警告ログが出力されること。<br>
     */
    @Test
    public void testCloseError() {
        StructuredDataRecordFormatterSupport formatter = createFormatter();
        formatter.setInputStream(new InputStream() {
            @Override
            public int read() {
                return 0;
            }
            @Override
            public void close() throws IOException {
                throw new IOException("this is test exception");
            }
        });
        formatter.setOutputStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
            @Override
            public void close() throws IOException {
                throw new IOException("this is test exception");
            }
        });

        OnMemoryLogWriter.clear();
        List<String> messages = OnMemoryLogWriter.getMessages("writer.memory");
        formatter.close();

        assertThat(messages.get(0), containsString("WARN I/O error happened while closing the input stream."));
        assertThat(messages.get(1), containsString("WARN I/O error happened while closing the output stream."));
    }

    /**
     * 残データが存在しない状態でのレコード読み取りテストを行います。<br>
     * 
     * 条件：<br>
     *   空のデータを設定し、レコード読み取り処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   nullが返却されること<br>
     */
    @Test
    public void testHasNoData() throws Exception {
        StructuredDataRecordFormatterSupport formatter = createFormatter();
        formatter.setInputStream(new ByteArrayInputStream(new byte[]{}));
        DataRecord rec = formatter.readRecord();
        assertThat(rec, nullValue());
    }
}
