package nablarch.common.io;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * {@link FileRecordWriterDisposeHandler}のテストクラス
 */
public class FileRecordWriterDisposeHandlerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mocked
    public ExecutionContext ctx;

    /** テスト対象 */
    FileRecordWriterDisposeHandler sut = new FileRecordWriterDisposeHandler();

    /** テストファイル１ */
    File testFile1;

    /** テストファイル２ */
    File testFile2;

    @Before
    public void setUp() throws Exception {
        testFile1 = folder.newFile("test1.txt");
        testFile2 = folder.newFile("test2.txt");

        FilePathSetting.getInstance()
                .addBasePathSetting("test", "file:" + folder.getRoot().getPath())
                .addBasePathSetting("format", "file:src/test/resources/nablarch/common/io/");
    }

    @Test
    public void 後続のハンドラ呼び出し前にファイルがcloseされること() throws Exception {

        FileRecordWriterHolder.open("test", "test1.txt", "test.fmt");
        FileRecordWriterHolder.open("test", "test2.txt", "test.fmt");

        // openしているので、ファイルを削除できないこと
        assertThat(testFile1.delete(), is(false));
        assertThat(testFile2.delete(), is(false));


        // 後続のハンドラが実行される前にcloseされているため、削除できること
        new NonStrictExpectations() {{
            ctx.handleNext(null);
            result = new Delegate<Object>() {
                public String delegate(Object obj) {
                    assertThat(testFile1.delete(), is(true));
                    assertThat(testFile2.delete(), is(true));
                    return null;
                }
            };
        }};

        // 実行
        sut.handle(null, ctx);
    }

    @Test
    public void 後続のハンドラ内でopenされたファイルが復路でcloseされること() throws Exception {

        // 後続のハンドラ内でファイルをopenする
        new NonStrictExpectations() {{
            ctx.handleNext(null);
            result = new Delegate<Object>() {
                public String delegate(Object obj) {
                    FileRecordWriterHolder.open("test", "test1.txt", "test.fmt");
                    FileRecordWriterHolder.open("test", "test2.txt", "test.fmt");
                    return null;
                }
            };
        }};

        // 実行
        sut.handle(null, ctx);

        // 後続のハンドラ内でopenされたファイルが復路でcloseされているため削除できること
        assertThat(testFile1.delete(), is(true));
        assertThat(testFile2.delete(), is(true));
    }

    @Test
    public void 後続のハンドラ内で例外が発生しても復路でファイルがcloseされること() throws Exception {

        // 後続のハンドラ内でファイルをopenする
        new NonStrictExpectations() {{
            ctx.handleNext(null);
            result = new Delegate<Object>() {
                public String delegate(Object obj) {
                    FileRecordWriterHolder.open("test", "test1.txt", "test.fmt");
                    FileRecordWriterHolder.open("test", "test2.txt", "test.fmt");

                    throw new RuntimeException();
                }
            };
        }};

        // 実行
        try {
            sut.handle(null, ctx);
        } catch (RuntimeException e) {
            // 後続のハンドラ内でopenされたファイルが復路でcloseされているため削除できること
            assertThat(testFile1.delete(), is(true));
            assertThat(testFile2.delete(), is(true));
        }


    }
}