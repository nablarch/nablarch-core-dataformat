package nablarch.core.dataformat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.util.annotation.Published;


/**
 * フィールド定義ユーティリティクラス。<br/>
 * フィールド定義に関する汎用的な処理を提供する。
 *
 * @author Kohei Sawaki
 */
public final class FieldDefinitionUtil {
    /** privateコンストラクタ。 */
    private FieldDefinitionUtil() {
    }

    /** 単語文字列を示す正規表現パターン */
    private static final Pattern WORD_CHAR_PTN = Pattern.compile("[a-zA-Z0-9\\.\\[\\]_]+");

    /**
     * フィールド名を正規化する。
     * <p/>
     * 非単語文字（英数字 、"."、"["、"]"、"_"以外の文字）で区切られた文字列を下記仕様に基いて正規化する。
     * <p/>
     * 仕様：
     * <ol>
     *     <li>文字列を非単語文字でセパレートする</li>
     *     <li>セパレートされた文字列のうち、先頭以外の文字列をキャメル記法にして連結する</li>
     * </ol>
     *
     * 例：<br/>
     * form:user:input:data //--> formUserInputData <br/>
     * formuserinputdata //--> formuserinputdata
     * <p/>
     * 文字列がnullの場合は、nullを返す。<br/>
     * 文字列が空文字の場合は、空文字を返す。
     *
     * @param nonWordCharSeparated 非単語文字区切りの文字列
     * @return 変換後の文字列
     */
    @Published
    public static String normalizeWithNonWordChar(String nonWordCharSeparated) {
        if (nonWordCharSeparated == null) {
            return null;
        }

        StringBuilder ret = new StringBuilder();

        Matcher m = WORD_CHAR_PTN.matcher(nonWordCharSeparated);
        while (m.find()) {
            String part = m.group();
            ret.append(toUpperFirstChar(part));
        }
        return (ret.length() == 0)
                ? nonWordCharSeparated
                : toLowerFirstChar(ret.toString());
    }

    /**
     * 文字列の先頭文字を大文字に置きかえる。
     * <p>
     * 文字列がnullの場合は、nullを返す。<br/>
     * 文字列が空文字の場合は、空文字を返す。
     *
     * @param s 対象文字列
     * @return 変換後の文字列
     */
    @Published
    public static String toUpperFirstChar(String s) {
        if (s == null) {
            return null;
        }

        return (s.length() == 0)
                ? ""
                : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 文字列の先頭文字を小文字に置きかえる。
     * <p>
     * 文字列がnullの場合は、nullを返す。<br/>
     * 文字列が空文字の場合は、空文字を返す。
     *
     * @param s 対象文字列
     * @return 変換後の文字列
     */
    public static String toLowerFirstChar(String s) {
        if (s == null) {
            return null;
        }

        return (s.length() == 0)
                ? ""
                : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
