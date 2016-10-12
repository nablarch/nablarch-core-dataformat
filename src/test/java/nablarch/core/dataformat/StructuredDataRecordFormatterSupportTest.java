/**
 * 
 */
package nablarch.core.dataformat;

import nablarch.core.dataformat.StructuredDataRecordFormatterSupport.StructuredDataDirective;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.tool.Hereis;
import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link StructuredDataRecordFormatterSupport}のテスト<br>
 * 
 * @author TIS
 */
public class StructuredDataRecordFormatterSupportTest {

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
     * 
     * 条件：<br>
     *   レコード読み取り処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testReadRecord() throws Exception {
        // テスト用フォーマット
        FilePathSetting fps = FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("format", "fmt");
        String formatFileName = 
                Builder.concat(
                    fps.getBasePathSettings().get("format").getPath(),
                    "/", "JsonDataRecordFormatterTest", ".", 
                    fps.getFileExtensions().get("format"));
        
        File requestFormatFile = Hereis.file(formatFileName);
        /****************************
        file-type:      "JSON"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.setDefinition(def).initialize();
        
        String json = Hereis.string();
        /****************************
        {"id":"1","name":"name1"}
        ****************************/
        DataRecord expected = new DataRecord();
        expected.put("id", "1");
        expected.put("name", "name1");
        
        // ソース設定前は何も起こらない
        formatter.close();
        
        // ソース設定前はfalse
        assertFalse(formatter.hasNext());
        
        formatter.setInputStream(new ByteArrayInputStream(json.getBytes("UTF-8")));
        
        // 読み取り前はtrue
        assertTrue(formatter.hasNext());
        
        DataRecord record = formatter.readRecord();
        assertEquals(expected, record);
        
        // 読み取り後はfalse
        assertFalse(formatter.hasNext());
        
        formatter.close();
    }
    

    /**
     * レコード書き込みテストを行います。<br>
     * 
     * 条件：<br>
     *   レコード書き込み処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testWriteRecord() throws Exception {
        // テスト用フォーマット
        FilePathSetting fps = FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("format", "fmt");
        String formatFileName = 
                Builder.concat(
                    fps.getBasePathSettings().get("format").getPath(),
                    "/", "JsonDataRecordFormatterTest", ".", 
                    fps.getFileExtensions().get("format"));
        
        File requestFormatFile = Hereis.file(formatFileName);
        /****************************
        file-type:      "JSON"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.setDefinition(def).initialize();
        
        String json = Hereis.string().trim();
        /****************************
        {"id":"1","name":"name1"}
        ****************************/
        DataRecord record = new DataRecord();
        record.put("id", "1");
        record.put("name", "name1");
        
        // ソース設定前は何も起こらない
        formatter.close();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatter.setOutputStream(baos);
    
        formatter.writeRecord(record);
        assertArrayEquals(json.getBytes("UTF-8"), baos.toByteArray());
        
        formatter.close();
    }
    
    /**
     * 入力ストリームを指定しない状態でのレコード読み取りテストを行います。<br>
     * 
     * 条件：<br>
     *   入力ストリームを指定せず、レコード読み取り処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testReadRecordOnSourceNotSet() throws Exception {
        StructuredDataRecordFormatterSupport formatter = createFormatter();
        try {
            formatter.readRecord();
            fail("例外が発生する");
        } catch(IllegalStateException e) {
            assertTrue(e.getMessage().contains("input stream was not set. input stream must be set before reading."));
        }
    }

    /**
     * 出力ストリームを指定しない状態でのレコード書き込みテストを行います。<br>
     * 
     * 条件：<br>
     *   出力ストリームを指定せず、レコード書き込み処理を実行する。<br>
     *   
     * 期待結果：<br>
     *   例外IllegalStateExceptionが発生すること。。<br>
     */
    @Test
    public void testWriteRecordOnDestNotSet() throws Exception {
        StructuredDataRecordFormatterSupport formatter = createFormatter();
        try {
            formatter.writeRecord(new HashMap<String, Object>());
            fail("例外が発生する");
        } catch(IllegalStateException e) {
            assertTrue(e.getMessage().contains("output stream was not set. output stream must be set before writing."));
        }
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
            public int read() throws IOException {
                return 0;
            }
            @Override
            public void close() throws IOException {
                throw new IOException("this is test exception");
            }
        });
        formatter.setOutputStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
            @Override
            public void close() throws IOException {
                throw new IOException("this is test exception");
            }
        });

        OnMemoryLogWriter.clear();
        List<String> messages = OnMemoryLogWriter.getMessages("writer.memory");
        formatter.close();
        
        assertTrue(messages.get(0).contains("WARN I/O error happened while closing the input stream."));
        assertTrue(messages.get(1).contains("WARN I/O error happened while closing the output stream."));
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
        assertNull(rec);
    }
}
