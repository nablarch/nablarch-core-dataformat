package nablarch.core.dataformat;

/**
 * 汎用データフォーマット機能の設定クラス。
 *
 * @author Kiyohito Itoh
 */
public class DataFormatConfig {

    private boolean flushEachRecordInWriting = true;

    /**
     * レコードの書き込み毎にflushをするか否かを取得する。
     *
     * デフォルトはtrue。
     *
     * @return レコードの書き込み毎にflushする場合はtrue、しない場合はfalse
     */
    public boolean isFlushEachRecordInWriting() {
        return flushEachRecordInWriting;
    }

    /**
     * レコードの書き込み毎にflushをするか否かを設定する。
     * @param flushEachRecordInWriting レコードの書き込み毎にflushする場合はtrue、しない場合はfalse
     */
    public void setFlushEachRecordInWriting(boolean flushEachRecordInWriting) {
        this.flushEachRecordInWriting = flushEachRecordInWriting;
    }
}
