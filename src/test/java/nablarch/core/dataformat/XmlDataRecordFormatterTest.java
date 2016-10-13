/**
 * 
 */
package nablarch.core.dataformat;

import nablarch.core.dataformat.convertor.XmlDataConvertorSetting;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link XmlDataRecordFormatter}のテストを行います。
 * 
 * @author TIS
 */
public class XmlDataRecordFormatterTest {

    /**
     * MimeTypeの取得テストを行います。<br>
     * 
     * 条件：<br>
     *   MimeType取得処理を呼び出す。<br>
     *   
     * 期待結果：<br>
     *   "application/xml"が返却されること。<br>
     */
    @Test
    public void testGetMimeType() {
        XmlDataRecordFormatter formatter = new XmlDataRecordFormatter();
        assertEquals("application/xml", formatter.getMimeType());
    }

    /**
     * XMLパーサーを指定したXMLフォーマッターの生成テストを行います。<br>
     * 
     * 条件：<br>
     *   リポジトリにテスト用パーサークラスを指定し、XMLフォーマッターを生成する。<br>
     *   
     * 期待結果：<br>
     *   コンバータの設定情報として{@link XmlDataConvertorSetting}が設定されていること。<br>
     *   {@link TestXmlDataParser}がパーサーとして使用されていること。<br>
     */
    @Test
    public void testXmlDataRecordFormatter() {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/XmlDataRecordFormatter.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        
        XmlDataRecordFormatter formatter = new XmlDataRecordFormatter();
        assertEquals(XmlDataConvertorSetting.getInstance(), formatter.getConvertorSetting());
        assertEquals(TestXmlDataParser.class, formatter.getDataParser().getClass());
    }

    /**
     * XMLパーサーを指定しないXMLフォーマッターの生成テストを行います。<br>
     * 
     * 条件：<br>
     *   リポジトリにテスト用パーサークラスを指定せず、XMLフォーマッターを生成する。<br>
     *   
     * 期待結果：<br>
     *   コンバータの設定情報として{@link XmlDataConvertorSetting}が設定されていること。<br>
     *   {@link XmlDataParser}がパーサーとして使用されていること。<br>
     */
    @Test
    public void testXmlDataRecordFormatterDefaultParser() {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/XmlDataRecordFormatterDefault.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
        
        XmlDataRecordFormatter formatter = new XmlDataRecordFormatter();
        assertEquals(XmlDataConvertorSetting.getInstance(), formatter.getConvertorSetting());
        assertEquals(XmlDataParser.class, formatter.getDataParser().getClass());
    }
    
    /**
     * テスト用パーサークラス
     */
    public static class TestXmlDataParser implements StructuredDataParser {
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
    public static class TestXmlDataBuilder implements StructuredDataBuilder {
        @Override
        public void buildData(Map<String, ?> map, LayoutDefinition layoutDef,
                OutputStream out) throws IOException,
                InvalidDataFormatException {
        }
    }
}
