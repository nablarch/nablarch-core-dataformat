package nablarch.core.dataformat;

/**
 * 寄せ字変換処理の設定を保持するクラス。
 * @author Masato Inoue
 */
public class CharacterReplacementConfig {
    
    /** 寄せ字変換タイプ名 */
    private String typeName;
    
    /** 寄せ字変換定義ファイルのパス */
    private String filePath;
    
    /** 寄せ字処理の際に使用するエンコーディング */
    private String encoding;

    /** 変換前と変換後の文字のバイト長一致チェックの要否 */
    private boolean byteLengthCheck = true;
    
    /**
     * 寄せ字変換定義ファイルのパスを取得する。
     * @return 寄せ字変換定義ファイルのパス
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 寄せ字変換定義ファイルのパスを設定する。
     * @param filePath 寄せ字変換定義ファイルのパス
     * @return このオブジェクト自体
     */
    public CharacterReplacementConfig setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }
    
    /**
     * 寄せ字処理の際に使用するエンコーディングを取得する。
     * @return 寄せ字処理の際に使用するエンコーディング
     */
    public String getEncoding() {
        return encoding;
    }
    
    /**
     * 寄せ字処理の際に使用するエンコーディングを設定する。
     * @param encoding 寄せ字処理の際に使用するエンコーディング
     * @return このオブジェクト自体
     */
    public CharacterReplacementConfig setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }
    
    /**
     * 寄せ字変換タイプ名を取得する。
     * @return 寄せ字変換タイプ名
     */
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * 寄せ字変換タイプ名を設定する。
     * @param typeName 寄せ字変換タイプ名
     * @return このオブジェクト自体
     */
    public CharacterReplacementConfig setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    /**
     * 変換前と変換後の文字のバイト長一致チェックの要否を取得する。
     * @return 変換前と変換後の文字のバイト長一致チェックを行う場合、true
     */
    public boolean isByteLengthCheck() {
        return byteLengthCheck;
    }

    /**
     * 変換前と変換後の文字のバイト長一致チェックの要否を設定する。
     * @param byteLengthCheck 変換前と変換後の文字のバイト長一致チェックの要否
     * @return このオブジェクト自体
     */
    public CharacterReplacementConfig setByteLengthCheck(boolean byteLengthCheck) {
        this.byteLengthCheck = byteLengthCheck;
        return this;
    }
    
    
}
