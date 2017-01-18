package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nablarch.core.dataformat.convertor.FixedLengthConvertorSetting;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;

import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * 固定長レコードフォーマッタのシングルレイアウトを使用した場合のテスト。
 * <p>
 * 観点：
 * 固定長ファイルが、レイアウト定義ファイルの内容に伴って正しく読み書きできるかのテストを行う。
 * シングルレイアウファイルの読み書き、固定長ファイル関連のディレクティブの妥当性検証、
 * データタイプ（X、Nなど）が正常に使用されること、また、このクラスが担う異常系のテストを網羅する。
 * また、ゾーンビット（正/負）について、リポジトリのシステム共通定義を使用するパターンと、ディレクティブを使用する場合で動作テストを行う。
 *
 * @author Masato Inoue
 */
public class FixedLengthDataRecordFormatterSingleLayoutTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DataRecordFormatter formatter;

    /** フォーマッタを生成する。 */
    private DataRecordFormatter createFormatter(File formatFile) {
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(formatFile);
        formatter.initialize();
        return formatter;
    }

    private void createFile(File file, String encoding, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), encoding));
        try {
            for (final String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
        SystemRepository.clear();
    }

    @Test
    public void シングルレイアウトのファイルが読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "record-separator: \"\\n\"",
                "[Default]",
                "1    dataKbn       X(1)  \"2\"",
                "2   ?tegataNum     X(4)  \"9999\"",
                "6    syumoku       X(1)",
                "7    withEdi       X(1)  \"Y\"",
                "8    ?unused        X(7)  pad(\"0\")"
        );
        createFormatter(formatFile);

        final File inputFile = temporaryFolder.newFile();
        createFile(inputFile, "ms932",
                "299991Y0000000",
                "299992N0000001"
        );

        DataRecord record = formatter.setInputStream(new FileInputStream(inputFile))
                                     .initialize()
                                     .readRecord();

        assertThat(record.getRecordType(), is("Default"));
        assertThat(record.size(), is(3));
        assertThat(record.getRecordNumber(), is(1));

        assertThat((String) record.get("dataKbn"), is("2"));
        assertThat(record, not(hasKey("tegataNum")));
        assertThat((String) record.get("syumoku"), is("1"));
        assertThat((String) record.get("withEdi"), is("Y"));
        assertThat(record, not(hasKey("unused")));

        // 2レコード目
        assertThat(formatter.readRecord()
                            .getString("withEdi"), is("N"));

        // 3レコード目は無し
        assertThat(formatter.readRecord(), is(nullValue()));
    }

    @Test
    public void シングルレイアウトのファイルが書き込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 14",
                "record-separator: \"\\n\"",
                "[Default]",
                "1    dataKbn       X(1)  \"2\"",
                "2   ?tegataNum     X(4)  \"9999\"",
                "6    syumoku       X(1)",
                "7    withEdi       X(1)  \"Y\"",
                "8    ?unused        X(7)  pad(\"0\")"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        // write first record
        final Map<String, Object> recordMap = new HashMap<String, Object>();
        recordMap.put("syumoku", "2");
        recordMap.put("withEdi", "N");
        formatter.writeRecord(recordMap);

        // write second record
        recordMap.put("withEdi", "Y");
        formatter.writeRecord(recordMap);

        final byte[] actual = outputStream.toByteArray();

        // first record
        assertThat(Arrays.copyOfRange(actual, 0, 1), is("2".getBytes("ms932")));
        assertThat(Arrays.copyOfRange(actual, 1, 5), is("9999".getBytes("ms932")));
        assertThat(Arrays.copyOfRange(actual, 5, 6), is("2".getBytes("ms932")));
        assertThat(Arrays.copyOfRange(actual, 6, 7), is("N".getBytes("ms932")));
        assertThat(Arrays.copyOfRange(actual, 7, 14), is("0000000".getBytes("ms932")));

        // record-separator
        assertThat(Arrays.copyOfRange(actual, 14, 15), is("\n".getBytes("ms932")));

        // second record
        assertThat(Arrays.copyOfRange(actual, 21, 22), is("Y".getBytes("ms932")));
    }

    @Test
    public void 読み込みでのデータタイプ網羅テスト() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 110",
                "[Default]",
                "1    byteString     X (5)",
                "6    string         XN(5)",
                "11   wordString     N (10)",
                "21   zoneDigits     Z (10)",
                "31   signedZDigits  SZ(10)",
                "41   packedDigits   P (10)",
                "51   signedPDigits  SP(10)",
                "61   nativeBytes    B (10)",
                "71   zDecimalPoint  Z(5, 3)",
                "76   pDecimalPoint  P(3, 2)",
                "79  ?endMark        X(2)   \"00\"",
                "81   X9             X9(10) pad(\"X\")",
                "91   X92            X9(5)",
                "96   SX9            SX9(10) pad(\"X\")",
                "106  SX92           SX9(5)"
        );

        ByteBuffer buff = ByteBuffer.wrap(new byte[110]);
        buff.put("ｱｲｳ  ".getBytes("sjis")); //X(5)
        buff.put("ｱあ  ".getBytes("sjis")); // XN(5)
        buff.put("あいうえ　".getBytes("sjis"));  //N(10)
        buff.put("0034567890".getBytes("sjis")); //9(10)
        buff.put("000456789".getBytes("sjis"))   //S9(10)
            .put((byte) 0x70); // -1234567890
        buff.put(new byte[] {                    //P(10)
                0x00, 0x00, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x93
        }); // 1234567890123456789
        buff.put(new byte[] {                    //SP(10)
                0x02, 0x34, 0x56, 0x78, (byte) 0x90,
                0x12, 0x34, 0x56, 0x78, (byte) 0x97
        }); // -1234567890123456789
        buff.put(new byte[] {                    // B(10)
                (byte) 0x00, (byte) 0x00, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB,
                (byte) 0xAA, (byte) 0x99, (byte) 0x88, (byte) 0x77, (byte) 0x66,
        });
        buff.put("12345".getBytes("sjis"));      //99.999
        // = 12.345
        buff.put(new byte[] {                    //PPP.PP
                0x12, 0x34, 0x53
        }); // = 123.45
        buff.put("  ".getBytes());
        buff.put("XXXXX12345".getBytes());      // X9:12345
        buff.put("00005".getBytes());           // X9:5
        buff.put("-XXXX54321".getBytes());      // SX9:-54321
        buff.put("+0055".getBytes());           // SX9:55

        formatter = createFormatter(formatFile);
        formatter.setInputStream(new ByteArrayInputStream(buff.array()))
                 .initialize();

        assertThat("レコードがあること", formatter.hasNext(), is(true));
        DataRecord record = formatter.readRecord(); // #1
        assertThat("シングルバイト", record.getString("byteString"), is("ｱｲｳ"));
        assertThat("混在", record.getString("string"), is("ｱあ"));
        assertThat("非シングルバイト", record.getString("wordString"), is("あいうえ"));
        assertThat("ゾーン数値", record.getBigDecimal("zoneDigits"), is(new BigDecimal("34567890")));
        assertThat("符号付きゾーン数値", record.getBigDecimal("signedZDigits"), is(new BigDecimal("-4567890")));
        assertThat("パック数値", record.getBigDecimal("packedDigits"), is(new BigDecimal("567890123456789")));
        assertThat("符号付きパック数値", record.getBigDecimal("signedPDigits"), is(new BigDecimal("-234567890123456789")));
        assertThat("バイナリ", record.getBytes("nativeBytes"),
                is(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0xDD, (byte) 0xCC,
                        (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88, 0x77, 0x66}));
        assertThat(record.getBigDecimal("zDecimalPoint"), is(new BigDecimal("12.345")));
        assertThat(record.getBigDecimal("pDecimalPoint"), is(new BigDecimal("123.45")));
        assertThat("X9でパディング文字がトリムされる", record.getBigDecimal("X9"), is(new BigDecimal("12345")));
        assertThat("X9でデフォルトのパディング文字がトリムされる", record.getBigDecimal("X92"), is(new BigDecimal("5")));
        assertThat("SX9で指定したパディング文字がトリムされる", record.getBigDecimal("SX9"), is(new BigDecimal("-54321")));
        assertThat("SX9でデフォルトのパディング文字がトリムされる", record.getBigDecimal("SX92"), is(new BigDecimal("55")));
    }

    @Test
    public void 書き込みでのデータタイプ網羅テスト() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 79",
                "[Default]",
                "1    byteString     X (2)",
                "3    string         XN(8)",
                "11    wordString    N (4)",
                "15    zoneDigits    Z (5)",
                "20   signedZDigits  SZ(5)",
                "25   packedDigits   P (5)",
                "30   signedPDigits  SP(5)",
                "35   nativeBytes    B (5)",
                "40   zDecimalPoint  Z(5, 3)",
                "45   pDecimalPoint  P(3, 2)",
                "48  ?endMark        X(2)   \"00\"",
                "50   X9             X9(10) pad(\"X\")",
                "60   X92            X9(5)",
                "65   SX9            SX9(10) pad(\"X\")",
                "75   SX92           SX9(5)"
        );

        formatter = createFormatter(formatFile);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final Map<String, Object> output = new HashMap<String, Object>();
        output.put("byteString", "A");
        output.put("string", "ｱあ");
        output.put("wordString", "あ");
        output.put("zoneDigits", "1");
        output.put("signedZDigits", "-12");
        output.put("packedDigits", "123");
        output.put("signedPDigits", "-1234");
        output.put("nativeBytes", new byte[] {0x00, 0x00, 0x00, 0x00, 0x01});
        output.put("zDecimalPoint", "1.2");
        output.put("pDecimalPoint", "3.4");
        output.put("X9", "9");
        output.put("X92", "99");
        output.put("SX9", "-999");
        output.put("SX92", "+9999");
        formatter.writeRecord(output);

        final byte[] actual = outputStream.toByteArray();
        assertThat(new String(Arrays.copyOfRange(actual, 0, 2), "sjis"), is("A "));
        assertThat(new String(Arrays.copyOfRange(actual, 2, 10), "sjis"), is("ｱあ     "));
        assertThat(new String(Arrays.copyOfRange(actual, 10, 14), "sjis"), is("あ　"));
        assertThat(new String(Arrays.copyOfRange(actual, 14, 19), "sjis"), is("00001"));
        assertThat(Arrays.copyOfRange(actual, 19, 24), is(new byte[] {0x30, 0x30, 0x30, 0x31, 0x72}));
        assertThat(Arrays.copyOfRange(actual, 24, 29), is(new byte[] {0x00, 0x00, 0x00, 0x12, 0x33}));
        assertThat(Arrays.copyOfRange(actual, 29, 34), is(new byte[] {0x00, 0x00, 0x01, 0x23, 0x47}));
        assertThat(Arrays.copyOfRange(actual, 34, 39), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x01}));
        assertThat(Arrays.copyOfRange(actual, 39, 44), is(new byte[] {0x30, 0x30, 0x30, 0x31, 0x32}));
        assertThat(Arrays.copyOfRange(actual, 44, 47), is(new byte[] {0x00, 0x03, 0x43}));
        assertThat(new String(Arrays.copyOfRange(actual, 47, 49), "sjis"), is("00"));
        assertThat(new String(Arrays.copyOfRange(actual, 49, 59), "sjis"), is("XXXXXXXXX9"));
        assertThat(new String(Arrays.copyOfRange(actual, 59, 64), "sjis"), is("00099"));
        assertThat(new String(Arrays.copyOfRange(actual, 64, 74), "sjis"), is("-XXXXXX999"));
        assertThat(new String(Arrays.copyOfRange(actual, 74, 79), "sjis"), is("09999"));
    }

    @Test
    public void バイナリ以外の項目で空文字列を読み込む場合にnullまたは0として読み込まれること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 51",
                "[Default]",
                "1  byteString        X(2)",
                "3  wordString        N(4)",
                "7  zoneDigits        Z(5)",
                "12 signedZDigits     SZ(5)",
                "17 packedDigits      P(5)",
                "22 signedPDigits     SP(5)",
                "27 zDecimalPoint     Z(5, 3)",
                "32 pDecimalPoint     P(5, 2)",
                "37 numberString      X9(5)",
                "42 signedNString     SX9(5)",
                "47 string            XN(5)"
        );

        ByteBuffer buff = ByteBuffer.wrap(new byte[51]);
        buff.put("  ".getBytes("sjis"));
        buff.put("　　".getBytes("sjis"));
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put("00000".getBytes("sjis"));
        buff.put("00000".getBytes("sjis"));
        buff.put("     ".getBytes("sjis"));

        formatter = createFormatter(formatFile);
        formatter.setInputStream(new ByteArrayInputStream(buff.array()))
                 .initialize();
        DataRecord record = formatter.readRecord();
        assertThat(record.getString("byteString"), is(nullValue()));
        assertThat(record.getString("wordString"), is(nullValue()));
        assertThat(record.getBigDecimal("zoneDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedZDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("packedDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedPDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("zDecimalPoint"), is(new BigDecimal("0.000")));
        assertThat(record.getBigDecimal("pDecimalPoint"), is(new BigDecimal("00.00")));
        assertThat(record.getBigDecimal("numberString"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedNString"), is(BigDecimal.ZERO));
        assertThat(record.getString("string"), is(nullValue()));
    }

    @Test
    public void バイナリ以外の項目でnullを出力した場合データタイプのデフォルト値が出力されること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 80",
                "[Default]",
                "1    byteString     X (2)",
                "3   wordString     N (4)",
                "7   zoneDigits     Z (5)",
                "12   signedZDigits  SZ(5)",
                "17   packedDigits   P (5)",
                "22   signedPDigits  SP(5)",
                "27   nativeBytes    B (5)",
                "32   zDecimalPoint  Z(5, 3)",
                "37   pDecimalPoint  P(3, 2)",
                "40  ?endMark        X(2)   \"00\"",
                "42   X9             X9(10) pad(\"X\")",
                "52   X92            X9(5)",
                "57   SX9            SX9(10) pad(\"X\")",
                "67  SX92           SX9(5)",
                "72   string        XN(9)"
        );

        formatter = createFormatter(formatFile);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final Map<String, Object> output = new HashMap<String, Object>();
        output.put("nativeBytes", new byte[] {0x00, 0x00, 0x00, 0x00, 0x01});
        formatter.writeRecord(output);

        final byte[] actual = outputStream.toByteArray();
        assertThat("byteString", new String(Arrays.copyOfRange(actual, 0, 2), "sjis"), is("  "));
        assertThat("wordString", new String(Arrays.copyOfRange(actual, 2, 6), "sjis"), is("　　"));
        assertThat("zoneDigits", new String(Arrays.copyOfRange(actual, 6, 11), "sjis"), is("00000"));
        assertThat("signedZDigits", Arrays.copyOfRange(actual, 11, 16), is(new byte[] {0x30, 0x30, 0x30, 0x30, 0x30}));
        assertThat("packedDigits", Arrays.copyOfRange(actual, 16, 21), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x03}));
        assertThat("signedPDigits", Arrays.copyOfRange(actual, 21, 26), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x03}));
        assertThat("nativeBytes", Arrays.copyOfRange(actual, 26, 31), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x01}));
        assertThat("zDecimalPoint", Arrays.copyOfRange(actual, 31, 36), is(new byte[] {0x30, 0x30, 0x30, 0x30, 0x30}));
        assertThat("pDecimalPoint", Arrays.copyOfRange(actual, 36, 39), is(new byte[] {0x00, 0x00, 0x03}));
        assertThat("endMark", new String(Arrays.copyOfRange(actual, 39, 41), "sjis"), is("00"));
        assertThat("X9", new String(Arrays.copyOfRange(actual, 41, 51), "sjis"), is("XXXXXXXXX0"));
        assertThat("X92", new String(Arrays.copyOfRange(actual, 51, 56), "sjis"), is("00000"));
        assertThat("SX9", new String(Arrays.copyOfRange(actual, 56, 66), "sjis"), is("XXXXXXXXX0"));
        assertThat("SX92", new String(Arrays.copyOfRange(actual, 66, 71), "sjis"), is("00000"));
        assertThat("string", new String(Arrays.copyOfRange(actual, 71, 80), "sjis"), is("         "));

    }

    @Test
    public void バイナリ以外の全てのデータタイプでデフォルト値を出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 71",
                "required-decimal-point: false",
                "[Default]",
                "1    byteString     X(2)       \"a\"",
                "3    wordString     N(4)       \"あ\"",
                "7    zoneDigits     Z(5)       1",
                "12   signedZDigits  SZ(5)     -2",
                "17   packedDigits   P(5)       3",
                "22   signedPDigits  SP(5)     -4",
                "27   nativeBytes    B(5)",        // バイナリはデフォルト値指定ができない
                "32   zDecimalPoint  Z(5, 3)    12000",
                "37   pDecimalPoint  P(3, 2)    300",
                "40  ?endMark        X(2)       \"00\"",
                "42   X9             X9(10)     99 pad(\"X\")",
                "52   X92            X9(5)      88",
                "57   SX9            SX9(10)    -77 pad(\"X\")",
                "67   SX92           SX9(5)     66"
        );

        formatter = createFormatter(formatFile);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final Map<String, Object> output = new HashMap<String, Object>();
        output.put("nativeBytes", new byte[] {0x00, 0x00, 0x00, 0x00, 0x01});
        formatter.writeRecord(output);

        final byte[] actual = outputStream.toByteArray();
        assertThat(new String(Arrays.copyOfRange(actual, 0, 2), "sjis"), is("a "));
        assertThat(new String(Arrays.copyOfRange(actual, 2, 6), "sjis"), is("あ　"));
        assertThat(new String(Arrays.copyOfRange(actual, 6, 11), "sjis"), is("00001"));
        assertThat(Arrays.copyOfRange(actual, 11, 16), is(new byte[] {0x30, 0x30, 0x30, 0x30, 0x72}));
        assertThat(Arrays.copyOfRange(actual, 16, 21), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x33}));
        assertThat(Arrays.copyOfRange(actual, 21, 26), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x47}));
        assertThat(Arrays.copyOfRange(actual, 26, 31), is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x01}));
        assertThat(Arrays.copyOfRange(actual, 31, 36), is(new byte[] {0x31, 0x32, 0x30, 0x30, 0x30}));
        assertThat(Arrays.copyOfRange(actual, 36, 39), is(new byte[] {0x00, 0x30, 0x03}));
        assertThat(new String(Arrays.copyOfRange(actual, 39, 41), "sjis"), is("00"));
        assertThat(new String(Arrays.copyOfRange(actual, 41, 51), "sjis"), is("XXXXXXXX99"));
        assertThat(new String(Arrays.copyOfRange(actual, 51, 56), "sjis"), is("00088"));
        assertThat(new String(Arrays.copyOfRange(actual, 56, 66), "sjis"), is("-XXXXXXX77"));
        assertThat(new String(Arrays.copyOfRange(actual, 66, 71), "sjis"), is("00066"));

    }

    @Test
    public void textEncodingディレクティブが未定義の場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "record-length: 1",
                "[Default]",
                "1 x X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive 'text-encoding' was not specified.");
        createFormatter(formatFile);
    }

    @Test
    public void recordLengthディレクティブが未定義の場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "[Default]",
                "1 x X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive 'record-length' was not specified.");
        createFormatter(formatFile);
    }

    @Test
    public void textEncodingに非文字列を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: 1",
                "record-length: 1",
                "[Default]",
                "1 x X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive value was invalid type. the value of the directive 'text-encoding'"
                + " must be java.lang.String but was java.lang.Integer. ");
        createFormatter(formatFile);
    }

    @Test
    public void recordLengthディレクティブの値とレコードのフィールド長の合計が一致しない場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 1",
                "[Default]",
                "1 x X(2)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid record length was specified by 'record-length' directive."
                + " sum of length of fields must be '1' byte but was '2'. ");
        createFormatter(formatFile);
    }

    @Test
    public void マルチバイトフィールドに奇数バイトを指定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 3",
                "[Default]",
                "1 n N(3)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "invalid field size was specified. the length of DoubleByteCharacter data field must be a even number. field size=[3].");
        createFormatter(formatFile);
    }

    @Test
    public void レコード名称を設定しなかった場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 3",
                "",
                "1 n N(3)"
        );
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("encountered unexpected token. allowed token types are: RECORD_TYPE_HEADER ");
        createFormatter(formatFile);
    }

    @Test
    public void 不正なエスケープ文字を使用した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 8",
                "[strings]",
                "1 string1 X(1) pad(\"\\t\")",
                "2 string2 X(1) pad(\"\\f\")",
                "3 string3 X(1) pad(\"\\\"\")",
                "4 string4 X(1) pad(\"\\'\")",
                "5 string5 X(1) pad(\"\\r\")",
                "6 string6 X(1) pad(\"\\n\")",
                "7 string7 X(1) pad(\"\\\\\")",
                "8 string8 X(1) pad(\"\\o\")"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid escape sequence was specified. value=[\\o]");

        createFormatter(formatFile);
    }

    @Test
    public void recordLengthディレクティブの長さよりも実際のストリームの長さが短い場合は例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);
        final InputStream inputStream = new ByteArrayInputStream("1あ".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage(
                "invalid data record found. the length of a record must be 4 byte but read data was only 3 byte. record number=[1]. ");
        formatter.readRecord();
    }

    @Test
    public void 最終レコードがrecordLengthディレクティブに設定した長さより短い場合は例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);
        final InputStream inputStream = new ByteArrayInputStream("1あ \n2い".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        assertThat("最初のレコードは読み込める", formatter.readRecord(), is(notNullValue()));

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage(
                "invalid data record found. the length of a record must be 4 byte but read data was only 3 byte. record number=[2]. ");
        formatter.readRecord();
    }

    @Test
    public void 最終レコードに改行コードが存在しない場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);
        final InputStream inputStream = new ByteArrayInputStream("1あ \n2い ".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        assertThat("最初のレコードは読み込める", formatter.readRecord(), is(notNullValue()));

        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid record separator was specified by 'record-separator' directive.");
        formatter.readRecord();
    }

    @Test
    public void レコード区切りが存在しないファイルが出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("x", 1);
        record.put("n", "あ");
        formatter.writeRecord(record);
        record.put("x", 2);
        record.put("n", "い");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("1あ 2い "));
    }
    
    @Test
    public void レコード区切りが存在しないファイルを読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("1あ 2い ".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getString("x"), is("1"));
        assertThat(record1.getString("n"), is("あ"));
        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getString("x"), is("2"));
        assertThat(record2.getString("n"), is("い"));
        assertThat(formatter.readRecord(), is(nullValue()));
    }

    @Test
    public void レコード区切りがLFのファイルが出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("x", 1);
        record.put("n", "あ");
        formatter.writeRecord(record);
        record.put("x", 2);
        record.put("n", "い");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("1あ \n2い \n"));
    }
    
    @Test
    public void レコード区切りがLFのファイルが読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("1あ \n2い \n".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getString("x"), is("1"));
        assertThat(record1.getString("n"), is("あ"));
        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getString("x"), is("2"));
        assertThat(record2.getString("n"), is("い"));
        assertThat(formatter.readRecord(), is(nullValue()));
    }
    
    @Test
    public void レコード区切りがCRのファイルが出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("x", 1);
        record.put("n", "あ");
        formatter.writeRecord(record);
        record.put("x", 2);
        record.put("n", "い");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("1あ \r2い \r"));
    }

    @Test
    public void レコード区切りがCRのファイルが読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("1あ \r2い \r".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getString("x"), is("1"));
        assertThat(record1.getString("n"), is("あ"));
        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getString("x"), is("2"));
        assertThat(record2.getString("n"), is("い"));
        assertThat(formatter.readRecord(), is(nullValue()));
    }
    
    @Test
    public void レコード区切りがCRLFのファイルが出力できること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("x", 1);
        record.put("n", "あ");
        formatter.writeRecord(record);
        record.put("x", 2);
        record.put("n", "い");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("ms932"), is("1あ \r\n2い \r\n"));
    }

    @Test
    public void レコード区切りがCRLFのファイルが読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "[Default]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?x X(1)"
        );
        createFormatter(formatFile);

        final InputStream inputStream = new ByteArrayInputStream("1あ \r\n2い \r\n".getBytes("ms932"));
        formatter.setInputStream(inputStream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getString("x"), is("1"));
        assertThat(record1.getString("n"), is("あ"));
        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getString("x"), is("2"));
        assertThat(record2.getString("n"), is("い"));
        assertThat(formatter.readRecord(), is(nullValue()));
    }

    @Test
    public void ゾーン数値の符号ビット値_positiveZoneSignNibble_に不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "positive-zone-sign-nibble: \"11\"",
                "[data]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid sign nibble was specified by 'positive-zone-sign-nibble' directive. " +
                "value=[11]. sign nibble format must be [[0-9a-fA-F]].");
        createFormatter(formatFile);
    }

    @Test
    public void ゾーン数値の負の符号ビット値_negativeZoneSignNibble_に不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "positive-zone-sign-nibble: \"2\"",
                "negative-zone-sign-nibble: \"ff\"",
                "[data]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid sign nibble was specified by 'negative-zone-sign-nibble' directive. " +
                "value=[ff]. sign nibble format must be [[0-9a-fA-F]].");
        createFormatter(formatFile);
    }

    @Test
    public void ゾーン数値の符号ビット値を指定した場合その値で読み込みができること() throws Exception {
        
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "positive-zone-sign-nibble: \"4\"",
                "negative-zone-sign-nibble: \"8\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );

        createFormatter(formatFile);
        final byte[] bytes = new byte[12 * 2];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {0x31, 0x32, 0x33, 0x34}); // 1234
        byteBuffer.put(new byte[] {0x31, 0x32, 0x43});       // 123
        byteBuffer.put(new byte[] {0x39, 0x38, 0x37, (byte) 0x86});  // -9876
        byteBuffer.put("\n".getBytes("sjis"));
        byteBuffer.put(Arrays.copyOf(bytes, 12));

        final InputStream stream = new ByteArrayInputStream(bytes);
        formatter.setInputStream(stream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record1.getBigDecimal("sz1"), is(new BigDecimal("123")));
        assertThat(record1.getBigDecimal("sz2"), is(new BigDecimal("-9876")));
        
        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record2.getBigDecimal("sz1"), is(new BigDecimal("123")));
        assertThat(record2.getBigDecimal("sz2"), is(new BigDecimal("-9876")));

        assertThat(formatter.hasNext(), is(false));
        assertThat(formatter.readRecord(), is(nullValue()));
    }

    @Test
    public void ゾーン数値の符号ビット値を指定した場合その値で書き込みができること() throws Exception {

        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "positive-zone-sign-nibble: \"4\"",
                "negative-zone-sign-nibble: \"8\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );

        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("z", 1);
        record.put("sz1", 2);
        record.put("sz2", -3);
        formatter.writeRecord(record);
        
        final byte[] actual = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(actual, 0, 4), is(new byte[] {0x30, 0x30, 0x30, 0x31}));
        assertThat(Arrays.copyOfRange(actual, 4, 7), is(new byte[] {0x30, 0x30, 0x42}));
        assertThat(Arrays.copyOfRange(actual, 7, 11), is(new byte[] {0x30, 0x30, 0x30, (byte) 0x83}));
    }

    @Test
    public void ゾーン数値の符号ビット値をリポジトリに設定した場合その値で読み込みが使われるこ() throws Exception {
        final FixedLengthConvertorSetting convertorSetting = new FixedLengthConvertorSetting();
        convertorSetting.setDefaultPositiveZoneSignNibble("6");
        convertorSetting.setDefaultNegativeZoneSignNibble("5");
        convertorSetting.setDefaultPositivePackSignNibble("8");
        convertorSetting.setDefaultNegativePackSignNibble("7");
        
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> result = new HashMap<String, Object>();
                result.put("fixedLengthConvertorSetting", convertorSetting);
                return result;
            }
        });
        
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );

        createFormatter(formatFile);
        final byte[] bytes = new byte[12 * 2];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {0x31, 0x32, 0x33, 0x34}); // 1234
        byteBuffer.put(new byte[] {0x31, 0x32, 0x63});       // 123
        byteBuffer.put(new byte[] {0x39, 0x38, 0x37, (byte) 0x56});  // -9876
        byteBuffer.put("\n".getBytes("sjis"));
        byteBuffer.put(Arrays.copyOf(bytes, 12));

        final InputStream stream = new ByteArrayInputStream(bytes);
        formatter.setInputStream(stream)
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record1.getBigDecimal("sz1"), is(new BigDecimal("+123")));
        assertThat(record1.getBigDecimal("sz2"), is(new BigDecimal("-9876")));

        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record2.getBigDecimal("sz1"), is(new BigDecimal("+123")));
        assertThat(record2.getBigDecimal("sz2"), is(new BigDecimal("-9876")));

        assertThat(formatter.hasNext(), is(false));
        assertThat(formatter.readRecord(), is(nullValue()));
    }
    
    @Test
    public void ゾーン数値の符号ビット値をリポジトリに設定した場合その値で書き込みが使われるこ() throws Exception {
        final FixedLengthConvertorSetting convertorSetting = new FixedLengthConvertorSetting();
        convertorSetting.setDefaultPositiveZoneSignNibble("6");
        convertorSetting.setDefaultNegativeZoneSignNibble("5");
        convertorSetting.setDefaultPositivePackSignNibble("8");
        convertorSetting.setDefaultNegativePackSignNibble("7");

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> result = new HashMap<String, Object>();
                result.put("fixedLengthConvertorSetting", convertorSetting);
                return result;
            }
        });

        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );

        createFormatter(formatFile);


        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        
        final DataRecord record = new DataRecord();
        record.put("z", 1);
        record.put("sz1", 2);
        record.put("sz2", -3);
        formatter.writeRecord(record);

        final byte[] actual = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(actual, 0, 4), is(new byte[] {0x30, 0x30, 0x30, 0x31}));
        assertThat(Arrays.copyOfRange(actual, 4, 7), is(new byte[] {0x30, 0x30, 0x62}));
        assertThat(Arrays.copyOfRange(actual, 7, 11), is(new byte[] {0x30, 0x30, 0x30, (byte) 0x53}));
    }

    @Test
    public void EBCDICでゾーン数値が読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );
        createFormatter(formatFile);
        
        final byte[] bytes = new byte[12 * 2];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {(byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4}); // 1234
        byteBuffer.put(new byte[] {(byte) 0xF1, (byte) 0xF2, (byte) 0xC3});       // 123
        byteBuffer.put(new byte[] {(byte) 0xF9, (byte) 0xF8, (byte) 0xF7, (byte) 0xD6});  // -9876
        byteBuffer.put("\n".getBytes("IBM1047"));
        byteBuffer.put(Arrays.copyOf(bytes, 12));

        formatter.setInputStream(new ByteArrayInputStream(bytes))
                 .initialize();

        final DataRecord record1 = formatter.readRecord();
        assertThat(record1.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record1.getBigDecimal("sz1"), is(new BigDecimal("+123")));
        assertThat(record1.getBigDecimal("sz2"), is(new BigDecimal("-9876")));

        final DataRecord record2 = formatter.readRecord();
        assertThat(record2.getBigDecimal("z"), is(new BigDecimal("1234")));
        assertThat(record2.getBigDecimal("sz1"), is(new BigDecimal("+123")));
        assertThat(record2.getBigDecimal("sz2"), is(new BigDecimal("-9876")));

        assertThat(formatter.hasNext(), is(false));
        assertThat(formatter.readRecord(), is(nullValue()));
    }
    
    @Test
    public void EBCDICでゾーン数値が書き込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 11",
                "record-separator: \"\\n\"",
                "[data]",
                "1 z   Z(4)",
                "5 sz1 SZ(3)",
                "8 sz2 SZ(4)"
        );
        createFormatter(formatFile);


        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("z", 1);
        record.put("sz1", 2);
        record.put("sz2", -3);
        formatter.writeRecord(record);

        final byte[] actual = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(actual, 0, 4), is(new byte[] {(byte) 0xF0, (byte) 0xF0, (byte) 0xF0, (byte) 0xF1}));
        assertThat(Arrays.copyOfRange(actual, 4, 7), is(new byte[] {(byte) 0xF0, (byte) 0xF0, (byte) 0xC2}));
        assertThat(Arrays.copyOfRange(actual, 7, 11), is(new byte[] {(byte) 0xF0, (byte) 0xF0, (byte) 0xF0, (byte) 0xD3}));
    }

    @Test
    public void パック数値の符号ビット値_positivePackSignNibble_に不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "positive-pack-sign-nibble: \"11\"",
                "[data]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid sign nibble was specified by 'positive-pack-sign-nibble' directive. " +
                "value=[11]. sign nibble format must be [[0-9a-fA-F]].");
        createFormatter(formatFile);
    }

    @Test
    public void パック数値の負の符号ビット値_negativePackSignNibble_に不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 4",
                "record-separator: \"\\r\\n\"",
                "positive-pack-sign-nibble: \"2\"",
                "negative-pack-sign-nibble: \"ff\"",
                "[data]",
                "1 x X(1)",
                "2 n N(2)",
                "4 ?z X(1)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid sign nibble was specified by 'negative-pack-sign-nibble' directive. " +
                "value=[ff]. sign nibble format must be [[0-9a-fA-F]].");
        createFormatter(formatFile);
    }

    @Test
    public void 有効なパック数値の符号ビットを設定した場合その値で読み込みができること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "positive-pack-sign-nibble: \"4\"",
                "negative-pack-sign-nibble: \"8\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);
        final byte[] bytes = new byte[6];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {0x13});                  // 1
        byteBuffer.put(new byte[] {(byte) 0x98, 0x74});     // 987
        byteBuffer.put(new byte[] {0x12, 0x38});            // -123
        byteBuffer.put("\n".getBytes("sjis"));
        
        formatter.setInputStream(new ByteArrayInputStream(bytes))
                 .initialize();

        final DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("p"), is(new BigDecimal("1")));
        assertThat(record.getBigDecimal("sp1"), is(new BigDecimal("+987")));
        assertThat(record.getBigDecimal("sp2"), is(new BigDecimal("-123")));
    }
    
    @Test
    public void 有効なパック数値の符号ビットを設定した場合その値で出力ができること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "positive-pack-sign-nibble: \"4\"",
                "negative-pack-sign-nibble: \"8\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("p", 9);
        record.put("sp1", 30);
        record.put("sp2", -31);
        formatter.writeRecord(record);
        
        final byte[] actual = outputStream.toByteArray();

        assertThat(Arrays.copyOfRange(actual, 0, 1), is(new byte[] {(byte) 0x93}));
    }

    @Test
    public void パック数値の符号ビット値をリポジトリに設定した値で読み込めること() throws Exception {
        final FixedLengthConvertorSetting convertorSetting = new FixedLengthConvertorSetting();
        convertorSetting.setDefaultPositiveZoneSignNibble("6");
        convertorSetting.setDefaultNegativeZoneSignNibble("5");
        convertorSetting.setDefaultPositivePackSignNibble("8");
        convertorSetting.setDefaultNegativePackSignNibble("7");

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> result = new HashMap<String, Object>();
                result.put("fixedLengthConvertorSetting", convertorSetting);
                return result;
            }
        });
        
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);
        final byte[] bytes = new byte[6];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {0x13});                  // 1
        byteBuffer.put(new byte[] {(byte) 0x98, 0x78});     // 987
        byteBuffer.put(new byte[] {0x12, 0x37});            // -123
        byteBuffer.put("\n".getBytes("sjis"));

        formatter.setInputStream(new ByteArrayInputStream(bytes))
                 .initialize();

        final DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("p"), is(new BigDecimal("1")));
        assertThat(record.getBigDecimal("sp1"), is(new BigDecimal("+987")));
        assertThat(record.getBigDecimal("sp2"), is(new BigDecimal("-123")));
    }
    
    @Test
    public void パック数値の符号ビット値をリポジトリに設定した値で書き込めること() throws Exception {
        final FixedLengthConvertorSetting convertorSetting = new FixedLengthConvertorSetting();
        convertorSetting.setDefaultPositiveZoneSignNibble("6");
        convertorSetting.setDefaultNegativeZoneSignNibble("5");
        convertorSetting.setDefaultPositivePackSignNibble("8");
        convertorSetting.setDefaultNegativePackSignNibble("7");

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> result = new HashMap<String, Object>();
                result.put("fixedLengthConvertorSetting", convertorSetting);
                return result;
            }
        });

        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("p", "1");
        record.put("sp1", "+1");
        record.put("sp2", "-2");
        formatter.writeRecord(record);

        final byte[] actual = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(actual, 0, 1), is(new byte[] {0x13}));
        assertThat(Arrays.copyOfRange(actual, 1, 3), is(new byte[] {0x00, 0x18}));
        assertThat(Arrays.copyOfRange(actual, 3, 5), is(new byte[] {0x00, 0x27}));
    }

    @Test
    public void EBCDICでパック数値が読み込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        
        createFormatter(formatFile);
        final byte[] bytes = new byte[6];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(new byte[] {0x1F});                  // 1
        byteBuffer.put(new byte[] {(byte) 0x98, 0x7C});     // 987
        byteBuffer.put(new byte[] {0x12, 0x3D});            // -123
        byteBuffer.put("\n".getBytes("IBM1047"));

        formatter.setInputStream(new ByteArrayInputStream(bytes))
                 .initialize();

        final DataRecord record = formatter.readRecord();
        assertThat(record.getBigDecimal("p"), is(new BigDecimal("1")));
        assertThat(record.getBigDecimal("sp1"), is(new BigDecimal("+987")));
        assertThat(record.getBigDecimal("sp2"), is(new BigDecimal("-123")));
        
    }
    
    @Test
    public void EBCDICでパック数値が書き込めること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );

        createFormatter(formatFile);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        
        final DataRecord record = new DataRecord();
        record.put("p", "1");
        record.put("sp1", "+1");
        record.put("sp2", "-2");
        formatter.writeRecord(record);

        final byte[] actual = outputStream.toByteArray();
        assertThat(Arrays.copyOfRange(actual, 0, 1), is(new byte[] {0x1F}));
        assertThat(Arrays.copyOfRange(actual, 1, 3), is(new byte[] {0x00, 0x1C}));
        assertThat(Arrays.copyOfRange(actual, 3, 5), is(new byte[] {0x00, 0x2D}));
    }

    @Test
    public void レコードタイプにnullを指定してレコードを出力した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);
        formatter.setOutputStream(new ByteArrayOutputStream())
                 .initialize();

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("record type was blank. record type must not be blank.");
        formatter.writeRecord(null, new HashMap<String, Object>());
    }

    @Test
    public void レコードタイプに空文字列を指定してレコードを出力した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"IBM1047\"",
                "record-length: 5",
                "record-separator: \"\\n\"",
                "[data]",
                "1 p   P(1)",
                "2 sp1 SP(2)",
                "4 sp2 SP(2)"
        );
        createFormatter(formatFile);
        formatter.setOutputStream(new ByteArrayOutputStream())
                 .initialize();

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("record type was blank. record type must not be blank.");
        formatter.writeRecord("", new HashMap<String, Object>());
    }

    @Test
    public void 初期化前にレコードを読み込んだ場合例外が送出されること() throws Exception {
        final FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("input stream was not set. input stream must be set before reading.");
        formatter.readRecord();
    }

    @Test
    public void 初期化前にレコードを出力した場合例外が送出されること() throws Exception {
        final FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("output stream was not set. output stream must be set before writing.");
        formatter.writeRecord(new HashMap<String, Object>());
    }
    
    @Test
    public void 初期化前にレコードタイプ指定でレコードを出力した場合例外が送出されること() throws Exception {
        final FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("output stream was not set. output stream must be set before writing.");
        formatter.writeRecord("type", new HashMap<String, Object>());
    }

    @Test
    public void decimalPointがデフォルトの場合X9とSX9に小数点が出力されること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "-1.4");
        formatter.writeRecord(record);

        assertThat(outputStream.toString("sjis"), is("01.12-1.40"));
    }

    @Test
    public void decimalPointをoffにした場合X9とSX9に小数点が出力されないこと() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");

        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "required-decimal-point: false",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "-12.4");
        formatter.writeRecord(record);

        assertThat("小数点は出力されない", outputStream.toString("sjis"), is("00112-1240"));
    }
    
    @Test
    public void decimalPointに不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "record-separator: \"\\n\"",
                "required-decimal-point: \"a\"",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive value was invalid type."
                + " the value of the directive 'required-decimal-point' must be java.lang.Boolean but was java.lang.String. ");
        createFormatter(formatFile);
    }

    @Test
    public void fixedSignPositionがデフォルト設定の場合符号は先頭に付加されること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "-1.4");
        formatter.writeRecord(record);

        assertThat("符号は先頭につく", outputStream.toString("sjis"), is("01.12-1.40"));
    }

    @Test
    public void fixedSignPositionをoffに設定した場合符号はパディング文字より前に設定されること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "fixed-sign-position: false",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "-01.4");
        formatter.writeRecord(record);
        assertThat("符号はパディング文字の前につく", outputStream.toString("sjis"), is("01.120-1.4"));
    }

    @Test
    public void fixedSignPositionに不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "record-separator: \"\\n\"",
                "fixed-sign-position: \"a\"",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive value was invalid type." 
                + " the value of the directive 'fixed-sign-position' must be java.lang.Boolean but was java.lang.String. format definition ");
        createFormatter(formatFile);
    }

    @Test
    public void requiredPlusSignがデフォルトの場合正の符号は付加されないこと() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "fixed-sign-position: false",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "01.4");
        formatter.writeRecord(record);
        assertThat("符号はパディング文字の前につく", outputStream.toString("sjis"), is("01.12001.4"));
    }
    
    @Test
    public void requiredPlusSignをonにした場合正の符号が付加されること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "fixed-sign-position: false",
                "required-plus-sign: true",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 1)"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        final DataRecord record = new DataRecord();
        record.put("X9", "1.12");
        record.put("SX9", "01.4");
        formatter.writeRecord(record);
        assertThat("符号はパディング文字の前につく", outputStream.toString("sjis"), is("01.120+1.4"));
    }

    @Test
    public void requiredPlusSignに不正な値を設定した場合例外が送出されること() throws Exception {
        final File formatFile = temporaryFolder.newFile();
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"ms932\"",
                "record-length: 10",
                "record-separator: \"\\n\"",
                "required-plus-sign: \"a\"",
                "[Default]",
                "1   X9             X9(5, 2) ",
                "6   SX9            SX9(5, 2)"
        );

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("directive value was invalid type." 
                + " the value of the directive 'required-plus-sign' must be java.lang.Boolean but was java.lang.String. ");
        createFormatter(formatFile);
    }
    
    @Test
    public void X9とSX9でnumber系コンバータを設定した場合でnullを出力した場合デフォルト値が書き込まれること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 10",
                "fixed-sign-position: false",
                "[Default]",
                "1   X9             X9(5, 2)   number",
                "6   SX9            SX9(5, 1)  signed_number"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();
        formatter.writeRecord(new DataRecord());
        assertThat(outputStream.toString("sjis"), is("00.00000.0"));
    }

    @Test
    public void closeで出力ストリームが閉じられること() throws Exception {
        final FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

        final AtomicBoolean closed = new AtomicBoolean(false);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed.set(true);
            }
        };
        formatter.setOutputStream(stream);
        formatter.close();

        assertThat(closed.get(), is(true));
    }
    
    @Test
    public void 初期差されていない状態でhasNextを呼び出した場合falseが戻されること() throws Exception {
        assertThat(new FixedLengthDataRecordFormatter().hasNext(), is(false));
    }

    @Test
    public void closeで入力ストリームが閉じられること() throws Exception {
        final FixedLengthDataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

        final AtomicBoolean closed = new AtomicBoolean(false);
        final ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                closed.set(true);
            }
        };
        formatter.setInputStream(stream);
        formatter.close();

        assertThat(closed.get(), is(true));
    }

    @Test
    public void 空文字列をnullに変換しない設定で各データタイプが空文字列もしくは０を返すこと() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/core/dataformat/convertor/ConvertorSettingCompatible.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        final File formatFile = temporaryFolder.newFile("format.fmt");
        createFile(formatFile, "utf-8",
                "file-type: \"Fixed\"",
                "text-encoding: \"sjis\"",
                "record-length: 51",
                "[Default]",
                "1  byteString        X(2)",
                "3  wordString        N(4)",
                "7  zoneDigits        Z(5)",
                "12 signedZDigits     SZ(5)",
                "17 packedDigits      P(5)",
                "22 signedPDigits     SP(5)",
                "27 zDecimalPoint     Z(5, 3)",
                "32 pDecimalPoint     P(5, 2)",
                "37 numberString      X9(5)",
                "42 signedNString     SX9(5)",
                "47 string            XN(5)"
        );

        ByteBuffer buff = ByteBuffer.wrap(new byte[51]);
        buff.put("  ".getBytes("sjis"));
        buff.put("　　".getBytes("sjis"));
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30});
        buff.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x03});
        buff.put("00000".getBytes("sjis"));
        buff.put("00000".getBytes("sjis"));
        buff.put("     ".getBytes("sjis"));

        formatter = createFormatter(formatFile);
        formatter.setInputStream(new ByteArrayInputStream(buff.array()))
                .initialize();
        DataRecord record = formatter.readRecord();
        assertThat(record.getString("byteString"), is(""));
        assertThat(record.getString("wordString"), is(""));
        assertThat(record.getBigDecimal("zoneDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedZDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("packedDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedPDigits"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("zDecimalPoint"), is(new BigDecimal("0.000")));
        assertThat(record.getBigDecimal("pDecimalPoint"), is(new BigDecimal("00.00")));
        assertThat(record.getBigDecimal("numberString"), is(BigDecimal.ZERO));
        assertThat(record.getBigDecimal("signedNString"), is(BigDecimal.ZERO));
        assertThat(record.getString("string"), is(""));

        SystemRepository.clear();
    }
}
