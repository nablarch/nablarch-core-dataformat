package nablarch.core.dataformat;

import nablarch.core.dataformat.LayoutFileParser.Token;
import nablarch.core.dataformat.LayoutFileParser.TokenType;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import static nablarch.core.log.Logger.LS;

/**
 * フォーマット定義ファイルの内容に問題がある場合に送出される実行時例外。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class SyntaxErrorException extends RuntimeException {
    
    /** 構文エラーの発生箇所 */
    private Token token = null;
    
    /** 構文エラーが発生したフォーマット定義ファイルのパス */
    private String filePath = "";
    
    /** エラーメッセージ **/
    private String message = "";
    
    /**
     * コンストラクタ。
     * @param message 構文エラーメッセージ
     */
    public SyntaxErrorException(String message) {
        this.message = message;
    }
    
    /**
     * コンストラクタ。
     * @param message 構文エラーメッセージ
     * @param throwable 例外
     */
    public SyntaxErrorException(String message, Throwable throwable) {
        super(message, throwable);
        this.message = message;
    }

    /**
     * コンストラクタ。
     * @param expectedTypes 構文上許容されている後続トークン
     */
    public SyntaxErrorException(TokenType... expectedTypes) {
        StringBuilder report = new StringBuilder(
            "encountered unexpected token. allowed token types are: "
        );
        for (TokenType type : expectedTypes) {
            report.append(type.toString())
                  .append(" ");
        }
        this.message = report.toString();
    }

    
    @Override
    public String getMessage() {
        if (token == null) {
            if (StringUtil.hasValue(filePath)) {
                return Builder.concat(message, " format definition file=[", filePath, "]");
            }
            return message;
        }
        return "invalid file format: in \""  + filePath + "\""
             + " at line: " + token.beginLine()
             + " col: " + token.beginColumn()
             + LS + "token: " + token.image()
             + "(" + token.type().toString() + ")"
             + LS + message;
    }

    /**
     * 構文エラーの発生箇所を返却する。
     * @return 構文エラーの発生箇所
     */
    public Token getToken() {
        return token;
    }
    
    /**
     * 構文エラーの発生箇所を設定する。
     * @param t 構文エラーの発生箇所
     * @return このオブジェクト自体
     */
    public SyntaxErrorException setToken(Token t) {
        token = t;
        return this;
    }
    
    /**
     * 構文エラーが発生したフォーマット定義ファイルのパスを設定する。
     * @param path フォーマット定義ファイルのパス
     * @return このオブジェクト自体
     */
    public SyntaxErrorException setFilePath(String path) {
        filePath = path;
        return this;
    }

    /**
     * 構文エラーが発生したフォーマット定義ファイルのパスを取得する。
     * @return 構文エラーが発生したフォーマット定義ファイルのパス
     */
    public String getFilePath() {
        return filePath;
    }
}
