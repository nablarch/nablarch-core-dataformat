package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.repository.SystemRepository;

/**
 * 可変長ファイルの読み書きを行う際に使用するコンバータの設定情報を保持するクラス。
 * コンバータ名とコンバータ実装クラスの対応表 や、タイトルのレコードタイプ名などを、DIコンテナから設定できる。
 * @author Masato Inoue
 */
public class VariableLengthConvertorSetting implements ConvertorSetting {

    /** コンバータのファクトリクラス */
    private VariableLengthConvertorFactory factory = new VariableLengthConvertorFactory();

    /**
     * 未入力値を{@code null}に変換するフラグ。
     * デフォルトでは{@code null}に変換する({@code true})。
     */
    private boolean convertEmptyToNull = true;

    /** システムリポジトリ上の登録名 */
    private static final String REPOSITORY_KEY =  "variableLengthConvertorSetting";

    /**
     * デフォルトのコンバータ設定情報保持クラスのインスタンス。
     * リポジトリからインスタンスを取得できなかった場合に、デフォルトでこのインスタンスが使用される。
     */
    private static final VariableLengthConvertorSetting DEFAULT_SETTING = new VariableLengthConvertorSetting();

    /**
     * このクラスのインスタンスをリポジトリより取得する。
     * リポジトリにインスタンスが存在しない場合は、デフォルトの設定で生成したこのクラスのインスタンスを返却する。
     * @return このクラスのインスタンス
     */
    public static VariableLengthConvertorSetting getInstance() {
        VariableLengthConvertorSetting setting = SystemRepository
                .get(REPOSITORY_KEY);
        if (setting == null) {
            return DEFAULT_SETTING;
        }
        return setting;
    }
    
    /**
     * コンバータのファクトリを返却する。
     * @return コンバータのファクトリ
     */
    public ConvertorFactorySupport getConvertorFactory() {
        return factory;
    }
    
    /**
     * デフォルトのコンバータ名とコンバータ実装クラスの対応表を設定する。
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
     * 未入力値を{@code null}に変換するかを設定する。
     * <p/>
     * デフォルトは{@code null}に変換する({@code true})。
     * @param convertEmptyToNull 未入力値を{@code null}に変換するならtrue
     */
    public void setConvertEmptyToNull(boolean convertEmptyToNull) {
        this.convertEmptyToNull = convertEmptyToNull;
    }

    /**
     * 未入力値を{@code null}に変換するかを取得する。
     * @return 未入力値を{@code null}に変換するならtrue
     */
    public boolean isConvertEmptyToNull() {
        return convertEmptyToNull;
    }
}
