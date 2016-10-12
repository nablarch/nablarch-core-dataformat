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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

import static nablarch.test.StringMatcher.endsWith;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * パック10進数のデータタイプコンバータのテスト。
 * 
 * 観点：
 * 正常系は、フォーマッタのテストで確認しているので、ここでは異常系、およびオプション設定関連のテストを行う。
 *   ・全角文字のパディング、トリムが行われること。
 *   ・パディング・トリム処理のテスト。
 *   
 * @author Masato Inoue
 */
public class PackedDecimalTest {

    private FieldDefinition field;
    private FixedLengthConvertorFactory factory = new FixedLengthConvertorFactory();
    
    private DataRecordFormatter formatter = null;
    
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
     * ASCII規格での符号なしパック10進のテスト
     */
    @Test
    public void testUnsignedPackedDigitType() throws Exception {
        
        DataType<BigDecimal, byte[]> t = factory.typeOf("P", field, 5, 0);
        PackedDecimal packedType = (PackedDecimal)t;

        byte packNibble = (byte) ((Charset.forName("sjis").encode("1").get() & 0xF0) >>> 4);
        packedType.setPackNibble(packNibble);
        
        byte[] bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x13
        };
        
        BigDecimal value = packedType.convertOnRead(bytes);
        assertEquals(87654321, value.intValue());
        
        
        byte[] convertedByte = packedType.convertOnWrite(new BigDecimal(87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
        
        bytes = new byte[] {
            (byte) 0x98, 0x76, 0x54, 0x32, 0x13
        };
            
        value = packedType.convertOnRead(bytes);
        assertEquals(987654321, value.intValue());
        
        convertedByte = packedType.convertOnWrite(new BigDecimal(987654321));
        
        assertTrue(isSameSequence(bytes, convertedByte));
        
        
        // 符号ビットが不正な場合
        bytes = new byte[] {
            (byte) 0x98, 0x76, 0x54, 0x32, 0x1C
        };
        try {
            packedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    /**
     * ASCII規格での符号ありパック10進のテスト
     */
    @Test
    public void testSignedPackedDigitType() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("sjis"));

        byte packNibble = (byte) 0x03;
        
        DataType<BigDecimal, byte[]> t = factory.typeOf("SP", field, 5, 0);
        PackedDecimal packedType = (PackedDecimal)t;
        packedType.setPackNibble(packNibble);
        packedType.setPackSignNibblePositive(3);
        packedType.setPackSignNibbleNegative(7);
        
        byte[] bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x13
        };
        
        BigDecimal value = packedType.convertOnRead(bytes);
        assertEquals(87654321, value.intValue());
        
        byte[] convertedByte = packedType.convertOnWrite(new BigDecimal(87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
        
        bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x17
        };
            
        value = packedType.convertOnRead(bytes);
        assertEquals(-87654321, value.intValue());
        
        convertedByte = packedType.convertOnWrite(new BigDecimal(-87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
        
        

        // 符号ビットが不正な場合
        bytes = new byte[] {
                0x08, 0x76, 0x54, 0x32, 0x16
            };
        try {
            packedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    

    /**
     * EBCDIC規格での符号なしパック10進のテスト
     */
    @Test
    public void testUnsignedPackedDigitType_EBCDIC() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("IBM1047"));

        byte packNibble = (byte) 0x0F;
        byte packSignNibblePositive = (byte) 0x0C;
        byte packSignNibbleNegative = (byte) 0x0D;
        
        DataType<BigDecimal, byte[]> t = factory.typeOf("P", field, 5, 0);
        PackedDecimal packedType = (PackedDecimal)t;

        packedType.setPackNibble(packNibble);
        packedType.setPackSignNibblePositive(Integer.valueOf(packSignNibblePositive));
        packedType.setPackSignNibbleNegative(Integer.valueOf(packSignNibbleNegative));
        
        byte[] bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x1F
        };
        
        BigDecimal value = packedType.convertOnRead(bytes);
        assertEquals(87654321, value.intValue());
        
        
        byte[] convertedByte = packedType.convertOnWrite(new BigDecimal(87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
        
        bytes = new byte[] {
            (byte) 0x98, 0x76, 0x54, 0x32, 0x1F
        };
            
        value = packedType.convertOnRead(bytes);
        assertEquals(987654321, value.intValue());
        
        convertedByte = packedType.convertOnWrite(new BigDecimal(987654321));
        
        assertTrue(isSameSequence(bytes, convertedByte));
        
        
        // 符号ビットが不正な場合
        bytes = new byte[] {
            (byte) 0x98, 0x76, 0x54, 0x32, 0x1C
        };
        try {
            packedType.convertOnRead(bytes);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataFormatException);
        }
    }
    
    /**
     * EBCDIC規格での符号ありパック10進のテスト
     */
    @Test
    public void testSignedPackedDigitType_EBCDIC() throws Exception {

        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("sjis"));

        byte packNibble = (byte) 0xF0;
        
        DataType<BigDecimal, byte[]> t = factory.typeOf("SP", field, 5, 0);
        PackedDecimal packedType = (PackedDecimal)t;
        packedType.setPackNibble(packNibble);
        packedType.setPackSignNibblePositive(Integer.parseInt("C", 16));
        packedType.setPackSignNibbleNegative(Integer.parseInt("D", 16));
        
        byte[] bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x1C
        };
        
        BigDecimal value = packedType.convertOnRead(bytes);
        assertEquals(87654321, value.intValue());
        
        byte[] convertedByte = packedType.convertOnWrite(new BigDecimal(87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
        
        bytes = new byte[] {
            0x08, 0x76, 0x54, 0x32, 0x1D
        };
            
        value = packedType.convertOnRead(bytes);
        assertEquals(-87654321, value.intValue());
        
        convertedByte = packedType.convertOnWrite(new BigDecimal(-87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
    }
    
    /**
     * パディング・トリム処理のテスト
     */
    @Test
    public void testPaddingAndTrimming() throws Exception {
        byte packNibble = (byte) 0x03;
        field  = new FieldDefinition();
        field.setEncoding(Charset.forName("sjis"));
        
        DataType<BigDecimal, byte[]> t = factory.typeOf("P", field, 10, 0);
        PackedDecimal packedType = (PackedDecimal)t;
        packedType.setPackNibble(packNibble);
        packedType.setPackSignNibblePositive(3);
        packedType.setPackSignNibbleNegative(7);

        byte[] bytes = new byte[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 
            0x08, 0x76, 0x54, 0x32, 0x13
        };
        
        BigDecimal value = packedType.convertOnRead(bytes);
        assertEquals(87654321, value.intValue());
        
        byte[] convertedByte = packedType.convertOnWrite(new BigDecimal(87654321));
        assertTrue(isSameSequence(bytes, convertedByte));
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
    
    

    /** フォーマッタ(read)を生成する。 */
    private void createReadFormatter(File filePath, InputStream source) {
        formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).createFormatter(filePath).setInputStream(source).initialize();
    }

    /**
     * 正常系のテスト。レイアウト定義ファイルからパラメータを設定する。
     */
    @Test
    public void testNormal() throws Exception {

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
        1  signedZDigits  SP(10, "", "7", "4")    # 
        11  signedZDigits2  SP(10, "", "7", "4")   # 
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        byte[] bytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x14
            };        
        byte[] bytes2 = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x17
            };        
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes2);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(2, record.size());
        assertEquals(new BigDecimal("-87654321"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("87654321"),          record.get("signedZDigits2"));
        
        source.close();
        new File("record.dat").deleteOnExit();
        
    }
    
    
    /**
     * 不正なパラメータを設定する。
     */
    @Test
    public void testInvalidParameter() throws Exception {

        /**
         * パラメータが空
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
        1  signedZDigits  SP()    # 
        11  signedZDigits2  SP(10, "", "7", "4")   # 
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        byte[] bytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x14
            };        
        byte[] bytes2 = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x17
            };        
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes2);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch (SyntaxErrorException e){
            assertThat(e.getMessage(), startsWith(
                    "parameter was not specified. parameter must be specified. convertor=[PackedDecimal]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }

        /**
         * パラメータが数値でない。
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
        1  signedZDigits  SP("a")    # 
        11  signedZDigits2  SP(10, "", "7", "4")   # 
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        bytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x14
            };        
        bytes2 = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x17
            };        
        
        dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes2);
        dest.close();

        source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        try {
            createReadFormatter(new File("format.fmt"), source);
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter type was specified. " +
                            "1st parameter type must be 'Integer' but was: " +
                            "'java.lang.String'. parameter=[a]. " +
                            "convertor=[PackedDecimal]."));
            assertThat(e.getFilePath(), endsWith("format.fmt"));
        }

        /**
         * scaleが数値でない（普通にscaleが無視されれて処理が行われる）
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
        1  signedZDigits  SP(10, "a", "7", "4")    # 
        11  signedZDigits2  SP(10, "", "7", "4")   # 
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
        .addBasePathSetting("format", "file:./")
        .addFileExtensions("format", "fmt");

        bytes = new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 
        0x08, 0x76, 0x54, 0x32, 0x14
        };        
        bytes2 = new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 
        0x08, 0x76, 0x54, 0x32, 0x17
        };        
        
        dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes2);
        dest.close();
        
        source = new BufferedInputStream(
        new FileInputStream("record.dat"));
        
        createReadFormatter(new File("format.fmt"), source);
        DataRecord record = formatter.readRecord();
        
        assertEquals(2, record.size());
        assertEquals(new BigDecimal("-87654321"),          record.get("signedZDigits"));
        assertEquals(new BigDecimal("87654321"),          record.get("signedZDigits2"));
        
        source.close();
        new File("record.dat").deleteOnExit();

        /**
         * 引数がnull。
         */
        PackedDecimal dataType = new PackedDecimal();
        try {
            dataType.initialize(null, null);
            fail();
        } catch (SyntaxErrorException e) {
            assertEquals("1st parameter was null. parameter=[null, null]. convertor=[PackedDecimal].", e.getMessage());
        }
    }
    
    

    /**
     * 出力するオブジェクトが文字列
     */
    @Test
    public void testArgString() throws Exception{

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
        1  signedZDigits  SP(10, "", "7", "4")    # 
        11  signedZDigits2  SP(10, "", "7", "4")   # 
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        

        DataRecord record = new DataRecord(){{
            put("signedZDigits", "-87654321");
            put("signedZDigits2", "87654321");
        }};
        
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        createWriteFormatter(new File("format.fmt"), dest);
        formatter.writeRecord(record);
        
        formatter.close();
        
        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        byte[] bytes5 = new byte[10];
        byte[] bytes6 = new byte[10];
        source.read(bytes5);
        source.read(bytes6);
        
        byte[] bytes = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x14
            };        
        byte[] bytes2 = new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 
                0x08, 0x76, 0x54, 0x32, 0x17
            };        
        
        assertTrue(Arrays.equals(bytes, bytes5));
        assertTrue(Arrays.equals(bytes2, bytes6));
        
        source.close();
        new File("record.dat").deleteOnExit();
                

    }
    


    /**
     * BigDecimalに変換できないオブジェクトが渡された場合に例外がスローされるテスト。
     * InvalidDataFormatExceptionがスローされる。
     */
    @Test
    public void testWriteObjectNotBigDecimal() {
        PackedDecimal decimal = new PackedDecimal();

        /*
         * nullの場合
         */
        try {
            decimal.convertOnWrite(null);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must not be null."));
        }

        /*
         * 文字列の場合
         */
        try {
            decimal.convertOnWrite("");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[]."));
        }
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
     * 出力時の最大値／最小値のテスト。
     * 桁数が不正な場合、InvalidDataFormatExceptionがスローされる。
     */
    @Test
    public void testMaxAndMinDigitsOnWrite() throws Exception {

        /*
         * 符号なしパック10進のテスト。
         */
        DataType<BigDecimal, byte[]> t = factory.typeOf("P", field, 10, 0);
        PackedDecimal packedType = (PackedDecimal)t;

        byte packNibble = (byte) ((Charset.forName("sjis").encode("1").get() & 0xF0) >>> 4); // 3
        packedType.setPackNibble(packNibble);
        
        byte[] bytes = new byte[] {
                0x09, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x93
        };

        /*
         * 【正常系】正数のパターン：999999999999999999（18桁）はOK。
         */
        byte[] convertOnWrite = packedType.convertOnWrite(new BigDecimal("999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数のパターン：1000000000000000000（19桁）はNG。
         */
        try {
            packedType.convertOnWrite("1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000]."));
        }
        
        /*
         * 【正常系】正数＆小数点のパターン：99999999999.9999999（小数点を除くと18桁）はOK。
         */
        convertOnWrite = packedType.convertOnWrite(new BigDecimal("99999999999.9999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数＆小数点のパターン：1000000000000.000000（小数点を除くと19桁）はNG。
         */
        try {
            packedType.convertOnWrite("1000000000000.000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000]."));
        }
        
        
        bytes = new byte[] {
                0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03
        };

        /*
         * 【正常系】正数＆小数点（スケール5）のパターン：1000000000000[.00000]（スケールを追加すると18桁）はOK。
         */
        convertOnWrite = packedType.convertOnWrite(new BigDecimal("1000000000000").setScale(5));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【正常系】正数＆小数点（スケール6）のパターン：1000000000000[.000000]（スケールを追加すると19桁）はNG。
         */
        try {
            convertOnWrite = packedType.convertOnWrite(new BigDecimal("1000000000000").setScale(6));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[1000000000000000000], original parameter=[1000000000000.000000]."));
        }
        
        /*
         * 符号ありパック10進のテスト。
         */
        DataType<BigDecimal, byte[]> st = factory.typeOf("SP", field, 10, 0);
        SignedPackedDecimal signedPackType = (SignedPackedDecimal)st;

        packNibble = (byte) 0x30;
        signedPackType.setPackSignNibblePositive(4);
        signedPackType.setPackSignNibbleNegative(7);
        
        bytes = new byte[] {
                0x09, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x94
        };

        /*
         * 【正常系】正数のパターン：999999999999999999（18桁）はOK。
         */
        convertOnWrite = signedPackType.convertOnWrite(new BigDecimal("999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【異常系】正数のパターン：1000000000000000000（19桁）はNG。
         */
        try {
            signedPackType.convertOnWrite("1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[1000000000000000000]."));
        }
        

        bytes = new byte[] {
                0x09, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x97
        };
        
        /*
         * 【正常系】負数のパターン：-999999999999999999（18桁）はOK。
         */
        convertOnWrite = signedPackType.convertOnWrite(new BigDecimal("-999999999999999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));
        
        /*
         * 【正常系】負数＆小数点のパターン：-99999999999.9999999（18桁）はOK。
         */
        convertOnWrite = signedPackType.convertOnWrite(new BigDecimal("-99999999999.9999999"));
        assertTrue(isSameSequence(bytes, convertOnWrite));

        /*
         * 【異常系】負数のパターン：-1000000000000000000（19桁）はNG。
         */
        try {
            signedPackType.convertOnWrite("-1000000000000000000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of parameter digits must be 18 or less, but was '19'. parameter=[-1000000000000000000]."));
        }

        /*
         * 【異常系】負数＆小数点のパターン：-10000000000000.00000（19桁）はNG。
         */
        try {
            signedPackType.convertOnWrite("-10000000000000.00000");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the number of unscaled parameter digits must be 18 or less, but was '19'. unscaled parameter=[-1000000000000000000], original parameter=[-10000000000000.00000]."));
        }
    }

}
