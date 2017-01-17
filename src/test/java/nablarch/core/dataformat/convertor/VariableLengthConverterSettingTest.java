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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;

/**
 * VariableLengthConverterSettingのテスト。
 * 
 * 観点：
 * デフォルト設定での動作は別のテストで確認しているので、ここではリポジトリから設定できることのテストを行う。
 * 
 * @author Masato Inoue
 */
public class VariableLengthConverterSettingTest {
    
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

    /**
     * 後方互換用の未入力を空文字にするフラグが
     * デフォルトでfalseになってること。
     */
    @Test
    public void testDefaultNotEnteredToEmpty() throws Exception {
        VariableLengthConvertorSetting setting = new VariableLengthConvertorSetting();
        assertThat(setting.getNotEnteredToEmpty(), is(false));
    }

    /**
     * 後方互換用の未入力を空文字にするフラグが
     * 設定を記述することでtrueになること。
     */
    @Test
    public void testSetterNotEnteredToEmpty() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/core/dataformat/convertor/ConvertorSettingCompatible.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        VariableLengthConvertorSetting setting = VariableLengthConvertorSetting.getInstance();
        assertThat(setting.getNotEnteredToEmpty(), is(true));
    }
}
