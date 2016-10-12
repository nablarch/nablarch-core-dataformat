package nablarch.core.dataformat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * FormatterFactoryのテストで使用されるスタブクラス。
 * @author Masato Inoue
 *
 */
public class FormatterFactoryStub02 extends FormatterFactory {
    
    public LayoutDefinition createDefinition;
    
    
    @Override
    protected LayoutDefinition createDefinition(File layoutFile) {
        createDefinition = super.createDefinition(layoutFile);
        return createDefinition;
    }

    
    @Override
    protected DataRecordFormatter createFormatter(String fileType, String formatFilePath) {
        return new DataRecordFormatterStub();
    }
    
    public class DataRecordFormatterStub implements DataRecordFormatter{

        
        
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            throw new IOException("");
        }

        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
            throw new IOException("");
        }

        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
            
        }

        public DataRecordFormatter initialize() {
            return this;
        }

        public DataRecordFormatter setInputStream(InputStream stream) {
            return this;
        }

        public void close() {
        }

        public DataRecordFormatter setDefinition(LayoutDefinition definition) {
            return this;
        }

        public DataRecordFormatter setOutputStream(OutputStream stream) {
            return this;
        }

        public boolean hasNext() throws IOException {
            throw new IOException("");
        }

        public int getRecordNumber() {
            return 0;
        }
        
    }
}
