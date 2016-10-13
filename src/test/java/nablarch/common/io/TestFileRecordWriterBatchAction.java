package nablarch.common.io;

import nablarch.core.dataformat.DataRecord;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.action.FileBatchAction;
import nablarch.fw.launcher.CommandLine;

import java.math.BigDecimal;
import java.util.Map;

/**
 * FileRecordWriterHolderクラスのテストで使用されるAction。
 */
public class TestFileRecordWriterBatchAction extends FileBatchAction{
    @Override
    public String getDataFileName() {
        return "test.dat";
    }
    @Override
    public String getFormatFileName() {
        return "test";
    }

    private static final String WRITE_FILE_BASEPATH = "outputTest";
    private static final String WRITE_FILE_NAME = "result.dat";
    private static final String WRITE_FILE_NAME2 = "result2.dat";
    
    /**
     * ファイルをオープンする。
     */
    @Override
    protected void initialize(CommandLine command, ExecutionContext context) {
        FileRecordWriterHolder.open(WRITE_FILE_NAME, getFormatFileName());
        FileRecordWriterHolder.open(WRITE_FILE_BASEPATH, WRITE_FILE_NAME2, getFormatFileName());
    }
    
    /**
     * ヘッダメソッドでは、Holderを用いてファイルオープンおよびヘッダ行の書き出しを行う。
     */
    public Result doHeader(DataRecord record, ExecutionContext ctx) {
        Map<String, Object> header = new DataRecord() {{
            put("type", "H");
            put("name", "ヘッダ");
        }};
        FileRecordWriterHolder.get(WRITE_FILE_NAME).write(header);
        ctx.setSessionScopedVar("count", new Integer(1));
        return new Result.Success(record.getString("type"));
    }
    
    public Result doData(DataRecord record, ExecutionContext ctx) {
        final Integer count = ctx.getSessionScopedVar("count");
        
        Map<String, Object> data1 = new DataRecord() {{
            put("type", "D");
            put("amount", String.format("%s回目のデータ書き込み", count));
        }};
        FileRecordWriterHolder.get(WRITE_FILE_NAME).write(data1);
        
        ctx.setSessionScopedVar("count", count + 1);
        

        Map<String, Object> dataResult2 = new DataRecord() {{
            put("type", "D");
            put("amount", String.format("result2 write", count));
        }};
        FileRecordWriterHolder.write(dataResult2, WRITE_FILE_BASEPATH, WRITE_FILE_NAME2);
        
        return new Result.Success(record.getString("type"));
    }
    
    public Result doTrailer(DataRecord record, ExecutionContext ctx) {

        Map<String, Object> data = new DataRecord() {{
            put("type", "T");
            put("records", 3);
            put("totalAmount", new BigDecimal("5000"));
        }};
        FileRecordWriterHolder.get(WRITE_FILE_NAME).write(data);
        
        
        return new Result.Success(record.getString("type"));
    }
}
