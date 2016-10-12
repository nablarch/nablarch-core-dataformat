package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.core.dataformat.FormatterFactoryStub02.DataRecordFormatterStub;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * FormatterFactoryのテスト。
 * 
 * 観点：
 * 正常系のテストは別のテストケースで行っているので、
 * ここではFormatterFactoryクラスをリポジトリから取得するテストと、
 * 異常系のファイルタイプが不正な場合のテストを行う。
 * 
 * @author Masato Inoue
 *
 */
public class FormatterFactoryTest {

    private File formatFile;

    @Before
    public void setUp() {
        
        // レイアウト定義ファイル
        // (シングルフォーマット)
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 120

        # データレコード定義
        [Default]
        1    dataKbn       X(1)  "2"      # 1. データ区分
        2    FIcode        X(4)           # 2. 振込先金融機関コード
        6    FIname        X(15)          # 3. 振込先金融機関名称
        21   officeCode    X(3)           # 4. 振込先営業所コード
        24   officeName    X(15)          # 5. 振込先営業所名
        39  ?tegataNum     X(4)  "9999"   # (手形交換所番号)
        43   syumoku       X(1)           # 6. 預金種目
        44   accountNum    X(7)           # 7. 口座番号
        51   recipientName X(30)          # 8. 受取人名
        81   amount        X(10)          # 9. 振込金額
        91   isNew         X(1)           # 10.新規コード
        92   ediInfo       X(20)          # 11.EDI情報
        112  transferType  X(1)           # 12.振込区分
        113  withEdi       X(1)  "Y"      # 13.EDI情報使用フラグ
        114 ?unused        X(7)  pad("0") # (未使用領域)
        ***************************************************/
        formatFile.deleteOnExit();
    }
    
    /**
     * レイアウト定義ファイルを読み込む際の文字コード（ms932）を指定するテスト。
     * レイアウト定義ファイルのデフォルト値が正常にms932で読み込まれる。
     */
    @Test
    public void testFormatFileEncodingSjis() throws IOException {

        // FormatterFactoryのプロパティにms932をDIする
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/FormatterFactoryEncoding.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        // レイアウト定義ファイル
        // (シングルフォーマット)
        String formatFileData = Hereis.string();
        /**********************************************
        file-type:    "Variable"
        text-encoding: "utf-8"
        field-separator: ","
        record-separator: "\n"
        
        # データレコード定義
        [Default]
        1    dataKbn       X  "データ区分"
        2    FIcode        X  
        ***************************************************/
        
        File formatFile = new File("./format.fmt");
        formatFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(formatFile);
        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, "ms932");
        writer.append(formatFileData);
        writer.close();
        
        FilePathSetting.getInstance().addBasePathSetting("format", "file:./")
        .addBasePathSetting("output", "file:./").addFileExtensions("format", "fmt");
        
        FileRecordWriterHolder.open("layoutFileEncodingTest.dat", "format");
        DataRecord dataRecord = new DataRecord(){{put("FIcode", "テストコード");}};
        FileRecordWriterHolder.write(dataRecord, "layoutFileEncodingTest.dat");
        FileRecordWriterHolder.close("layoutFileEncodingTest.dat");
        
        byte[] result = new byte["データ区分,テストコード".getBytes("UTF-8").length];
        new FileInputStream("./layoutFileEncodingTest.dat").read(result);
        assertEquals("データ区分,テストコード", new String(result, "UTF-8"));
        
        SystemRepository.clear();
    }
    

    /**
     * レイアウト定義ファイルを読み込む際の文字コードに不正な文字コードを指定するテスト。
     */
    @Test
    public void testFormatFileEncodingInvalid() throws IOException {

        // FormatterFactoryのプロパティにms932をDIする
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/FormatterFactoryEncodingInvalid.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        // レイアウト定義ファイル
        // (シングルフォーマット)
        String formatFileData = Hereis.string();
        /**********************************************
        file-type:    "Variable"
        text-encoding: "utf-8"
        field-separator: ","
        record-separator: "\n"
        
        # データレコード定義
        [Default]
        1    dataKbn       X  "データ区分"
        2    FIcode        X  
        ***************************************************/
        
        File formatFile = new File("./format.fmt");
        formatFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(formatFile);
        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, "ms932");
        writer.append(formatFileData);
        writer.close();
        
        FilePathSetting.getInstance().addBasePathSetting("format", "file:./")
        .addBasePathSetting("output", "file:./").addFileExtensions("format", "fmt");
        
        try {
            FileRecordWriterHolder.open("layoutFileEncodingTest.dat", "format");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("layout file encoding was invalid. encoding=[error].", e.getMessage());
        }

        FileRecordWriterHolder.close("layoutFileEncodingTest.dat");
        SystemRepository.clear();
    }

    /**
     * FormatterFactoryクラスをキャッシュから生成するテスト。
     */
    @Test
    public void testDefinitionFromCache(){
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory02.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        FormatterFactoryStub02 factory = (FormatterFactoryStub02) FormatterFactory.getInstance().setCacheLayoutFileDefinition(true);
        
        factory.setAllowedRecordSeparatorList(new ArrayList<String>());
        
        factory.createFormatter(formatFile);
        LayoutDefinition createDefinition = factory.createDefinition;
        factory = (FormatterFactoryStub02) FormatterFactory.getInstance();
        factory.createFormatter(formatFile);
        assertSame(createDefinition, factory.createDefinition);
        SystemRepository.clear();
    }
    

    /**
     * ファイルタイプが不正な場合のテスト。
     */
    @Test
    public void testIllegalFormatterName(){

        // レイアウト定義ファイル
        // (シングルフォーマット)
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Hoge"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 120

        # データレコード定義
        [Default]
        1    dataKbn       X(1)  "2"      # 1. データ区分
        2    FIcode        X(4)           # 2. 振込先金融機関コード
        6    FIname        X(15)          # 3. 振込先金融機関名称
        21   officeCode    X(3)           # 4. 振込先営業所コード
        24   officeName    X(15)          # 5. 振込先営業所名
        39  ?tegataNum     X(4)  "9999"   # (手形交換所番号)
        43   syumoku       X(1)           # 6. 預金種目
        44   accountNum    X(7)           # 7. 口座番号
        51   recipientName X(30)          # 8. 受取人名
        81   amount        X(10)          # 9. 振込金額
        91   isNew         X(1)           # 10.新規コード
        92   ediInfo       X(20)          # 11.EDI情報
        112  transferType  X(1)           # 12.振込区分
        113  withEdi       X(1)  "Y"      # 13.EDI情報使用フラグ
        114 ?unused        X(7)  pad("0") # (未使用領域)
        ***************************************************/
        formatFile.deleteOnExit();
        
        try {
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false)
                    .createFormatter(formatFile);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "invalid file type was specified. value=[Hoge]."));
        }
    }
    


    /**
     * Formatterの型がDataRecordFormatterSupportでない場合でも、インスタンスが生成できることのテスト。
     * このテストにより、allowedRecordSeparatorListプロパティがないインスタンスでも生成できることを確認できる。
     */
    @Test
    public void testNotDataRecordFormatterSupport(){

        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/core/dataformat/StubFormatterFactory02.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        FormatterFactoryStub02 factory = (FormatterFactoryStub02) FormatterFactory.getInstance().setCacheLayoutFileDefinition(true);
        factory.createFormatter(formatFile);
        LayoutDefinition createDefinition = factory.createDefinition;
        factory = (FormatterFactoryStub02) FormatterFactory.getInstance();
        DataRecordFormatter formatter = factory.createFormatter(formatFile);
        assertEquals(DataRecordFormatterStub.class, formatter.getClass());
        SystemRepository.clear();
    }
    
    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }
    
}
