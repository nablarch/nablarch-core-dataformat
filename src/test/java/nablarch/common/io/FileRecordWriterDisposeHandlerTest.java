package nablarch.common.io;

import nablarch.fw.ExecutionContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * {@link FileRecordWriterDisposeHandler}のテストクラス
 */
public class FileRecordWriterDisposeHandlerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public final ExecutionContext ctx = mock(ExecutionContext.class);

    /** テスト対象 */
    public FileRecordWriterDisposeHandler sut = new FileRecordWriterDisposeHandler();

    @Test
    public void 往路と復路でそれぞれclose処理が実行されていること() throws Exception {
        try (final MockedStatic<FileRecordWriterHolder> mocked = mockStatic(FileRecordWriterHolder.class)) {
            // 後続のハンドラが実行される前にcloseAllが実行されていること
            when(ctx.handleNext(null)).thenAnswer(invocation -> {
                mocked.verify(FileRecordWriterHolder::init);
                mocked.verify(FileRecordWriterHolder::closeAll, never());
                return null;
            });
            
            // 実行
            sut.handle(null, ctx);

            // ハンドラの復路で再度closeAllが実行されていること
            mocked.verify(FileRecordWriterHolder::closeAll);
        }
    }

    @Test
    public void 後続のハンドラ内で例外が発生しても復路でclose処理が実行されること() throws Exception {
        try (final MockedStatic<FileRecordWriterHolder> mocked = mockStatic(FileRecordWriterHolder.class)) {
            // 後続のハンドラ内で例外を投げる
            when(ctx.handleNext(null)).thenThrow(new RuntimeException());

            // 実行
            try {
                sut.handle(null, ctx);
                fail();
            } catch (RuntimeException ignored) {
                mocked.verify(FileRecordWriterHolder::init);
                mocked.verify(FileRecordWriterHolder::closeAll);
            }
        }
    }
}