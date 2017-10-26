package nablarch.core.dataformat;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.convertor.value.NumberString;
import nablarch.test.support.tool.Hereis;

import org.junit.After;
import org.junit.Test;

/**
 * DataRecordFormatterの抽象基底クラスのテストケース。
 * 
 * 観点：
 * 通常の正常系に関しては、サブクラスのテストで網羅しているので、
 * ここでは、FixedLengthDataRecordFormatterおよび、VariableLengthDataRecordFormatterの
 * 共用ロジックについての異常系テストを網羅する。
 *   ・レイアウト定義ファイルの開始位置と、フィールド長の整合性が取れない場合のテスト
 *   ・フィールドセパレータが指定されている場合に、レコードセパレータの指定が行われていない場合のテスト。
 *   ・ディレクティブの検証（text-encoding/quoting-delimiter/field-separator/改行コード
 * 
 * @author Masato Inoue
 */
public class DataRecordFormatterSupportTest {

    private String LS = System.getProperty("line.separator");

    private DataRecordFormatter formatter;
    
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(File filePath, InputStream source) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        return formatter.setInputStream(source).initialize();
    }
    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatterWrite(File filePath, OutputStream source) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        return formatter.setOutputStream(source).initialize();
    }



    /**
     * 同一のレイアウト定義（Definition）をもとに、異なる２つのデータファイルを読み込むパターン。
     * フォーマッタのインスタンスは、データファイルごとに作成する。
     * 
     * このテストで期待する動作を以下に示す。
     * ■ １つ目のフォーマッタ実行時
     *   ・Definitionが初期化される。
     *   ・プロパティが初期化される。
     *   
     * ■ ２つ目のフォーマッタ実行時
     *   ・Definitionは初期化されない。
     *   ・プロパティが初期化される。
     * 
     * 期待値：２つ目のDefinitionの内容が２回更新されず、
     */
    @Test
    public void testInitialize() throws Exception{

        /**
         * 共用のレイアウト定義ファイル。
         */
        File formatFile = Hereis.file("./format.fmt ");
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "ms932"                # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n" # レコード区切り文字
        field-separator:   ","        # フィールド区切り文字
        quoting-delimiter: "\""                # クオート文字    

        [DataRecord]
        1   userName         X    number          
        2   userCode         X             
        3   price            X             
        ************************************************************/
        formatFile.deleteOnExit();


        /**
         * １つ目のフォーマッタが読み込むデータファイル。
         */
        String data = Hereis.string().replace(LS, "\n");
        /*********************************************
        0001,A01,10
        0001,A01,20
        0001,A02,30
        0002,A02,40
        0003,A03,50
        **********************************************/
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(data.getBytes("ms932"));
        dest.close();
        
        /**
         * ２つ目のフォーマッタが読み込むデータファイル。
         */
        String data2 = Hereis.string().replace(LS, "\n");
        /*********************************************
        0001,A01,1000
        0001,A01,2000
        0001,A02,3000
        0002,A02,4000
        0003,A03,5000
        **********************************************/
        OutputStream dest2 = new FileOutputStream("./record2.dat", false);
        dest2.write(data2.getBytes("ms932"));
        dest2.close();


        /**
         * 共用のDefinitionを生成する。
         */        
        LayoutFileParser layoutFileParser = new LayoutFileParser(formatFile.getPath());
        LayoutDefinition definition = layoutFileParser.parse();
        
        
        /**
         * １つ目のフォーマッタインスタンスを使用し、DefinitionとFormatterが共に初期化され、ファイルが正常に読み込めることを確認する。
         */
        VariableLengthDataRecordFormatter formatter = new VariableLengthDataRecordFormatter();
        
        // フォーマッタのプロパティが初期化されていないことを確認する
        assertNull(formatter.getRecordSeparator());

        // Definitionが初期化されていないことを確認する（コンバータが空）
        assertTrue(definition.getRecords().get(0).getFields().get(0).getConvertors().isEmpty());
        assertFalse(definition.isInitialized());
        
        // フォーマッタの初期化を行う
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File("./record.dat")));
        formatter.setDefinition(definition).setInputStream(stream).initialize();
        
        // Definitionが初期化されたことを確認する（コンバータが設定されている）
        assertFalse(definition.getRecords().get(0).getFields().get(0).getConvertors().isEmpty());
        assertSame(NumberString.class, definition.getRecords().get(0).getFields().get(0).getConvertors().get(0).getClass());
        assertTrue(definition.isInitialized());
        
        // フォーマッタのプロパティが初期化されたことを確認する
        assertEquals("\n", formatter.getRecordSeparator());
        assertEquals("Variable", formatter.getFileType());
        assertEquals("text/plain", formatter.getMimeType());
        assertEquals(Charset.forName("ms932"), formatter.getDefaultEncoding());
        
        // 正常にファイルを読み込めることを確認する
        assertEquals("10", formatter.readRecord().get("price"));
        
        formatter.close();
        
        
        /**
         * ２つ目のフォーマッタインスタンスを使用し、Definitionは初期化されず、Formatterのみが初期化され、ファイルが正常に読み込めることを確認する。
         */
        VariableLengthDataRecordFormatter formatter2 = new VariableLengthDataRecordFormatter();

        // フォーマッタのプロパティが初期化されていないことを確認する
        assertNull(formatter2.getRecordSeparator());
        
        // Definitionのコンバータを空にする（コンバータが空）
        definition.getRecords().get(0).getFields().get(0).getConvertors().clear();
        assertTrue(definition.getRecords().get(0).getFields().get(0).getConvertors().isEmpty());
        
        // フォーマッタの初期化を行う
        stream = new BufferedInputStream(new FileInputStream(new File("./record2.dat")));
        formatter2.setDefinition(definition).setInputStream(stream).initialize();
        
        // Definitionが初期化されなかったことを確認する（コンバータが空のまま）
        assertTrue(definition.getRecords().get(0).getFields().get(0).getConvertors().isEmpty());
        assertTrue(definition.isInitialized());
        
        // フォーマッタのプロパティが初期化されたことを確認する
        assertEquals("\n", formatter2.getRecordSeparator());
        
        // 正常にファイルを読み込めることを確認する
        assertEquals("1000", formatter2.readRecord().get("price"));
        

        formatter.close();
        
        dest.close();
        dest2.close();
        
        

    }

    
    /**
     * Definitionがnullの場合のテスト。
     */
    @Test
    public void testDefinitionNull() throws Exception {

        VariableLengthDataRecordFormatter formatter = new VariableLengthDataRecordFormatter();
        try{
            formatter.initialize();
            fail();
        } catch(IllegalStateException e){
            assertTrue(true);
        }
    }
    
    /**
     * ポジションとフィールドの長さが一致しない場合のエラー。
     */
    @Test
    public void testInvalidFieldLength() throws Exception {

        /**
         * 可変長のパターン。（長いパターン）
         */
        String charset = "UTF-8";
        String fieldSeparator = ",";
        String delimiter = "\\\"";
        String rs = "\n";

        String escapedRs = rs.replace("\r", "\\r").replace("\n", "\\n");
        File layoutFile = Hereis.file("./format.dat", delimiter, fieldSeparator, charset, escapedRs);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        record-separator:  "$escapedRs" # レコード区切り文字
        quoting-delimiter: "$delimiter"         # クオート文字    

        [DataRecord]
        1   test1  X             # 振込先金融機関コード
        2   test2  X             # 振込先金融機関名称
        4   test3  X             # 振込先金融機関名称
        5   test4  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        try{
            createFormatter(layoutFile, createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), containsString(
                    "invalid field position was specified. field 'test3' must at 3. but 4."));
        }

        /**
         * 可変長のパターン。（短いパターン）
         */

        layoutFile = Hereis.file("./format.dat", delimiter, fieldSeparator, charset, escapedRs);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        record-separator:  "$escapedRs" # レコード区切り文字
        quoting-delimiter: "$delimiter"         # クオート文字    

        [DataRecord]
        1   test1  X             # 振込先金融機関コード
        2   test2  X             # 振込先金融機関名称
        2   test3  X             # 振込先金融機関名称
        3   test4  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        
        try{
            createFormatter(layoutFile, createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), containsString(
                    "invalid field position was specified. field 'test3' must at 3. but 2."));
        }       
        

        /**
         * 固定長のパターン。（1つ長いパターン）
         */
        // レイアウト定義ファイル
        layoutFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 40

        # データレコード定義
        [Default]
        1    byteString     X(10)   # 長さが正常
        11   wordString     N(10)   # 長さが正常
        21   zoneDigits     Z(11)   # 長さが異常！(1つ多い)
        31   signedZDigits  SZ(9)  # 長さが正常
        ***************************************************/
        layoutFile.deleteOnExit();

        
        try {
            createFormatter(layoutFile, createInputStreamFrom("test", charset));
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "invalid field position was specified. field 'signedZDigits' must at 32. but 31."));
        }
        
        
        /**
         * 固定長のパターン。（1つ短いパターン）
         */
        // レイアウト定義ファイル
        layoutFile = Hereis.file("./format.fmt");
        /**********************************************
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 40

        # データレコード定義
        [Default]
        1    byteString     X(10)   # 長さが正常
        11   wordString     N(10)   # 長さが正常
        21   zoneDigits     Z(9)   # 長さが異常！(1つ短い)
        31   signedZDigits  SZ(9)  # 長さが正常
        ***************************************************/
        layoutFile.deleteOnExit();

        try {
            createFormatter(layoutFile, createInputStreamFrom("test", charset));
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "invalid field position was specified. field 'signedZDigits' must at 30. but 31."));
        }
    }
    
    
    
    /**
     * 可変長の場合に、レコードセパレータの指定が行われていない場合のテスト。
     */
    @Test
    public void testExistFieldSeparatorNotExistRecordSeparator() throws Exception {
        String charset = "UTF-8";
        String fieldSeparator = ",";
        String delimiter = "\\\"";
        
        File layoutFile = Hereis.file("./format.dat", delimiter, fieldSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        # レコードセパレータの定義がない
        quoting-delimiter: "$delimiter"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        
        try{
            createFormatter(layoutFile, createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), startsWith(
                    "directive 'record-separator' was not specified. " +
                            "directive 'record-separator' must be specified."));
            assertThat(e.getFilePath(), containsString(layoutFile.getName()));
        }
    }
    
    

    
    
    /**
     * 存在しないデータ型を指定した場合のテスト。
     */
    @Test
    public void testNonExistDataType() throws Exception {

        /**
         * ファイルタイプが存在しない場合。
         */
        String charset = "UTF-8";
        String fieldSeparator = ",";
        String delimiter = "\\\"";
        String rs = "\n";

        String escapedRs = rs.replace("\r", "\\r").replace("\n", "\\n");
        File layoutFile = Hereis.file("./format.dat", delimiter, fieldSeparator, charset, escapedRs);
        /***********************************************************
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        record-separator:  "$escapedRs" # レコード区切り文字
        quoting-delimiter: "$delimiter"         # クオート文字    

        [DataRecord]
        1   test1  X             
        2   test2  X             
        ************************************************************/
        layoutFile.deleteOnExit();

        LayoutFileParser parser = new LayoutFileParser(layoutFile.getPath());
        LayoutDefinition def = parser.parse();
        VariableLengthDataRecordFormatter formatter = new VariableLengthDataRecordFormatter();
        formatter.setDefinition(def);
        
        try{
            formatter.initialize();
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "directive 'file-type' was not specified. directive 'file-type' must be specified."));
        }
    }
    
    
    /**
     * ファイルタイプが存在しない場合のテスト。
     */
    @Test
    public void testIllegalCharset1() throws Exception {

        String charset = "";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        File layoutFile = Hereis.file("./format.dat", enclose, fieldSeparator, recordSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$illegalEnclose"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        try{
            createFormatter(layoutFile, createInputStreamFrom("test"));
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "invalid encoding was specified by 'text-encoding' directive. value=[]."));
        }
    }
    
    /**
     * データタイプが存在しない場合のテスト。
     */
    @Test
    public void testNonExistDirective() throws Exception {

        String charset = "";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        File layoutFile = Hereis.file("./format.dat", enclose, fieldSeparator, recordSeparator, charset);
        /***********************************************************
        hoge: "abc"
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$illegalEnclose"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        try{
            createFormatter(layoutFile, createInputStreamFrom("test"));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), containsString(
                    "unknown directive was specified. value=[hoge]."));
        }
    }
    
    
    
    /**
     * レイアウト定義ファイルに設定されたtext-encodingが不正な場合のテスト。
     */
    @Test
    public void testIllegalCharset2() throws Exception {

        String charset = "illegal";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        File layoutFile = Hereis.file("./format.dat", enclose, fieldSeparator, recordSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$illegalEnclose"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        layoutFile.deleteOnExit();

        
        try{
            createFormatter(layoutFile, createInputStreamFrom("test"));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), containsString(
                    "invalid encoding was specified by 'text-encoding' directive. value=[illegal]."));
        }
    }

    /**
     * レイアウト定義ファイルのClassifierおよび、通常のレコードタイプのフィールドに対してデータタイプが指定されていない場合のテスト。
     */
    @Test
    public void testNotSpecifiedDataType() throws Exception {

        String charset = "ms932";
        String fieldSeparator = ",";
        
        
        /**
         * Classifierのデータタイプが存在しないパターン。
         */
        File layoutFile = Hereis.file("./format.dat", fieldSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字

        [Classifier]
        1   test1                    
        2   test2  X            
        
        [DataRecord]
        1   test1         X            
        2   test2         X           
        ************************************************************/
        layoutFile.deleteOnExit();

        try {
            createFormatter(layoutFile, createInputStreamFrom("test"));
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "data type was not specified. data type must be specified. record type=[Classifier], field name=[test1]."));
        }
        
        formatter.close();
        
        
        /**
         * DataRecordのデータタイプが存在しないパターン。
         */
        layoutFile = Hereis.file("./format.dat", fieldSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字

        [Classifier]
        1   test1  X                  
        2   test2  X            
                
        [HogeRecord]
        1   test1         X            
        2   test2         X    
        
        [DataRecord]
        1   test1         X            
        2   test2             
        ************************************************************/
        layoutFile.deleteOnExit();

        try {
            createFormatter(layoutFile, createInputStreamFrom("test"));
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), containsString(
                    "data type was not specified. data type must be specified. record type=[DataRecord], field name=[test2]."));
        }
        
    }

    /**
     * ディレクティブの妥当性確認が正しく行われることの確認。
     */
    @Test
    public void testValidateDirectives() throws Exception {

        String charset = "UTF-8";
        String recordSeparator = "\n";
        String enclose = "\\\"";
        String fieldSeparator = ",";
        
        /**
         * quoting-delimiterの長さが0のパターン。
         */
        String illegalEnclose = "";
        
        Map<String, Object> recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        String rs = escapeRs(recordSeparator);
        
        File formatFile = Hereis.file("./format.dat", illegalEnclose, fieldSeparator, rs, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$illegalEnclose"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        
        FileOutputStream stream = new FileOutputStream(outputData);
        
        try{
            createFormatter(new File("./format.dat"), createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertTrue(e.getMessage().contains("invalid quoting delimiter was specified by 'quoting-delimiter' directive. value=[]. Quoting delimiter length must be [1]."));
        }

        formatter.close();
        
        
        /**
         * quoting-delimiterの長さが2のパターン。
         */
        illegalEnclose = "\\\"\\\"";
        
        recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatFile = Hereis.file("./format.dat", illegalEnclose, fieldSeparator, rs, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"         # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs" # レコード区切り文字
        field-separator:   "$fieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$illegalEnclose"         # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        try{
            createFormatter(new File("./format.dat"), createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertTrue(e.getMessage().contains("invalid quoting delimiter was specified by 'quoting-delimiter' directive. value=[\"\"]. Quoting delimiter length must be [1]."));
        }

        formatter.close();
        
        
        /**
         * field-separatorの長さが0のパターン。
         */

        String illegalFieldSeparator = "";
        
        recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatFile = Hereis.file("./format.dat", enclose, illegalFieldSeparator, rs, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"               # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"                    # レコード区切り文字
        field-separator:   "$illegalFieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$enclose"               # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        try{
            createFormatter(new File("./format.dat"), createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertTrue(e.getMessage().contains("invalid field separator was specified by 'field-separator' directive. value=[]. field separator length must be [1]."));
        }

        formatter.close();
        
        /**
         * field-separatorの長さが2のパターン。
         */

        illegalFieldSeparator = "||";
        
        recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatFile = Hereis.file("./format.dat", enclose, illegalFieldSeparator, rs, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"               # 文字列型フィールドの文字エンコーディング
        record-separator:  "$rs"                    # レコード区切り文字
        field-separator:   "$illegalFieldSeparator" # フィールド区切り文字
        quoting-delimiter: "$enclose"               # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        try{
            createFormatter(new File("./format.dat"), createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertTrue(e.getMessage().contains("invalid field separator was specified by 'field-separator' directive. value=[||]. field separator length must be [1]."));
        }
        
        formatter.close();
        
        
        /**
         * 改行コードが許容されないパターン（長さが0）。
         * 特に動作に問題なく初期化が行われることを確認する。
         */
        String illegalRecordSeparator = "";
        
        recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatFile = Hereis.file("./format.dat", enclose, fieldSeparator, illegalRecordSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"                # 文字列型フィールドの文字エンコーディング
        record-separator:  "" # レコード区切り文字
        field-separator:   "$fieldSeparator"        # フィールド区切り文字
        quoting-delimiter: "$enclose"                # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        createFormatter(new File("./format.dat"), createInputStreamFrom("test", charset));
        formatter.close();
        
        
        /**
         * 改行コードが許容されない文字のパターン。
         */
        illegalRecordSeparator = "a";
        
        recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        
        outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        formatFile = Hereis.file("./format13.dat", enclose, fieldSeparator, illegalRecordSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"                # 文字列型フィールドの文字エンコーディング
        record-separator:  "$illegalRecordSeparator" # レコード区切り文字
        field-separator:   "$fieldSeparator"        # フィールド区切り文字
        quoting-delimiter: "$enclose"                # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();
        
        try{
            createFormatter(new File("./format13.dat"), createInputStreamFrom("test", charset));
            fail();
        } catch(SyntaxErrorException e){
            assertThat(e.getMessage(), containsString(
                    "not allowed record separator was specified by 'record-separator' directive. " +
                            "value=[a]. record separator was must be [\\r or \\n or \\r\\n]."));
        }
    }
    

    /**
     * setterを使用して、許容する改行コードを変更し、確認するパターン。
     */
    @Test
    public void testSetRecordSeparator() throws Exception{

        String charset = "UTF-8";
        String enclose = "\\\"";
        String fieldSeparator = ",";

        String recordSeparator = "sep"; // sepを許容
        
        List<String> allowedRecordSeparatorList = new ArrayList<String>();
        allowedRecordSeparatorList.add(recordSeparator);

        
        HashMap<String, Object> recordMap = new HashMap<String, Object>() {{
            put("intTest",        1234);
            put("bigdecimalTest",    new BigDecimal("-1234.56"));
        }};
        Map<String, Object> recordMap2 = new HashMap<String, Object>() {{
            put("intTest",        5678);
            put("bigdecimalTest",    new BigDecimal("+789"));
        }};
        
        File outputData = new File("./output.dat");
        outputData.deleteOnExit();
        
        File formatFile = Hereis.file("./format5.fmt ", enclose, fieldSeparator, charset);
        /***********************************************************
        file-type:    "Variable"
        text-encoding:     "$charset"                # 文字列型フィールドの文字エンコーディング
        record-separator:  "sep" # レコード区切り文字
        field-separator:   "$fieldSeparator"        # フィールド区切り文字
        quoting-delimiter: "$enclose"                # クオート文字    

        [DataRecord]
        1   intTest         X             # 振込先金融機関コード
        2   bigdecimalTest  X             # 振込先金融機関名称
        ************************************************************/
        formatFile.deleteOnExit();

        FileOutputStream stream = new FileOutputStream(outputData);
        
        FormatterFactory.getInstance().setAllowedRecordSeparatorList(allowedRecordSeparatorList);
        
        formatter = createFormatterWrite(formatFile, stream);
        formatter.writeRecord(recordMap);
        formatter.writeRecord(recordMap2);
             
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(outputData), "ms932"
           ));

        // 「sep」というセパレータが使用される。
        assertEquals("\"1234\",\"-1234.56\"sep\"5678\",\"789\"sep",
                reader.readLine());
        assertNull(reader.readLine());
    }

    /**
     * 不正なレコード区切り文字を指定した場合、例外が発生し、
     * そのメッセージに指定した値がエスケープされて格納されること。
     */
    @Test
    public void testInvalidRecordSeparatorEscaped() {
        DataRecordFormatterSupport support = new FixedLengthDataRecordFormatter();
        support.setDefinition(new LayoutDefinition("source"));
        HashMap<String, Object> directive = new HashMap<String, Object>() {
            {
                put("file-type", "Fixed");
                put("text-encoding", "UTF-8");
                put("record-separator", "a\r\n\t");   // syntax error.
            }
        };

        try {
            support.validateDirectives(directive);
            fail();
        } catch (SyntaxErrorException e) {
            // 指定した区切り文字がエスケープされていること
            assertThat(e.getMessage(), containsString("value=[a\\r\\n\\t]"));
        }
    }
    
    
    private String escapeRs(String recordSeparator) {
        return recordSeparator.replace("\r", "\\r").replace("\n", "\\n");
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
        FormatterFactory.getInstance().setAllowedRecordSeparatorList(null);
    }
}
