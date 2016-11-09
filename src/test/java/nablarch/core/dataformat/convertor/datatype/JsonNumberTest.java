package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.test.support.tool.Hereis;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonNumber}のテスト
 * 
 * @author TIS
 */
public class JsonNumberTest {

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonNumber}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        JsonNumber dataType = new JsonNumber();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        JsonNumber converter = new JsonNumber();
        assertEquals("\"data\"", converter.convertOnRead("\"data\""));
        assertEquals("data", converter.convertOnRead("data"));
        assertEquals("\"data", converter.convertOnRead("\"data"));
        assertEquals("data\"", converter.convertOnRead("data\""));
        assertEquals("", converter.convertOnRead(""));
        assertEquals(null, converter.convertOnRead(null));
    }
    
    /**
     * 書き込み時のテスト
     */
    @Test
    public void testConvertOnWrite() {
        // 入力値がそのまま返却される
        JsonNumber converter = new JsonNumber();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        assertEquals("", converter.convertOnWrite(""));
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * BigDecimalの書き込みのテスト
     * @throws Exception
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final JsonNumber sut = new JsonNumber();
        Assert.assertThat(sut.convertOnWrite(new BigDecimal(("1"))), is("1"));
        Assert.assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
    }

    /**
     * 出力時にnullが渡された場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type:    "JSON"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "UTF-8"

         # データレコード定義
         [Default]
         1    number     X9   "123"
         ***************************************************/
        formatFile.deleteOnExit();
        File outputFile = new File("record.dat");
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("number", null);
        }});
        assertThat(readLineFrom(outputFile, "UTF-8"), is("{\"number\":123}"));
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
}
