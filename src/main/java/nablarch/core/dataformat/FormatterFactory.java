package nablarch.core.dataformat;

import static nablarch.core.util.Builder.concat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecordFormatterSupport.Directive;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;

/**
 * データレコードフォーマッタ（{@link DataRecordFormatter}）を生成するファクトリクラス。
 * <p>
 * フォーマット定義ファイルのパースを行い、ディレクティブで指定されたファイルタイプに対応するデータレコードフォーマッタを生成して返却する。<br/>
 * 具体的には、ファイルタイプが "Variable" の場合に{@link VariableLengthDataRecordFormatter}を、
 * ファイルタイプが "Fixed" の場合に{@link FixedLengthDataRecordFormatter}を生成する。
 * </p>
 * <p>
 * フォーマット定義情報保持クラスは、本クラスの内部でキャッシュし、同一のフォーマット定義ファイルが何度もパースされないように制御する。
 * デフォルトではフォーマット定義ファイルのパース結果はキャッシュされる。
 * </p>
 * <p>
 * また、リポジトリに「formatterFactory」のキーで本クラスのインスタンスを格納することで、設定を変更することができる。<br/><br/>
 * 以下に、設定可能な項目の一覧を示す。
 * <table border="1">
 * <tr bgcolor="#cccccc">
 * <th>プロパティ名</th>
 * <th>型</th>
 * <th>概要</th>
 * </tr>
 * <tr>
 * <td>allowedRecordSeparatorList</td>
 * <td>{@code List<String>}</td>
 * <td>レコード終端文字列として許容する文字列のリスト</td>
 * </tr>
 * <tr>
 * <td>defaultReplacementType</td>
 * <td>{@code Map<String, String>}</td>
 * <td>フィールドタイプ名に対応するデフォルトの寄せ字変換タイプ名のMap</td>
 * </tr>
 * <tr>
 * <td>encoding</td>
 * <td>String</td>
 * <td>フォーマット定義ファイルのエンコーディング</td>
 * </tr>
 * <tr>
 * <td>cacheLayoutFileDefinition</td>
 * <td>boolean</td>
 * <td>フォーマット定義ファイルのパース結果のキャッシュ要否</td>
 * </tr>
 * </table>
 * <br/>
 * また、以下にフォーマット定義ファイルの定義例を示す。
 * <pre>
 * {@code
 * <component name="formatterFactory"
 *     class="nablarch.core.dataformat.FormatterFactory">
 *   <property name="cacheLayoutFileDefinition" value="false" />
 *   <property name="defaultReplacementType">
 *     <map>
 *       <entry key="X" value="type_hankaku" />
 *       <entry key="N" value="type_zenkaku" />
 *     </map>
 *   </property>
 * </component>
 * }
 * </pre>
 * </p>
 * @see DataRecordFormatter
 * @see LayoutDefinition
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class FormatterFactory {

    // ------------------------------------------------------------ structure
    /** フォーマット定義ファイルのパース結果クラスのキャッシュ */
    private Map<String, LayoutDefinition> layoutDefinitionCache = new HashMap<String, LayoutDefinition>();

    /** フォーマット定義ファイルのパース結果のキャッシュ要否 */
    private boolean cacheLayoutFileDefinition = true;

    /** ファクトリクラスのコンポーネント設定ファイル上の名前 */
    private static final String REPOSITORY_KEY = "formatterFactory";

    /** 可変長ファイルタイプの名前 */
    private static final String FILE_TYPE_VARIABLE = "Variable";

    /** 可変長ファイルタイプの名前 */
    private static final Object FILE_TYPE_FIXED = "Fixed";

    /** JSONファイルタイプの名前 */
    private static final Object FILE_TYPE_JSON = "JSON";
    
    /** XMLファイルタイプの名前 */
    private static final Object FILE_TYPE_XML = "XML";
    
    /** 許容するレコード終端文字列のリスト*/
    private List<String> allowedRecordSeparatorList = null;
    
    /** データタイプ名に対応するデフォルトの寄せ字変換タイプ名のMap */
    private Map<String, String> defaultReplacementType = new HashMap<String, String>();

    /** フォーマット定義ファイルのファイルエンコーディング */
    private String encoding;

    /**
     * デフォルトのファクトリクラスのインスタンス。
     * リポジトリからインスタンスを取得できなかった場合に、デフォルトでこのインスタンスが使用される。
     */
    private static FormatterFactory defaultSetting = new FormatterFactory();
    
    // --------------------------------------------------- managing singleton
    /**
     * FormatterFactoryクラスのインスタンスをリポジトリより取得する。
     * リポジトリより取得できなかった場合は、デフォルトで本クラスのインスタンスを返却する。
     * @return このクラスのインスタンス
     */
    public static FormatterFactory getInstance() {
        FormatterFactory setting = SystemRepository.get(REPOSITORY_KEY);
        if (setting == null) {
            return defaultSetting;
        }
        return setting;
    }
    
    /**
     * フォーマット定義ファイルのパース結果のキャッシュ要否を設定する。
     * @param cacheLayoutFileDefinition フォーマット定義ファイルのパース結果のキャッシュ要否
     * @return このオブジェクト自体
     */
    public synchronized FormatterFactory setCacheLayoutFileDefinition(boolean cacheLayoutFileDefinition) {
        this.cacheLayoutFileDefinition = cacheLayoutFileDefinition;
        return this;
    }

    // ------------------------------------------------------------- helpers
    /** 
     * データレコードフォーマッタのインスタンスを生成する。
     * フォーマット定義ファイルのパース結果をキャッシュする設定の場合は、フォーマット定義ファイルのパースは２度行わない。
     * @param layoutFile フォーマット定義ファイル
     * @return データレコードフォーマッタのインスタンス
     */
    public synchronized DataRecordFormatter createFormatter(File layoutFile) {
        LayoutDefinition definition = null;
        if (cacheLayoutFileDefinition) {
            definition = getDefinitionFromCache(layoutFile);
        } else {
            definition = createDefinition(layoutFile);
        }
        return createFormatter(definition);
    }

    /**
     * フォーマット定義情報保持クラスをもとに、データレコードフォーマッタのインスタンスを生成する。
     * @param definition フォーマット定義情報保持クラス
     * @return データレコードフォーマッタのインスタンス
     */
    public synchronized DataRecordFormatter createFormatter(LayoutDefinition definition) {
        String fileType = Directive.getFileType(definition.getDirective());
        String formatFilePath = definition.getSource();
        DataRecordFormatter formatter
                = createFormatter(fileType, formatFilePath).setDefinition(definition);
        setFormatterProperty(formatter);
        return formatter;
    }

    /**
     * キャッシュからフォーマット定義情報保持クラスを取得する。
     * フォーマット定義情報保持クラスをキャッシュから取得できない場合は、生成する。
     * @param layoutFile フォーマット定義ファイル
     * @return フォーマット定義情報保持クラス
     */
    protected LayoutDefinition getDefinitionFromCache(File layoutFile) {
        if (!layoutDefinitionCache.containsKey(layoutFile.getAbsolutePath())) {
            LayoutDefinition definition = createDefinition(layoutFile);
            layoutDefinitionCache.put(layoutFile.getAbsolutePath(), definition);
            return definition;
        }
        return layoutDefinitionCache.get(layoutFile.getAbsolutePath());
    }
    
    
    /**
     * データレコードフォーマッタを生成する。
     * 
     * ファイルタイプにより下記のとおりフォーマッタの生成を行い、
     * これら以外のファイルタイプの場合は例外をスローする。
     * <table border="1">
     * <tr bgcolor="#cccccc">
     * <th>ファイルタイプ</th>
     * <th>フォーマッタクラス</th>
     * </tr>
     * <tr>
     * <td>Variable</td>
     * <td>VariableLengthDataRecordFormatter</td>
     * </tr>
     * <tr>
     * <td>Fixed</td>
     * <td>FixedLengthDataRecordFormatter</td>
     * </tr>
     * <tr>
     * <td>JSON</td>
     * <td>JsonDataRecordFormatter</td>
     * </tr>
     * <tr>
     * <td>XML</td>
     * <td>XmlDataRecordFormatter</td>
     * </tr>
     * </table>
     * 
     * @param fileType ファイルタイプ
     * @param formatFilePath フォーマット定義ファイルのパス（例外発生時に使用する）
     * @return フォーマッタ
     */
    protected DataRecordFormatter createFormatter(String fileType, String formatFilePath) {
        DataRecordFormatter formatter = null;
        if (FILE_TYPE_VARIABLE.equals(fileType)) {
            formatter = new VariableLengthDataRecordFormatter();
        } else if (FILE_TYPE_FIXED.equals(fileType)) {
            formatter = new FixedLengthDataRecordFormatter();
        } else if (FILE_TYPE_JSON.equals(fileType)) {
            formatter = new JsonDataRecordFormatter();
        } else if (FILE_TYPE_XML.equals(fileType)) {
            formatter = new XmlDataRecordFormatter();
        } else {
            throw new SyntaxErrorException(concat(
                    "invalid file type was specified. value=[", fileType, "].")
            ).setFilePath(formatFilePath);
        }
        return formatter;
    }
    
    /**
     * データレコードフォーマッタにプロパティを設定する。
     * <p>
     * 具体的には、データレコードフォーマッタの型がDataRecordFormatterSupportの場合に、
     * 本クラスに設定された以下のプロパティを、データレコードフォーマッタのプロパティに設定する。
     * <ul>
     * <li>データタイプ名に対応するデフォルトの寄せ字変換タイプ名のMap</li>
     * <li>許容するレコード終端文字列のリスト</li>
     * </ul>
     * </p>
     * @param formatter データレコードフォーマッタ
     */
    protected void setFormatterProperty(DataRecordFormatter formatter) {
        if (formatter instanceof DataRecordFormatterSupport) {
            ((DataRecordFormatterSupport) formatter)
                    .setDefaultReplacementType(defaultReplacementType);
            if (allowedRecordSeparatorList != null) {
                ((DataRecordFormatterSupport) formatter)
                .setAllowedRecordSeparatorList(allowedRecordSeparatorList);
            }
        }
    }

    /**
     * フォーマット定義ファイルを読み込み、フォーマット定義情報保持クラスを生成する。
     * @param layoutFile フォーマット定義ファイル
     * @return フォーマット定義情報保持クラス
     */
    protected LayoutDefinition createDefinition(File layoutFile) {
        return createLayoutFileParser(layoutFile.getAbsolutePath()).parse();
    }
    
    /**
     * フォーマット定義ファイルのパーサを生成する。
     * @param layoutFilePath フォーマット定義ファイルのパス
     * @return フォーマット定義ファイルのパーサ
     */
    protected LayoutFileParser createLayoutFileParser(
            String layoutFilePath) {
        return new LayoutFileParser(layoutFilePath, encoding);
    }
    
    /**
     * 許容するレコード終端文字列のリストを設定する。
     * @param allowedRecordSeparatorList 許容されるレコード終端文字列のリスト
     * @return このオブジェクト自体
     */
    public FormatterFactory setAllowedRecordSeparatorList(
            List<String> allowedRecordSeparatorList) {
        this.allowedRecordSeparatorList = allowedRecordSeparatorList;
        return this;
    }
    
    /**
     * データタイプ名に対応するデフォルトの寄せ字変換タイプ名のMapを設定する。
     * @param defaultReplacementType データタイプ名に対応するデフォルトの寄せ字変換タイプ名のMap
     * @return このオブジェクト自体
     */
    public FormatterFactory setDefaultReplacementType(
            Map<String, String> defaultReplacementType) {
        this.defaultReplacementType = defaultReplacementType;
        return this;
    }

    /**
     * フォーマット定義ファイルのファイルエンコーディングを設定する。
     * @param encoding フォーマット定義ファイルのファイルエンコーディング
     * @return このオブジェクト自体
     */
    public FormatterFactory setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
    
}
