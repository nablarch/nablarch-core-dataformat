package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonBoolean}のテスト
 * 
 * @author TIS
 */
public class JsonBooleanTest {

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link JsonBoolean}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        JsonBoolean dataType = new JsonBoolean();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

    /**
     * 読み取り時のテスト
     */
    @Test
    public void testConvertOnRead() {
        // 入力値がそのまま返却される
        JsonBoolean converter = new JsonBoolean();
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
        JsonBoolean converter = new JsonBoolean();
        assertEquals("data", converter.convertOnWrite("data"));
        assertEquals("\"data\"", converter.convertOnWrite("\"data\""));
        assertEquals("\"data", converter.convertOnWrite("\"data"));
        assertEquals("data\"", converter.convertOnWrite("data\""));
        assertEquals("", converter.convertOnWrite(""));
        // nullはnull
        assertEquals(null, converter.convertOnWrite(null));
    }

    /**
     * {@link BigDecimal}を書き込むテスト。
     */
    @Test
    public void testConvertOnWrite_BigDecimal() throws Exception {
        final JsonBoolean sut = new JsonBoolean();

        assertThat(sut.convertOnWrite(BigDecimal.ONE), is("1"));
        assertThat(sut.convertOnWrite(new BigDecimal("0.0000000001")), is("0.0000000001"));
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
         1    bool     BL   "true"
         ***************************************************/
        formatFile.deleteOnExit();
        File outputFile = new File("record.dat");
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("bool", null);
        }});
        assertThat(readLineFrom(outputFile, "UTF-8"), is("{\"bool\":true}"));
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
