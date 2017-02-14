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
import static org.hamcrest.MatcherAssert.assertThat;
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

        FixedLengthConvertorSetting setting = FixedLengthConvertorSetting.getInstance();
        Map<String, Class<?>> resultTable = setting.getConvertorFactory().getConvertorTable();
        assertSame(SingleByteCharacterString.class, resultTable.get("Test"));
        assertSame(Bytes.class, resultTable.get("Hoge"));
    }

    /**
     * 後方互換用の空文字列を{@code null}にするフラグが
     * デフォルトで{@code true}になってること。
     */
    @Test
    public void testDefaultConvertEmptyToNull() throws Exception {
        FixedLengthConvertorSetting setting = FixedLengthConvertorSetting.getInstance();

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

        FixedLengthConvertorSetting setting = FixedLengthConvertorSetting.getInstance();
        assertThat(setting.isConvertEmptyToNull(), is(false));
    }
}
