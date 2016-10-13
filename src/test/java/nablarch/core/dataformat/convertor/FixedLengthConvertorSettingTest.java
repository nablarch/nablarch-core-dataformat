package nablarch.core.dataformat.convertor;

import nablarch.core.dataformat.convertor.datatype.Bytes;
import nablarch.core.dataformat.convertor.datatype.SingleByteCharacterString;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertSame;

/**
 * FixedLengthConvertorのテスト。
 * 
 * 観点：
 * デフォルト設定での動作は別のテストで確認しているので、ここではリポジトリから設定できることのテストを行う。
 * 
 * @author Masato Inoue
 */
public class FixedLengthConvertorSettingTest {

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }
    
    /**
     * コンバータをリポジトリから設定するテスト
     */
    @Test
    public void testRepository() throws Exception {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/ConvertorSetting.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        FixedLengthConvertorSetting setting = new FixedLengthConvertorSetting();
        Map<String, Class<?>> resultTable = setting.getConvertorFactory().getConvertorTable();
        assertSame(SingleByteCharacterString.class, resultTable.get("Test"));
        assertSame(Bytes.class, resultTable.get("Hoge"));
        
        SystemRepository.clear();
        
        
        // デフォルトのリポジトリに戻す
        loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/DefaultConvertorSetting.xml");
        container = new DiContainer(loader);
        SystemRepository.load(container);
        
    }
    
}
