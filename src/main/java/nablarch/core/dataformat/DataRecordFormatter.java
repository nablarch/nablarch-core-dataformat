package nablarch.core.dataformat;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * データファイルとJavaオブジェクトのシリアライズ／デシリアライズを行うクラスが実装するインタフェース。
 * @author Iwauo Tajima
 */
@Published
public interface DataRecordFormatter extends Closeable {
    /**
     * 入力ストリームから1レコード分のデータを読み込み、データレコードを返却する。
     * 入力ストリームが既に終端に達していた場合は{@code null}を返却する。
     * 
     * @return データレコード
     * @throws IOException 入力ストリームの読み込みに失敗した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    DataRecord readRecord()
    throws IOException, InvalidDataFormatException;
    
    /**
     * 出力ストリームに1レコード分の内容を書き込む。
     * <p/>
     * 出力時に使用するデータレイアウト（レコードタイプ）は、{@link Map}の内容をもとに自動的に判定される。
     * <p/>
     * 引数が{@link DataRecord}型かつレコードタイプが指定されている場合、
     * フォーマット定義ファイルのレコードタイプ識別フィールド定義よりも、指定されたレコードタイプを優先して書き込みを行う。
     *
     * @param record 出力するレコードの内容を格納したMap
     * @throws IOException 出力ストリームの書き込みに失敗した場合
     * @throws InvalidDataFormatException 書き込むデータの内容がフォーマット定義に違反している場合
     */
    void writeRecord(Map<String, ?> record)
    throws IOException, InvalidDataFormatException;
    
    /**
     * 指定したデータレイアウト（レコードタイプ）で、出力ストリームに1レコード分の内容を書き込む。
     * 
     * @param recordType レコードタイプ
     * @param record 出力するレコードの内容を格納したMap
     * @throws IOException 出力ストリームの書き込みに失敗した場合
     * @throws InvalidDataFormatException 書き込むデータの内容がフォーマット定義に違反している場合
     */
    void writeRecord(String recordType, Map<String, ?> record)
    throws IOException, InvalidDataFormatException;

    /**
     * 初期化処理を行う。
     *
     * @return  本クラスのインスタンス
     */
    DataRecordFormatter initialize();
    
    /**
     * 入力ストリームを設定する。
     *
     * @param stream 入力ストリーム
     * @return 本クラスのインスタンス
     */
    DataRecordFormatter setInputStream(InputStream stream);

    /**
     * 内部的に保持している各種リソースを開放する。
     */
    void close();
    
    /**
     * フォーマット定義ファイルの情報を保持するクラスを設定する。
     *
     * @param definition フォーマット定義ファイルの定義情報
     * @return 本クラスのインスタンス
     */
    DataRecordFormatter setDefinition(LayoutDefinition definition);
    

    /**
     * 出力ストリームを設定する。
     *
     * @param stream 出力ストリーム
     * @return 本クラスのインスタンス
     */
    DataRecordFormatter setOutputStream(OutputStream stream);

    /**
     * 次に読み込む行の有無を判定する。
     *
     * @return 次に読み込む行がある場合{@code true}
     * @throws IOException 入力ストリームの読み込みに失敗した場合
     */
    boolean hasNext() throws IOException;

    /**
     * 読み込みまたは書き込み中のレコードのレコード番号を返却する。
     *
     * @return レコード番号
     */
    int getRecordNumber();
}
