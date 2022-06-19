package nablarch.core.dataformat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.convertor.VariableLengthConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.core.util.annotation.Published;

/**
 * フォーマット定義ファイルの内容に従い、可変長ファイルデータの読み書きを行うクラス。
 * <p>
 * 本クラスはスレッドセーフを考慮した実装にはなっていないので、呼び出し元で同期化の制御を行うこと。
 * </p>
 * <b>ディレクティブの設定</b>
 * <p>
 * 可変長ファイルデータを読み込む際は、以下のディレクティブの設定が必須となる。
 * <ul>
 * <li>ファイルの文字エンコーディング</li>
 * <li>フィールド区切り文字（カンマ、タブなど）</li>
 * <li>レコード終端文字列（\n、\r、\r\nなど）</li>
 * </ul>
 * </p>
 * <p>
 * また、任意で以下の設定を行うことができる。
 * <ul>
 * <li>囲み文字（デフォルトはダブルクォート）</li>
 * <li>最初の行をタイトルとして読み書きするかどうか</li>
 * <li>読み込みを許容する1行の文字列数（デフォルトは100万文字まで読み込み可能）</li>
 * </ul>
 * </p>
 * <p>
 * フィールド区切り文字および囲み文字は、任意の１文字を指定できる。<br/>
 * また、レコード終端文字列は、許容する文字列のみ指定できる。
 * デフォルトでは「\n」「\r」「\r\n」の3種類の文字列をレコード終端文字列として許容するが、
 * それを変更したい場合は、任意の許容する文字列のリストを{@link FormatterFactory}クラスの allowedRecordSeparatorList プロパティに設定すること。
 * </p>
 * </p>
 * <p>
 * <b>タイトル行の読み書き</b>
 * </p>
 * <p>
 * requires-titleディレクティブにtrueを設定することで、タイトル行を読み書きする機能が有効となり、最初の行をタイトル行として通常のレコードタイプとは別に取り扱うことができるようになる。
 * この場合、最初の行はタイトル固定のレコードタイプ名[Title]で読み書きできるようになる。<br/>
 * （※ここでいうタイトルとは、ファイルの最初の行に存在し、個々のフィールドの論理名などが記載されるレコードのことを示す）
 * </p>
 * <p>
 * 通常、タイトル行・データ行のような2つのレコードタイプが存在するファイルを読み込むためにはマルチフォーマットの定義を行う必要があるが、
 * タイトル行とデータ行に対してフォーマットの適用条件となるフィールドが存在しない場合、マルチフォーマットの定義では読み込むことができない。<br/>
 * しかし、本機能を使用すれば、最初の行はタイトル固有のレコードタイプが、最初の行以降はそれ以外のレコードタイプが適用されるので、
 * タイトル行とデータ行を識別するためのフィールドが存在しなくても、以下のとおりシングルフォーマットの定義で読み込むことができる。
 * <pre>
 * requires-title: true  # requires-titleがtrueの場合、最初の行をタイトルとして読み書きできる。
 * 
 * [Title]               # タイトル固有のレコードタイプ。最初の行はこのレコードタイプで読み書きされる。
 * 1   Kubun      N  
 * 2   Name       N  
 * 3   Publisher  N  
 * 4   Authors    N  
 * 5   Price      N  
        
 * [DataRecord]          # 最初の行以降の行はこのレコードタイプで読み書きされる。
 * 1   Kubun      X  
 * 2   Name       N  
 * 3   Publisher  N  
 * 4   Authors    N  
 * 5   Price      N  
 * </pre>
 * </p>
 * <p>
 * また、本機能を使用する場合、最初の行がタイトル行であることが保証されるので、ファイルレイアウトのバリデーションを省略できるといったメリットもある。
 * </p>
 * <p>
 * 以上の点より、最初の行にタイトルが存在するファイルを読み込む場合は、本機能を使用することを推奨する。
 * </p>
 * <p>
 * 最初の行をタイトルとして読み書きする場合、以下の制約が発生する。この制約を満たさない場合、例外がスローされるので注意すること。
 * <ul>
 * <li>レコードタイプ [Title] を必ずフォーマット定義しなければならない。</li>
 * <li>最初の行を書き込む際に指定するレコードタイプは [Title] でなければならない。</li>
 * <li>最初の行以降を書き込む際に指定するレコードタイプは  [Title] 以外でなければならない。</li>
 * <li>レコードタイプ  [Title] にフォーマットの適用条件が定義されている場合、読み書きする最初の行は必ずその適用条件を満たさなければならない。また、最初の行以降の行はその適用条件を満たしてはいけない。</li>
 * </ul>
 * </p>
 * <p>
 * タイトル固有のレコードタイプ名はデフォルトでは [Title] だが、title-record-type-name ディレクティブでタイトル固有のレコードタイプ名を個別に指定することも可能である。
 * </p>
 * @author Iwauo Tajima
 */
public class VariableLengthDataRecordFormatter extends DataRecordFormatterSupport {

    /** ロガー **/
    private static final Logger LOGGER = LoggerManager.get(VariableLengthDataRecordFormatter.class);
    
    /** コンバータの設定情報保持クラス */
    private VariableLengthConvertorSetting convertorSetting;

    /** 入力ストリーム。 */
    private InputStream source;
    
    /** ファイル読み込みに使用するリーダ */
    private BufferedReader reader;
    
    /** 出力ストリーム。 */
    private OutputStream dest; 
    
    /** ライタ */
    private Writer writer;

    /** フィールド値のクォート処理で使用する文字 */
    private Character quotingDelimiter;

    /** 空行の存在を認める */
    private boolean ignoreBlankLines;

    /** フィールド終端文字列 */
    private char fieldSeparator;
    
    /** 最初の行をタイトルとして読み書きするかどうか */
    private boolean requiresTitle;

    /** タイトルのレコードタイプ名（デフォルト値は"Title"） */
    private String titleRecordTypeName = "Title";

    /** 読み込みを許容する1行の文字列数（デフォルトは100万文字まで読み込み可能） */
    private Integer maxRecordLength = 1000000;

    /**
     * デフォルトコンストラクタ。
     * デフォルトでは、VariableLengthConvertorSettingをコンバータとして使用する。
     */
    @Published(tag = "architect")
    public VariableLengthDataRecordFormatter() {
        convertorSetting = VariableLengthConvertorSetting.getInstance();
    }

    /** {@inheritDoc} 
     * <p/>
     * また、入力ストリームをBufferedReaderにラップする処理および、
     * 出力ストリームをBufferedWriterにラップする処理を行う。
     */
    public DataRecordFormatter initialize() {        
        super.initialize();
        if (source != null && reader == null) {  // reader生成済みの場合は初期化しない
            initializeReader();
        } 
        if (dest != null) {
            initializeWriter();
        }
        return this;
    }

    /**
     * 可変長ファイルフォーマッタが共通的に使用するディレクティブの名前と値の型。（タイプセーフEnum）
     * 以下に一覧を示す。<br>
     * <ul>
     * <li>field-separator：String</li>
     * <li>quoting-delimiter：String</li>
     * <li>ignore-blank-lines：Boolean</li>
     * <li>requires-title：Boolean</li>
     * <li>max-record-length：Integer</li>
     * <li>title-record-type-name：String</li>
     * </ul>
     * @author Masato Inoue
     */
    public static class VariableLengthDirective extends Directive {
        /** フィールド区切り文字 */
        public static final Directive FIELD_SEPARATOR = new Directive("field-separator", String.class);
        /** 囲み文字 */
        public static final Directive QUOTING_DELIMITER = new Directive("quoting-delimiter", String.class);
        /** 空行の存在を認める */
        public static final Directive IGNORE_BLANK_LINES = new Directive("ignore-blank-lines", Boolean.class);
        /** 最初の行をタイトルとして読み書きするかどうか */
        public static final Directive REQUIRES_TITLE = new Directive("requires-title", Boolean.class);
        /** 読み込みを許容する1行の文字列数 */
        public static final Directive MAX_RECORD_LENGTH = new Directive("max-record-length", Integer.class);
        /** タイトルのレコードタイプ名 */
        public static final Directive TITLE_RECORD_TYPE_NAME = new Directive("title-record-type-name", String.class);

        /** 列挙型の全要素(親クラスの要素を含む） */
        public static final Map<String, Directive> VALUES = Directive.createDirectiveMap(
                FIELD_SEPARATOR,
                QUOTING_DELIMITER,
                IGNORE_BLANK_LINES,
                REQUIRES_TITLE,
                MAX_RECORD_LENGTH,
                TITLE_RECORD_TYPE_NAME);

        /**
         * コンストラクタ。
         * @param name ディレクティブ名
         * @param type ディレクティブの値の型
         */
        public VariableLengthDirective(String name, Class<?> type) {
            super(name, type);
        }

        /**
         * フィールド区切り文字を取得する。
         * @param directive ディレクティブ
         * @return フィールド区切り文字
         */
        public static String getFieldSeparator(Map<String, Object> directive) {
            return (String) directive.get(FIELD_SEPARATOR.getName());
        }

        /**
         * 囲み文字を取得する。
         * @param directive ディレクティブ
         * @return 囲み文字
         */
        public static String getQuotingDelimiter(Map<String, Object> directive) {
            return (String) directive.get(QUOTING_DELIMITER.getName());
        }

        /**
         * 空行をスキップするかどうかの設定を取得する。
         * @param directive ディレクティブ
         * @return 空行をスキップするかどうか
         */
        public static Boolean getIgnoreBlankLines(Map<String, Object> directive) {
            return (Boolean) directive.get(VariableLengthDirective.IGNORE_BLANK_LINES.getName());
        }

        /**
         * 最初の行をタイトルとして読み書きするかどうかの設定を取得する。
         * @param directive ディレクティブ
         * @return 最初の行をタイトルとして読み書きするかどうか
         */
        public static Boolean getRequiresTitle(Map<String, Object> directive) {
            return (Boolean) directive.get(VariableLengthDirective.REQUIRES_TITLE.getName());
        }

        /**
         * 読み込みを許容する1行の文字列数を取得する。
         * @param directive ディレクティブ
         * @return 読み込みを許容する1行の文字列数
         */
        public static Integer getMaxRecordLength(Map<String, Object> directive) {
            return (Integer) directive.get(VariableLengthDirective.MAX_RECORD_LENGTH.getName());
        }

        /**
         * タイトルのレコードタイプ名を取得する。
         * @param directive ディレクティブ
         * @return タイトルのレコードタイプ名
         */
        public static String getTitleRecordTypeName(Map<String, Object> directive) {
            return (String) directive.get(VariableLengthDirective.TITLE_RECORD_TYPE_NAME.getName());
        }
        
        /**
         * ディレクティブを取得する。
         * @param name ディレクティブ名
         * @return ディレクティブ
         */
        public static Directive valueOf(String name) {
            return VALUES.get(name);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected Map<String, Directive> createDirectiveMap() {
        return VariableLengthDirective.VALUES;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void validateDirectives(Map<String, Object> directive) {
        super.validateDirectives(directive);

        String quotingDelimiterStr = VariableLengthDirective.getQuotingDelimiter(directive);
        if (quotingDelimiterStr != null) {
            // 囲み文字の長さが1であることのチェック
            if (quotingDelimiterStr.length() != 1) {
                throw new SyntaxErrorException(
                        String.format(
                                "invalid quoting delimiter was specified by '%s' directive. value=[%s]. Quoting delimiter length must be [1].",
                                VariableLengthDirective.QUOTING_DELIMITER.getName(), quotingDelimiterStr));
            }
        }

        // フィールドセパレータの定義は必須
        String fieldSeparatorStr = VariableLengthDirective.getFieldSeparator(directive);
        if (fieldSeparatorStr == null) {
            throw new SyntaxErrorException(String.format(
                    "directive '%s' was not specified. directive '%s' must be specified."
                        , VariableLengthDirective.FIELD_SEPARATOR.getName(), VariableLengthDirective.FIELD_SEPARATOR.getName()));
        }
        
        // フィールドセパレータの長さが1であることのチェック
        if (fieldSeparatorStr.length() != 1) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid field separator was specified by '%s' directive. value=[%s]. field separator length must be [1].",
                            VariableLengthDirective.FIELD_SEPARATOR.getName(), fieldSeparatorStr));
        }

        // レコードセパレータの定義は必須
        if (VariableLengthDirective.getRecordSeparator(directive) == null) {
            throw new SyntaxErrorException(String.format(
                "directive '%s' was not specified. directive '%s' must be specified."
                    , Directive.RECORD_SEPARATOR.getName(), Directive.RECORD_SEPARATOR.getName()));
        }
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * {@link VariableLengthDataRecordFormatter}では、ディレクティブまたはコンポーネント設定ファイルに設定された以下の値をフィールドに設定する。
     * <ul>
     * <li>囲み文字</li>
     * <li>フィールド区切り文字</li>
     * <li>空行をスキップするかどうか</li>
     * <li>最初の行をタイトルとして読み書きするかどうか</li>
     * <li>タイトルのレコードタイプ名</li>
     * <li>読み込みを許容する1行の文字列数</li>
     * <li>タイトルのレコードタイプ名</li>
     * </ul>
     * 
     * @param directive ディレクティブ
     */
    protected void initializeField(Map<String, Object> directive) {
        super.initializeField(directive);
        // 囲み文字
        String quotingDelimiterStr = VariableLengthDirective.getQuotingDelimiter(directive);
        if (quotingDelimiterStr != null) {
            quotingDelimiter = quotingDelimiterStr.charAt(0);
        }
        // フィールド区切り文字
        fieldSeparator = VariableLengthDirective.getFieldSeparator(directive).charAt(0);
        // 空行をスキップするかどうか
        Boolean ignoreBlankLinesWrapper = VariableLengthDirective.getIgnoreBlankLines(directive);
        if (ignoreBlankLinesWrapper == null) {
            ignoreBlankLines = false;
        } else {
            ignoreBlankLines = ignoreBlankLinesWrapper;
        }
        // 最初の行をタイトルとして読み書きするかどうか
        Boolean requiresTitleWrapper = VariableLengthDirective.getRequiresTitle(directive);
        if (requiresTitleWrapper == null) {
            requiresTitle = false;
        } else {
            requiresTitle = requiresTitleWrapper;
        }
        // タイトルのレコードタイプ名
        String titleRecordTypeName = VariableLengthDirective.getTitleRecordTypeName(directive);
        if (titleRecordTypeName != null) {
            this.titleRecordTypeName = titleRecordTypeName;
        }
        // 読み込みを許容する1行の文字列数
        Integer maxRecordLength = VariableLengthDirective.getMaxRecordLength(directive);
        if (maxRecordLength != null) {
            this.maxRecordLength = maxRecordLength;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * {@link VariableLengthDataRecordFormatter}では、上記処理に加え、フォーマット定義が以下の条件を満たしているかどうかの検証を行う。条件を満たしていない場合、{@link SyntaxErrorException}をスローする。
     * <ul>
     * <li>requires-titleディレクティブの値がtrueかつ、titleRecordTypeNameに設定されたレコードタイプ名と一致するレコードタイプがフォーマット定義されている。</li>
     * </ul>
     */
    @Override
    protected void initializeFieldDefinition() {
        super.initializeFieldDefinition();
        if (requiresTitle) {
            RecordDefinition titleRecordDef = getDefinition().getRecordType(titleRecordTypeName);
            if (titleRecordDef == null) {
                throw new SyntaxErrorException(Builder.concat(
                        "record type '", titleRecordTypeName, "' was not found. "
                      , "When directive '", VariableLengthDirective.REQUIRES_TITLE.getName(), "' is true,"
                      , " must be specified record type '", titleRecordTypeName, "'."));
            }
        }
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setInputStream(InputStream stream) {
        source = stream;
        return this;
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setOutputStream(OutputStream stream) {
        dest = stream;
        return this;
    }

    /** 最初の行のタイトルを読み込んだかどうか */
    private boolean readTitle;
    
    /**{@inheritDoc} */
    @Published(tag = "architect")
    public DataRecord readRecord() throws IOException {
        
        if (reader == null) {
            throw new IllegalStateException("input stream was not set. input stream must be set before reading.");
        }

        // 次に読み込むレコードが存在しない場合、nullを返却する
        if (!hasNextIgnoreBlankLines()) {
            return null;
        }

        incrementRecordNumber(); // レコード番号をインクリメントする

        //  1レコード分のデータを分割し、文字列のリストとして取得する
        List<String> fieldStrList = readRecordAsString();
        
        // requiresTitleがtrueの場合、最初の行はtitleRecordTypeNameに設定されたレコードタイプ名と一致するレコードタイプで読み込む
        if (requiresTitle && !readTitle) {
            readTitle = true;
            RecordDefinition titleRecordDef = getDefinition().getRecordType(titleRecordTypeName);
            validateFieldLength(fieldStrList, titleRecordDef);
            DataRecord convertToRecord = convertToRecord(fieldStrList, titleRecordDef);
            // タイトル固有のレコードタイプに条件が存在する場合、その条件に合致するレコードであることを検証する
            validateMeetConditions(convertToRecord, titleRecordDef);
            return convertToRecord;
        } 
        
        // シングルフォーマットの場合
        if (getDefinition().getRecordClassifier() == null) {
            for (RecordDefinition recordDef : getDefinition().getRecords()) {
                if (titleRecordTypeName.equals(recordDef.getTypeName())) {
                    // titleRecordTypeNameに設定されたレコードタイプ名は最初の行のみ有効なので、処理をスキップする
                    continue;
                }
                validateFieldLength(fieldStrList, recordDef);
                return convertToRecord(fieldStrList, recordDef);
            }
            // タイトルしか存在しないフォーマットの場合に、最初の行以降の行を読み込もうとした場合、例外をスローする
            throw newInvalidDataFormatException(
                    "an applicable record type was not found. This format must not have non-title records.");
        }
        
        // マルチフォーマットの場合
        Map<String, Object> record = convertToRecord(fieldStrList, getDefinition().getRecordClassifier());
        if (record.isEmpty()) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid field position was defined by Classifier. Field position must be less than [%s].",
                            fieldStrList.size()));
        }
        
        for (RecordDefinition recordDef : getDefinition().getRecords()) {
            if (titleRecordTypeName.equals(recordDef.getTypeName())) {
                // タイトル固有のレコードタイプに条件が存在し、かつその条件にレコードが合致する場合、例外をスローする。
                // 最初の行以降の行は、タイトル固有のレコードタイプと一致してはいけない。
                validateNotMeetTitleConditions(record, recordDef);
                // タイトル固有のレコードタイプに条件が存在しない場合、または読み込んだレコードがタイトル固有のレコードタイプの条件に合致しない場合、このレコードはタイトルでないと判断し処理をスキップする
                continue;
            }            
            if (recordDef.isApplicableTo(record)) {
                validateFieldLength(fieldStrList, recordDef);
                return convertToRecord(fieldStrList, recordDef);
            }
        }
        
        throw newInvalidDataFormatException("an applicable record type was not found. record=[", record, "].");
    }

    /**
     * 対象レコードがタイトル固有のレコードタイプが持つ条件を満たしていないことを検証する。
     * @param record 1レコード分のデータをフィールドごとに格納したMap
     * @param recordDef レコード定義情報保持クラス
     */
    private void validateNotMeetTitleConditions(Map<String, ?> record,
            RecordDefinition recordDef) {
        if (recordDef.getConditionsToApply().size() > 0 && recordDef.isApplicableTo(record, false)) {
            throw newInvalidDataFormatException(
                    "title record occurred after the first line. When directive '", VariableLengthDirective.REQUIRES_TITLE.getName(), "' is true, "
                  , "can not apply the record type '", titleRecordTypeName, "' to after the first line. "
                  , "record type=[", recordDef.getTypeName() , "]. record=[", record, "], conditionToApply=[", recordDef.getConditionsToApply(), "]. ");
        }
    }

    /**
     * マルチフォーマットの場合に、適用するレコードタイプが条件を満たしていることを検証する。
     * <p/>
     * 条件を満たしていない場合、{@link InvalidDataFormatException}をスローする。
     * @param record 出力するレコードの内容を格納したMap
     * @param recordDef レコード定義情報保持クラス
     */
    private void validateMeetConditions(Map<String, ?> record, RecordDefinition recordDef) {
        if (getDefinition().getRecordClassifier() != null && !recordDef.isApplicableTo(record, false)) {
            throw newInvalidDataFormatException(
                    "this record could not be applied to the record type. ",
                    "record type=[", recordDef.getTypeName(), "], record=[", record, "]. following conditions must be met: ",
                    recordDef.getConditionsToApply(), ".");
        }
    }

    /**
     * 1レコード分のフィールド数が正しいかどうか検証する。
     * @param fields フィールドのリスト
     * @param recordDef レコード定義情報保持クラス
     */
    @Published(tag = "architect")
    protected void validateFieldLength(List<String> fields, RecordDefinition recordDef) {
        int sizeOfFields = fields.size();
        int sizeOfRecordDef = recordDef.getFields().size();
        if (fields.size() != sizeOfRecordDef) {
            throw newInvalidDataFormatException(
                    "number of input fields was invalid. ",
                    "number of fields must be [", sizeOfRecordDef, "], ",
                    "but number of input fields was [", sizeOfFields, "].");
        }
    }
    

    
    /**
     * フォーマット定義ファイルで指定されたエンコーディングで、可変長データを読み込むリーダを生成する。
     */
    protected void initializeReader() {
        this.reader = new BufferedReader(new InputStreamReader(source,
                getDefaultEncoding()));
    }

    /**
     * 読み込んだ1レコード分の文字列を、コンバータを用いてオブジェクトに変換し、返却する。
     * @param fieldStrList 読み込んだフィールド文字列のリスト
     * @param recordDef レコード定義情報保持クラス
     * @return データレコード
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合。
     */
    @Published(tag = "architect")
    protected DataRecord convertToRecord(List<String> fieldStrList, RecordDefinition recordDef)
            throws IOException {
        DataRecord record = new DataRecord().setRecordType(recordDef
                .getTypeName());
        record.setRecordNumber(getRecordNumber()); // データレコードにレコード番号を設定する
        for (FieldDefinition field : recordDef.getFields()) {
            if (fieldStrList.size() < field.getPosition()) { 
                break;
            }
            Object value = convertToField(fieldStrList.get(field.getPosition() - 1), field);
            String name = field.getName();
            if (!field.isFiller()) {
                record.put(name, value);
            }
        }
        return record;
    }   

    /**
     * 読み込んだフィールド文字列をコンバータを用いてオブジェクトに変換し、返却する。
     * @param fieldStr 読み込んだフィールド文字列
     * @param field フィールド定義情報保持クラス
     * @return コンバートしたフィールドの内容
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object convertToField(String fieldStr, FieldDefinition field) {
        CharacterStreamDataString dataType = (CharacterStreamDataString) field
                .getDataType();

        Object value;
        try {
            // データタイプのコンバータを実行する
            value = dataType.convertOnRead(fieldStr);
    
            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) { 
                value = convertor.convertOnRead(value);
            }
        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対して、レコード番号とフィールド名の情報を付与する
            throw e.setRecordNumber(getRecordNumber()).setFieldName(field.getName());
        }
        return value;
    }


    /** 最初の行のタイトルを書き込んだかどうか */
    private boolean writeTitle;
    
    /** {@inheritDoc} */
    public void writeRecord(Map<String, ?> record)
    throws IOException, InvalidDataFormatException  {
        
        if (writer == null) {
            throw new IllegalStateException(
                    "output stream was not set. output stream must be set before writing.");
        }

        // requiresTitleがtrueの場合、最初の行はtitleRecordTypeNameに設定されたレコードタイプ名と一致するレコードタイプで書き込む
        if (!writeTitle && requiresTitle) {
            writeRecord(titleRecordTypeName, record);
            return;
        }
        
        // レコード内にデータタイプが明示的に指定されている場合は、
        // そのデータタイプに沿って出力処理を行う。
        if (record instanceof DataRecord) {
            String recordType = ((DataRecord) record).getRecordType();
            if (recordType != null && recordType.length() != 0) {
                writeRecord(recordType, record);
                return;
            }
        }
        
        
        incrementRecordNumber(); // レコード番号をインクリメントする

        // シングルフォーマットの場合
        if (getDefinition().getRecordClassifier() == null) {
            for (RecordDefinition recordDef : getDefinition().getRecords()) {
                if (titleRecordTypeName.equals(recordDef.getTypeName())) {
                    // titleRecordTypeNameに設定されたレコードタイプ名は最初の行のみ有効なので、処理をスキップする
                    continue;
                }
                writeRecord(record, recordDef);
                return;
            }
            // タイトルしか存在しないフォーマットの場合に、最初の行以降の行を書き込もうとした場合、例外をスローする
            throw newInvalidDataFormatException(
                "an applicable record type was not found. This format must not have non-title records.");
        }
        
        // マルチフォーマットの場合
        for (RecordDefinition recordDef : getDefinition().getRecords()) {
            if (titleRecordTypeName.equals(recordDef.getTypeName())) {
                // タイトル固有のレコードタイプに条件が存在し、かつその条件にレコードが合致する場合、例外をスローする。
                // 最初の行以降の行は、タイトル固有のレコードタイプと一致してはいけない。
                validateNotMeetTitleConditions(record, recordDef);
                // タイトル固有のレコードタイプに条件が存在しない場合、または読み込んだレコードがタイトル固有のレコードタイプの条件に合致しない場合、このレコードはタイトルでないと判断し処理をスキップする
                continue;
            }
            if (recordDef.isApplicableTo(record)) {
                writeRecord(record, recordDef);
                return;
            }
        }
        
        throw newInvalidDataFormatException(
                "an applicable record type was not found. record=[", record, "]."
        );
    }

    /**
     * ライタを生成する。
     */
    protected void initializeWriter() {
        writer = new BufferedWriter(
            new OutputStreamWriter(dest, getDefaultEncoding())
        );
    }

    /** {@inheritDoc} */
    public void writeRecord(String recordType, Map<String, ?> record)
    throws IOException {
        
        if (writer == null) {
            throw new IllegalStateException(
                    "output stream was not set. output stream must be set before writing.");
        }
        
        if (recordType == null || recordType.length() == 0) {
            throw newInvalidDataFormatException(
                    "record type was blank. record type must not be blank.");
        }

        
        incrementRecordNumber(); // レコード番号をインクリメントする

        RecordDefinition writeRecordDef = null;
        for (RecordDefinition recordDef : getDefinition().getRecords()) {
            if (recordType.equals(recordDef.getTypeName())) {
                writeRecordDef = recordDef;
                break;
            }
        }
        if (writeRecordDef == null) {
            throw newInvalidDataFormatException(
                    "an applicable record type was not found. ",
                    "specified record type=[", recordType, "].");
        }
        
        if (requiresTitle && !writeTitle) {
            // 最初の行以降の行に、titleRecordTypeNameに設定されたレコードタイプ名と一致しないレコードタイプが指定された場合、例外をスローする
            if (!(titleRecordTypeName.equals(writeRecordDef.getTypeName()))) {
                throw newInvalidDataFormatException(
                        "invalid record type was specified. When directive '", VariableLengthDirective.REQUIRES_TITLE.getName(), "' is true, "
                      , "record type of first line must be '", titleRecordTypeName, "'. record type=[", recordType , "].");
            } 
            writeTitle = true;
        } else {
            // 最初の行以降の行に、titleRecordTypeNameに設定されたレコードタイプ名と一致するレコードタイプが指定された場合、例外をスローする。
            if (titleRecordTypeName.equals(writeRecordDef.getTypeName())) {
                throw newInvalidDataFormatException(
                        "invalid record type was specified. When directive '", VariableLengthDirective.REQUIRES_TITLE.getName(), "' is true, "
                        , "record type of after the first line must not be '", titleRecordTypeName, "'. record type=[", recordType , "].");
            }
        }

        // レコードタイプに条件が存在する（マルチフォーマット）の場合に、レコードがその条件を満たしているかどうかを検証する
        validateMeetConditions(record, writeRecordDef);

        writeRecord(record, writeRecordDef);
    }

    /**
     * 1レコード分の内容を、出力ストリームへ書き込む。
     * @param record 出力するレコードの内容を格納したMap
     * @param recordType レコードタイプ
     * @throws IOException 書き込みに伴うIO処理で問題が発生した場合。
     */
    protected void writeRecord(Map<String, ?> record, RecordDefinition recordType)
    throws IOException {
        for (int i = 0; i < recordType.getFields().size(); i++) {
            if (i > 0) {
                writer.write(fieldSeparator);
            }
            writeField(record, recordType.getFields().get(i));
        }
        writer.write(getRecordSeparator());
        if (DataFormatConfigFinder.getDataFormatConfig().isFlushEachRecordInWriting()) {
            writer.flush();
        }
    }

    /**
     * コンバータによる変換を行ったフィールドの内容を、出力ストリームへ書き込む。
     * @param record 出力するレコードの内容を格納したMap
     * @param field  フィールド定義情報保持クラス
     * @throws IOException 書き込みに伴うIO処理で問題が発生した場合
     */
    @SuppressWarnings("rawtypes")
    protected void writeField(Map<String, ?> record, FieldDefinition field)
    throws IOException {
        
        Object data = record.get(field.getName());
        
 
        String outData;
        try {
            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) {
                data = convertor.convertOnWrite(data);
            }

            // データタイプを実行する       
            CharacterStreamDataString dataType = (CharacterStreamDataString) field
                    .getDataType();
            outData = dataType.convertOnWrite(data);
        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対してフィールド名の情報を付与する
            throw e.setFieldName(field.getName());
        }

        Character quote = quotingDelimiter;
        if (quote == null) {
            writer.write(outData);
            return;
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(quote);
        
        // 囲み文字をエスケープする（二重にする）
        int pos = outData.indexOf(quote);
        int startPos = 0;
        while (pos != -1) {
            builder.append(outData.substring(startPos, pos + 1));
            builder.append(quote);
            startPos = pos + 1;
            pos = outData.indexOf(quote, startPos);
        }
        builder.append(outData.substring(startPos));
        
        builder.append(quote);
        writer.write(builder.toString());
    }
    
    /** 読み込んだ1行の文字数 */
    private int readRecordSize = 0;
    
    /**
     * 入力ストリームから、1行分のレコードに存在するフィールドを、囲み文字などを取り除いた文字列のリストとして読み込む。
     * @return 1行分のレコードを変換した文字列のリスト
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合。
     */
    @Published(tag = "architect")
    protected List<String> readRecordAsString()
            throws IOException {
        
        readRecordSize = 0;
        
        List<String> fieldStrList = new ArrayList<String>();

        boolean readEnd = false;
        
        // フィールド区切り文字をCharacter型として取得
        while (true) {
            boolean quoted = false;
            boolean closedQuote = false;
            StringBuilder buff = new StringBuilder();
            String endSeparator = null;

            int read;
            while ((read = reader.read()) != -1) {
                checkRecordSize();
                if (quotingDelimiter != null && buff.length() == 1) {
                    if (quotingDelimiter.equals(buff.charAt(0))) {
                        quoted = true;
                    }
                }
                buff.append((char) read);

                if (quoted) {  // 囲み文字で囲まれたフィールドの場合
                    if (endsWithChar(quotingDelimiter, buff)) {
                        if (consumeQuoteIfExists(reader, quotingDelimiter)) {
                            buff.append(quotingDelimiter);
                            continue;
                        }
                        endSeparator = consumeSeparator(reader, fieldSeparator, getRecordSeparator());
                        closedQuote = true;
                        if (endSeparator == null) {
                            readEnd = true; // もしセパレータの先読み段階でファイルの終端に到達した場合は、readEndフラグにtrueを設定する
                        }
                        readRecordSize += 1; // 読み込んだ文字数をインクリメントする
                        break;
                    }
                } else { // 囲み文字で囲まれていないフィールドの場合
                    if (endsWithChar(fieldSeparator, buff)) {
                        endSeparator = String.valueOf(fieldSeparator);
                        break;
                    }
                    if (endsWithString(getRecordSeparator(), buff)) {
                        endSeparator = getRecordSeparator();
                        break;
                    }
                }
            }
            // ダブルクォートが閉じられていなかったら、例外をスローする
            if (quoted && !closedQuote) {
                throw newInvalidDataFormatException("Unclosed quotation.");
            }
            // 先頭がダブルクォートの場合はバッファから1文字削除する
            if (quoted) {
                buff.deleteCharAt(0);
            }
            
            // レコードの終端に到達するまで、フィールド文字列のリストに追加
            fieldStrList.add(buff.toString());
            
            // 以下の条件に合致した場合、フィールド文字列のリストを返却する
            //  ・囲み文字の処理で、セパレータを先読みした段階でファイル終端に到達した場合
            //  ・ファイルの終端に到達した場合
            //  ・レコード終端文字列に到達した場合
            if (read == -1 || readEnd || getRecordSeparator().equals(endSeparator)) {
                return fieldStrList;
            }
        }
    }

    /**
     * 読み込んだ1行の文字数をチェックする。
     * <p/>
     * 読み込んだ文字数が上限を超えた場合、例外をスローする。
     */
    private void checkRecordSize() {
        readRecordSize += 1; // 読み込んだ文字数をインクリメントする
        if (readRecordSize > maxRecordLength) {
            throw newInvalidDataFormatException("the number of the read characters exceeded the upper limit. "
                  , "the reading upper limit for 1 record is '", maxRecordLength, "'.");
        }
    }

    /**
     * 後続の記号が囲み文字であれば読み込む。
     * @param in 入力ストリーム
     * @param quote 囲み文字
     * @return 後続の記号が囲み文字であればtrue
     * @throws IOException IOエラー
     */
    protected boolean consumeQuoteIfExists(Reader in, char quote) throws IOException {
        in.mark(1);
        int read = in.read();
        if (quote == (char) read) {
            checkRecordSize();
            return true;
        }
        in.reset();
        return false;
    }
    
    /**
     * セパレータを読み込む。
     * セパレータが読み込めなければ実行時例外を送出する。
     * @param in 入力ストリーム
     * @param recordSeparator レコード終端文字列
     * @param fieldSeparator フィールド区切り文字
     * @return 読み込んだ文字列
     * @throws IOException IOエラー
     */
    protected String consumeSeparator(Reader in, Character fieldSeparator, String recordSeparator) throws IOException {

        in.mark(recordSeparator.length()); // 可変長なレコード終端文字列の長さの分だけ読み込む
        char[] cbuf = new char[recordSeparator.length()];
        if (in.read(cbuf) == -1) {
            return null; // EOF
        }
        
        String read = new String(cbuf); // レコード終端文字列は文字列として比較
        if (recordSeparator.equals(read)) {
            checkRecordSize();
            return read;
        }
        
        if (fieldSeparator == cbuf[0]) { // フィールド区切り文字は1文字固定なので、1文字目で比較
            in.reset();
            return String.valueOf(in.read());
        }

        // エラー。不正なフィールド区切り文字
        throw newInvalidDataFormatException(
                "the field value was delimited by a wrong separator. : ",
                new String(cbuf), ".");
    }
    
    /**
     * バッファの末尾が指定した文字列で終了しているのであれば、
     * その文字列をバッファから除去した上でtrueを返却する。
     * @param str  終了条件の文字列
     * @param buff バッファ
     * @return バッファの末尾が指定した文字列で終了しているのであればtrue
     */
    protected boolean endsWithString(String str, StringBuilder buff) {
        if (buff.length() < str.length()) {
            return false;
        }
        if (str.equals(buff.substring(buff.length() - str.length()))) {
            buff.delete(buff.length() - str.length(), buff.length());
            return true;
        }
        return false;
    }
    
    /**
     * バッファの末尾が指定した１文字で終了しているのであれば、
     * その１文字をバッファから除去した上でtrueを返却する。
     * @param character １文字
     * @param buff バッファ
     * @return バッファの末尾が指定した１文字で終了しているのであればtrue
     */
    protected boolean endsWithChar(Character character, StringBuilder buff) {
        if (character == buff.charAt(buff.length() - 1)) {
            buff.deleteCharAt(buff.length() - 1);
            return true;
        }
        return false;
    }
    
    /** {@inheritDoc}
     * <p/>
     * 空行の存在を無視する設定の場合、ファイル末尾に空行が存在しても、次に読み込む行がないと判定する。
     */
    public boolean hasNext() throws IOException {
        if (reader == null && source == null) {
            return false;
        }
        if (reader != null) {
            return hasNextIgnoreBlankLines();
        } else {
            source.mark(1);
            int readByte = source.read();
            source.reset();
            return (readByte != -1);
        }
    }
    
    /**
     * 次に読み込む行があるかどうかを返却する。<br/>
     * 空行の存在を無視する設定の場合、ファイル末尾に空行が存在しても、次に読み込む行がないと判定する。
     * @return 次に読み込む行がある場合、true
     * @throws IOException - IOエラー
     */
    @Published(tag = "architect")
    protected boolean hasNextIgnoreBlankLines() throws IOException {
        String recordSeparator = getRecordSeparator();
        int tmpRecordNumber = getRecordNumber(); // ファイル末尾の空行の場合、レコード番号のインクリメントを取り消す必要があるので、一時的にレコード番号を保存しておく
        while (true) {
            reader.mark(1);
            int readByte = reader.read();
            if (readByte == -1) {
                setRecordNumber(tmpRecordNumber); // ファイル末尾の空行の場合、レコード番号のインクリメントを取り消す（ファイル末尾の改行はカウントしない）
                return false;
            }
            reader.reset();
            //空レコード読み飛ばし
            if (ignoreBlankLines) {
                reader.mark(recordSeparator.length());
                char[] cbuf = new char[recordSeparator.length()];
                reader.read(cbuf); // 読み込めたサイズがレコード区切り文字の長さに満たない場合は、ストリームのリセットを行う。実際に何バイトのデータが読み込めたかどうかを確認する必要はないので、readメソッドの戻り値の確認は行わない
                if (recordSeparator.equals(new String(cbuf))) {
                    incrementRecordNumber(); // レコード番号をインクリメントする
                    continue;
                }
                reader.reset();
            }
            return true;
        }
    }
    
    
    /**
     * {@inheritDoc}
     * この実装では、{@link #setInputStream}メソッドおよび{@link #setOutputStream}メソッドで渡されたストリーム、
     * および内部でそれらをラップする{@link Reader}、{@link Writer}のストリームをクローズする。
     */
    public void close() {
        if (reader != null) {
            try {
                reader.close();
                reader = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the reader.", e);
            }           
        }
        if (source != null) {
            try {
                source.close();
                source = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the input stream.", e);
            }           
        }
        if (writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the writer.", e);
            }           
        }
        if (dest != null) {
            try {
                dest.close();
                dest = null;
            } catch (IOException e) {
                LOGGER.logWarn("I/O error happened while closing the output stream.", e);
            }           
        }
    }

    /**
     *  コンバータの設定情報保持クラスを取得する
     *  @return コンバータの設定情報保持クラス
     */
    protected VariableLengthConvertorSetting getConvertorSetting() {
        return convertorSetting;
    }

    /**
     * 空文字列を{@code null}に変換するかどうかを各データタイプに設定する。
     * @param datatype 対象のデータタイプ
     * @return このオブジェクト自体
     */
    @Override
    protected DataRecordFormatterSupport setDataTypeProperty(DataType<?, ?> datatype) {
        datatype.setConvertEmptyToNull(convertorSetting.isConvertEmptyToNull());
        return this;
    }
}
