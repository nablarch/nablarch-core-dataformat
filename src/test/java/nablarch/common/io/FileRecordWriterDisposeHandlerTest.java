package nablarch.common.io;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import nablarch.fw.ExecutionContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link FileRecordWriterDisposeHandler}のテストクラス
 */
public class FileRecordWriterDisposeHandlerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mocked
    public ExecutionContext ctx;

    @Mocked
    public FileRecordWriterHolder holder;

    /** テスト対象 */
    public FileRecordWriterDisposeHandler sut = new FileRecordWriterDisposeHandler();

    @Test
    public void 往路と復路でそれぞれclose処理が実行されていること() throws Exception {

        // 後続のハンドラが実行される前にcloseAllが実行されていること
        new NonStrictExpectations() {{
            ctx.handleNext(null);
            result = new Delegate<Object>() {
                public String delegate(Object obj) {
                    new Verifications() {{
                        FileRecordWriterHolder.closeAll();
                        times = 1;
                    }};
                    return null;
                }
            };
        }};

        // 実行
        sut.handle(null, ctx);

        // ハンドラの復路で再度closeAllが実行されていること
        new Verifications() {{
            FileRecordWriterHolder.closeAll();
            times = 2;
        }};
    }

    @Test
    public void 後続のハンドラ内で例外が発生しても復路でclose処理が実行されること() throws Exception {

        // 後続のハンドラ内で例外を投げる
        new NonStrictExpectations() {{
            ctx.handleNext(null);
            result = new Delegate<Object>() {
                public String delegate(Object obj) {
                    throw new RuntimeException();
                }
            };
        }};

        // 実行
        try {
            sut.handle(null, ctx);
        } catch (RuntimeException ignored) {
            new Verifications() {{
                FileRecordWriterHolder.closeAll();
                times = 2;
            }};
        }
    }
}