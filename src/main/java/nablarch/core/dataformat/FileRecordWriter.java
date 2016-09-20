package nablarch.core.dataformat;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.annotation.Published;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * データレコードをファイルに出力するクラス。
 * <p>
 * 出力形式のフォーマット定義ファイルと出力先ファイルを指定する。
 * 明示的に指定しなかった場合のフォーマット定義ファイルの参照ディレクトリは、
 * "format"論理ベースパスに設定されたパスとなる。
 * 同様に、データファイルの出力先は"output"論理ベースパスに設定されたパスとなる。
 * </p>
 * <p>
 * アプリケーションから、本クラスを直接使用することは許可しない。
 * </p>
 * <p>
 * 本クラスはスレッドセーフな実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class FileRecordWriter implements Closeable {

    /** ロガー **/
    private static final Logger LOGGER = LoggerManager.get(FileRecordWriter.class);

    // ---------------------------------------------------- structure
    /** 出力先データファイル */
    private final File dataFile;

    /** フォーマット定義ファイル */
    private File layoutFile = null;

    /** データレコードフォーマッタ */
    private DataRecordFormatter formatter = null;

    /** ファイルストリーム */
    private OutputStream dest = null;

    /** ファイル読み込みの際に使用するバッファのサイズ（デフォルト:8192B） */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** ファイル読み込みの際に使用するバッファのサイズ */
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * 書き込むデータファイルのファイル名を指定するコンストラクタ。
     *
     * "input"論理ベースパス配下に存在する当該のファイル名のファイルにデータを書き出す。
     * @param dataFile データファイル
     * @param layoutFile フォーマット定義ファイル
     */
    public FileRecordWriter(File dataFile, File layoutFile) {
        this(dataFile, layoutFile, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 書き込むデータファイルのベースパス論理名およびファイル名を指定するコンストラクタ。
     *
     * 指定されたベースパス配下に存在する当該のファイル名のファイルにデータを書き出す。
     * @param dataFile データファイル
     * @param layoutFile フォーマット定義ファイル
     * @param bufferSize ファイル読み込みの際に使用するバッファのサイズ
     */
    public FileRecordWriter(File dataFile, File layoutFile, int bufferSize) {
        if (dataFile == null) {
            throw new IllegalArgumentException("data file was null. data file must not be null.");
        }
        if (layoutFile == null) {
            throw new IllegalArgumentException("layout file was null. layout file must not be null.");
        }
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
     * コンストラクタ。<br/>
     * フォーマット定義ファイルを読まずに、{@link LayoutDefinition}を直接指定する。
     *
     * @param dataFile データファイル
     * @param layoutDefinition フォーマット定義
     */
    public FileRecordWriter(File dataFile, LayoutDefinition layoutDefinition) {
        this.dataFile = dataFile;
        formatter = FormatterFactory.getInstance().createFormatter(layoutDefinition);
        initialize(formatter);
    }

    /** フォーマット定義を読み込み、出力先ストリームを作成する。 */
    protected void initialize() {
        formatter = FormatterFactory.getInstance().createFormatter(layoutFile);
        initialize(formatter);
    }

    /**
     * 初期化処理を行う。<br/>
     * 出力ストリームを作成し、フォーマッタに設定する。
     *
     * @param formatter フォーマッタ
     */
    protected void initialize(DataRecordFormatter formatter) {
        createOutputStream();
        formatter.setOutputStream(dest).initialize();
        this.formatter = formatter;
    }

    // ---------------------------------------------------- API
    /**
     * 指定されたレコードデータファイルに出力する。
     *
     * 本メソッドでは、データファイルのストリームに対して書き込みを行うが、
     * フラッシュは行わず、ストリームも開いたまま維持される。
     * そのため、try-finally句で囲うなどして、書き込み終了後に
     * 必ずclose()メソッドを実行する必要がある。
     *
     * なお、本メソッドの処理中に例外が発生した場合、データファイルのストリームは
     * 自動的にクローズされる。
     * （クローズメソッドを複数回呼んだとしても特に問題は発生しない。）
     *
     * @param record ファイルに出力するレコード
     * @return このオブジェクト自体
     */
    public FileRecordWriter write(Map<String, ?> record) {
        return doWrite(null, record);
    }

    /**
     * レコードタイプを明示的に指定してレコードを出力する。
     *
     * @param recordType 出力するレコードのレコードタイプ
     * @param record     出力するレコード
     * @return このオブジェクト自体
     */
    public FileRecordWriter write(String recordType, Map<String, ?> record) {
        if (recordType == null || recordType.length() == 0) {
            throw new IllegalArgumentException("record type was blank. record type must not be blank.");
        }
        return doWrite(recordType, record);
    }

    /**
     * レコードタイプを指定してレコードを出力する。
     *
     * @param recordType 出力するレコードのレコードタイプ
     * @param record     出力するレコード
     * @return このオブジェクト自体
     */
    protected FileRecordWriter doWrite(String recordType, Map<String, ?> record) {
        return doWrite(formatter, recordType, record);
    }

    /**
     *　 レコードタイプを指定してレコードを出力する。
     * @param formatter フォーマッタ
     * @param recordType 出力するレコードのレコードタイプ
     * @param record     出力するレコード
     * @return このオブジェクト自体
     */
    protected FileRecordWriter doWrite(DataRecordFormatter formatter, String recordType, Map<String, ?> record) {
        try {
            if (recordType == null) {
                formatter.writeRecord(record);
            } else {
                formatter.writeRecord(recordType, record);
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error occurred while writing a record. record=["
                            + record.toString() + "]", e
            );
        }
    }

    /** 出力ストリームを生成する。 */
    protected void createOutputStream() {
        dest = createOutputStream(dataFile, bufferSize);
    }

    /**
     * 出力ストリームを生成する。
     *
     * @param dataFile   出力先ファイル
     * @param bufferSize バッファサイズ
     * @return 出力ストリーム
     */
    protected OutputStream createOutputStream(File dataFile, int bufferSize) {
        try {
            return new BufferedOutputStream(new FileOutputStream(dataFile),
                    bufferSize);
        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error happened when open the file. file path=["
                            + dataFile.getPath() + "]", e);
        }
    }

    /**
     * 書き込み先のファイルストリームを閉じる。
     */
    public void close() {
        formatter.close();
        try {
            dest.close();
        } catch (IOException e) {
            // リーダを閉じる際にエラーが発生しても処理を継続する。
            LOGGER.logWarn("I/O error happened while closing the file.", e);
        }
    }


}
