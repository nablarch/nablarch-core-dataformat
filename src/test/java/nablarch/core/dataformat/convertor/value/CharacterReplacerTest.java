package nablarch.core.dataformat.convertor.value;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.CharacterReplacementResult;
import nablarch.core.dataformat.CharacterReplacementUtil;
import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link CharacterReplacer}のテスト。
 * @author Masato Inoue
 */
public class CharacterReplacerTest {

    private String LS = System.getProperty("line.separator");

    private DataRecordFormatter formatter = null;
    
    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(file);
    }

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
        SystemRepository.clear();
    }

    @Before
    public void setUp() throws Exception {
        
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
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="ms932"/>
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
     
     
     ThreadContext.clear();
     
    }
    
    /**
     * ファイルを読み込む場合に、寄せ字が行われることのテスト。
     */
    @Test
    public void testRead() throws Exception {
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        
        /*
         * 半角文字について寄せ字が行われることのテスト。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X    replacement("type_hankaku")   # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        001,DEF,あ髙﨑
        002,G\~,え唖か
        **********************************************************************/
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes("ms932"));
        
        createFormatter(formatFile);
        formatter.setInputStream(inputStream);
        formatter.initialize();

        // ~ → [ 、 \ → [ 、に寄せ字変換されることの確認
        DataRecord readRecord = formatter.readRecord();
        assertEquals("001", readRecord.get("str1"));
        assertEquals("DEF", readRecord.get("str2"));
        assertEquals("あ髙﨑", readRecord.get("str3"));
        readRecord = formatter.readRecord();
        assertEquals("002", readRecord.get("str1"));
        assertEquals("G[[", readRecord.get("str2"));
        assertEquals("え唖か", readRecord.get("str3"));
        
        formatter.close();
        inputStream.close();
        
        

        /*
         * 全角文字について寄せ字が行われることのテスト。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N  replacement("type_zenkaku")     # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        001,DEF,あ髙﨑
        002,G\~,え唖か
        **********************************************************************/
        
        inputStream = new ByteArrayInputStream(data.getBytes("ms932"));
        
        createFormatter(formatFile);
        formatter.setInputStream(inputStream);
        formatter.initialize();

        // 髙　→　高　、　﨑　→　崎　、唖　→　■ 、に寄せ字変換されることの確認
        readRecord = formatter.readRecord();
        assertEquals("001", readRecord.get("str1"));
        assertEquals("DEF", readRecord.get("str2"));
        assertEquals("あ高崎", readRecord.get("str3"));

    	// スレッドコンテキストの内容を検証
        CharacterReplacementResult resultData = CharacterReplacementUtil.getResult("str1");
        assertEquals(null, resultData);
        
        resultData = CharacterReplacementUtil.getResult("str2");
        assertEquals("G\\~", resultData.getInputString());
        assertEquals("G[[", resultData.getResultString());
        
        resultData = CharacterReplacementUtil.getResult("str3");
        assertEquals("あ髙﨑", resultData.getInputString());
        assertEquals("あ高崎", resultData.getResultString());
        
        readRecord = formatter.readRecord();
        assertEquals("002", readRecord.get("str1"));
        assertEquals("G\\~", readRecord.get("str2"));
        assertEquals("え■か", readRecord.get("str3"));

    	// スレッドコンテキストの内容を検証
        resultData = CharacterReplacementUtil.getResult("str1");
        assertEquals(null, resultData);
        
        resultData = CharacterReplacementUtil.getResult("str2");
        assertEquals("G\\~", resultData.getInputString());
        assertEquals("G[[", resultData.getResultString());
        
        resultData = CharacterReplacementUtil.getResult("str3");
        assertEquals("え唖か", resultData.getInputString());
        assertEquals("え■か", resultData.getResultString());
        
        formatter.close();
        inputStream.close();
    }


    /**
     * ファイルを書き込む場合に、寄せ字が行われることのテスト。
     */
    @Test
    public void testWrite() throws Exception {
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        
        /*
         * 半角文字について寄せ字が行われることのテスト。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X   replacement("type_hankaku")   # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream);
        formatter.initialize();

        DataRecord dataRecord1 = new DataRecord(){{
            put("str1", "001");
            put("str2", "DEF");
            put("str3", "あ髙﨑");
        }};
        DataRecord dataRecord2 = new DataRecord(){{
            put("str1", "002");
            put("str2", "G\\~");
            put("str3", "え唖か");
        }};
        
        formatter.writeRecord(dataRecord1);
        formatter.writeRecord(dataRecord2);
        
        
        String result = outputStream.toString("ms932");

        // ~ → [ 、 \ → [ 、に寄せ字変換されることの確認
        String expected = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        "001","DEF","あ髙﨑"
        "002","G[[","え唖か"
        **********************************************************************/
        
        assertEquals(expected, result);
        
        formatter.close();
        outputStream.close();
        
        
        
        /*
         * 全角文字について寄せ字が行われることのテスト。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N   replacement("type_zenkaku")   # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        
        outputStream = new ByteArrayOutputStream();
        
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream);
        formatter.initialize();

        dataRecord1 = new DataRecord(){{
            put("str1", "001");
            put("str2", "DEF");
            put("str3", "あ髙﨑");
        }};
        dataRecord2 = new DataRecord(){{
            put("str1", "002");
            put("str2", "G\\~");
            put("str3", "え唖か");
        }};
        
        formatter.writeRecord(dataRecord1);
        formatter.writeRecord(dataRecord2);
        
        
        result = outputStream.toString("ms932");

        // 高 → 高 、 﨑 → 崎 、唖 → ■ 、に寄せ字変換されることの確認
        expected = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        "001","DEF","あ高崎"
        "002","G\~","え■か"
        **********************************************************************/
        
        assertEquals(expected, result);
        
        formatter.close();
        outputStream.close();
        

    }


    /**
     * 不正なデータタイプでWriteするテスト。
     */
    @Test
    public void testInvalidDataType() throws Exception {
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "file:./test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X   replacement("type_hankaku")   # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream);
        formatter.initialize();

        DataRecord dataRecord1 = new DataRecord(){{
            put("str1", "001");
            put("str2", 500); // 不正なデータタイプ
            put("str3", "あ髙﨑"); 
        }};

        try {
            formatter.writeRecord(dataRecord1);
            fail();
        } catch (InvalidDataFormatException e) {
            assertEquals("invalid parameter type was specified. parameter type must be 'java.lang.String'. type=[class java.math.BigDecimal]. field name=[str2].", e.getMessage());
        }
        

        formatter.close();
        outputStream.close();
    }


    /**
     * デフォルトの寄せ字変換が行われることのテスト。
     * コンポーネント設定ファイルにデフォルトの寄せ字変換定義を行う。
     */
    @Test
    public void testDefaultReplacement() throws Exception {
        
        
        // Xと、Nのデータタイプ名に対して、デフォルトで半角（type_hankaku）と全角（type_zenkaku）の寄せ字コンバータを設定する
        File diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
     <?xml version="1.0" encoding="UTF-8"?>
     <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
           <!-- FormatterFactoryの設定 -->
           <component name="formatterFactory"
               class="nablarch.core.dataformat.FormatterFactory">
               <property name="cacheLayoutFileDefinition" value="false" />
               <property name="defaultReplacementType">
                   <map>
                        <entry key="X" value="type_hankaku" />
                        <entry key="N" value="type_zenkaku" />
                   </map>
               </property>
           </component>

           <component name="characterReplacementManager"
                 class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_zenkaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_zenkaku.properties"/>
                                    <property name="encoding" value="ms932"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="ms932"/>
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
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        /*
         * 半角および全角文字について寄せ字が行われることのテスト。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X      # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        001,DEF,あ髙﨑
        002,G\~,え唖か
        **********************************************************************/
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes("ms932"));
        
        createFormatter(formatFile);
        formatter.setInputStream(inputStream);
        formatter.initialize();

        // 髙　→　高　、　﨑　→　崎　、唖　→　■ 、 ~ → [ 、 \ → [ 、に寄せ字変換されることの確認
        DataRecord readRecord = formatter.readRecord();
        assertEquals("001", readRecord.get("str1"));
        assertEquals("DEF", readRecord.get("str2"));
        assertEquals("あ高崎", readRecord.get("str3"));
        readRecord = formatter.readRecord();
        assertEquals("002", readRecord.get("str1"));
        assertEquals("G[[", readRecord.get("str2"));
        assertEquals("え■か", readRecord.get("str3"));
        
        formatter.close();
        inputStream.close();
        
    }


    /**
     * フィールドに対する寄せ字変換タイプ名と、データタイプに対する寄せ字変換タイプ名がどちらも指定されている場合に、
     * フィールドに対する寄せ字変換タイプ名が優先されて使用されることのテスト。
     */
    @Test
    public void testFieldReplacement() throws Exception {
        
        
        // Xと、Nのデータタイプ名に対して、デフォルトで半角（type_hankaku）と全角（type_zenkaku）の寄せ字コンバータを設定する
        File diConfig = Hereis.file("./test.xml");
        /**************************************************************************************************************
     <?xml version="1.0" encoding="UTF-8"?>
     <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
           <!-- FormatterFactoryの設定 -->
           <component name="formatterFactory"
               class="nablarch.core.dataformat.FormatterFactory">
               <property name="cacheLayoutFileDefinition" value="false" />
               <property name="defaultReplacementType">
                   <map>
                        <entry key="X" value="type_hankaku" />
                        <entry key="N" value="type_zenkaku" />
                   </map>
               </property>
           </component>

           <component name="characterReplacementManager"
                 class="nablarch.core.dataformat.CharacterReplacementManager">

                        <property name="configList">
                            <list>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_zenkaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_zenkaku.properties"/>
                                    <property name="encoding" value="ms932"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="ms932"/>
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku_field"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku_field.properties"/>
                                    <property name="encoding" value="ms932"/>
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
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        /*
         * 半角および全角文字について寄せ字が行われることのテスト。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X                                       # 半角文字        
        2  str2        X  replacement("type_hankaku_field")    # 半角文字           
        3  str3        N                                       # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();

        String data = Hereis.string().replace(LS, "\n");
        /**********************************************************************
        001,DEF,あ髙﨑
        002,G\~,え唖か
        **********************************************************************/
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes("ms932"));
        
        createFormatter(formatFile);
        formatter.setInputStream(inputStream);
        formatter.initialize();

        // 髙　→　高　、　﨑　→　崎　、唖　→　■ 、 ~ → Z 、 \ → Z 、に寄せ字変換されることの確認
        // 半角文字が「Z」なのは、フィールドに対する寄せ字タイプ名「type_hankaku_field」が優先されたため
        DataRecord readRecord = formatter.readRecord();
        assertEquals("001", readRecord.get("str1"));
        assertEquals("DEF", readRecord.get("str2"));
        assertEquals("あ高崎", readRecord.get("str3"));
        readRecord = formatter.readRecord();
        assertEquals("002", readRecord.get("str1"));
        assertEquals("GZZ", readRecord.get("str2"));
        assertEquals("え■か", readRecord.get("str3"));
        
        formatter.close();
        inputStream.close();
        
    }

    
    /**
     * レイアウト定義ファイルに、不正な寄せ字タイプ名が設定された場合のテスト。
     */
    @Test
    public void testInvalidParameter() throws Exception {

        // Xと、Nのデータタイプ名に対して、デフォルトで半角（type_hankaku）と全角（type_zenkaku）の寄せ字コンバータを設定する
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
                                    <property name="byteLengthCheck" value="true" />
                                </component>
                                <component class="nablarch.core.dataformat.CharacterReplacementConfig">
                                    <property name="typeName" value="type_hankaku"/>
                                    <property name="filePath" value="classpath:nablarch/core/dataformat/type_hankaku.properties"/>
                                    <property name="encoding" value="ms932"/>
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
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        /*
         * 寄せ字タイプにnullが設定されるパターン。
         */
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X   replacement()   # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        formatter.setInputStream(createInputStreamFrom(
                "\"\",\"234.56\",\"34567\""));
        
        try {
            formatter.initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter size was invalid. " +
                            "parameter size must be one, but was '0'. " +
                            "parameter=[], convertor=[CharacterReplacer]."));
            assertThat(e.getFilePath(), endsWith("format.dat"));
        }

        /*
         * 寄せ字タイプに空文字が設定されるパターン。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X   replacement("")   # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        formatter.setInputStream(createInputStreamFrom("\"\",\"234.56\",\"34567\""));
        
        try {
            formatter.initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter was empty. parameter must not be empty. parameter=[], " +
                    "convertor=[CharacterReplacer]."));
            assertThat(e.getFilePath(), endsWith("format.dat"));
        }
        
        
        /*
         * 寄せ字タイプに数値が設定されるパターン。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X   replacement(1)   # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);
        formatter.setInputStream(createInputStreamFrom("\"\",\"234.56\",\"34567\""));
        
        try {
            formatter.initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. parameter type must be 'String', " +
                    "but was 'java.lang.Integer'. parameter=[1], convertor=[CharacterReplacer]."));
            assertThat(e.getFilePath(), endsWith("format.dat"));
        }
        
        
        /*
         * 寄せ字タイプに存在しないタイプ名が設定されるパターン。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X   replacement("notExist")   # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);

        try {
            formatter.setInputStream(createInputStreamFrom("abc,def,ghi"))
                    .initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "replacement type name was not found. value=[notExist]. " +
                    "must specify defined replacement type name. " +
                    "convertor=[CharacterReplacer]."));
            assertThat(e.getFilePath(), endsWith("format.dat"));
        }
        
        
        /*
         * 寄せ字タイプに紐付けられた文字エンコーディングと、フィールドの文字エンコーディングが一致しない場合、例外がスローされるパターン。
         */
        formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "utf8" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1  str1        X   replacement("type_hankaku")   # 半角文字        
        2  str2        X      # 半角文字           
        3  str3        N      # 全角文字        
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(formatFile);

        try {
            formatter.setInputStream(createInputStreamFrom("abc,def,ghi"))
                    .initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "field encoding 'UTF-8' was invalid. " +
                    "field encoding must match the encoding " +
                    "that is defined as replacement type 'type_hankaku'."));
            assertThat(e.getFilePath(), endsWith("format.dat"));
        }
        
        
        /*
         * 寄せ時タイプにnullが設定されるパターン。
         */
        CharacterReplacer replacer = new CharacterReplacer();
        try {
            replacer.initialize(new FieldDefinition(), new Object[]{null});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "1st parameter was null. parameter=[null], convertor=[CharacterReplacer]."));
        }
        
        
    }


    
    /**
     * 読み書きする際に、変換前のオブジェクトがnullの場合、そのままnullが返却されることのテスト。
     */
    @Test
    public void testValueNull() throws Exception {

        CharacterReplacer replacer = new CharacterReplacer();
        assertNull(replacer.convertOnRead(null));
        assertNull(replacer.convertOnWrite(null));
        
    }
    
}
