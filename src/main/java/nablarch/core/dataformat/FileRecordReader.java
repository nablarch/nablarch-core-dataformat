package nablarch.core.dataformat;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.annotation.Published;

/**
 * ファイルからの読み込みを行うリーダ。
 * <p>
 * 入力形式のフォーマット定義ファイルと入力先ファイルを指定する。
 * 明示的に指定しなかった場合のフォーマット定義ファイルの参照ディレクトリは、
 * "format"論理ベースパスに設定されたパスとなる。
 * 同様に、データファイルの入力先は"input"論理ベースパスに設定されたパスとなる。
 * </p>
 * <p>
 * アプリケーションから、本クラスを直接使用することは許可しない。
 * </p>
 * <p>
 * 本クラスはスレッドセーフを考慮した実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class FileRecordReader implements Closeable {
    
    /** ロガー **/
    private static final Logger LOGGER = LoggerManager.get(FileRecordReader.class);
    
    /** フォーマット定義ファイル */
    private File layoutFile = null;
    
    /** データファイル */
    private File dataFile = null;
    
    /** ファイルレコードリーダ */
    private DataRecordFormatter formatter = null;
    
    /** ファイル読み込みの際に使用するバッファのサイズ（デフォルト:8192B） */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** ファイル読み込みの際に使用するバッファのサイズ */
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    
    /** ファイルストリーム */
    private InputStream source = null;

    /**
     * コンストラクタ。
     * @param dataFile   データファイル
     * @param layoutFile フォーマット定義ファイル
     */
    public FileRecordReader(File dataFile, File layoutFile) {
        this(dataFile, layoutFile, DEFAULT_BUFFER_SIZE);
    }

    /**
     * コンストラクタ。
     * @param dataFile   データファイル
     * @param layoutFile フォーマット定義ファイル
     * @param bufferSize ファイル読み込みの際に使用するバッファのサイズ
     */
    public FileRecordReader(File dataFile, File layoutFile, int bufferSize) {
        this.dataFile = dataFile;
        this.layoutFile = layoutFile;
        if (bufferSize > 0) {
            this.bufferSize = bufferSize;
        } else {
            throw new IllegalArgumentException("buffer size was invalid. buffer size must be bigger than 0.");
        }
        initialize();
    }

    /**
     * コンストラクタ。
     * @param dataFile データファイル
     * @param layoutDefinition フォーマット定義情報保持クラス
     */
    public FileRecordReader(File dataFile, LayoutDefinition layoutDefinition) {
        this.dataFile = dataFile;
        formatter = FormatterFactory.getInstance().createFormatter(layoutDefinition);
        initialize(formatter);
    }

    /**
     * 初期化処理を行う。
     * @return このオブジェクト自体
     */
    protected FileRecordReader initialize() {
        formatter = FormatterFactory.getInstance().createFormatter(layoutFile);
        initialize(formatter);
        return this;
    }


    /**
     * 初期化を行う。
     * @param formatter フォーマッタ
     * @return このオブジェクト自体
     */
    protected FileRecordReader initialize(DataRecordFormatter formatter) {
        createInputStream();
        formatter.setInputStream(source).initialize();
        return this;
    }
    
    /**
     * 入力ストリームを生成する。
     */
    protected void createInputStream() {
        try {
            source = new BufferedInputStream(
                new FileInputStream(dataFile),
                bufferSize
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "I/O error happened while opening the file. file path=[" + dataFile.getAbsolutePath() + "]"
              , e 
            );
        }
    }

    /**
     * 指定されたデータファイルから次のレコードを読み込んで返す。
     * @return データレコード
     */
    public DataRecord read() {
        if (!hasNext()) {
            return null;
        }
        // 一行読み込む
        DataRecord record = readRecord();
        
        return record;
    }

    /**
     * 次に読み込むレコードがあるかどうかを返却する。
     * @return 次に読み込むレコードがある場合、true
     */
    public boolean hasNext() {
        try {
            return formatter.hasNext();
        } catch (IOException e) {
            throw new RuntimeException(
                "I/O error happened while reading the file. file path=[" + dataFile.getPath() + "]"
              , e 
            );
        }
    }
    
    /**
     * レコードを1行読み込み、結果を返却する。
     * @return 読み込んだ結果が格納されるデータレコード
     */
    protected DataRecord readRecord() {
        DataRecord read;
        try {
            read = formatter.readRecord();
        } catch (InvalidDataFormatException e) {
            throw e.setInputSourcePath(dataFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error happened while reading the file. file path=["
                            + dataFile.getPath() + "]", e);
        }
        return read;
    }

    /** 
     * 指定されたデータファイルに対するストリームを閉じ、
     * ファイルハンドラを開放する。
     */
    public void close() {
        formatter.close();
        try {
            source.close();
        } catch (IOException e) {
            LOGGER.logWarn("I/O error happened while closing the file.", e);
        }
    }

    /**
     * 読み込み中のレコードのレコード番号を返却する。
     * @return レコード番号
     */
    public int getRecordNumber() {
        return formatter.getRecordNumber();
    }
}
