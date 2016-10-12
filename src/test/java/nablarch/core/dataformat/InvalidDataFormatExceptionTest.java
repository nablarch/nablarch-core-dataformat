package nablarch.core.dataformat;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * InvalidDataFormatExceptionのgetterのテスト。
 *
 * @author Masato Inoue
 */
public class InvalidDataFormatExceptionTest {

    /** getterのテスト。 */
    @Test
    public void testGetter() {
        InvalidDataFormatException ex = new InvalidDataFormatException("this is an error message.");
        ex.setRecordNumber(100);
        ex.setFieldName("hoge");
        ex.setFormatFilePath("path/to/format.fmt");
        ex.setInputSourcePath("IamReadingThis.txt");

        assertEquals(100, ex.getRecordNumber());
        assertEquals("hoge", ex.getFieldName());
        assertEquals("path/to/format.fmt", ex.getFormatFilePath());
        assertEquals("IamReadingThis.txt", ex.getInputSourcePath());
    }

    /**
     * 付与された情報がメッセージ出力時に出力されること。
     */
    @Test
    public void testMessageFormatting() {
        // 付加情報なし
        InvalidDataFormatException ex
                = new InvalidDataFormatException("this is an error message.");
        assertThat(ex.getMessage(), is(
                "this is an error message."));

        // レコード行
        ex.setRecordNumber(100);
        assertThat(ex.getMessage(), is(
                "this is an error message. record number=[100]."));

        // フィールド名
        ex.setFieldName("hoge");
        assertThat(ex.getMessage(), is(
                "this is an error message. field name=[hoge]. record number=[100]."));

        // 入力元
        ex.setInputSourcePath("IamReadingThis.txt");
        assertThat(ex.getMessage(), is(
                "this is an error message. source=[IamReadingThis.txt]. field name=[hoge]. record number=[100]."));

        // フォーマットファイル
        ex.setFormatFilePath("path/to/format.fmt");
        assertThat(ex.getMessage(), is(
                "this is an error message. " +
                "source=[IamReadingThis.txt]. " +
                "field name=[hoge]. record number=[100]. " +
                "format file=[path/to/format.fmt]."));
    }
}
