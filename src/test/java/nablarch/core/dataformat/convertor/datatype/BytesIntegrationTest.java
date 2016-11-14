package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * {@link Bytes}の機能結合テストクラス。
 *
 * @author TIS
 */
public class BytesIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private DataRecordFormatter formatter = null;

    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(file);
    }

    private void createFile(File formatFile, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(formatFile), "utf-8"));
        try {
            for (final String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }

    /**
     * 正常系のバイト配列の入力テスト。
     */
    @Test
    public void testRead() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 3",
                "",
                "[Default]",
                "1    byteString     B(3)   # バイト列"
        );
        createFormatter(formatFile);


        final InputStream inputStream = new ByteArrayInputStream("abc".getBytes("sjis"));
        formatter.setInputStream(inputStream)
                 .initialize();

        DataRecord record = formatter.readRecord();

        assertThat(record.getBytes("byteString"), is("abc".getBytes("sjis")));
    }

    /**
     * 正常系の、バイト配列の出力テスト。
     * CI上でconvertOnWriteメソッドのカバレッジが通っていなかったので、念のため追加。
     */
    @Test
    public void testWrite() throws Exception {

        // レイアウト定義ファイル
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 3",
                "",
                "[Default]",
                "1    byteString     B(3)   # バイト列"
         );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        DataRecord record = new DataRecord();
        record.put("byteString", "abc".getBytes());
        formatter.writeRecord(record);

        final String actual = outputStream.toString("sjis");

        assertThat(actual, is("abc"));
    }

    /**
     * レイアウト定義ファイルにパラメータを設定しなかった場合、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayoutNotSpecified() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteString     B()   # バイト列（引数を指定しない）"
        );
        createFormatter(formatFile);

        InputStream source = new ByteArrayInputStream(
                "testtesttest".getBytes("sjis"));

        exception.expect(allOf(
                instanceOf(SyntaxErrorException.class),
                hasProperty("message", is(startsWith("parameter was not specified. parameter must be specified. convertor=[Bytes]."))),
                hasProperty("filePath", is(endsWith("format.fmt")))
        ));

        formatter.setInputStream(source).initialize();
    }

    /**
     * レイアウト定義ファイルに不正なバイト列のパラメータを設定した場合、例外がスローされることの確認。
     */
    @Test
    public void testInvalidLayoutWrongArgument() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile,
                "file-type:    \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "",
                "[Default]",
                "1    byteString     B(\"a\")   # バイト列（引数を指定しない）"
        );
        createFormatter(formatFile);

        InputStream source = new ByteArrayInputStream(
                "testtesttest".getBytes("sjis"));

        exception.expect(allOf(
                instanceOf(SyntaxErrorException.class),
                hasProperty("message", is(startsWith("invalid parameter type was specified. parameter type must be 'Integer' " +
                        "but was: 'java.lang.String'. parameter=[a]. convertor=[Bytes]."))),
                hasProperty("filePath", is(endsWith("format.fmt")))
        ));

        formatter.setInputStream(source).initialize();
    }
}
