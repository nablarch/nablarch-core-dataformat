package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.text.IsEqualIgnoringWhiteSpace;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.CharacterReplacementConfig;
import nablarch.core.dataformat.CharacterReplacementManager;
import nablarch.core.dataformat.CharacterReplacementResult;
import nablarch.core.dataformat.CharacterReplacementUtil;
import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link CharacterReplacer}の機能結合テスト。
 *
 * @author Masato Inoue
 */
public class CharacterReplacerIntegrationTest {

    private DataRecordFormatter formatter;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ThreadContext.clear();
        SystemRepository.clear();

        final FormatterFactory factory = new FormatterFactory();
        factory.setCacheLayoutFileDefinition(false);
        final HashMap<String, String> defaultReplacement = new HashMap<String, String>();
        defaultReplacement.put("X", "type_hankaku");
        factory.setDefaultReplacementType(defaultReplacement);

        final CharacterReplacementConfig zenkaku = new CharacterReplacementConfig();
        zenkaku.setTypeName("type_zenkaku");
        zenkaku.setFilePath("classpath:nablarch/core/dataformat/type_zenkaku.properties");
        zenkaku.setEncoding("ms932");
        zenkaku.setByteLengthCheck(true);

        final CharacterReplacementConfig zenkakuSurrogatePairUtf8 = new CharacterReplacementConfig();
        zenkakuSurrogatePairUtf8.setTypeName("type_zenkaku_surrogate_pair_utf8");
        zenkakuSurrogatePairUtf8.setFilePath("classpath:nablarch/core/dataformat/type_zenkaku_surrogate_pair_utf8.properties");
        zenkakuSurrogatePairUtf8.setEncoding("utf-8");
        zenkakuSurrogatePairUtf8.setByteLengthCheck(true);


        final CharacterReplacementConfig hankaku = new CharacterReplacementConfig();
        hankaku.setTypeName("type_hankaku");
        hankaku.setFilePath("classpath:nablarch/core/dataformat/type_hankaku.properties");
        hankaku.setEncoding("ms932");
        hankaku.setByteLengthCheck(true);

        final CharacterReplacementConfig hankaku2 = new CharacterReplacementConfig();
        hankaku2.setEncoding("ms932");
        hankaku2.setByteLengthCheck(true);
        hankaku2.setFilePath("classpath:nablarch/core/dataformat/type_hankaku_field.properties");
        hankaku2.setTypeName("type_hankaku2");

        final CharacterReplacementManager characterReplacementManager = new CharacterReplacementManager();
        characterReplacementManager.setConfigList(new ArrayList<CharacterReplacementConfig>() {{
            add(zenkaku);
            add(zenkakuSurrogatePairUtf8);
            add(hankaku);
            add(hankaku2);
        }});

        characterReplacementManager.initialize();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final HashMap<String, Object> result = new HashMap<String, Object>();
                result.put("characterReplacementManager", characterReplacementManager);
                result.put("formatterFactory", factory);
                return result;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
        SystemRepository.clear();
    }


    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(file);
    }

    private void createFile(File formatFile, String encoding, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(formatFile), encoding));
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

    private String concat(String... strings) {
        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
            builder.append('"')
                   .append(string)
                   .append('"')
                   .append(',');
        }
        return builder.substring(0, builder.length() - 1);
    }

    @Test
    public void シングルバイト文字の読み込み時の寄せ字ができること() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 X",
                "2  str2 X    replacement(\"type_hankaku\")",
                "3  str3 N"
        );

        final File inputFile = temporaryFolder.newFile("inputFile.csv");
        createFile(inputFile, "ms932",
                "001,DEF,あ髙﨑",
                "002,G\\~,え唖か"
        );

        createFormatter(formatFile);
        formatter.setInputStream(new FileInputStream(inputFile));
        formatter.initialize();

        // ~ → [ 、 \ → [ 、に寄せ字変換されることの確認
        final DataRecord first = formatter.readRecord();
        assertThat(first.getString("str1"), is("001"));
        assertThat(first.getString("str2"), is("DEF"));
        assertThat(first.getString("str3"), is("あ髙﨑"));
        assertThat(CharacterReplacementUtil.getResult("str2")
                                           .isReplacement(), is(false));

        final DataRecord second = formatter.readRecord();
        assertThat(second.getString("str1"), is("002"));
        assertThat(second.getString("str2"), is("G[["));
        assertThat(second.getString("str3"), is("え唖か"));

        final CharacterReplacementResult str1 = CharacterReplacementUtil.getResult("str1");
        assertThat(str1.isReplacement(), is(false));

        final CharacterReplacementResult str2 = CharacterReplacementUtil.getResult("str2");
        assertThat(str2.isReplacement(), is(true));
        assertThat(str2.getInputString(), is("G\\~"));
        assertThat(str2.getResultString(), is("G[["));

        final CharacterReplacementResult str3 = CharacterReplacementUtil.getResult("str3");
        assertThat(str3, is(nullValue()));
    }

    @Test
    public void 非シングルバイト文字の読み込み時寄せ字ができること() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 X",
                "2  str2 X",
                "3  str3 N replacement(\"type_zenkaku\")"
        );

        final File inputFile = temporaryFolder.newFile();
        createFile(inputFile, "ms932",
                "001,DEF,あ髙﨑",
                "002,G\\~,え唖か");

        createFormatter(formatFile);

        formatter.setInputStream(new FileInputStream(inputFile))
                 .initialize();

        final DataRecord record = formatter.readRecord();
        assertThat(record.getString("str1"), is("001"));
        assertThat(record.getString("str2"), is("DEF"));
        assertThat(record.getString("str3"), is("あ高崎"));

        final CharacterReplacementResult str3 = CharacterReplacementUtil.getResult("str3");
        assertThat(str3.isReplacement(), is(true));
        assertThat(str3.getInputString(), is("あ髙﨑"));
        assertThat(str3.getResultString(), is("あ高崎"));
    }

    @Test
    public void サロゲートペア文字の読み込み時寄せ字ができること() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"utf-8\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 N replacement(\"type_zenkaku_surrogate_pair_utf8\")"
        );

        final File inputFile = temporaryFolder.newFile();
        createFile(inputFile, "utf-8",
                "\uD840\uDC0B");

        createFormatter(formatFile);

        formatter.setInputStream(new FileInputStream(inputFile))
                .initialize();

        final DataRecord record = formatter.readRecord();
        assertThat(record.getString("str1"), is("\uD844\uDE3D"));

        final CharacterReplacementResult str1 = CharacterReplacementUtil.getResult("str1");
        assertThat(str1.isReplacement(), is(true));
        assertThat(str1.getInputString(), is("\uD840\uDC0B"));
        assertThat(str1.getResultString(), is("\uD844\uDE3D"));
    }


    @Test
    public void 書き込み時の寄せ字ができること() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 X",
                "2  str2 X replacement(\"type_hankaku\")",
                "3  str3 N replacement(\"type_zenkaku\")"
        );

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record1 = new DataRecord();
        record1.put("str1", "001");
        record1.put("str2", "DEF");
        record1.put("str3", "あ髙﨑");

        final DataRecord record2 = new DataRecord();
        record2.put("str1", "002");
        record2.put("str2", "G\\~");
        record2.put("str3", "え唖か");

        formatter.writeRecord(record1);
        formatter.writeRecord(record2);

        final String actual = outputStream.toString("ms932");
        assertThat(actual, IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace(
                concat("001", "DEF", "あ高崎")
                        + '\n' + concat("002", "G[[", "え■か")));
    }

    @Test
    public void サロゲートペアの書き込み時に寄せ字ができること() throws Exception {

        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"utf-8\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 N replacement(\"type_zenkaku_surrogate_pair_utf8\")"
        );

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream)
                .initialize();

        final DataRecord record1 = new DataRecord();
        record1.put("str1", "\uD840\uDC0B");
        formatter.writeRecord(record1);

        final String actual = outputStream.toString("utf-8");
        assertThat(actual, IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace("\uD844\uDE3D"));
    }

    @Test
    public void データタイプによるデフォルトの寄せ字変換が行えること() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile, "utf-8", "",
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  str1 X replacement(\"type_hankaku2\")",
                "2  str2 X",              // データタイプによる寄せ字が行われる
                "3  str3 N replacement(\"type_zenkaku\")"
        );

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        createFormatter(formatFile);
        formatter.setOutputStream(outputStream)
                 .initialize();

        final DataRecord record1 = new DataRecord();
        record1.put("str1", "001\\");
        record1.put("str2", "DEF");
        record1.put("str3", "あ髙﨑");

        final DataRecord record2 = new DataRecord();
        record2.put("str1", "002");
        record2.put("str2", "G\\~");
        record2.put("str3", "え唖か");

        formatter.writeRecord(record1);
        formatter.writeRecord(record2);

        final String actual = outputStream.toString("ms932");
        assertThat(actual, IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace(
                concat("001Z", "DEF", "あ高崎")
                        + '\n' + concat("002", "G[[", "え■か")));
    }
}
