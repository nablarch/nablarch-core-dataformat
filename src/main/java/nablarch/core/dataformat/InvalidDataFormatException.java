package nablarch.core.dataformat;

import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 入力データおよび出力データの不正により処理が継続できないことを示す例外クラス。
 * 
 * @author Iwauo Tajima
 */
@Published
public class InvalidDataFormatException extends RuntimeException {
    
    /** エラーが発生したレコード番号 */
    private int recordNumber;
    
    /** エラーが発生したフィールド名 */
    private String fieldName;

    /** 例外発生原因となった入出力元（ファイルなど）のパス */
    private String sourcePath;

    /** 使用していたフォーマットファイルのパス */
    private String formatFilePath;

    /**
     * エラーメッセージを使用して、{@code InvalidDataFormatException}を生成する。
     * @param message エラーメッセージ
     */
    public InvalidDataFormatException(String message) {
        super(message);
    }

    /**
     * エラーメッセージと起因となった例外を使用して、{@code InvalidDataFormatException}を生成する。
     * @param message エラーメッセージ
     * @param throwable 起因となった例外
     */
    public InvalidDataFormatException(String message, Throwable throwable) {
        super(message, throwable);
    }
    
    /**
     * エラーメッセージを返却する。
     * <p/>
     * エラーメッセージには、以下のうち設定されている項目のみが含まれる。
     * <ul>
     *     <li>例外発生原因となった入出力元（ファイルなど）のパス</li>
     *     <li>エラーが発生したフィールド名</li>
     *     <li>エラーが発生したレコード番号</li>
     *     <li>使用していたフォーマットファイルのパス</li>
     * </ul>
     *
     * @return エラーメッセージ
     */
    @Override
    public String getMessage() {
        StringBuilder msg = new StringBuilder(super.getMessage());
        if (StringUtil.hasValue(sourcePath)) {
            msg.append(" source=[").append(sourcePath).append("].");
        }
        if (StringUtil.hasValue(fieldName)) {
            msg.append(" field name=[").append(fieldName).append("].");
        } 
        if (recordNumber != 0) {
            msg.append(" record number=[").append(recordNumber).append("].");
        }
        if (StringUtil.hasValue(formatFilePath)) {
            msg.append(" format file=[").append(formatFilePath).append("].");
        }
        return msg.toString();
    }

    /**
     * エラーが発生したレコード番号を設定する。
     * @param recordNumber エラーが発生したレコード番号
     * @return このオブジェクト自体
     */
    public InvalidDataFormatException setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
        return this;
    }
    
    /**
     * エラーが発生したフィールド名を設定する。
     * @param fieldName フィールド名
     * @return このオブジェクト自体
     */
    public InvalidDataFormatException setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    /**
     * 例外発生原因となった入出力元（ファイルなど）のパスを設定する。
     * @param sourcePath 入出力元のパス
     * @return このオブジェクト自体
     */
    public InvalidDataFormatException setInputSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    /**
     * 例外発生原因となった入出力元（ファイルなど）のパスを取得する。
     * @return 入出力元のパス（設定されていない場合は{@code null}）
     */
    public String getInputSourcePath() {
        return sourcePath;
    }

    /**
     * 入出力時に使用していたフォーマットファイルのパスを取得する。
     * @return フォーマットファイルのパス（設定されていない場合は{@code null}）
     */
    public String getFormatFilePath() {
        return formatFilePath;
    }

    /**
     * 使用していたフォーマットファイルのパスを設定する。
     * @param formatFilePath フォーマットファイルのパス
     * @return このオブジェクト自体
     */
    public InvalidDataFormatException setFormatFilePath(String formatFilePath) {
        this.formatFilePath = formatFilePath;
        return this;
    }

    /**
     * エラーが発生したフィールド名を取得する。
     * @return フィールド名（設定されていない場合は{@code null}）
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * エラーが発生したレコード番号を取得する。
     * @return レコード番号（設定されていない場合は0）
     */
    public int getRecordNumber() {
        return recordNumber;
    }
}
