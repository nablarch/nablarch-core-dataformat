package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterDisposeHandler;
import nablarch.fw.ExecutionContext;

/**
 * FileRecordReaderResumeMultithreadTestで例外メッセージを確認するための使用するスタブのハンドラ。
 * @author Masato Inoue
 */
public class FileRecordWriterDisposeHandlerStub extends FileRecordWriterDisposeHandler {
    
    public static String exceptionMessage = null;
    
    @Override
    public Object handle(Object data, ExecutionContext ctx) {
        try{
            return super.handle(data, ctx);
        } catch (RuntimeException e) {
            exceptionMessage = e.getMessage();
            throw e;
        }
    }
    
    public static void clearExceptionMessage() {
        exceptionMessage = null;
    }
}
