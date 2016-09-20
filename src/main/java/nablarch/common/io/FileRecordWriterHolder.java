package nablarch.common.io;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.dataformat.FileRecordWriter;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * {@link FileRecordWriter}のインスタンスをスレッド毎に管理するクラス。
 * <p/>
 * スレッド毎に管理する{@link FileRecordWriter}インスタンスの生成及び取得、クローズ機能を持つ。
 * {@link FileRecordWriterDisposeHandler}をハンドラとして設定する場合、
 * 本クラスがスレッド上で管理するすべての{@link FileRecordWriter}が{@link FileRecordWriterDisposeHandler}により自動的にクローズされるので、
 * 業務アプリケーションで本クラスの{@link #close}メソッドを呼び出す必要はない。
 *
 * @see FileRecordWriter
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class FileRecordWriterHolder {

    /** ファイル読み込みの際に使用するバッファのサイズ（デフォルトは8192B） */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** コンバータを保持するキーとして使用する、ベースパスとファイル名の区切り文字 */
    private static final String KEY_SEPARATOR = "&";

    /** システムリポジトリ上の登録名 */
    private static final String REPOSITORY_KEY =  "fileRecordWriterHolder";

    /**
     * デフォルトのコンバータ設定情報保持クラスのインスタンス。
     * リポジトリからインスタンスを取得できなかった場合に、デフォルトでこのインスタンスが使用される。
     */
    private static final FileRecordWriterHolder DEFAULT_HOLDER = new FileRecordWriterHolder();

    /**
     * 本クラスのインスタンスを{@link SystemRepository}より取得する。
     * <p>
     * {@link SystemRepository}にインスタンスが存在しない場合は、クラスロード時に生成した本クラスのインスタンスを返却する。
     *
     * @return 本クラスのインスタンス
     */
    public static FileRecordWriterHolder getInstance() {
        FileRecordWriterHolder holder = SystemRepository.get(REPOSITORY_KEY);
        if (holder == null) {
            return DEFAULT_HOLDER;
        }
        return holder;
    }


    /**
     * カレントスレッド上で管理されるファイル毎のファイルレコードライタを格納する。
     * ファイルレコードライタは{@link InheritableThreadLocal}クラスで管理されるので、親スレッドから子スレッドへインスタンスが引き継がれる。
     */
    private static final ThreadLocal<Map<String, FileRecordWriter>> WRITERS = new InheritableThreadLocal<Map<String, FileRecordWriter>>() {
        @Override
        protected Map<String, FileRecordWriter> initialValue() {
            return new HashMap<String, FileRecordWriter>();
        }
    };

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のファイルをオープンする。
     * <p/>
     * このとき、フォーマット定義ファイルも{@link FilePathSetting}から"format"という論理名で取得したベースパス配下より読み込む。<br/>
     * また、バッファサイズには、デフォルトの値(8192B)が使用される。
     *
     * @param  dataFileName 書き込むデータファイルのファイル名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     */
    @Published
    public static void open(String dataFileName, String layoutFileName) {
        open(dataFileName, layoutFileName, DEFAULT_BUFFER_SIZE);
    }

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のファイルをオープンする。
     * <p/>
     * このとき、フォーマット定義ファイルも{@link FilePathSetting}から"format"という論理名で取得したベースパス配下より読み込む。<br/>
     * また、引数でデータファイルに書き込む際のバッファサイズを指定する。
     *
     * @param  dataFileName 書き込むデータファイルのファイル名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     * @param  bufferSize バッファサイズ
     */
    @Published
    public static void open(String dataFileName, String layoutFileName, int bufferSize) {
        open("output", dataFileName, layoutFileName, bufferSize);
    }


    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルをオープンする。
     * <p/>
     * このとき、フォーマット定義ファイルは{@link FilePathSetting}から"format"という論理名で取得したベースパス配下より読み込む。<br/>
     * また、データファイルに書き込む際のバッファサイズはデフォルト値(8192B)が使用される。
     *
     * @param  dataFileBasePathName 書き込むデータファイルのベースパスの論理名
     * @param  dataFileName     書き込むデータファイルのファイル名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     */
    @Published
    public static void open(String dataFileBasePathName,
                            String dataFileName, String layoutFileName) {
        open(dataFileBasePathName, dataFileName, layoutFileName, DEFAULT_BUFFER_SIZE);
    }


    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルをオープンする。
     * <p/>
     * このとき、フォーマット定義ファイルは{@link FilePathSetting}から"format"という論理名で取得したベースパス配下より読み込む。<br/>
     * また、引数でデータファイルに書き込む際のバッファサイズを指定する。
     *
     * @param  dataFileBasePathName 書き込むデータファイルのベースパスの論理名
     * @param  dataFileName     書き込むデータファイルのファイル名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     * @param  bufferSize バッファサイズ
     */
    @Published
    public static void open(String dataFileBasePathName,
                            String dataFileName, String layoutFileName, int bufferSize) {
        open(dataFileBasePathName, dataFileName, "format", layoutFileName, bufferSize);
    }

    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルをオープンする。
     * <p/>
     * このとき、フォーマット定義ファイルは{@link FilePathSetting}から"format"という論理名で取得したベースパス配下より読み込む。
     * また、データファイルに書き込む際のバッファサイズはデフォルト値(8192B)が使用される。
     *
     * @param  dataFileBasePathName 書き込むデータファイルのベースパスの論理名
     * @param  dataFileName     書き込むデータファイルのファイル名
     * @param  layoutFileBasePathName フォーマット定義ファイルのベースパス論理名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     */
    @Published
    public static void open(String dataFileBasePathName,
                            String dataFileName, String layoutFileBasePathName,
                            String layoutFileName) {
        open(dataFileBasePathName, dataFileName, layoutFileBasePathName, layoutFileName, DEFAULT_BUFFER_SIZE);
    }

    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルをオープンする。
     * <p>
     * また、引数でデータファイルに書き込む際のバッファサイズと、{@link FilePathSetting}に設定したフォーマット定義ファイルの論理名を指定する。
     * </p>
     * @param  dataFileBasePathName 書き込むデータファイルのベースパスの論理名
     * @param  dataFileName     書き込むデータファイルのファイル名
     * @param  layoutFileBasePathName フォーマット定義ファイルのベースパスの論理名
     * @param  layoutFileName フォーマット定義ファイルのファイル名
     * @param  bufferSize バッファサイズ
     * @throws IllegalArgumentException {@code bufferSize}以外の引数がnullまたは空の場合
     * @throws IllegalStateException カレントスレッド上の{@link FileRecordWriter}が既にオープンしている場合
     *
     */
    @Published
    public static void open(String dataFileBasePathName,
                            String dataFileName, String layoutFileBasePathName,
                            String layoutFileName, int bufferSize) {
        if (StringUtil.isNullOrEmpty(dataFileName)) {
            throw new IllegalArgumentException("data file name was blank. data file name must not be blank.");
        }
        if (StringUtil.isNullOrEmpty(layoutFileName)) {
            throw new IllegalArgumentException("layout file name was blank. layout file name must not be blank.");
        }
        if (StringUtil.isNullOrEmpty(dataFileBasePathName)) {
            throw new IllegalArgumentException("data file base path name was blank. data file base path name must not be blank.");
        }
        if (StringUtil.isNullOrEmpty(layoutFileBasePathName)) {
            throw new IllegalArgumentException("layout file base path name was blank. layout file base path name must not be blank.");
        }

        String key = getInstance().createKey(dataFileBasePathName, dataFileName);

        synchronized (WRITERS) { // WRITERSの単位で同期化を行う
            FileRecordWriter writer = WRITERS.get().get(key);
            if (writer != null) {
                throw new IllegalStateException(String.format(
                        "writer was already open. basePathName=[%s]. fileName=[%s].", dataFileBasePathName, dataFileName));
            }
            writer = getInstance().createFileRecordWriter(dataFileBasePathName, dataFileName, layoutFileBasePathName, layoutFileName, bufferSize);
            WRITERS.get().put(key, writer);
        }
    }

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のデータファイルにレコードを出力する。
     *
     * @param record ファイルに出力するレコード
     * @param fileName 書き込むデータファイルのファイル名
     */
    @Published
    public static void write(Map<String, ?> record, String fileName) {
        write(record, "output", fileName);
    }

    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のデータファイルにレコードを出力する。
     *
     * @param record ファイルに出力するレコード
     * @param basePathName 書き込むデータファイルのベースパスの論理名
     * @param fileName 書き込むデータファイルのファイル名
     */
    @Published
    public static void write(Map<String, ?> record, String basePathName, String fileName) {
        FileRecordWriter writer = get(basePathName, fileName);
        synchronized (writer) { // ライタ（データファイル）の単位で同期化を行う
            writer.write(record);
        }
    }

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のデータファイルにレコードを出力する。
     * <p>
     * また、引数で出力するレコードのレコードタイプを指定する。
     * </p>
     * @param recordType 出力するレコードのレコードタイプ
     * @param record ファイルに出力するレコード
     * @param fileName 書き込むデータファイルのファイル名
     */
    @Published
    public static void write(String recordType, Map<String, ?> record, String fileName) {
        write(recordType, record, "output", fileName);
    }

    /**
     * 引数で指定したデータファイルにレコードを出力する。
     *
     * @param recordType 出力するレコードのレコードタイプ
     * @param record ファイルに出力するレコード
     * @param basePathName 書き込むデータファイルのベースパスの論理名
     * @param fileName 書き込むデータファイルのファイル名
     */
    @Published
    public static void write(String recordType, Map<String, ?> record, String basePathName, String fileName) {
        FileRecordWriter writer = get(basePathName, fileName);
        synchronized (writer) { // ライタ（データファイル）の単位で同期化を行う
            writer.write(recordType, record);
        }
    }

    /**
     * {@link FileRecordWriter}のインスタンスを生成する。
     *
     * @param  dataFileBasePathName ベースパスの論理名
     * @param  dataFileName     書き込むデータファイルのファイル名
     * @param  layoutFileName レイアウトファイル名
     * @param  layoutFileBasePathName  レイアウトファイルの配置ディレクトリの論理名
     * @param bufferSize ファイル読み込み時のバッファサイズ
     * @return FileRecordWriterのインスタンス
     */
    protected FileRecordWriter createFileRecordWriter(String dataFileBasePathName,
                                                      String dataFileName, String layoutFileBasePathName, String layoutFileName, int bufferSize) {

        // データファイルオブジェクトの生成
        FilePathSetting filePathSetting = FilePathSetting.getInstance();
        File dataFile = filePathSetting.getFile(dataFileBasePathName, dataFileName);
        // レイアウトファイルオブジェクトの生成
        File layoutFile = filePathSetting.getFileWithoutCreate(layoutFileBasePathName, layoutFileName);
        return new FileRecordWriter(dataFile, layoutFile, bufferSize);
    }

    /**
     * スレッドに保持するキーを生成する。
     *
     * @param  basePathName ベースパスの論理名
     * @param  fileName     書き込むデータファイルのファイル名
     * @return キー
     * @throws IllegalArgumentException {@code basePathName}に"&"が含まれていなかった場合
     */
    protected String createKey(String basePathName, String fileName) {
        if (basePathName.contains(KEY_SEPARATOR)) {
            throw new IllegalArgumentException(String.format(
                    "base path name was invalid. base path name must not contain [%s].", KEY_SEPARATOR));
        }
        return new StringBuilder(basePathName).append(KEY_SEPARATOR).append(fileName).toString();
    }

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のファイルに書き出しを行う{@link FileRecordWriter}を取得する。
     *
     * @param  fileName 書き込むデータファイルのファイル名
     * @return {@link FileRecordWriter}
     */
    public static FileRecordWriter get(String fileName) {
        return get("output", fileName);
    }

    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルに書き出しを行う{@link FileRecordWriter}を取得する。
     *
     * @param  basePathName 書き込むデータファイルのベースパスの論理名
     * @param  fileName     書き込むデータファイルのファイル名
     * @return {@link FileRecordWriter}
     * @throws IllegalArgumentException カレントスレッド上の{@link FileRecordWriter}が閉じている場合
     */
    public static FileRecordWriter get(String basePathName,
                                       String fileName) {
        String key = getInstance().createKey(basePathName, fileName);
        synchronized (WRITERS) { // WRITERSの単位で同期化を行う
            if (!WRITERS.get().containsKey(key)) {
                throw new IllegalStateException(
                        String.format(
                                "writer was not open or already closed. necessary to call open method before call this method."
                                        + " basePathName=[%s], fileName=[%s].",
                                basePathName, fileName));
            }
            return WRITERS.get().get(key);
        }
    }

    /**
     * {@link FilePathSetting}から"output"という論理名で取得したベースパス配下のファイルに書き出しを行う{@link FileRecordWriter}をクローズし、
     * インスタンスをカレントスレッド上から削除する。
     *
     * @param  fileName 書き込むデータファイルのファイル名
     */
    public static void close(String fileName) {
        close("output", fileName);
    }

    /**
     * {@link FilePathSetting}に設定した論理名(論理ベースパス）配下のファイルに書き出しを行う{@link FileRecordWriter}をクローズし、
     * インスタンスをカレントスレッド上から削除する。
     *
     * @param  basePathName 書き込むデータファイルのベースパスの論理名
     * @param  fileName     書き込むデータファイルのファイル名
     */
    public static void close(String basePathName, String fileName) {
        String key = getInstance().createKey(basePathName, fileName);
        FileRecordWriter writer;
        synchronized (WRITERS) { // WRITERSの単位で同期化を行う
            writer = WRITERS.get().get(key);
            if (writer == null) {
                return;
            }
            WRITERS.get().remove(key);
        }
        synchronized (writer) { // ライタ（データファイル）の単位で同期化を行う
            writer.close();
        }
    }

    /**
     * 本クラスがカレントスレッド上で管理している全ての{@link FileRecordWriter}のファイルストリームを
     * クローズし、また、それら全ての{@link FileRecordWriter}をカレントスレッド上から削除する。
     */
    public static void closeAll() {
        synchronized (WRITERS) {
            try {
                for (FileRecordWriter writer : WRITERS.get().values()) {
                    writer.close();
                }
            } finally {
                WRITERS.get().clear();
            }
        }
    }

}
