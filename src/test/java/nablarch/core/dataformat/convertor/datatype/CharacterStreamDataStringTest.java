package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link CharacterStreamDataSupport}のテスト。
 * 
 * @author Masato Inoue
 */
public class CharacterStreamDataStringTest {

    /**
     * 初期化時にnullが渡されたときのテスト。
     * {@link CharacterStreamDataSupport}では初期化時になにもしないため、nullを許容する。
     */
    @Test
    public void testInitializeNull() {
        CharacterStreamDataString dataType = new CharacterStreamDataString();

        assertThat(dataType.initialize(null), is((DataType<String, String>)dataType));
    }

    /**
     * 読込のテスト。
     * 空文字とそれ以外をテストする。
     */
    @Test
    public void testReadParameterEmpty() {
        CharacterStreamDataString dataType = new CharacterStreamDataString();

        assertThat(dataType.convertOnRead(""), is(""));
        assertThat(dataType.convertOnRead("abc"), is("abc"));
    }

    /**
     * 出力のテスト。
     * nullが渡された場合に、空文字に変換が行われること。
     */
    @Test
    public void testWriteObjectNotBigDecimal() {
        CharacterStreamDataString dataType = new CharacterStreamDataString();

        assertThat(dataType.convertOnWrite("abc"), is("abc"));
        // null の場合
        assertThat(dataType.convertOnWrite(null), is(""));
        // 空文字の場合
        assertThat(dataType.convertOnWrite(""), is(""));
    }

    @Test
    public void testWriteObject_BigDecimal() throws Exception {
        final CharacterStreamDataString sut = new CharacterStreamDataString();

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
         file-type:    "Variable"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "utf8"
         # 改行コード
         record-separator: "\r\n"
         # csv
         field-separator: ","

         # データレコード定義
         [Default]
         1    string     X   "0123"
         ***************************************************/
        formatFile.deleteOnExit();
        File outputFile = new File("record.dat");
        DataRecordFormatter formatter = createWriteFormatter(formatFile, outputFile);
        formatter.writeRecord(new DataRecord(){{
            put("string", null);
        }});
        assertThat(readLineFrom(outputFile, "utf8"), is("0123"));
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
