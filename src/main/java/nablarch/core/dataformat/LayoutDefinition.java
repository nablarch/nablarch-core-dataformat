package nablarch.core.dataformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;


/**
 * フォーマット定義ファイル全体の定義情報を保持するクラス。
 * フォーマット定義ファイルのパース結果が本クラスとなる。
 * <p>
 * フォーマット定義ファイルの情報は、LayoutFileParserによって読み込まれ、
 * {@link DataRecordFormatterSupport}のサブクラスにより、
 * ファイルタイプ（固定長・可変長）に応じた初期化処理が行われる。
 * </p>
 * @see LayoutFileParser
 * @see DataRecordFormatterSupport
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class LayoutDefinition {

    /** レコードタイプごとのフォーマット定義を格納するList */ 
    private List<RecordDefinition> records = new ArrayList<RecordDefinition>();
    
    /** レコードタイプを識別するフィールドを読み込むためのフォーマット定義 */
    private RecordDefinition recordClassifier = null;

    /** ディレクティブの定義。 */
    private Map<String, Object> directive = new HashMap<String, Object>();

    /**
     * 本インスタンスの入力元となったレイアウト定義ファイルのパス。
     * （障害発生時の原因究明に使用する）
     */
    private final String source;

    /**
     * 初期化が行われたかどうかのフラグ。
     * 初期化は、{@link DataRecordFormatterSupport}クラスが行う。
     */
    private boolean initialized = false;

    /**
     * レコードタイプを識別するフィールドを読み込むためのフォーマット定義を取得する。
     * @return レコードタイプを識別するフィールドを読み込むためのフォーマット定義
     */
    public RecordDefinition getRecordClassifier() {
        return recordClassifier;
    }

    /** デフォルトコンストラクタ。*/
    public LayoutDefinition() {
        source = "";
    }

    /**
     * コンストラクタ。
     * @param source 本インスタンスの入力元となるレイアウト定義ファイルのパス
     */
    public LayoutDefinition(String source) {
        this.source = source;
    }

    /**
     * 入力元となったレイアウト定義ファイルのパスを返却する。
     * @return レイアウト定義ファイルのパス
     */
    String getSource() {
        return source;
    }

    /**
     * レコードタイプ識別用フィールド定義を設定する。
     * @param classifier レコード種別識別フィールド定義
     * @return このオブジェクト自体
     */
    public LayoutDefinition setRecordClassifier(RecordDefinition classifier) {
        recordClassifier = classifier;
        return this;
    }

    /**
     * レコードタイプの定義を追加する。
     * @param records レコードタイプ定義
     * @return このオブジェクト自体
     */
    public LayoutDefinition addRecord(RecordDefinition... records) {
        this.records.addAll(Arrays.asList(records));
        return this;
    }
    
    /**
     * レコードタイプ名に紐付くレコードタイプの定義を返却する。
     * @param typeName レコードタイプ名
     * @return レコードタイプの定義
     */
    public RecordDefinition getRecordType(String typeName) {
        for (RecordDefinition record : records) {
            if (record.getTypeName().equals(typeName)) {
                return record;
            }
        }
        return null;
    }
    
    /**
     * レコードタイプの定義のリストを返却する。
     * @return レコードタイプの定義のリスト
     */
    public List<RecordDefinition> getRecords() {
        return records;
    }
    
    /**
     * 初期化が行われたかどうかのフラグを設定する。
     * @param initialized 初期化が行われたかどうかのフラグ
     * @return このオブジェクト自体
     */
    public LayoutDefinition setInitialized(boolean initialized) {
        this.initialized = initialized;
        return this;
    }
    
    /**
     * 初期化が行われたかどうかのフラグを取得する
     * @return 初期化が行われたかどうかのフラグ
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * ディレクティブの定義を取得する。
     * @return ディレクティブの定義
     */
    public Map<String, Object> getDirective() {
        return directive;
    }
    
}
