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
public class FormatterFactoryStub extends FormatterFactory{
    
    @Override
    public DataRecordFormatter createFormatter(File layoutFile) {
        return new DataRecordFormatterStub();
    }
    
    public static class DataRecordFormatterStub implements DataRecordFormatter{

        public DataRecordFormatterStub() {
            isCallClose = false;
        }
        
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            return null;
        }

        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
            
        }

        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
            
        }

        public DataRecordFormatter setInputStream(InputStream stream) {
            return this;
        }

        public static boolean isCallClose = false;
        
        public void close() {
            isCallClose = true;
        }


        public DataRecordFormatter setDefinition(LayoutDefinition definition) {
            return null;
        }

        public DataRecordFormatter setOutputStream(OutputStream stream) {
            return this;
        }

        public boolean hasNext() throws IOException {
            return false;
        }

        public DataRecordFormatter initialize() {
            return null;
        }

        public int getRecordNumber() {
            return 0;
        }
    }
    
    
}
