
// MOVE: coreをモジュール分割したので、nablarch.common.handlerから移動(当該パッケージはnablarch-core-jdbcimplが使用中)。
package nablarch.common.io;

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
            FileRecordWriterHolder.init();
            return ctx.handleNext(data);
        } finally {
            FileRecordWriterHolder.closeAll();
        }
    }
    
    
}
