package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.dataformat.convertor.FixedLengthConvertorFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ゾーン10進整数データタイプのテスト
 * 
 * @author Iwauo Tajima
 */
public class ZonedDecimalTest {
    
    private FieldDefinition field;
    private FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
    private DataRecordFormatter formatter = null;

    /** フォーマッタ(read)を生成する。 */
    private void createReadFormatter(File filePath, InputStream source) {
        formatter = new FormatterFactory().setCacheLayoutFileDefinition(false).createFormatter(filePath).setInputStream(source).initialize();
    }

    @After
    public void tearDown() throws Exception {
        if(formatter != null) {
            formatter.close();
        }
    }
    /** フォーマッタ(write)を生成する。 */
    private DataRecordFormatter createWriteFormatter(File filePath, OutputStream dest) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath);
        formatter.setOutputStream(dest).initialize();
        return formatter;
    }

    /**
     * フィールドごとのパラメータでゾーンビット（正/負）の値を渡して、正常に動作することの確認。
     */
    @Test
    public void testNibble() throws Exception {
    {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  signedZDigits  SZ(10, "", "3", "7")    # 正が1、負が9
        11  signedZDigits2  SZ(10, "", "4", "6")   # 正が1、負が9
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        byte[] bytes = new byte[30];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        
        buff.put("123456789".getBytes("sjis"))   //S9(10)
            .put((byte) 0x70); // -1234567890  // 0x70が負を表す
        buff.put("123456789".getBytes("sjis"))   //S9(10)
        .put((byte) 0x40); // 1234567890   // 0x40が正を表す

        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes);
        dest.write(bytes);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(2, record.size());
        assertEquals(new BigDecimal("-1234567890"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("+1234567890"),          record.get("signedZDigits2"));
        
        
        source.close();
        new File("record.dat").deleteOnExit();
        }
    }
    
    /**
     * ASCII規格での符号なしゾーン10進のテスト
     */
    @Test
    public void testUnsignedZonedDigitType_ASCII() throws Exception {

        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 5, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;

        byte zoneNibble = (byte) (Charset.forName("sjis").encode("1").get() & 0xF0);
        zonedType.setZoneNibble(zoneNibble);
        
        byte[] bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x35
        };
        
        BigDecimal value = zonedType.convertOnRead(bytes);
        assertEquals(12345, value.intValue());
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal(12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        
        // ゾーンビットが崩れていた場合。
        bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, (byte)0x75
        };
        
        try {
            zonedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    
    /**
     * ASCII規格での符号つきゾーン10進のテスト
     */
    @Test
    public void testSignedZonedDigitType_ASCII() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("sjis"));
        
        byte zoneNibble = (byte) 0x30;
        
        DataType<BigDecimal, byte[]> t  = factory.typeOf("SZ",field, 5, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;
 
        zonedType.setZoneNibble(zoneNibble);
        zonedType.setZoneSignNibblePositive(3);
        zonedType.setZoneSignNibbleNegative(7);
        
        byte[] bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x35
        };
        
        BigDecimal value = zonedType.convertOnRead(bytes);
        assertEquals(12345, value.intValue());
        
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal(12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x75
        };
        
        value = zonedType.convertOnRead(bytes);
        assertEquals(-12345, value.intValue());
        
        convertOnWrite = zonedType.convertOnWrite(new BigDecimal(-12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        // ゾーンビットが崩れていた場合。
        bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, (byte)0xF5
        };
        try {
            zonedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    
    /**
     * EBCDIC規格での符号なしゾーン10進のテスト
     */
    @Test
    public void testUnsignedZonedDigitType_EBCDIC() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("IBM1047"));

        byte zoneNibble = (byte) 0xF0;
        byte zoneSignNibblePositive = (byte) 0xC0;
        byte zoneSignNibbleNegative = (byte) 0xD0;

        DataType<BigDecimal, byte[]> t  = factory.typeOf("Z", field, 5, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;
        
        zonedType.setZoneNibble(zoneNibble);
        zonedType.setZoneSignNibblePositive(Integer.valueOf(zoneSignNibblePositive));
        zonedType.setZoneSignNibbleNegative(Integer.valueOf(zoneSignNibbleNegative));
        
        
        byte[] bytes = new byte[] {
            (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5
        };
        
        BigDecimal value = zonedType.convertOnRead(bytes);
        assertEquals(12345, value.intValue());
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal(12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        // ゾーンビットが崩れていた場合。
        bytes = new byte[] {
            (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0x35
        };
        try {
            zonedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    /**
     * EBCDIC規格での符号つきゾーン10進のテスト
     */
    @Test
    public void testSignedZonedDigitType_EBCDIC() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("Cp1047"));

        
        byte zoneNibble = (byte) 0xF0;

        DataType<BigDecimal, byte[]> t = factory.typeOf("SZ", field, 5, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;
        
        zonedType.setZoneNibble(zoneNibble);
        zonedType.setZoneSignNibblePositive(Integer.parseInt("C", 16));
        zonedType.setZoneSignNibbleNegative(Integer.parseInt("D", 16));
        
        
        byte[] bytes = new byte[] {
            (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xC5
        };
        
        BigDecimal value = zonedType.convertOnRead(bytes);
        assertEquals(12345, value.intValue());
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal(12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        bytes = new byte[] {
            (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xD5
        };
        
        value = zonedType.convertOnRead(bytes);
        assertEquals(-12345, value.intValue());
        
        convertOnWrite = zonedType.convertOnWrite(new BigDecimal(-12345));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        // ゾーンビットが崩れていた場合。
        bytes = new byte[] {
            (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5
        };
        try {
            zonedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    /**
     * パディング・トリム処理のテスト
     */
    @Test
    public void testPaddingAndTrimming() throws Exception {

        byte zoneNibble = (byte) 0x30;
        field  = new FieldDefinition();
        
        field.setEncoding(Charset.forName("sjis"));

        // --------------------- 正数 ------------------------ //
        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 10, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;
        
        zonedType.setZoneNibble(zoneNibble);
        zonedType.setZoneSignNibblePositive(3);
        zonedType.setZoneSignNibbleNegative(7);
        
        byte[] b12345 = new byte[] {
            0x30, 0x30, 0x30, 0x30, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35
        };
        
        BigDecimal value = zonedType.convertOnRead(b12345);
        
        assertEquals(12345, value.intValue());
        
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal(12345));
        
        assertEquals(10, convertOnWrite.length);
        assertTrue(isSameSequence(b12345, convertOnWrite));
        
        
        // --------------------- 負数 ------------------------ //
         t = factory.typeOf("SZ", field, 10, 0);
         zonedType = (ZonedDecimal)t;

         zonedType.setZoneNibble(zoneNibble);
         zonedType.setZoneSignNibblePositive(3);
         zonedType.setZoneSignNibbleNegative(7);
         
        
        byte[] bN12345 = new byte[] {
            0x30, 0x30, 0x30, 0x30, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x75
        };
        
        value = zonedType.convertOnRead(bN12345);
        
        assertEquals(-12345, value.intValue());
        
        convertOnWrite = zonedType.convertOnWrite(new BigDecimal(-12345));
        
        assertEquals(10, convertOnWrite.length);
        assertTrue(isSameSequence(bN12345, convertOnWrite));
    }
    
    /**
     * 仮想小数点のテスト
     */
    @Test
    public void testVirtualDecimalPoint() throws Exception {

        byte zoneNibble = (byte) 0x30;
        
        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("sjis"));
        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 5, 3);
        ZonedDecimal zonedType = (ZonedDecimal)t;

        zonedType.setZoneNibble(zoneNibble);
        
        byte[] bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x35
        };
        
        BigDecimal value = zonedType.convertOnRead(bytes);
        assertEquals("12.345", value.toString());
        
        bytes = new byte[] {
            0x31, 0x32, 0x33, 0x34, 0x35,
        };
        
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal("12.345"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
    }

    /**
     * 不正なパラメータのテスト。
     */
    @Test
    public void testInvalidParameter() throws Exception {
        
        /**
         * 引数が空
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  signedZDigits  SZ()    # 正が1、負が9
        11  signedZDigits2  SZ(10, "", "4", "6")   # 正が1、負が9
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");

        InputStream source = new BufferedInputStream(
                new FileInputStream("./record.dat"));

        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "parameter was not specified. parameter must be specified. " +
                            "convertor=[ZonedDecimal]."));

        }
        
        
        /**
         * 引数が数値型でない
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  signedZDigits  SZ("a")    # 正が1、負が9
        11  signedZDigits2  SZ(10, "", "4", "6")   # 正が1、負が9
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");

        source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. " +
                            "1st parameter type must be 'Integer' " +
                            "but was: 'java.lang.String'. " +
                            "parameter=[a]. convertor=[ZonedDecimal]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }
        

        /**
         * 入力時にコンバータの例外がキャッチされて、レコード番号、フィールド名が付与されることの確認。
         */
        formatFile = Hereis.file("./format2.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "sjis"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  signedZDigits  SZ(10, "", "5", "6")    # 正が1、負が9
        11  signedZDigits2  SZ(10, "", "4", "6")   # 正が1、負が9
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        dest.write("123456789".getBytes("sjis"));   // S9(10)
        dest.write((byte) 0x70);                    // 0x70が負を表す
        dest.write("123456789".getBytes("sjis"));   // S9(10));
        dest.write((byte) 0x40);                    // 0x40が正を表す
        source = new ByteArrayInputStream(dest.toByteArray());

        createReadFormatter(new File("format2.fmt"), source);
        try {
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith("invalid zone bits was specified."));
            assertThat(e.getRecordNumber(), is(1));
            assertThat(e.getFieldName(), is("signedZDigits"));
        }
        
        
        /**
         * 引数がnull。
         */
        
        ZonedDecimal zonedDecimal = new ZonedDecimal();
        try {
            zonedDecimal.initialize(null, "hoge");
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("1st parameter was null. parameter=[null, hoge]. convertor=[ZonedDecimal].", e.getMessage());
        }
        
    }

        
    private boolean isSameSequence(byte[] a , byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for(int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 空文字を入力するテスト。
     */
    @Test
    public void testReadEmpty() {
        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 0, 0);
        ZonedDecimal decimal = (ZonedDecimal)t;
        byte zoneNibble = (byte) (Charset.forName("sjis").encode("1").get() & 0xF0);
        decimal.setZoneNibble(zoneNibble);

        // 空文字 の場合
        assertThat(decimal.convertOnRead("".getBytes()), is(new BigDecimal("0")));
    }

    /**
     * null と 空文字を出力するテスト。
     * デフォルト値(0)を出力する。
     */
    @Test
    public void testWriteValidSingularValue() {
        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 5, 0);
        ZonedDecimal decimal = (ZonedDecimal)t;
        byte zoneNibble = (byte) (Charset.forName("sjis").encode("1").get() & 0xF0);
        decimal.setZoneNibble(zoneNibble);

        // null の場合
        assertThat(decimal.convertOnWrite(null), is("00000".getBytes()));
        // 空文字 の場合
        assertThat(decimal.convertOnWrite(""), is("00000".getBytes()));
    }

    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * InvalidDataFormatExceptionがスローされる。
     */
    @Test
    public void testWriteObjectNotBigDecimal() {
        ZonedDecimal decimal = new ZonedDecimal();

        try {
            decimal.convertOnWrite("abc");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[abc]."));
        }
        
        /*
         * オブジェクトの場合
         */
        try {
            decimal.convertOnWrite(new Object());
            fail();
        } catch (InvalidDataFormatException e) {
            assertSame(NumberFormatException.class, e.getCause().getClass());
            assertThat(e.getMessage(), startsWith("invalid parameter was specified. parameter must be able to convert to BigDecimal."));
        }
    }

    /**
     * 出力時のパラメータがnullの場合にデフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type:    "Fixed"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "sjis"

         # 各レコードの長さ
         record-length: 10

         # データレコード定義
         [Default]
         1  signedZDigits  SZ(10, "", "3", "7")   123
         ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                .addBasePathSetting("format", "file:./")
                .addFileExtensions("format", "fmt");

        DataRecord record = new DataRecord() {{
            put("signedZDigits", null);
        }};

        OutputStream dest = new FileOutputStream("./record.dat", false);
        createWriteFormatter(new File("format.fmt"), dest);
        formatter.writeRecord(record);

        formatter.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        byte[] actual = new byte[10];
        source.read(actual);

        assertThat(actual, is("0000000123".getBytes()));

        source.close();
        new File("record.dat").deleteOnExit();
    }
    
    /**
     * 出力時の最大値／最小値のテスト。
     * 桁数が不正な場合、InvalidDataFormatExceptionがスローされる。
     */
    @Test
    public void testUnsignedZonedDigitType_ASCII_Long() throws Exception {

        /*
         * 符号なしゾーン10進のテスト。
         */
        DataType<BigDecimal, byte[]> t = factory.typeOf("Z", field, 18, 0);
        ZonedDecimal zonedType = (ZonedDecimal)t;

        byte zoneNibble = (byte) (Charset.forName("sjis").encode("1").get() & 0xF0);
        zonedType.setZoneNibble(zoneNibble);
        
        byte[] bytes = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39
        };

        /*
         * 【正常系】正数のパターン：999999999999999999（18桁）はOK。
         */
        byte[] convertOnWrite = zonedType.convertOnWrite(new BigDecimal("999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数のパターン：1000000000000000000（19桁）はNG。
         */
        try {
            zonedType.convertOnWrite("1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000]."));
        }
        
        
        /*
         * 【正常系】正数＆小数点のパターン：99999999999.9999999（小数点を除くと18桁）はOK。
         */
        convertOnWrite = zonedType.convertOnWrite(new BigDecimal("99999999999.9999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数＆小数点のパターン：1000000000000.000000（小数点を除くと19桁）はNG。
         */
        try {
            zonedType.convertOnWrite("1000000000000.000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000]."));
        }
        
        bytes = new byte[] {
                0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
        };

        /*
         * 【正常系】正数＆小数点（スケール5）のパターン：1000000000000[.00000]（スケールを追加すると18桁）はOK。
         */
        convertOnWrite = zonedType.convertOnWrite(new BigDecimal("1000000000000").setScale(5));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【正常系】正数＆小数点（スケール6）のパターン：1000000000000[.000000]（スケールを追加すると19桁）はNG。
         */
        try {
            convertOnWrite = zonedType.convertOnWrite(new BigDecimal("1000000000000").setScale(6));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000]."));
        }
        
        
        /*
         * 符号ありゾーン10進のテスト。
         */
        DataType<BigDecimal, byte[]> st = factory.typeOf("SZ", field, 18, 0);
        SignedZonedDecimal signedZoneType = (SignedZonedDecimal)st;

        zoneNibble = (byte) 0x30;
        signedZoneType.setZoneNibble(zoneNibble);
        signedZoneType.setZoneSignNibblePositive(4);
        signedZoneType.setZoneSignNibbleNegative(7);
        
        bytes = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x49
        };

        /*
         * 【正常系】正数のパターン：999999999999999999（18桁）はOK。
         */
        convertOnWrite = signedZoneType.convertOnWrite(new BigDecimal("999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数のパターン：1000000000000000000（18桁）はNG。
         */
        try {
            signedZoneType.convertOnWrite("1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000]."));
        }
        
        
        bytes = new byte[] {
                0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x39, 0x79
        };

        /*
         * 【正常系】負数のパターン：999999999999999999（18桁）はOK。
         */
        convertOnWrite = signedZoneType.convertOnWrite(new BigDecimal("-999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【正常系】負数＆小数点のパターン：-99999999999.9999999（18桁）はOK。
         */
        convertOnWrite = signedZoneType.convertOnWrite(new BigDecimal("-99999999999.9999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));

        
        /*
         * 【異常系】負数のパターン：1000000000000000000（19桁）はNG。
         */
        try {
            signedZoneType.convertOnWrite("-1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[-1000000000000000000]."
            ));
        }
        
        /*
         * 【異常系】負数＆小数点のパターン：-10000000000000.00000（19桁）はNG。
         */
        try {
            signedZoneType.convertOnWrite("-10000000000000.00000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[-1000000000000000000], original parameter=[-10000000000000.00000]."));
        }
    }
    
    
}
