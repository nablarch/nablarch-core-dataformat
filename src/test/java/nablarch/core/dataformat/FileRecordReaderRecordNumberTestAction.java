package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.action.FileBatchAction;
import nablarch.fw.launcher.CommandLine;

import java.util.Map;

/**
 * FileRecordWriterHolderクラスのテストで使用されるAction。
 * @author Masato Inoue
 */
public class FileRecordReaderRecordNumberTestAction extends FileBatchAction {
    @Override
    public String getDataFileName() {
        return "test";
    }
    @Override
    public String getFormatFileName() {
        return "test";
    }

    private static final String WRITE_FILE_NAME = "result.dat";
    
    /**
     * ファイルをオープンする。
     */
    @Override
    protected void initialize(CommandLine command, ExecutionContext context) {
        FileRecordWriterHolder.open(WRITE_FILE_NAME, getFormatFileName());
    }
    
    /**
     * ヘッダメソッドでは、Holderを用いてファイルオープンおよびヘッダ行の書き出しを行う。
     */
    public Result doHeader(DataRecord record, ExecutionContext ctx) {
        final int recordNumber = record.getRecordNumber();
        final int lastRecordNumber = ctx.getLastRecordNumber();
        
        Map<String, Object> header = new DataRecord() {{
            put("type", "H");
            put("result", String.format("レコード番号=[%s], 物理的に読み込んだレコード番号=[%s]", recordNumber, lastRecordNumber));
        }};
        FileRecordWriterHolder.write(header, WRITE_FILE_NAME);
        ctx.setSessionScopedVar("count", new Integer(1));
        return new Result.Success(record.getString("type"));
    }
    
    public Result doData(final DataRecord record, ExecutionContext ctx) {

        final int recordNumber = record.getRecordNumber();
        final int lastRecordNumber = ctx.getLastRecordNumber();
        
        Map<String, Object> data1 = new DataRecord() {{
            put("type", "D");
            put("result", String.format("レコード番号=[%s], 物理的に読み込んだレコード番号=[%s]", recordNumber, lastRecordNumber));
        }};
        FileRecordWriterHolder.write(data1, WRITE_FILE_NAME);
        
        return new Result.Success(record.getString("type"));
    }
    
    public Result doTrailer(DataRecord record, ExecutionContext ctx) {
        
        final int recordNumber = record.getRecordNumber();
        final int lastRecordNumber = ctx.getLastRecordNumber();
        
        Map<String, Object> trailer = new DataRecord() {{
            put("type", "T");
            put("result", String.format("レコード番号=[%s], 物理的に読み込んだレコード番号=[%s]", recordNumber, lastRecordNumber));
        }};
        
        FileRecordWriterHolder.write(trailer, WRITE_FILE_NAME);
        
        return new Result.Success(record.getString("type"));
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
    
    @Override
    protected void terminate(Result result, ExecutionContext ctx) {

        final int lastRecordNumber = ctx.getLastRecordNumber();
        
        Map<String, Object> trailer = new DataRecord() {{
            put("type", "Terminate");
            put("result", String.format("物理的に読み込んだレコード番号=[%s]", lastRecordNumber));
        }};
        
        FileRecordWriterHolder.write(trailer, WRITE_FILE_NAME);

        super.terminate(result, ctx);
    }
    
}
