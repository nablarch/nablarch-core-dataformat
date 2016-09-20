package nablarch.core.dataformat;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.convertor.ConvertorFactorySupport;
import nablarch.core.dataformat.convertor.ConvertorSetting;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.CharacterReplacer;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.Builder.join;


/**
 * フォーマット定義ファイルの内容に従い、ファイルデータの読み書きを行うクラスの抽象基底クラス。
 * <p>
 * 本クラスでは、フォーマット定義情報保持クラス（LayoutDefinition）の初期化および内容の妥当性検証を行い、
 * 実際のファイルデータの読み書きはサブクラスにて行う。
 * </p>
 * <p>
 * 本クラスを継承するクラスでは、以下のディレクティブを指定することができる。
 * <ul>
 * <li>ファイルタイプ</li>
 * <li>文字エンコーディング</li>
 * <li>レコード終端文字列（改行コード）</li>
 * </ul>
 * ※ディレクティブとは、文字エンコーディングや改行コード、フィールド区切り文字など、
 * ファイルを読み書きする際に"ファイル単位"で定義できる各種設定項目のことを示す。
 * </p>
 * 
 * @author Masato Inoue
 */
@Published(tag = "architect")
public abstract class DataRecordFormatterSupport implements
        DataRecordFormatter {

    /** 読み込みまたは書き込み中のレコードのレコード番号 */
    private int recordNumber = 0;

    /** フォーマット定義ファイルの情報を保持するクラス */
    private LayoutDefinition definition = null;

    /** ディレクティブの名前と、ディレクティブの値のデータ型の定義を保持するMap */
    private Map<String, Directive> directiveMap = new HashMap<String, Directive>();

    /** デフォルトの文字エンコーディング */
    private Charset defaultEncoding;

    /** レコード終端文字列 */
    private String recordSeparator;
    
    /**
     * コンストラクタ。
     * ディレクティブを初期化する。
     */
    public DataRecordFormatterSupport() {
        directiveMap = createDirectiveMap();
    }

    /**
     * ファイルフォーマッタが共通的に使用するディレクティブの名前と値の型。（タイプセーフEnum）
     * 以下に一覧を示す。<br>
     * <ul>
     * <li>file-type：String</li>
     * <li>text-encoding：String</li>
     * <li>record-separator：String</li>
     * </ul>
     *
     * @author Masato Inoue
     */
    @Published(tag = "architect")
    public static class Directive {
        /** ファイルタイプ */
        public static final Directive FILE_TYPE = new Directive("file-type", String.class);
        /** 文字エンコーディング */
        public static final Directive TEXT_ENCODING = new Directive("text-encoding", String.class);
        /** レコード終端文字列 */
        public static final Directive RECORD_SEPARATOR = new Directive("record-separator", String.class);

        /** 列挙型の全要素(親クラスの要素を含む） */
        private static final Map<String, Directive> VALUES = createDirectiveMap(
                    new HashMap<String, Directive>()
                    , FILE_TYPE, TEXT_ENCODING, RECORD_SEPARATOR);

        /**
         * コンストラクタ。
         *
         * @param name ディレクティブ名
         * @param type ディレクティブの値の型
         */
        public Directive(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        /** ディレクティブの名前 */
        private final String name;

        /** ディレクティブの値の型 */
        private final Class<?> type;

        /**
         * ディレクティブの名前を取得する。
         *
         * @return ディレクティブの名前
         */
        public String getName() {
            return name;
        }

        /**
         * ディレクティブの値の型を取得する。
         *
         * @return ディレクティブの値の型
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * ファイルタイプを取得する
         *
         * @param directive ディレクティブ
         * @return ファイルタイプ
         */
        public static String getFileType(Map<String, Object> directive) {
            return (String) directive.get(FILE_TYPE.getName());
        }

        /**
         * レコード終端文字列を取得する
         *
         * @param directive ディレクティブ
         * @return レコード終端文字列
         */
        public static String getRecordSeparator(Map<String, Object> directive) {
            return (String) directive.get(RECORD_SEPARATOR.getName());
        }

        /**
         * エンコーディングを取得する
         *
         * @param directive ディレクティブ
         * @return レコード終端文字列
         */
        public static String getTextEncoding(Map<String, Object> directive) {
            return (String) directive.get(TEXT_ENCODING.getName());
        }
        
        /**
         * 使用するディレクティブの名前とDirectiveのMapを生成する。
         * @param <T> ディレクティブの型
         * @param additionalElements 追加するディレクティブ
         * @return 使用するディレクティブの名前とDirectiveのMap
         */
        protected static <T extends Directive> Map<String, Directive> createDirectiveMap(T... additionalElements) {
            return createDirectiveMap(
                    new HashMap<String, Directive>(VALUES)
                    , additionalElements);
        }

        /**
         * 使用するディレクティブの名前とDirectiveのMapを生成する。
         * @param <T> ディレクティブの型
         * @param base 元となるディレクティブ
         * @param additionalElements 追加するディレクティブ
         * @return 使用するディレクティブの名前とDirectiveのMap
         */
        private static <T extends Directive> Map<String, Directive> createDirectiveMap(
                Map<String, Directive> base, T... additionalElements) {
            for (T e : additionalElements) {
                base.put(e.getName(), e);
            }
            return Collections.unmodifiableMap(base);
        }
    }

    /**
     * 使用するディレクティブの名前とDirectiveのMapを生成する。
     * サブクラスで使用するディレクティブを追加する場合は、本メソッドをオーバーライドし、任意のディレクティブを追加すること。
     *
     * @return 使用するディレクティブの名前と値の型のMap
     */
    protected Map<String, Directive> createDirectiveMap() {
        return Directive.VALUES;
    }

    /**
     * コンバータの設定情報を取得する。
     *
     * @return コンバータの設定情報
     */
    protected abstract ConvertorSetting getConvertorSetting();

    /** 本クラスが初期化されたかどうかのフラグ */
    private boolean isInitialized = false;

    /**
     * フォーマット定義情報保持クラスの初期化を行う。
     * 初期化は本メソッドの1回目の実行時のみ行われ、2回目以降の実行時に初期化は行われない。
     * 
     * @return このオブジェクト自体
     */
    public DataRecordFormatter initialize() {
        if (!isInitialized) {
            initializeDefinition();
        }
        isInitialized = true;
        return this;
    }

    /**
     * フォーマット定義情報保持クラス({@link LayoutDefinition}）の初期化および内容の妥当性を検証し、
     * フォーマット定義情報保持クラスから必要な情報を本クラスのプロパティに設定する。
     * フォーマット定義情報保持クラスがすでに初期化されている場合、初期化は行わない。
     */
    protected void initializeDefinition() {

        if (definition == null) {
            throw new IllegalStateException(
                    "LayoutDefinition was not set. LayoutDefinition must be set before initialize.");
        }
        try {
            synchronized (definition) {
                initializeDefinition(definition);
            }
        } catch (SyntaxErrorException e) {
            throw e.setFilePath(definition.getSource());
        }
    }
    
    /**
     * フォーマット定義情報保持クラスの初期化および内容の妥当性を検証する。
     * @param definition 初期化対象インスタンス
     */
    private void initializeDefinition(LayoutDefinition definition) {
        Map<String, Object> directive = definition.getDirective();
        
        // すでにフォーマット定義情報保持クラスが初期化されている場合は、プロパティの初期化のみ行う
        if (definition.isInitialized()) {
            initializeField(directive);
            return;
        }

        // ディレクティブの妥当性を検証する
        validateDirectives(directive);

        // 本クラスのフィールドを初期化する
        initializeField(directive);

        // レコード識別情報の初期化
        if (definition.getRecordClassifier() != null) {
            initializeClassifier();
        }

        // フィールド定義の初期化
        initializeFieldDefinition();

        definition.setInitialized(true);
    }

    /**
     * ディレクティブの内容の妥当性を検証する。
     * <p>
     * サブクラスで独自のディレクティブを使用する場合は、このメソッドをオーバーライドし、独自のディレクティブに対して妥当性検証を行うこと。<br/>
     * <br/>
     * {@link DataRecordFormatter}では以下の仕様を満たしているかどうかの検証を行う。
     * <ul>
     * <li>ディレクティブの値のデータ型が正しい</li>
     * <li>ファイルタイプが定義されている</li>
     * <li>エンコーディングが定義されている</li>
     * <li>エンコーディングがCharset型に変換できる</li>
     * <li>レコード終端文字列が許容されている文字である</li>
     * </ul>
     * </p>
     * <p>
     * 妥当性検証に失敗した場合は、{@link SyntaxErrorException}がスローされる。
     * </p>
     * @param directive ディレクティブ
     */
    protected void validateDirectives(Map<String, Object> directive) {

        validateDirectivesDataType(directive);

        // ファイルタイプは必須
        validateFileType(directive);
        // エンコーディング指定は必須、またCharset型に変換できることのチェックを行う
        validateEncoding(directive);
        // レコード終端文字列
        validateRecordSeparator(directive);

    }


    /**
     * ディレクティブのファイルタイプをバリデーションする。
     * 
     * @param directive バリデーション対象ディレクティブ
     */
    private void validateFileType(Map<String, Object> directive) {
        // ファイルタイプは必須
        if (Directive.getFileType(directive) == null) {
            handleMissingDirective(Directive.FILE_TYPE);
        }
    }

    /**
     * ディレクティブのエンコーディングをバリデーションする。
     * 
     * @param directive バリデーション対象ディレクティブ
     */
    private void validateEncoding(Map<String, Object> directive) {
        String textEncoding = Directive.getTextEncoding(directive);
        if (textEncoding == null) {
            // エンコーディング指定は必須
            handleMissingDirective(Directive.TEXT_ENCODING);
        }
        // Charset型に変換できることのチェックを行う
        if (!isAvailable(textEncoding)) {
            throw newSyntaxError(
                    "invalid encoding was specified by '", Directive.TEXT_ENCODING.name, "' directive. ",
                    "value=[", textEncoding, "].");
        }
    }

    /**
     * レコード区切り文字をバリデーションする。
     * 
     * @param directive バリデーション対象ディレクティブ
     */
    private void validateRecordSeparator(Map<String, Object> directive) {
        String recordSeparator = Directive.getRecordSeparator(directive);
        // レコード終端文字列が許容される文字であることのチェック
        if (StringUtil.isNullOrEmpty(recordSeparator)
                || allowedRecordSeparatorList.contains(recordSeparator)) {
            return;  // OK
        }
        // \rと\tを表示用に\\rと\\tに変換
        List<String> separators = new ArrayList<String>();
        for (String e : allowedRecordSeparatorList) {
            separators.add(escape(e));
        }
        throw newSyntaxError(
                "not allowed record separator was specified by '",
                Directive.RECORD_SEPARATOR.getName(), "' directive. ",
                "value=[", escape(recordSeparator), "]. ",
                "record separator was must be [", join(separators, " or "), "].");
    }

    /**
     * 文字列中の'\r','\t','\n'をエスケープする。
     * 
     * @param s 対象文字列
     * @return エスケープ後の文字列
     */
    private String escape(String s) {
        return s.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    /**
     * エンコーディングが利用可能かどうか判定する。
     * 
     * @param encoding エンコーディング名
     * @return 利用可能な場合、真
     */
    private boolean isAvailable(String encoding) {
        try {
            return Charset.isSupported(encoding);
        } catch (IllegalCharsetNameException e) {
            return false;
        }
    }

    /**
     * 必須のディレクティブが指定されていない場合の例外をスローする。
     * 
     * @param notFound 必須のディレクティブ
     * @throws SyntaxErrorException 必ずスローされる
     */
    private void handleMissingDirective(Directive notFound) throws SyntaxErrorException {
        throw newSyntaxError(
                "directive '", notFound.name, "' was not specified. ",
                "directive '", notFound.name, "' must be specified."
        );
    }

    /**
     * {@link SyntaxErrorException}を生成する。
     * <p/>
     * 引数に与えられた要素を連結して例外メッセージとする。
     * 例外には、その例外の原因となったファイルパスが設定される。
     * @param msgElements メッセージの要素
     * @return {@link SyntaxErrorException}インスタンス
     */
    SyntaxErrorException newSyntaxError(Object... msgElements) {
        String msg = concat(msgElements);
        return new SyntaxErrorException(msg).setFilePath(definition.getSource());
    }

    
    /**
     * フィールドを初期化する。
     * <p/>
     * {@link DataRecordFormatterSupport}では、ディレクティブに設定された以下の値をフィールドに設定する。
     * <ul>
     * <li>レコード終端文字列</li>
     * <li>文字エンコーディング</li>
     * </ul>
     *
     * @param directive ディレクティブ
     */
    protected void initializeField(Map<String, Object> directive) {
        recordSeparator = Directive.getRecordSeparator(directive);
        defaultEncoding = Charset.forName(Directive.getTextEncoding(directive));
    }

    /**
     * 定義されたすべてのディレクティブの値のデータ型が正しいことを検証する。
     *
     * @param directive ディレクティブ
     */
    protected void validateDirectivesDataType(Map<String, Object> directive) {
        for (Map.Entry<String, Object> entry : directive.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            // 値のデータ型が定義されていない場合は、例外をスローする
            if (!directiveMap.containsKey(name)) {
                throw newSyntaxError("unknown directive was specified. value=[", name, "].");
            }

            // 値のデータ型が不正な場合は、例外をスローする
            Class<?> expected = directiveMap.get(name).getType();
            Class<?> valueClass = value.getClass();
            if (!expected.isAssignableFrom(valueClass)) {
                throw newSyntaxError(
                        "directive value was invalid type. the value of the directive '", name, "'",
                        " must be ", expected.getName(),
                        " but was ", valueClass.getName(), ".");
            }
        }
    }

    /**
     * レコード識別情報が存在する場合（マルチレイアウトファイルの場合）、
     * レコード識別情報に関連するフィールド定義クラスの初期化（コンバータ設定およびエンコーディング設定）を行う。
     * <p/>
     * レコード種別識別定義の場合は、フィールド位置の整合性チェックは行わない。
     */
    protected void initializeClassifier() {
        for (FieldDefinition field : getDefinition().getRecordClassifier()
                .getFields()) {
            setFieldProperty(field, getDefinition().getRecordClassifier());
        }
    }

    /**
     * フィールド定義クラスについて、以下の初期化処理を行う。
     * <ul>
     * <li>コンバータ設定</li>
     * <li>エンコーディング設定</li>
     * <li>差分定義が行われている場合は、親のフィールド定義情報を反映</li>
     * <li>レコード長、フィールド長の妥当性検証</li>
     * </ul>
     */
    protected void initializeFieldDefinition() {

        // 親の差分定義が反映されたフィールド定義クラスのmap
        // （後述のforループ内でRecordDefinitionの内容を変更できないため、ここに保持しておき、メソッドの最後でレコード情報を更新する
        Map<RecordDefinition, List<FieldDefinition>> fieldDefinitionCache = new HashMap<RecordDefinition, List<FieldDefinition>>();

        for (RecordDefinition record : getDefinition().getRecords()) {
            int head = 1;

            // 親の差分定義が反映されたフィールド定義クラスのリスト
            ArrayList<FieldDefinition> fieldList = new ArrayList<FieldDefinition>();

            for (FieldDefinition field : record.getFields()) {

                setFieldProperty(field, record);

                // フォーマットが差分定義になっている場合は、親のフィールド定義を反映する。
                if (record.getBaseRecordType() != null) {
                    for (FieldDefinition baseField : record.getBaseRecordType().getFields()) {
                        int p = baseField.getPosition();
                        if (head <= p && p < field.getPosition()) {
                            fieldList.add(baseField);
                            head += baseField.getSize();
                        }
                    }
                }

                // 開始位置と、現在位置の妥当性を検証する
                validatePosition(head, field);

                fieldList.add(field);
                head += field.getSize();
            }

            // フォーマットが差分定義になっている場合は、親のフィールド定義を使用する
            if (record.getBaseRecordType() != null) {
                for (FieldDefinition baseField : record.getBaseRecordType().getFields()) {
                    int p = baseField.getPosition();
                    if (head <= p) {
                        fieldList.add(baseField);
                        head += baseField.getSize();
                    }
                }
            }

            // レコード長の妥当性を検証する。
            validateRecordLength(head, record);

            // 差分が反映されたフィールド情報をキャッシュに格納する
            fieldDefinitionCache.put(record, fieldList);
        }

        // キャッシュしておいた差分が反映されたフィールド情報を、元のレコードに反映する
        for (Map.Entry<RecordDefinition, List<FieldDefinition>> entry : fieldDefinitionCache.entrySet()) {
            entry.getKey().setFields(entry.getValue());
        }
    }

    /**
     * フィールド定義情報保持クラスのプロパティを設定する。
     * @param field フィールド定義情報保持クラス
     * @param recordDef レコード定義情報保持クラス
     */
    protected void setFieldProperty(FieldDefinition field, RecordDefinition recordDef) {
        // エンコーディング設定を行う
        if (field.getEncoding() == null) {
            field.setEncoding(defaultEncoding);
        }
        // コンバータ設定を行う
        addConvertorToField(field, recordDef);
    }

    /**
     * レコード長の妥当性を検証する。
     *
     * @param head   位置
     * @param record レコード定義情報保持クラス
     */
    protected void validateRecordLength(int head, RecordDefinition record) {
        // 何もしない
    }

    /**
     * 開始位置と、現在位置の妥当性を検証する。
     *
     * @param head  位置
     * @param field フィールド定義情報保持クラス
     */
    protected void validatePosition(int head, FieldDefinition field) {
        // 開始位置と、現在位置の整合性チェック
        if (field.getPosition() != head) {
            throw newSyntaxError(
                    "invalid field position was specified. field '", field.getName(),
                    "' must at ", head, ". but ", field.getPosition(), ".");
        }
    }

    /**
     * フィールドのフォーマット定義を保持するクラスに関連するコンバータを生成し、フィールド定義クラスに設定する。
     * コンバータを生成する役割を担うコンバータファクトリは、サブクラスで指定されたコンバータ情報設定クラスをもとに生成する。
     * {@link #getConvertorSetting()}
     *
     * @param field フォーマット定義を保持するクラス
     * @param recordDefinition レコード定義情報保持クラス
     */
    protected void addConvertorToField(FieldDefinition field, RecordDefinition recordDefinition) {
        
        ConvertorFactorySupport factory = getConvertorSetting()
        .getConvertorFactory();

        DataType<?, ?> dataType = field.getDataType();
        String dataTypeName = null;
        Object[] dataTypeValues = new Object[0];
        for (Map.Entry<String, Object[]> entry : field.getConvertorSettingList().entrySet()) {
            if (dataType == null) {
                dataType = factory.typeOfWithoutInit(entry.getKey(), field, entry.getValue());
                dataTypeName = entry.getKey();
                dataTypeValues = entry.getValue();
            } else {
                ValueConvertor<?, ?> valueConvertor = factory.convertorOf(entry.getKey(), field, entry.getValue());
                field.addConvertor(valueConvertor);
            }
        }
        
        // フィールドに対するデータタイプが設定されていない場合、例外をスローする
        if (dataType == null) {
            throw newSyntaxError(
                    "data type was not specified. data type must be specified. ",
                    "record type=[", recordDefinition.getTypeName(), "], ",
                    "field name=[", field.getName(), "].");
        }

        // データタイプの初期化を行う。
        setDataTypeProperty(dataType);
        dataType.init(field, dataTypeValues);
        field.setDataType(dataType);

        // データタイプ名に対応するデフォルトの寄せ字変換タイプが存在し、かつ、フィールドに対する寄せ字コンバータが設定されていない場合に、
        // デフォルトの寄せ字コンバータを生成し、フィールドに設定する
        if (defaultReplacementType.containsKey(dataTypeName)) {
            boolean findCharacterReplacer = false;
            for (ValueConvertor<?, ?> convertor : field.getConvertors()) {
                if (convertor instanceof CharacterReplacer) {
                    findCharacterReplacer = true;
                }
            }
            if (!findCharacterReplacer) {
                String replacementType = defaultReplacementType.get(dataTypeName);
                field.addConvertor(createCharacterReplacer(field,
                        replacementType));
            }
        }
    }
    
    /**
     * デフォルトの寄せ字コンバータを生成する。
     * @param field フィールド
     * @param replacementType 寄せ字変換タイプ
     * @return デフォルトの寄せ字コンバータ
     */
    protected CharacterReplacer createCharacterReplacer(FieldDefinition field,
            String replacementType) {
        CharacterReplacer replacementCharacter = new CharacterReplacer();
        replacementCharacter.initialize(field, replacementType);
        return replacementCharacter;
    }
    
    /**
     * データタイプの設定を行う。ファイルタイプ個別の設定を行う必要がある場合、必要に応じてサブクラスでオーバーライドする。
     *
     * @param dataType データタイプ
     * @return このオブジェクト自体
     */
    protected DataRecordFormatterSupport setDataTypeProperty(DataType<?, ?> dataType) {
        // 何もしない
        return this;
    }
    
    /**
     * コンバータの設定を行う。ファイルタイプ個別の設定を行う必要がある場合、必要に応じてサブクラスでオーバーライドする。
     *
     * @param valueConvertor コンバータ
     */
    protected void setValueConvertorProperty(ValueConvertor<?, ?> valueConvertor) {
        // 何もしない
    }

    /** {@inheritDoc} */
    public DataRecordFormatter setDefinition(LayoutDefinition definition) {
        isInitialized = false;
        this.definition = definition;
        return this;
    }

    /**
     * フォーマット定義ファイルの情報を保持するクラスを取得する。
     *
     * @return フォーマット定義ファイルの情報を保持するクラス
     */
    protected LayoutDefinition getDefinition() {
        return definition;
    }

    /**
     * 許容するレコード終端文字列のリスト。
     * デフォルトでは、[\r][\n][\r\n]を許容する。
     */
    private List<String> allowedRecordSeparatorList = Arrays.asList("\r", "\n", "\r\n");

    /** データタイプ名に対応するデフォルトの寄せ字変換タイプ名 */
    private Map<String, String> defaultReplacementType = new HashMap<String, String>();

    /**
     * 許容するレコード終端文字列のリストを設定する。
     *
     * @param allowedRecordSeparatorList 許容されるレコード終端文字列のリスト
     * @return このオブジェクト自体
     */
    public DataRecordFormatterSupport setAllowedRecordSeparatorList(
            List<String> allowedRecordSeparatorList) {
        this.allowedRecordSeparatorList = allowedRecordSeparatorList;
        return this;
    }

    /**
     * 読み込みまたは書き込み中のレコードのレコード番号を取得する。
     *
     * @return recordNumber 読み込みまたは書き込み中のレコードのレコード番号
     */
    public int getRecordNumber() {
        return recordNumber;
    }

    /** 読み込みまたは書き込み中のレコードのレコード番号をインクリメントする。 */
    protected void incrementRecordNumber() {
        this.recordNumber++;
    }

    /** 読み込みまたは書き込み中のレコードのレコード番号を設定する。 
     * @param recordNumber 読み込みまたは書き込み中のレコードのレコード番号
     */
    protected void setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
    }

    /**
     * デフォルトの文字エンコーディングを取得する。
     *
     * @return 文字エンコーディング
     */
    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * レコード終端文字列を取得する。
     *
     * @return レコード終端文字列
     */
    protected String getRecordSeparator() {
        return recordSeparator;
    }

    /**
     * データタイプ名に対応するデフォルトの寄せ字変換タイプ名を設定する。
     * @param defaultReplacementType データタイプ名に対応するデフォルトの寄せ字変換タイプ名
     * @return このオブジェクト自体
     */
    public DataRecordFormatterSupport setDefaultReplacementType(
            Map<String, String> defaultReplacementType) {
        this.defaultReplacementType = defaultReplacementType;
        return this;
    }

    /**
     * 引数を連結したものをメッセージとして、{@link InvalidDataFormatException}を生成する。<br/>
     * この例外には、フォーマットファイルのパスと例外発生時の行番号が設定される。
     * @param msgElements メッセージ要素
     * @return {@link InvalidDataFormatException}インスタンス
     */
    protected final InvalidDataFormatException newInvalidDataFormatException(Object... msgElements) {
        String msg = Builder.concat(msgElements);
        return addFormatAndRecordNumberTo(new InvalidDataFormatException(msg));
    }

    /**
     * {@link InvalidDataFormatException}に
     * フォーマットファイルのパスと例外発生時の行番号を設定する。
     *
     * @param e 設定対象の例外インスタンス
     * @return 引数で与えられたインスタンス
     */
    protected final InvalidDataFormatException addFormatAndRecordNumberTo(InvalidDataFormatException e) {
        return e.setRecordNumber(getRecordNumber())
                .setFormatFilePath(definition.getSource());
    }
    
    /**
     * このフォーマッタが取り扱うファイル種別を返却する。
     * @return ファイル種別
     */
    public String getFileType() {
        return Directive.getFileType(definition.getDirective());
    }

    /**
     * このフォーマッタが取り扱うファイルのmime-typeを返却する。<br>
     * デフォルトではtext/plainを返却する。必要に応じサブクラスでオーバーライドすること。
     * @return ファイルのmime-type
     */
    public String getMimeType() {
        return "text/plain";
    }

}
