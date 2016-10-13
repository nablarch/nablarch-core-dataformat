package nablarch.core.dataformat;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link CharacterReplacementManager}のテスト。
 * @author Masato Inoue
 */
public class CharacterReplacementManagerTest {

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }

    /**
     * 寄せ字変換処理の正常系テスト（ms932）。
     */
    @Test
    public void testNormal() throws Exception{
        File diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
            <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
                  <component name="characterReplacementManager"
                        class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_zenkaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_zenkaku.properties"/>
                                    <property name="encoding" value="ms932"/>
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="ms932"/>
                                </component>
                            </list>
                        </property>
                  </component>
                    <component name="initializer"
                               class="nablarch.core.repository.initialization.BasicApplicationInitializer">
                        <property name="initializeList">
                            <list>
                                <component-ref name="characterReplacementManager" />
                            </list>
                        </property>
                    </component>
            </component-configuration>
        ***************************************************************************************************************/      
        diConfig.deleteOnExit();

        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~=#!:"; 

        OnMemoryLogWriter.clear();


        /*
         * 全角文字の寄せ字変換を行うテスト（髙→高、﨑→崎、唖→■に変換される）
         */
        String after = CharacterReplacementManager.getInstance().replaceCharacter("type_zenkaku", before);
        
        // 変換後の文字列
        assertEquals("ABCあいう高崎■\\~=#!:", after); 

        // ログの確認
        List<String> messages = OnMemoryLogWriter.getMessages("writer.memory");
        assertEquals(3, messages.size());
        assertTrue(messages.get(0).contains("replace character. from=[髙](\\u9ad9), to=[高](\\u9ad8) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));
        assertTrue(messages.get(1).contains("replace character. from=[﨑](\\ufa11), to=[崎](\\u5d0e) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));
        assertTrue(messages.get(2).contains("replace character. from=[唖](\\u5516), to=[■](\\u25a0) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));

        
        /*
         * 寄せ字タイプ名「type_hankaku」で半角文字の寄せ字変換を行うテスト（~→[、\→[、#→z 、!→z 、!→Z 、:→Zに変換される）
         */
        after = CharacterReplacementManager.getInstance().replaceCharacter("type_hankaku", before);
        
        // 変換後の文字列
        assertEquals("ABCあいう髙﨑唖[[zzZZ", after); 

    }

    /**
     * sjisの場合のテスト。
     * ms932とは異なる変換が行われることを確認する。
     */
    @Test
    public void testSjis() throws Exception{
        
        /*
         * 寄せ字変換定義ファイルの内容は{@link #testBasicUsage()}（MS932）と同じだが、
         * sjisでは、﨑という文字は1バイトの「?」に変換されてしまうため、初期化時にバイト長の不一致により例外がスローされる。
         */
        File diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
            <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
                  <component name="characterReplacementManager"
                        class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_zenkaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_zenkaku.properties"/>
                                    <property name="encoding" value="sjis"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="sjis"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                            </list>
                        </property>
                  </component>
                    <component name="initializer"
                               class="nablarch.core.repository.initialization.BasicApplicationInitializer">
                        <property name="initializeList">
                            <list>
                                <component-ref name="characterReplacementManager" />
                            </list>
                        </property>
                    </component>
            </component-configuration>
        ***************************************************************************************************************/      
        diConfig.deleteOnExit();
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");

        try {
            new DiContainer(loader);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("length of the byte string after the conversion is invalid. " +
                    "property file=[classpath:nablarch/core/dataformat/type_zenkaku.properties], type name=[type_zenkaku], encoding=[sjis], " +
                    "from: {character=[?], bytes=[63], byte length=[1], unicode character=[﨑](\\ufa11)}, " +
                    "to: {character=[崎], bytes=[-115, -24], bytes length=[2]}, unicode character=[崎](\\u5d0e)}.", e.getMessage());
        }
        
        SystemRepository.clear();
        
        
        /*
         * 初期化時にバイト長の不一致が起こらないようにして、正常系のテストを行う。
         */
        diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
            <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
                  <component name="characterReplacementManager"
                        class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="sjis"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                            </list>
                        </property>
                  </component>
                    <component name="initializer"
                               class="nablarch.core.repository.initialization.BasicApplicationInitializer">
                        <property name="initializeList">
                            <list>
                                <component-ref name="characterReplacementManager" />
                            </list>
                        </property>
                    </component>
            </component-configuration>
        ***************************************************************************************************************/      
        diConfig.deleteOnExit();
        
        
        // テスト用のリポジトリ構築
        loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);   
        
        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~"; 

        String after = CharacterReplacementManager.getInstance().replaceCharacter("type_hankaku", before);
        
        // 変換後の文字列
        assertEquals("ABCあいう髙﨑唖[[", after); 
    }
    
    /**
     * プロパティファイルに定義された変換後の文字列が空文字（文字列長が0）の場合のテスト。
     */
    @Test
    public void testConvertedLengthEmpty() throws Exception{
         
        // 右辺の文字数が0のパターン
        String filePath = "classpath:nablarch/core/dataformat/type_empty_string_length_right.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("invalid character was specified. replacement character length must be '1', but was '0'. " +
                    "property file=[classpath:nablarch/core/dataformat/type_empty_string_length_right.properties], key=[v](\\u0076), invalid str=[]().", e.getMessage());
        }
    }
    
    /**
     * プロパティファイルに定義された変換前または変換後の文字列長が2の場合のテスト。
     */
    @Test
    public void testInvalidStringLength() throws Exception{
         
        // 右辺の文字数が2のパターン
        String filePath = "classpath:nablarch/core/dataformat/type_invalid_string_length_left.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "invalid character was specified. " +
                    "replacement character length must be '1', but was '2'. " +
                    "property file=[classpath:nablarch/core/dataformat/type_invalid_string_length_left.properties], " +
                    "key=[~■](\\u007e\\u25a0), invalid str=[~■](\\u007e\\u25a0).", 
                    e.getMessage());
        }

        // 左辺の文字数が2のパターン
        filePath = "classpath:nablarch/core/dataformat/type_invalid_string_length_right.properties";
        config.setFilePath(filePath);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "invalid character was specified. " +
                    "replacement character length must be '1', but was '2'. " +
                    "property file=[classpath:nablarch/core/dataformat/type_invalid_string_length_right.properties], " +
                    "key=[v](\\u0076), invalid str=[~[](\\u007e\\u005b).", 
                    e.getMessage());
        }
    }


    /**
     * プロパティファイルに定義された変換前と変換後の文字のバイト長が異なる文字が設定された場合のテスト。
     */
    @Test
    public void testInvalidByteLength() throws Exception{
        
        String filePath = "classpath:nablarch/core/dataformat/type_invalid_byte_length.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";
        

        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        config.setByteLengthCheck(true); // バイト長チェックを有効にする
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "length of the byte string after the conversion is invalid. " +
                    "property file=[classpath:nablarch/core/dataformat/type_invalid_byte_length.properties], type name=[type_invalid], encoding=[ms932], " +
                    "from: {character=[~], bytes=[126], byte length=[1], unicode character=[~](\\u007e)}, " +
                    "to: {character=[■], bytes=[-127, -95], bytes length=[2]}, unicode character=[■](\\u25a0)}.", 
                    e.getMessage());
        }
        
        
        config.setByteLengthCheck(false); // バイト長チェックを無効にする
        
        // 初期化時にエラーが発生しない
        characterUtil.initialize();
        
        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~"; 

        
        /**
         * バイト長チェックを無効にした場合に、例外が発生せず、正常に寄せ字変換が行われることの確認。
         */
        String after = characterUtil.replaceCharacter("type_invalid", before);
        
        // 変換後の文字列
        assertEquals("ABCあいう髙﨑[\\■", after); 
    }

    /**
     * 寄せ字実行メソッドのパラメータのパターン。
     */
    @Test
    public void testExecutionArg() throws Exception{
        
        String filePath = "classpath:nablarch/core/dataformat/type_zenkaku.properties";
        String typeName = "type_zenkaku";
        String encoding = "ms932";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        CharacterReplacementConfig config2 = new CharacterReplacementConfig();
        config2.setTypeName("type_zenkaku_copy");
        config2.setFilePath(filePath);
        config2.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);
        
        characterUtil.setConfigList(list);
        characterUtil.initialize();
        
        String testInput = "ABCあいう髙﨑唖\\~";
        
        /*
         * タイプ名がnull。
         */
        try {
            characterUtil.replaceCharacter(null, testInput);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("type name was blank. type name must not be null. input=[" + testInput + "].", e.getMessage());
        }
        /*
         * タイプ名が空文字。
         */
        try {
            characterUtil.replaceCharacter("", testInput);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("type name was blank. type name must not be null. input=[" + testInput + "].", e.getMessage());
        }
        
        /*
         * 引数がnullまたは空文字の場合は何もしない。
         */
        assertNull(characterUtil.replaceCharacter("type_zenkaku", null));
        assertEquals("", characterUtil.replaceCharacter("type_zenkaku", ""));


        /*
         * 引数で指定された寄せ字タイプ名が存在しない場合。
         */
        try {
            characterUtil.replaceCharacter("notExist", testInput);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("type name was not found. type name=[notExist], settable type name="));
            assertTrue(e.getMessage().contains("type_zenkaku"));
            assertTrue(e.getMessage().contains("type_zenkaku_copy"));
            assertTrue(e.getMessage().contains("input=[" + testInput + "]."));
        }
        assertTrue(true);

        
    }
    
    /**
     * 定義された寄せ字変換のタイプ名が重複している場合のテスト。
     */
    @Test
    public void testDuplicateErrorTypeName() throws Exception{
        
        String filePath = "classpath:nablarch/core/dataformat/type_zenkaku.properties";
        String typeName = "type_zenkaku";
        String encoding = "ms932";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        // タイプ名が重複
        CharacterReplacementConfig config2 = new CharacterReplacementConfig();
        config2.setTypeName(typeName);
        config2.setFilePath(filePath);
        config2.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);
        
        characterUtil.setConfigList(list);

        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("duplicate replacement type was set. type name=[type_zenkaku].", e.getMessage());
        }
        
    }

    /**
     * プロパティが設定されない場合のテスト。
     */
    @Test
    public void testPropertyNotSet() throws Exception{

        /*
         * ファイルパスがnullのパターン。
         */
        String filePath = null;
        String typeName = "type_zenkaku";
        String encoding = "ms932";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'filePath' was not set. property 'filePath' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[type_zenkaku].", 
                    e.getMessage());
        }
     

        /*
         * ファイルパスが空文字のパターン。
         */
        filePath = "";
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'filePath' was not set. property 'filePath' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[type_zenkaku].", 
                    e.getMessage());
        }
        

        /*
         * タイプ名がnullのパターン。
         */
        filePath = "classpath:nablarch/core/dataformat/type_zenkaku.properties";
        typeName = null;
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'typeName' was not set. property 'typeName' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[null].", 
                    e.getMessage());
        }

        /*
         * タイプ名が空文字のパターン。
         */
        typeName = "";
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'typeName' was not set. property 'typeName' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[].", 
                    e.getMessage());
        }

        /*
         * エンコーディングがnullのパターン。
         */
        typeName = "type_normal";
        encoding = null;
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'encoding' was not set. property 'encoding' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[type_normal].", 
                    e.getMessage());
        }

        /*
         * エンコーディングが空文字のパターン。
         */
        typeName = "type_normal";
        encoding = "";
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "property 'encoding' was not set. property 'encoding' must be set. class=[nablarch.core.dataformat.CharacterReplacementConfig] type name=[type_normal].", 
                    e.getMessage());
        }

        /*
         * 不正なエンコーディングのパターン。
         */
        typeName = "type_normal";
        encoding = "abc";
        
        config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        characterUtil = new CharacterReplacementManager();
        list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "invalid encoding was specified. property file=[classpath:nablarch/core/dataformat/type_zenkaku.properties], type name=[type_normal], encoding=[abc].", 
                    e.getMessage());
        }
        
    }
    

    /**
     * 不正なエンコーディングを設定するテスト。
     */
    @Test
    public void testInvalidEncoding() throws Exception{
        
        String filePath = "classpath:nablarch/core/dataformat/type_zenkaku.properties";
        String typeName = "type_zenkaku";
        String encoding = "invalid!!";
        
        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        
        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        ArrayList<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        
        characterUtil.setConfigList(list);
        
        try {
            characterUtil.initialize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "invalid encoding was specified. property file=[classpath:nablarch/core/dataformat/type_zenkaku.properties], type name=[type_zenkaku], encoding=[invalid!!].", 
                    e.getMessage());
        }
        
    }
    
    /**
     * WAVE DUSHを置換するテスト。（バイト長のチェックは行わない）
     */
    @Test
    public void testWaveDush() throws Exception{
        
        File diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
        <?xml version="1.0" encoding="UTF-8"?>
            <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
                  <component name="characterReplacementManager"
                        class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_wave_dush"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_wave_dush.properties"/>
                                    <property name="encoding" value="sjis"/>
                                    <property name="byteLengthCheck" value="false" />
                                </component>
                            </list>
                        </property>
                  </component>
                    <component name="initializer"
                               class="nablarch.core.repository.initialization.BasicApplicationInitializer">
                        <property name="initializeList">
                            <list>
                                <component-ref name="characterReplacementManager" />
                            </list>
                        </property>
                    </component>
            </component-configuration>
        ***************************************************************************************************************/      
        diConfig.deleteOnExit();
        
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);   
        
        // 変換前の文字列
        // ～（WAVE DASH） ∥（DOUBLE VERTICAL LINE） - （MINUS SIGN） ￠（CENT SIGN）￡ （POUND SIGN） ￢（NOT SIGN）
        String before = "～ ∥- ￠ ￡ ￢"; 

        String after = CharacterReplacementManager.getInstance().replaceCharacter("type_wave_dush", before);
        
        // 変換後の文字列
        assertEquals("〜 ‖- ¢ £ ¬", after); 
    }
}
