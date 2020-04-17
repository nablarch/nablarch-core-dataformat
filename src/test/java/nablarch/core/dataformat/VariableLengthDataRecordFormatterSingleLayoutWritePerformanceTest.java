package nablarch.core.dataformat;

import nablarch.core.util.Builder;
import nablarch.test.support.tool.Hereis;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;

/**
 * 可変長ファイルフォーマッタの出力速度を計るテスト。
 *
 * <p>
 *  普段はテストメソッドをIgnoreにし、速度を計るときのみ有効にする。
 * </p>
 * @author Masaya Seko
 */

public class VariableLengthDataRecordFormatterSingleLayoutWritePerformanceTest {
    /** 実行回数*/
    private final int LOOP_NUM = 20;
    /**1カラム文字数*/
    private final int CHAR_NUM = 100;
    /** 1ファイルの行数*/
    private final int FILE_LINE = 10;

    private String LS = Builder.LS;

    /**
     * フォーマットファイル生成。
     */
    @Before
    public void setUp() {
        String qt = "\\\"";
        String fs = ",";
        String rs = escapeRs("\n");
        String encoding = "UTF-8";

        File formatFile = Hereis.file("./format.dat", qt, fs, rs, encoding);
        /***********************************************************
         file-type:        "Variable"
         text-encoding:     "$encoding" # 文字列型フィールドの文字エンコーディング
         record-separator:  "$rs"       # レコード区切り文字
         field-separator:   "$fs"       # フィールド区切り文字
         quoting-delimiter: "$qt"       # クオート文字

         [DataRecord]
         1   FIcode        X             # 振込先金融機関コード
         2   FIname        X             # 振込先金融機関名称
         3   officeCode    X             # 振込先営業所コード
         4   officeName    X             # 振込先営業所名
         5   syumoku       X             # 預金種目
         6   accountNum    X             # 口座番号
         7   recipientName X             # 受取人名
         8   amount        X             # 振込金額
         9   isNew         X             # 新規コード
         10  ediInfo       N             # EDI情報
         11  transferType  X             # 振込区分
         12 ?withEdi       X             # EDI情報使用フラグ
         ************************************************************/
        formatFile.deleteOnExit();
    }

    private String escapeRs(String recordSeparator) {
        return recordSeparator.replace("\r", "\\r").replace("\n", "\\n");
    }

    /**
     * 速度を計る
     * @throws Exception
     */
    @Test
    @Ignore
    public void testMeasureSpeed() throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < LOOP_NUM; i++) {
            writeOneFile(i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("合計時間：" + (endTime - startTime) + " ms");
        System.out.println("1ファイル平均時間：" + ((double) (endTime - startTime) / LOOP_NUM) + " ms");
    }

    /**
     * １ファイル出力する。
     * @param count 何ファイル目か
     * @throws Exception
     */
    private void writeOneFile(int count) throws Exception {
        StringBuilder outputSb = new StringBuilder();
        for (int i = 0; i < CHAR_NUM; i++) {
            outputSb.append("a");
        }
        final String outputString = outputSb.toString();

        DataRecord dataRecord = new DataRecord() {{
            put("FIcode", outputString);
            put("FIname", outputString);
            put("officeCode", outputString);
            put("officeName", outputString);
            put("syumoku", outputString);
            put("accountNum", outputString);
            put("recipientName", outputString);
            put("amount", outputString);
            put("isNew", outputString);
            put("ediInfo", outputString);
            put("transferType", outputString);
            put("withEdi", outputString);
        }};

        File outputData = new File("./output" + count + ".dat");
        outputData.deleteOnExit();

        DataRecordFormatter formatter = createFormatter("./format.dat");

        OutputStream dest = new FileOutputStream(outputData, false);
        formatter.setOutputStream(dest).initialize();
        for (int i = 0; i < FILE_LINE; i++) {
            formatter.writeRecord(dataRecord);
        }
        dest.close();
    }

    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(String filePath) {
        return FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(new File(filePath));
    }

}
