package nablarch.core.dataformat.convertor;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;

import nablarch.core.dataformat.convertor.datatype.Bytes;
import nablarch.core.dataformat.convertor.datatype.SingleByteCharacterString;
import nablarch.core.dataformat.convertor.logicbased.CustomType;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * VariableLengthConverterSettingのテスト。
 * 
 * 観点：
 * デフォルト設定での動作は別のテストで確認しているので、ここではリポジトリから設定できることのテストを行う。
 * 
 * @author Masato Inoue
 */
public class VariableLengthConverterSettingTest {

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
    }

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }

    /**
     * 不正なNibbleのテスト。
     */
    @Test
    public void testIllegalNibble(){
        FixedLengthConvertorSetting setting = new FixedLengthConvertorSetting();
        
        try{
            setting.setDefaultPositiveZoneSignNibble("g");
            fail();
        } catch(IllegalStateException e) {
            assertEquals("invalid nibble format was specified. nibble=[g]. valid format=[a-fA-F0-9].", e.getMessage());
        }

        try{
            setting.setDefaultNegativeZoneSignNibble("-1");
            fail();
        } catch(IllegalStateException e) {
            assertEquals("invalid nibble format was specified. nibble=[-1]. valid format=[a-fA-F0-9].", e.getMessage());
        }


        try{
            setting.setDefaultPositivePackSignNibble("G");
            fail();
        } catch(IllegalStateException e) {
            assertEquals("invalid nibble format was specified. nibble=[G]. valid format=[a-fA-F0-9].", e.getMessage());
        }

        try{
            setting.setDefaultNegativePackSignNibble("");
            fail();
        } catch(IllegalStateException e) {
            assertEquals("invalid nibble format was specified. nibble=[]. valid format=[a-fA-F0-9].", e.getMessage());
        }
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

        VariableLengthConvertorSetting setting = VariableLengthConvertorSetting.getInstance();
        Map<String, Class<?>> resultTable = setting.getConvertorFactory().getConvertorTable();
        assertSame(SingleByteCharacterString.class, resultTable.get("Test"));
        assertSame(Bytes.class, resultTable.get("Hoge"));
    }

    @Test
    public void testLogicBasedConverterTable() throws Exception {
        final XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/convertor/LogicBasedSetting.xml");
        SystemRepository.load(new DiContainer(loader));

        final VariableLengthConvertorSetting setting = VariableLengthConvertorSetting.getInstance();
        final Map<String, Class<?>> actual = setting.getConvertorFactory()
                                                   .getConvertorTable();

        assertSame("ロジックで追加したデータタイプが取得できる", CustomType.class, actual.get("custom"));
        
        final Map<String, Class<?>> defaultConverterTable = new VariableLengthConvertorFactory().getDefaultConvertorTable();

        System.out.println("actual.size() = " + actual.size());
        System.out.println("actual.keySet().size() = " + actual.keySet()
                                                               .size());
        System.out.println("defaultConverterTable = " + defaultConverterTable.size());
        assertThat("デフォルトから1つ追加されていること", actual.keySet(), hasSize(defaultConverterTable.size() + 1));
        
        for (final Map.Entry<String, Class<?>> entry : defaultConverterTable
                                                                                           .entrySet()) {
            assertSame("デフォルトの設定値が引き継がれていること", entry.getValue(), actual.get(entry.getKey()));
        }
    }

    /**
     * 後方互換用の空文字列を{@code null}にするフラグが
     * デフォルトで{@code true}になってること。
     */
    @Test
    public void testDefaultConvertEmptyToNull() throws Exception {
        VariableLengthConvertorSetting setting = new VariableLengthConvertorSetting();
        assertThat(setting.isConvertEmptyToNull(), is(true));
    }

    /**
     * 後方互換用の空文字列を{@code null}にするフラグが
     * 設定を記述することで{@code false}になること。
     */
    @Test
    public void testSetConvertEmptyToNull() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/core/dataformat/convertor/ConvertorSettingCompatible.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        VariableLengthConvertorSetting setting = VariableLengthConvertorSetting.getInstance();
        assertThat(setting.isConvertEmptyToNull(), is(false));
    }
}
