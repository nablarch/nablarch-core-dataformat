package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.test.support.tool.Hereis;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * バイト文字コンバータのテスト。
 * 
 * @author Masato Inoue
 */
public class ByteStreamDataStringTest {

    /** 読み込み用フォーマッタを生成する */
	private DataRecordFormatter createReadFormatter(String value, String encoding) {
		InputStream source = createInputStreamFrom(value, encoding);
        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        formatter.setInputStream(source).initialize();
		return formatter;
	}

    /** 書き込み用フォーマッタを生成する */
	private DataRecordFormatter createWriteFormatter(File formatFile, File outputFile)
			throws FileNotFoundException {
		DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
		formatter.setOutputStream(new FileOutputStream(outputFile)).initialize();
		return formatter;
	}

    /** 指定ファイルから一行読み込む */
	private String readLineFrom(File outputFile, String encoding)
			throws UnsupportedEncodingException, FileNotFoundException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(outputFile), encoding));
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
    /**
     * マルチバイト文字の読み込みができることの確認。
     * 観点①：シングルバイト文字が読み込める
     * 観点②：ダブルバイト文字が読み込める
     * 観点③：シングルバイト・ダブルバイト混合文字が読み込める
     * 観点④：シングルバイト・ダブルバイト・３バイト混在文字が読み込める
     */
    @Test
    public void testReadMultiByte() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)
        ***************************************************/
        formatFile.deleteOnExit();
        
        /*
         * 観点①
         */
        DataRecordFormatter formatter = createReadFormatter("0123456789", "sjis");
        DataRecord readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("0123456789"));

        /*
         * 観点②
         */
        formatter = createReadFormatter("０１２３４５６７８９", "sjis");
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("０１２３４"));
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("５６７８９"));

        /*
         * 観点③
         */
        formatter = createReadFormatter("012345６７８９０１２", "sjis");
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("012345６７"));
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("８９０１２"));
        
        formatFile.delete();
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)
        ***************************************************/
        formatFile.deleteOnExit();
        
        System.out.println("".getBytes().length);
        
        /*
         * 観点④
         */
        formatter = createReadFormatter("01А名武234羅5678", "utf8"); // Аは2バイト文字、名、武、羅は3バイト文字
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("01А名武"));
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("234羅5678"));
    }
	
    /**
     * マルチバイト文字の読み込み時にトリムできることの確認。
     * 観点①：左はトリムが行われない
     * 観点②：シングルバイト・ダブルバイト・３バイト混在文字の場合に、デフォルトの半角スペースで右トリムが行われる
     * 観点③：シングルバイト・ダブルバイト・３バイト混在文字の場合に、指定したパディング文字（"0"）で右トリムが行われる
     */
    @Test
    public void testReadMultiByteWithTrim() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)
        ***************************************************/
        formatFile.deleteOnExit();
        
        /*
         * 観点①
         */
        DataRecordFormatter formatter = createReadFormatter("    0А名", "utf8"); // Аは2バイト文字、名、武、羅は3バイト文字
        DataRecord readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("    0А名"));

        /*
         * 観点②
         */
        formatter = createReadFormatter("0А名    武234羅 ", "utf8");
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("0А名"));
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("武234羅"));

        /*
         * 観点③
         */
        formatFile.delete();
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)  pad("0")
        ***************************************************/
        formatFile.deleteOnExit();
        
        formatter = createReadFormatter("1А名0000武234羅0", "utf8");
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("1А名"));
        readRecord = formatter.readRecord();
        assertThat(String.valueOf(readRecord.get("multiByteString")), is("武234羅"));

    }
    
    /**
     * マルチバイト文字の書き込みができることの確認。
     * 観点①：シングルバイト文字が書き込める
     * 観点②：ダブルバイト文字が書き込める
     * 観点③：シングルバイト・ダブルバイト混合文字が書き込める
     * 観点④：シングルバイト・ダブルバイト・３バイト混在文字が書き込める
     */
    @Test
    public void testWriteMultiByte() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)
        ***************************************************/
        formatFile.deleteOnExit();

        File outputFile = new File("record.dat");
        
        /*
         * 観点①
         */
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "0123456789"); // Аは2バイト文字、名、武、羅は3バイト文字
        }});
        assertEquals("0123456789", readLineFrom(outputFile, "sjis"));
        
        
        /*
        * 観点②
        */
        formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "０１２３４"); 
        }});        
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "５６７８９"); 
        }});
        assertEquals("０１２３４５６７８９", readLineFrom(outputFile, "sjis"));
        
        /*
         * 観点③
         */
         formatter = createWriteFormatter(formatFile, outputFile);
         formatter.writeRecord(new DataRecord(){{
             put("multiByteString", "012345６７"); 
         }});        
         formatter.writeRecord(new DataRecord(){{
             put("multiByteString", "８９０１２"); 
         }});
         assertEquals("012345６７８９０１２", readLineFrom(outputFile, "sjis"));
         
          /*
           * 観点④
           */
         formatFile.delete();
         formatFile = Hereis.file("./format.fmt");
         /**********************************************
         # ファイルタイプ
         file-type:    "Fixed"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "utf8"
         # 各レコードの長さ
         record-length: 10

         # データレコード定義
         [Default]
         1    multiByteString     XN(10)
         ***************************************************/         
         formatFile.deleteOnExit();
         
         formatter = createWriteFormatter(formatFile, outputFile);
         formatter.writeRecord(new DataRecord(){{
             put("multiByteString", "01А名武"); // Аは2バイト文字、名、武、羅は3バイト文字
         }});        
         formatter.writeRecord(new DataRecord(){{
             put("multiByteString", "234羅5678"); 
         }});
         assertEquals("01А名武234羅5678", readLineFrom(outputFile, "utf8"));
    }

    /**
     * マルチバイト文字の書き込み時にパディングできることの確認。
     * 観点①：左はパディングが行われない
     * 観点②：シングルバイト・ダブルバイト・３バイト混在文字の場合に、デフォルトの半角スペースで右パディングが行われる
     * 観点③：シングルバイト・ダブルバイト・３バイト混在文字の場合に、指定したパディング文字（"0"）で右パディングが行われる
     */
    @Test
    public void testWriteMultiByteWithPadding() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)
        ***************************************************/
        formatFile.deleteOnExit();

        File outputFile = new File("record.dat");
        
        /*
         * 観点①
         */
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "    1А名"); // Аは2バイト文字、名、武、羅は3バイト文字
        }});
        assertEquals("    1А名", readLineFrom(outputFile, "utf8"));
        
        /*
        * 観点②
        */
        formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "0А名"); 
        }});        
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "武234羅"); 
        }});
        assertEquals("0А名    武234羅 ", readLineFrom(outputFile, "utf8"));
        
        /*
         * 観点③
         */
        formatFile.delete();
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)   pad("0")
        ***************************************************/     
        
        formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "0А名"); 
        }});        
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "武234羅"); 
        }});
        assertEquals("0А名0000武234羅0", readLineFrom(outputFile, "utf8"));
        
        /*
         * 観点④
         */
        formatFile.delete();
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "cp930"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)   pad("0")
        ***************************************************/     
        
        formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", "あaあ"); 
        }});        
//        formatter.writeRecord(new DataRecord(){{
//            put("multiByteString", "武234"); 
//        }});
        assertEquals("あaあ0", readLineFrom(outputFile, "cp930"));
    }
    

    /**
     * シングルバイト・ダブルバイト・３バイト文字が混在したデフォルト値を書き込めることのテスト。
     */
    @Test
    public void testWriteMultiByteWithDefaultValue() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)    "123А名"  # デフォルト値を設定。Аは2バイト文字、名、武、羅は3バイト文字
        ***************************************************/
        formatFile.deleteOnExit();

        File outputFile = new File("record.dat");
        
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("multiByteString", null); 
        }});        
        assertEquals("123А名  ", readLineFrom(outputFile, "utf8"));
        
    }
    
    /**
     * バイト長が数値型でない場合に、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout1() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "utf8"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN("a")   # 文字列（不正な値）
        ***************************************************/
        formatFile.deleteOnExit();
        
        InputStream source = createInputStreamFrom("0123456789");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. 1st parameter must be an integer. " +
                            "parameter=[a]. convertor=[ByteStreamDataString]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));       
        }
    }
    
    
    /**
     * レイアウト定義ファイルで設定したフィールドの長さを超える文字列を書きこもうとした場合に、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout3() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10)   # 文字列（不正な値）
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setOutputStream(outputStream).initialize();
        
        DataRecord dataRecord = new DataRecord(){{
            put("multiByteString", "01234567890"); // 11バイトの文字を書き込む
        }};

        try {
            formatter.writeRecord(dataRecord);
            fail();
        } catch (InvalidDataFormatException e) {
        	assertTrue(e.getMessage().contains("too large data."));
        	assertTrue(e.getMessage().contains("field size = '10' data size = '11"));
        	assertTrue(e.getMessage().contains("data: 01234567890"));
        	assertTrue(e.getMessage().contains("field name=[multiByteString]"));
        }
        formatter.close();
    }
    
    /**
     * パディング文字に２バイト文字を設定した場合、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout4() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10) pad("　")   # ２バイトの全角空白を設定
        ***************************************************/
        formatFile.deleteOnExit();

        FileOutputStream outputStream = new FileOutputStream("test.dat");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(formatFile);
        formatter.setOutputStream(outputStream).initialize();
        
        DataRecord dataRecord = new DataRecord(){{
            put("multiByteString", "012345678"); // 11バイトの文字を書き込む
        }};

        try {
            formatter.writeRecord(dataRecord);
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals(
                    "invalid parameter was specified. the length of padding string must be 1. but specified one was 2 byte long.",
                    e.getMessage());
        }
        formatter.close();
    }
    

    /**
     * シングルバイトのパラメータが存在しない場合に、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayout5() throws Exception {
        
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN()   # 文字列（不正な値）
        ***************************************************/
        formatFile.deleteOnExit();
        
        InputStream source = createInputStreamFrom("0123456789");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        try {
            formatter.setInputStream(source).initialize();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter was not specified. parameter must be specified. " +
                            "convertor=[ByteStreamDataString]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }
    }

    /**
     * 指定したパディング文字が1文字でない場合に、例外がスローされることの確認。
     * 観点①【異常系】パディング文字が0文字
     * 観点②【異常系】パディング文字が2文字
     * 観点③パディング文字が1文字
     */
    @Test
    public void testInvalidLayout6() throws Exception {
        
    	/*
    	 * 観点①
    	 */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10) pad("")  # 不正なパディング文字!!
        ***************************************************/
        formatFile.deleteOnExit();
        
        InputStream source = createInputStreamFrom("0123456789");

        DataRecordFormatter formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        formatter.setInputStream(source).initialize();

        try {
            formatter.readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid padding character was specified. Length of padding character must be '1', but was '0'. padding str = []"));
        }
        

    	/*
    	 * 観点②
    	 */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10) pad("  ")  # 不正なパディング文字!!半角スペース2個
        ***************************************************/
        formatFile.deleteOnExit();
        
        source = createInputStreamFrom("0123456789");

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        formatter.setInputStream(source).initialize();

        try {
            formatter.readRecord();
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid padding character was specified. Length of padding character must be '1', but was '2'. padding str = [  ]"));
        }

    	/*
    	 * 観点③
    	 */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1    multiByteString     XN(10) pad("a")  # 正常なパディング文字 "a"
        ***************************************************/
        formatFile.deleteOnExit();
        
        source = createInputStreamFrom("aaaa111aaa");

        formatter = 
            FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File("format.fmt"));
        
        formatter.setInputStream(source).initialize();

        DataRecord readRecord = formatter.readRecord();

        assertThat(readRecord.getString("multiByteString"), is("aaaa111"));
    }
    
    /**
     * 初期化時のパラメータ不正テスト。
     */
    @Test
    public void initializeArgError(){
        
        /**
         * 引数がnull。
         */
        
    	ByteStreamDataString zonedDecimal = new ByteStreamDataString();
        try {
            zonedDecimal.initialize(null, "hoge");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("1st parameter was null. parameter=[null, hoge]. convertor=[ByteStreamDataString].", e.getMessage());
        }
        
    }
    
    /**
     * 出力時にパラメータがnullまたは空白の場合のテスト。
     */
    @Test
    public void testWriteParameterNullOrEmpty() throws Exception {
        ByteStreamDataString MultiByteCharacter = new ByteStreamDataString();
        MultiByteCharacter.init(new FieldDefinition().setEncoding(Charset.forName("MS932")), 10);
        assertThat("          ".getBytes("MS932"), is(MultiByteCharacter.convertOnWrite(null)));
        assertThat("          ".getBytes("MS932"), is(MultiByteCharacter.convertOnWrite("")));
    }

    /**
     * {@link BigDecimal}書き込みのテスト
     * @throws Exception
     */
    @Test
    public void testWriteBigDecimal() throws Exception {
        final ByteStreamDataString sut = new ByteStreamDataString();
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")), 12);

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1           ".getBytes(Charset.forName("utf-8"))));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")),
                is("0.0000000001".getBytes(Charset.forName("utf-8"))));
    }

    /**
     * {@link BigDecimal}書き込みでサイズを超過した場合のテスト。
     */
    @Test
    public void testWriteBigDecimal_SizeOver() throws Exception {
        final ByteStreamDataString sut = new ByteStreamDataString();
        sut.init(new FieldDefinition().setEncoding(Charset.forName("utf-8")), 11);
        try {
            sut.convertOnWrite(new BigDecimal("0.0000000001"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(),
                    CoreMatchers.containsString("field size = '11' data size = '12'. data: 0.0000000001"));
        }
    }
}
