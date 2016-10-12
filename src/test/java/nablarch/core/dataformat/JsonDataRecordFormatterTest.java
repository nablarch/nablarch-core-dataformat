/**
 * 
 */
package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.JsonDataConvertorSetting;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link JsonDataRecordFormatter}のテストを行います。
 * 
 * @author TIS
 */
public class JsonDataRecordFormatterTest {

    /**
     * MimeTypeの取得テストを行います。<br>
     * 
     * 条件：<br>
     *   MimeType取得処理を呼び出す。<br>
     *   
     * 期待結果：<br>
     *   "application/json"が返却されること。<br>
     */
    @Test
    public void testGetMimeType() {
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        assertEquals("application/json", formatter.getMimeType());
    }

    /**
     * JSONパーサーを指定したJSONフォーマッターの生成テストを行います。<br>
     * 
     * 条件：<br>
     *   リポジトリにテスト用パーサークラスを指定し、JSONフォーマッターを生成する。<br>
     *   
     * 期待結果：<br>
     *   コンバータの設定情報として{@link JsonDataConvertorSetting}が設定されていること。<br>
     *   {@link TestJsonDataParser}がパーサーとして使用されていること。<br>
     */
    @Test
    public void testJsonDataRecordFormatter() {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/JsonDataRecordFormatter.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        assertEquals(JsonDataConvertorSetting.getInstance(), formatter.getConvertorSetting());
        assertEquals(TestJsonDataParser.class, formatter.getDataParser().getClass());
    }

    /**
     * JSONパーサーを指定しないJSONフォーマッターの生成テストを行います。<br>
     * 
     * 条件：<br>
     *   リポジトリにテスト用パーサークラスを指定せず、JSONフォーマッターを生成する。<br>
     *   
     * 期待結果：<br>
     *   コンバータの設定情報として{@link JsonDataConvertorSetting}が設定されていること。<br>
     *   {@link XmlDataParser}がパーサーとして使用されていること。<br>
     */
    @Test
    public void testJsonDataRecordFormatterDefaultParser() {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/JsonDataRecordFormatterDefault.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        assertEquals(JsonDataConvertorSetting.getInstance(), formatter.getConvertorSetting());
        assertEquals(JsonDataParser.class, formatter.getDataParser().getClass());
    }
    
    /**
     * エンコーディングにUTF-8を指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"UTF-8"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外が発生しないこと。<br>
     */
    @Test
    public void testValidateDirectives_UTF8() {
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
        formatter.validateDirectives(def.getDirective());
    }
    
    /**
     * エンコーディングにUTF-16LEを指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"UTF-16LE"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外が発生しないこと。<br>
     */
    @Test
    public void testValidateDirectives_UTF16LE() {
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
        text-encoding:  "UTF-16LE"
        [request]
        1 id            X
        2 name          X
         ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.validateDirectives(def.getDirective());
    }
    
    /**
     * エンコーディングにUTF-16BEを指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"UTF-16BE"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外が発生しないこと。<br>
     */
    @Test
    public void testValidateDirectives_UTF16BE() {
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
        text-encoding:  "UTF-16LE"
        [request]
        1 id            X
        2 name          X
         ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.validateDirectives(def.getDirective());
    }
    
    /**
     * エンコーディングにUTF-32LEを指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"UTF-32LE"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外が発生しないこと。<br>
     */
    @Test
    public void testValidateDirectives_UTF32LE() throws Exception {
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
        text-encoding:  "UTF-32LE"
        [request]
        1 id            X
        2 name          X
         ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.validateDirectives(def.getDirective());
    }
    
    /**
     * エンコーディングにUTF-32BEを指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"UTF-32LE"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外が発生しないこと。<br>
     */
    @Test
    public void testValidateDirectives_UTF32BE() throws Exception {
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
        text-encoding:  "UTF-32BE"
        [request]
        1 id            X
        2 name          X
         ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        formatter.validateDirectives(def.getDirective());
    }
    
    /**
     * エンコーディングにMS932を指定したディレクティブ検証を行います。<br>
     * 
     * 条件：<br>
     *   デフォルトエンコーディングに"MS932"を指定し、ディレクティブの検証を行う。<br>
     *   
     * 期待結果：<br>
     *   例外SyntaxErrorExceptionが発生すること。<br>
     */
    @Test
    public void testValidateDirectivesError() {
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
        text-encoding:  "MS932"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();
        
        JsonDataRecordFormatter formatter = new JsonDataRecordFormatter();
        
        LayoutDefinition def = new LayoutFileParser(requestFormatFile.getAbsolutePath()).parse();
        try {
            formatter.validateDirectives(def.getDirective());
            fail("例外が発生する");
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("when file-type is 'JSON', directive 'text-encoding' must be specified by [UTF-8, UTF-16LE, UTF-32LE, UTF-16BE, UTF-32BE]."));
        }
    }
    
    /**
     * テスト用パーサークラス
     */
    public static class TestJsonDataParser implements StructuredDataParser {
        @Override
        public Map<String, ?> parseData(InputStream in,
                LayoutDefinition layoutDef) throws IOException,
                InvalidDataFormatException {
            return null;
        }
    }
    /**
     * テスト用ビルダークラス
     */
    public static class TestJsonDataBuilder implements StructuredDataBuilder {
        @Override
        public void buildData(Map<String, ?> map, LayoutDefinition layoutDef,
                OutputStream out) throws IOException,
                InvalidDataFormatException {
        }
    }
}
