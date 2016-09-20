package nablarch.core.dataformat;

import nablarch.core.util.annotation.Published;

/**
 * 文字列の変換結果を格納するクラス。
 * 
 * @author Kohei Sawaki
 */
public class CharacterReplacementResult {

    /** 変換前文字列 */
    private final String inputString;
    
    /** 変換後文字列 */
    private final String resultString;

    /**
     * コンストラクタ
     * @param inputString 変換前文字列
     * @param resultString 変換後文字列
     */
    public CharacterReplacementResult(final String inputString, final String resultString) {
        this.inputString = inputString;
        this.resultString = resultString;
    }

    /**
     * 変換前文字列を返却する。
     * @return 変換前文字列
     */
    @Published
    public String getInputString() {
        return inputString;
    }
    
    /**
     * 変換後文字列を返却する。
     * @return 変換後文字列
     */
    @Published
    public String getResultString() {
        return resultString;
    }
    
    /**
     * 変換前後の文字列を比較し変換の有無を判定する。
     * @return 変換が行われていた場合、{@code true}
     */
    @Published
    public boolean isReplacement() {
        return !this.inputString.equals(this.resultString);
    }
    
}
