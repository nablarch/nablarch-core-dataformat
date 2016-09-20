
// MOVE: coreをモジュール分割したので、nablarch.common.handlerから移動(当該パッケージはnablarch-core-jdbcimplが使用中)。
package nablarch.common.io;

import nablarch.common.io.FileRecordWriterHolder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;

/**
 * 後続のハンドラの実行が終了した後に、
 * カレントスレッド上で管理されているファイルレコードライタ（{@link nablarch.core.dataformat.FileRecordWriter}）が保持するストリームのクローズ
 * およびDataRecordWriterのインスタンスを削除するクラス。
 * 
 * 本ハンドラが自動的にストリームのクローズを行うので、
 * 通常、業務アプリケーションでファイルレコードライタを扱う際に、ストリームをクローズする必要はない。
 * @author Masato Inoue
 */
public class FileRecordWriterDisposeHandler implements Handler<Object, Object>  {

    /**
     * 後続のハンドラの実行が終了した後に、
     * カレントスレッド上で管理されているファイルレコードライタが保持するストリームのクローズおよび
     * DataRecordWriterのインスタンスを削除する。
     * @param data 入力データ
     * @param ctx 実行コンテキスト
     * @return 処理結果データ
     */
    public Object handle(Object data, ExecutionContext ctx) {
        try {
            // マルチスレッド環境化で実行されるバッチ対応
            // 事前に親スレッド側で、closeAllを呼び出すことで、子スレッド側で開いたファイルもclose対象となる。
            // ※FileRecordWriterHolder内部で親スレッドで生成されたThreadLoacalを共有しているため、
            // 親スレッド側で事前にThreadLocal#getを呼び出しておく必要が有るため
            FileRecordWriterHolder.closeAll();
            return ctx.handleNext(data);
        } finally {
            FileRecordWriterHolder.closeAll();
        }
    }
    
    
}
