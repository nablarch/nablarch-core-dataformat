package nablarch.common.io;

import java.io.File;

import nablarch.core.dataformat.FileRecordWriter;

/**
 * FileRecordWriterHolderクラスのテストで使用されるスタブ。
 */
public class FileRecordWriterHolderStub extends FileRecordWriterHolder{

    @Override
    protected FileRecordWriter createFileRecordWriter(
            String dataFileBasePathName, String dataFileName,
            String layoutFileBasePathName, String layoutFileName, int bufferSize) {
        return new FileRecordWriterStub(new File(dataFileName), new File(layoutFileName));
    }
    
    public class FileRecordWriterStub extends FileRecordWriter {

        public FileRecordWriterStub(File dataFile, File layoutFile) {
            super(dataFile, layoutFile);
        }

        @Override
        protected void initialize() {
            // nop
        }

        @Override
        public void close() {
            //nop
        }
    }

}
