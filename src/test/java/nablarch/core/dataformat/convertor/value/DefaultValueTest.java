package nablarch.core.dataformat.convertor.value;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.LayoutFileParser;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * デフォルト値コンバータのテスト。
 * 
 * 観点：
 * 読み込み時にデフォルト値が無視されることのテスト、出力時にデフォルト値が正しく書き込まれることのテスト。
 * 
 * @author Masato Inoue
 */
public class DefaultValueTest {

    private static final String LS = System.getProperty("line.separator");

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
    }
    
    /**
     * 読み込み時にDefaultValueが無視されることのテスト。
     * @throws Exception
     */
    @Test
    public void testDefaultValue() throws Exception {

        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
        file-type:         "Variable"         
        text-encoding:     "UTF-8" # 文字列型フィールドの文字エンコーディング
        record-separator:  "\n"       # レコード区切り文字
        field-separator:   ","       # フィールド区切り文字
        quoting-delimiter: "\""       # クオート文字

        [TestDataRecord]
        1   FIcode        X  "a0"      # 振込先金融機関コード
        2   FIname        X  "a1"      # 振込先金融機関名称
        3   officeCode    X  "a2"      # 振込先営業所コード
        4   officeName    X  "a3"      # 振込先営業所名
        5   syumoku       X  "a4"      # 預金種目
        6   accountNum    X  "a5"      # 口座番号
        7   recipientName X   0        # 受取人名
        8   amount        X   1        # 振込金額
        9   isNew         X   2        # 新規コード
        10  ediInfo       X   3        # EDI情報
        11  transferType  X   4        # 振込区分
        12  ?withEdi      X   5        # EDI情報使用フラグ             
        ************************************************************/
        formatFile.deleteOnExit();
        
        LayoutDefinition definition = new LayoutFileParser("./format.dat").parse();
        createFormatter(formatFile);
        
        String testdata = Hereis.string().replaceAll(LS, "\n");
        /*
        "a","b","c","d","e","f","g","h","i","j","k","l"
        */
        
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testdata.getBytes("UTF-8"));
        DataRecord record = formatter.setInputStream(inputStream).initialize().readRecord();
        
        assertEquals("TestDataRecord", record.getRecordType()); 
        assertEquals(11, record.size());

        assertEquals("a",          record.get("FIcode"));       // 1. 振込先金融機関コード
        assertEquals("b",      record.get("FIname"));       // 2. 振込先金融機関名称
        assertEquals("c",           record.get("officeCode"));   // 3. 振込先営業所コード
        assertEquals("d", record.get("officeName"));   // 4. 振込先営業所名
        assertEquals("e",            record.get("syumoku"));       // 5. 預金種目
        assertEquals("f",      record.get("accountNum"));    // 6. 口座番号
        assertEquals("g",     record.get("recipientName")); // 7. 受取人名
        assertEquals("h",         record.get("amount"));        // 8. 振込金額
        assertEquals("i",            record.get("isNew"));         // 9.新規コード
        assertEquals("j", record.get("ediInfo"));       // 10.EDI情報
        assertEquals("k",            record.get("transferType"));  // 11.振込区分
        assertTrue  (!record.containsKey("withEdi"));            // 12.(手形交換所番号)
        
        record = formatter.setInputStream(inputStream).readRecord();
        assertNull(record);
    }

    
    
    /**
     * 書き込み時に、デフォルト値が正しく使用されることのテスト。
     */
    @Test
    public void testWriteDoubleByte() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    singleByteString     X(10) "DE" # 半角文字
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        createFormatter(formatFile);
        formatter.setOutputStream(outputStream).initialize();
        
        DataRecord dataRecord = new DataRecord(){{
            put("singleByteString", null);
        }};
        
        formatter.writeRecord(dataRecord);
        formatter.close();
        
        FileInputStream inputStream = new FileInputStream("test.dat");
        byte[] buffer = new byte[10];
        inputStream.read(buffer);
       
        assertEquals("DE        ", new String(buffer, "ms932"));
    }
    

    /**
     * 不正なデフォルト値が設定された場合のテスト。
     */
    @Test
    public void testInvalidParameter() throws Exception {
        
        /**
         * 引数がなし
         */
       DefaultValue defaultValue = new DefaultValue();
        try {
            defaultValue.initialize(new FieldDefinition());
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(true);
        }
        
        /**
         * 引数が２つ
         */
        try {
            defaultValue.initialize(new FieldDefinition(), new Object[]{"a","b"});
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(true);
        }
        
        /**
         * 引数がnull
         */
        try {
            defaultValue.initialize(null, new Object[] { null });
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(true);
        }
    }
    
    /**
     * 不正な引数が設定された場合のテスト。
     */
    @Test
    public void illegalArgument(){
        DefaultValue defaultValue = new DefaultValue();
        try {
            defaultValue.initialize(new FieldDefinition(), "a", "b");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("parameter size was invalid. parameter size must be one, but was [2]. parameter=[a, b]. convertor=[DefaultValue].", e.getMessage());
        }
   }
  
    /**
     * 出力時にパラメータがnullまたは空白の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() {
        DefaultValue defaultValue = new DefaultValue();
        defaultValue.initialize(new FieldDefinition(), new Object[]{"abc"});
        assertThat("abc", is(defaultValue.convertOnWrite(null)));
        assertThat("", is(defaultValue.convertOnWrite("")));
    }
}
