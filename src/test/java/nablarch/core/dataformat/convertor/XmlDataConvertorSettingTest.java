package nablarch.core.dataformat.convertor;

import nablarch.core.dataformat.convertor.datatype.Bytes;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.logicbased.CustomType;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * XmlDataConvertorのテスト。
 * 
 * 観点：
 * デフォルト設定での動作は別のテストで確認しているので、ここではリポジトリから設定できることのテストを行う。
 * 
 * @author TIS
 */
public class XmlDataConvertorSettingTest {

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
    }

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

        XmlDataConvertorSetting setting = XmlDataConvertorSetting.getInstance();
        Map<String, Class<?>> resultTable = setting.getConvertorFactory().getConvertorTable();
        assertSame(CharacterStreamDataString.class, resultTable.get("Test"));
        assertSame(Bytes.class, resultTable.get("Hoge"));
    }

    /**
     * コンバータファクトリをリポジトリから設定するテスト
     */
    @Test
    public void testCustomConvertorFactory() throws Exception {
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/LogicBasedSetting.xml")));

        XmlDataConvertorSetting setting = XmlDataConvertorSetting.getInstance();
        Map<String, Class<?>> resultTable = setting.getConvertorFactory().getConvertorTable();
        assertSame("データタイプが追加されていること", CustomType.class, resultTable.get("custom"));

        Map<String, Class<?>> defaultTable = new XmlDataConvertorFactory().getDefaultConvertorTable();
        assertThat("デフォルトから1つだけ追加されていること", resultTable.size(), is(defaultTable.size() + 1));
        for (Entry<String, Class<?>> entry : defaultTable.entrySet()) {
            assertSame("デフォルトと同一のデータタイプが設定されていること", entry.getValue(), resultTable.get(entry.getKey()));
        }
    }
}
