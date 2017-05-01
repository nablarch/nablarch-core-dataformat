package nablarch.core.dataformat.convertor;

import java.util.Map;
import java.util.regex.Pattern;

import nablarch.core.repository.SystemRepository;

/**
 * 固定長ファイルの読み書きを行う際に使用するコンバータの設定情報を保持するクラス。
 * データタイプのグローバル設定や、システム共通で使用するゾーン数値の符号ビットなどを、DIコンテナから設定できる。
 * @author Iwauo Tajima
 */
public class FixedLengthConvertorSetting implements ConvertorSetting {

    /** コンバータのファクトリクラス */
    private FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();

    /** ゾーン数値の符号ビット（正）のデフォルト設定 */
    private Byte defaultPositiveZoneSignNibble = null;
    
    /** ゾーン数値の符号ビット（負）のデフォルト設定 */
    private Byte defaultNegativeZoneSignNibble = null;

    /** パック数値の符号ビット（正）のデフォルト設定 */
    private Byte defaultPositivePackSignNibble = null;
    
    /** パック数値の符号ビット（負）のデフォルト設定 */
    private Byte defaultNegativePackSignNibble = null;

    /**
     * 空文字列を{@code null}に変換するフラグ。
     * <p/>
     * デフォルトでは{@code null}に変換する({@code true})。
     */
    private boolean convertEmptyToNull = true;

    /** システムリポジトリ上の登録名 */
    private static final String REPOSITORY_KEY =  "fixedLengthConvertorSetting";

    /** 符号ビットのパターン */
    private static final String SIGN_BIT_FORMAT = "a-fA-F0-9";
    
    /** 符号ビットのパターン */
    private static final Pattern SIGN_BIT_FORMAT_PATTERN = Pattern.compile("[" + SIGN_BIT_FORMAT + "]");
    
    /**
     * デフォルトのコンバータ設定情報保持クラスのインスタンス。
     * リポジトリからインスタンスを取得できなかった場合に、デフォルトでこのインスタンスが使用される。
     */
    private static final FixedLengthConvertorSetting DEFAULT_SETTING = new FixedLengthConvertorSetting();
    
    /**
     * このクラスのインスタンスをリポジトリから取得し、返却する。
     * @return このクラスのインスタンス
     */
    public static FixedLengthConvertorSetting getInstance() {
        FixedLengthConvertorSetting setting = SystemRepository.get(REPOSITORY_KEY);
        if (setting == null) {
            return DEFAULT_SETTING;
        }
        return setting;
    }

    /**
     * コンバータのファクトリクラスを返却する。
     * @return コンバータのファクトリクラス
     */
    public ConvertorFactorySupport getConvertorFactory() {
        return factory;
    }
    
    /**
     * ゾーン数値の符号ビット（正） を返却する。
     * @return ゾーン数値の符号ビット（正）
     */
    public Byte getDefaultPositiveZoneSignNibble() {
        return defaultPositiveZoneSignNibble;
    }

    /**
     * ゾーン数値の符号ビット（正） を設定する。
     * @param nibble 符号ビット（4bit）を表す文字列（[0-9a-zA-Z]）
     * @return このオブジェクト自体
     */
    public FixedLengthConvertorSetting setDefaultPositiveZoneSignNibble(String nibble) {
        if (!SIGN_BIT_FORMAT_PATTERN.matcher(nibble).matches()) {
            throw new IllegalStateException(
                    String.format(
                            "invalid nibble format was specified. nibble=[%s]. valid format=[%s].",
                            nibble, SIGN_BIT_FORMAT));
        }
        defaultPositiveZoneSignNibble = (byte) (Integer.parseInt(nibble) << 4);
        return this;
    }
    
    /**
     * ゾーン数値の符号ビット（負） を返却する。
     * @return ゾーン数値の符号ビット（負）
     */
    public Byte getDefaultNegativeZoneSignNibble() {
        return defaultNegativeZoneSignNibble;
    }
    
    /**
     * ゾーン数値の符号ビット（負） を設定する。
     * @param nibble 符号ビット（4bit）を表す文字列（[0-9a-zA-Z]）
     * @return このオブジェクト自体
     */
    public FixedLengthConvertorSetting setDefaultNegativeZoneSignNibble(String nibble) {
        if (!SIGN_BIT_FORMAT_PATTERN.matcher(nibble).matches()) {
            throw new IllegalStateException(
                    String.format(
                            "invalid nibble format was specified. nibble=[%s]. valid format=[%s].",
                            nibble, SIGN_BIT_FORMAT));
        }
        defaultNegativeZoneSignNibble = (byte) (Integer.parseInt(nibble) << 4);
        return this;
    }    
    
    
    

    /**
     * パック数値の符号ビット（正） を返却する。
     * @return パック数値の符号ビット（正）
     */
    public Byte getDefaultPositivePackSignNibble() {
        return defaultPositivePackSignNibble;
    }

    /**
     * パック数値の符号ビット（正） を設定する。
     * @param nibble 符号ビット（4bit）を表す文字列（[0-9a-zA-Z]）
     * @return このオブジェクト自体
     */
    public FixedLengthConvertorSetting setDefaultPositivePackSignNibble(String nibble) {
        if (!SIGN_BIT_FORMAT_PATTERN.matcher(nibble).matches()) {
            throw new IllegalStateException(
                    String.format(
                            "invalid nibble format was specified. nibble=[%s]. valid format=[%s].",
                            nibble, SIGN_BIT_FORMAT));
        }
        defaultPositivePackSignNibble = (byte) (Integer.parseInt(nibble));
        return this;
    }
    
    /**
     * パック数値の符号ビット（負） を返却する。
     * @return パック数値の符号ビット（負）
     */
    public Byte getDefaultNegativePackSignNibble() {
        return defaultNegativePackSignNibble;
    }
    
    /**
     * パック数値の符号ビット（負） を設定する。
     * @param nibble 符号ビット（4bit）を表す文字列（[0-9a-zA-Z]）
     * @return このオブジェクト自体
     */
    public FixedLengthConvertorSetting setDefaultNegativePackSignNibble(String nibble) {
        if (!SIGN_BIT_FORMAT_PATTERN.matcher(nibble).matches()) {
            throw new IllegalStateException(
                    String.format(
                            "invalid nibble format was specified. nibble=[%s]. valid format=[%s].",
                            nibble, SIGN_BIT_FORMAT));
        }
        defaultNegativePackSignNibble = (byte) (Integer.parseInt(nibble));
        return this;
    }    
    
    
    /**
     * コンバータ名と、コンバータの実装クラスを保持するテーブルを設定する。
     * @param table コンバータ名と、コンバータの実装クラスを保持するテーブル
     * @return このオブジェクト自体
     * @throws ClassNotFoundException 指定されたクラスが存在しなかった場合、
     * もしくは、指定されたクラスが ValueConvertorを実装していなかった場合に、スローされる例外
     */
    public ConvertorSetting setConvertorTable(Map<String, String> table) throws ClassNotFoundException {
        factory.setConvertorTable(table);
        return this;
    }

    /**
     * {@link FixedLengthConvertorFactory}を設定する。
     * @param factory {@link FixedLengthConvertorFactory}
     */
    public void setFixedLengthConvertorFactory(final FixedLengthConvertorFactory factory) {
        this.factory = factory;
    }

    /**
     * 空文字列を{@code null}に変換するかを設定する。
     * <p/>
     * デフォルトは{@code null}に変換する({@code true})。
     * @param convertEmptyToNull 空文字列を{@code null}に変換するならtrue
     */
    public void setConvertEmptyToNull(boolean convertEmptyToNull) {
        this.convertEmptyToNull = convertEmptyToNull;
    }

    /**
     * 空文字列を{@code null}に変換するかを取得する。
     * @return 空文字列を{@code null}に変換するならtrue
     */
    public boolean isConvertEmptyToNull() {
        return convertEmptyToNull;
    }
}
