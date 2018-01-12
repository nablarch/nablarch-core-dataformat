package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link CharacterReplacementManager}のテスト。
 *
 * @author Masato Inoue
 */
public class CharacterReplacementManagerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource(null);

    @Before
    public void setUp() throws Exception {
        OnMemoryLogWriter.clear();
    }

    /**
     * 寄せ字変換処理の正常系テスト（ms932）。
     */
    @Test
    public void testNormal() throws Exception {
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/CharacterReplacementManagerTest/testNormal.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~=#!:";

        // 全角文字の寄せ字変換を行うテスト（髙→高、﨑→崎、唖→■に変換される）
        OnMemoryLogWriter.clear();
        final CharacterReplacementManager sut = CharacterReplacementManager.getInstance();

        // 変換後の文字列
        assertThat(sut.replaceCharacter("type_zenkaku", before), is("ABCあいう高崎■\\~=#!:"));

        // ログの確認
        List<String> messages = OnMemoryLogWriter.getMessages("writer.memory");
        assertThat(messages.size(), is(3));
        assertThat(messages.get(0), containsString(
                "replace character. from=[髙](\\u9ad9), to=[高](\\u9ad8) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));
        assertThat(messages.get(1), containsString(
                "replace character. from=[﨑](\\ufa11), to=[崎](\\u5d0e) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));
        assertThat(messages.get(2), containsString(
                "replace character. from=[唖](\\u5516), to=[■](\\u25a0) input=[ABCあいう髙﨑唖\\~=#!:], typeName=[type_zenkaku]."));


        /*
         * 寄せ字タイプ名「type_hankaku」で半角文字の寄せ字変換を行うテスト（~→[、\→[、#→z 、!→z 、!→Z 、:→Zに変換される）
         */

        // 変換後の文字列
        assertThat(sut.replaceCharacter("type_hankaku", before), is("ABCあいう髙﨑唖[[zzZZ"));

    }

    /**
     * sjisの場合のテスト。
     * ms932とは異なる変換が行われることを確認する。
     */
    @Test
    public void testSjis_invalid() throws Exception {

        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/CharacterReplacementManagerTest/testSjis_invalid.xml");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("length of the byte string after the conversion is invalid. "
                + "property file=[classpath:nablarch/core/dataformat/type_zenkaku.properties], type name=[type_zenkaku], encoding=[sjis], "
                + "from: {character=[?], bytes=[63], byte length=[1], unicode character=[﨑](\\ufa11)}, "
                + "to: {character=[崎], bytes=[-115, -24], bytes length=[2]}, unicode character=[崎](\\u5d0e)}.");
        new DiContainer(loader);
    }

    /**
     *
     */
    @Test
    public void testSjis() {
        // テスト用のリポジトリ構築
        final XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/CharacterReplacementManagerTest/testSjis.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~";
        String after = CharacterReplacementManager.getInstance()
                                                  .replaceCharacter("type_hankaku", before);

        // 変換後の文字列
        assertThat(after, is("ABCあいう髙﨑唖[["));
    }

    /**
     * プロパティファイルに定義された変換後の文字列が空文字（文字列長が0）の場合のテスト。
     */
    @Test
    public void testConvertedLengthEmpty() throws Exception {

        // 右辺の文字数が0のパターン
        String filePath = "classpath:nablarch/core/dataformat/type_empty_string_length_right.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";

        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);

        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);

        characterUtil.setConfigList(list);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                "invalid character was specified. replacement character length must be '1', but was '0'. "
                        + "property file=[classpath:nablarch/core/dataformat/type_empty_string_length_right.properties], key=[v](\\u0076), invalid str=[]().");

        characterUtil.initialize();
    }

    /**
     * プロパティファイルに定義された変換前または変換後の文字列長が2の場合のテスト。
     */
    @Test
    public void testInvalidStringLength1() throws Exception {

        // 右辺の文字数が2のパターン
        String filePath = "classpath:nablarch/core/dataformat/type_invalid_string_length_left.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";

        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);

        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);

        characterUtil.setConfigList(list);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("invalid character was specified. "
                + "replacement character length must be '1', but was '2'. "
                + "property file=[classpath:nablarch/core/dataformat/type_invalid_string_length_left.properties], "
                + "key=[~■](\\u007e\\u25a0), invalid str=[~■](\\u007e\\u25a0).");
        characterUtil.initialize();
        fail();
    }

    @Test
    public void testInvalidStringLength2() {

        // 左辺の文字数が2のパターン
        String filePath = "classpath:nablarch/core/dataformat/type_invalid_string_length_right.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";

        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);

        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        final List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);

        characterUtil.setConfigList(list);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("invalid character was specified. "
                + "replacement character length must be '1', but was '2'. "
                + "property file=[classpath:nablarch/core/dataformat/type_invalid_string_length_right.properties], "
                + "key=[v](\\u0076), invalid str=[~[](\\u007e\\u005b).");
        characterUtil.initialize();
    }

    /**
     * プロパティファイルに定義された変換前と変換後の文字のバイト長が異なる文字が設定された場合のテスト。
     */
    @Test
    public void testInvalidByteLength() throws Exception {

        String filePath = "classpath:nablarch/core/dataformat/type_invalid_byte_length2.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";


        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);

        config.setByteLengthCheck(true); // バイト長チェックを有効にする

        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);

        characterUtil.setConfigList(list);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("length of the byte string after the conversion is invalid. "
                + "property file=[classpath:nablarch/core/dataformat/type_invalid_byte_length2.properties], type name=[type_invalid], encoding=[ms932], "
                + "from: {character=[~], bytes=[126], byte length=[1], unicode character=[~](\\u007e)}, "
                + "to: {character=[■], bytes=[-127, -95], bytes length=[2]}, unicode character=[■](\\u25a0)}.");
        characterUtil.initialize();
    }

    @Test
    public void testDisabledByteLength() throws Exception {

        String filePath = "classpath:nablarch/core/dataformat/type_invalid_byte_length.properties";
        String typeName = "type_invalid";
        String encoding = "ms932";


        CharacterReplacementConfig config = new CharacterReplacementConfig();
        config.setTypeName(typeName);
        config.setFilePath(filePath);
        config.setEncoding(encoding);
        config.setByteLengthCheck(false); // バイト長チェックを無効にする

        CharacterReplacementManager characterUtil = new CharacterReplacementManager();
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);

        characterUtil.setConfigList(list);
        characterUtil.initialize();

        // 変換前の文字列
        String before = "ABCあいう髙﨑唖\\~";
        String after = characterUtil.replaceCharacter("type_invalid", before);

        // 変換後の文字列
        assertThat(after, is("ABCあいう髙﨑[\\■"));
    }

    @Test
    public void testTypeNull() throws Exception {
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
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);

        characterUtil.setConfigList(list);
        characterUtil.initialize();

        String testInput = "ABCあいう髙﨑唖\\~";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("type name was blank. type name must not be null. input=[" + testInput + "].");
        characterUtil.replaceCharacter(null, testInput);
    }

    @Test
    public void testTypeBlank() throws Exception {
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
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);

        characterUtil.setConfigList(list);
        characterUtil.initialize();

        String testInput = "ABCあいう髙﨑唖\\~";

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("type name was blank. type name must not be null. input=[" + testInput + "].");
        characterUtil.replaceCharacter("", testInput);
    }

    @Test
    public void testNotFoundTypeName() throws Exception {
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


        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("type name was not found. type name=[notExist], settable type name=");
        characterUtil.replaceCharacter("notExist", testInput);
    }

    @Test
    public void 変換対象文字がnullや空文字列はなにもしないこと() throws Exception {

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
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);

        characterUtil.setConfigList(list);
        characterUtil.initialize();

        assertThat(characterUtil.replaceCharacter("type_zenkaku", null), nullValue());
        assertThat(characterUtil.replaceCharacter("type_zenkaku", ""), isEmptyString());
    }

    /**
     * 定義された寄せ字変換のタイプ名が重複している場合のテスト。
     */
    @Test
    public void testDuplicateErrorTypeName() throws Exception {

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
        List<CharacterReplacementConfig> list = new ArrayList<CharacterReplacementConfig>();
        list.add(config);
        list.add(config2);

        characterUtil.setConfigList(list);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("duplicate replacement type was set. type name=[type_zenkaku].");
        characterUtil.initialize();
    }

    /**
     * プロパティが設定されない場合のテスト。
     */
    @Test
    public void testPropertyNotSet() throws Exception {

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
    public void testInvalidEncoding() throws Exception {

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

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("invalid encoding was specified."
                + " property file=[classpath:nablarch/core/dataformat/type_zenkaku.properties],"
                + " type name=[type_zenkaku], encoding=[invalid!!].");

        characterUtil.initialize();
    }

    /**
     * WAVE DUSHを置換するテスト。（バイト長のチェックは行わない）
     */
    @Test
    public void testWaveDush() throws Exception {
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/CharacterReplacementManagerTest/testWaveDush.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        // 変換前の文字列
        // ～（WAVE DASH） ∥（DOUBLE VERTICAL LINE） - （MINUS SIGN） ￠（CENT SIGN）￡ （POUND SIGN） ￢（NOT SIGN）
        String before = "～ ∥- ￠ ￡ ￢";

        String after = CharacterReplacementManager.getInstance()
                                                  .replaceCharacter("type_wave_dush", before);

        // 変換後の文字列
        assertThat(after, is("〜 ‖- ¢ £ ¬"));
    }
}
