package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DataFormatConfigFinderTest {

    @Before
    public void setUp() {
        System.setProperty("nablarch.dataformat.flushEachRecordInWriting", Boolean.TRUE.toString());
        SystemRepository.clear();
    }

    @After
    public void tearDown() {
        System.getProperties().remove("nablarch.dataformat.flushEachRecordInWriting");
        SystemRepository.clear();
    }

    /**
     * 設定変更のテスト。
     */
    @Test
    public void testConfiguration() {
        String configPath = "nablarch/core/dataformat/data-format-config-finder-test.xml";
        DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(configPath));
        SystemRepository.load(container);
        assertThat(
                DataFormatConfigFinder.getDataFormatConfig().isFlushEachRecordInWriting(),
                is(Boolean.FALSE));
    }

    /**
     * デフォルトコンフィグレーションのテスト。
     */
    @Test
    public void testDefaultConfiguration() {
        String configPath = "nablarch/core/dataformat.xml";
        DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(configPath));
        SystemRepository.load(container);
        assertThat(
                DataFormatConfigFinder.getDataFormatConfig().isFlushEachRecordInWriting(),
                is(Boolean.TRUE));
    }
}
