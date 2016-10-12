package nablarch.core.dataformat;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.action.FileBatchAction;
import nablarch.fw.launcher.CommandLine;

import java.math.BigDecimal;
import java.util.Map;

/**
 * FileRecordWriterHolderクラスのテストで使用されるAction。
 * @author Masato Inoue
 */
public class FileRecordReaderTestAction extends FileBatchAction{
    @Override
    public String getDataFileName() {
        return "test.dat";
    }
    @Override
    public String getFormatFileName() {
        return "resume";
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
        Map<String, Object> header = new DataRecord() {{
            put("type", "H");
            put("name", "結果OK");
        }};
        FileRecordWriterHolder.write(header, WRITE_FILE_NAME);
        ctx.setSessionScopedVar("count", new Integer(1));
        return new Result.Success(record.getString("type"));
    }
    
    public Result doData(final DataRecord record, ExecutionContext ctx) {
        Integer count = ctx.getSessionScopedVar("count");
        if(count == null) {
            count = 1;
        }
        final int finalcount = count;
        
        Map<String, Object> data1 = new DataRecord() {{
            put("type", "D");
            put("amount", String.format("%s回目のデータ書き込み。値=[%s]", finalcount, record.get("amount")));
        }};
        FileRecordWriterHolder.write(data1, WRITE_FILE_NAME);
        
        ctx.setSessionScopedVar("count", count + 1);
        
        return new Result.Success(record.getString("type"));
    }
    
    public Result doTrailer(DataRecord record, ExecutionContext ctx) {

        Map<String, Object> data = new DataRecord() {{
            put("type", "T");
            put("records", 3);
            put("totalAmount", new BigDecimal("5000"));
        }};
        FileRecordWriterHolder.write(data, WRITE_FILE_NAME);
        
        return new Result.Success(record.getString("type"));
    }
}
