package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link NumberStringDecimal}の固定長テスト。
 * @author Masato Inoue
 */
public class NumberStringDecimalTest {

    /**
     * 読み込みテスト。スケールなし。
     */
    @Test
    public void testReadNonScale() throws Exception {
        doTestReadNonScale(NumberStringDecimal.class);
    }
    
    /**
     * 読み込みテスト。スケールなしの浮動小数点。（SignedSingleByteNumberTestでもこのメソッドのテストを行うため、publicメソッドとして切り出す）
     */
    public void doTestReadNonScale(Class<? extends NumberStringDecimal> target) throws Exception {
        
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        NumberStringDecimal convertor = (NumberStringDecimal) target.newInstance().init(field, new Object[]{10});

        // 入力データがnull
        try {
            convertor.convertOnRead(toBytesNull());
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must not be null."));
        }
        
        // 入力データが空のバイト配列
        try {
            convertor.convertOnRead(toBytes(""));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[]."));
        }

        // 入力データが不正な数値
        try {
            convertor.convertOnRead(toBytes("1.23."));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[1.23.]."));
        }

        // 入力データが0
        BigDecimal result = convertor.convertOnRead(toBytes("0"));
        assertThat(result, is(new BigDecimal("0")));

        // 入力データが1
        result = convertor.convertOnRead(toBytes("1"));
        assertThat(result, is(new BigDecimal("1")));
        
        // 入力データが整数
        result = convertor.convertOnRead(toBytes("12340"));
        assertThat(result, is(new BigDecimal("12340")));

        // 入力データが小数
        result = convertor.convertOnRead(toBytes("1.23"));
        assertThat(result, is(new BigDecimal("1.23")));

    }

    private byte[] toBytesNull() {
        return null;
    }

    /**
     * 読み込みテスト。スケールあり。
     */
    @Test
    public void testReadScale() throws Exception {
        doTestReadScale(NumberStringDecimal.class);
    }
    
    /**
     * 読み込みテスト。スケールありの固定小数点。（SignedSingleByteNumberTestでもこのメソッドのテストを行うため、publicメソッドとして切り出す）
     */
    public void doTestReadScale(Class<? extends NumberStringDecimal> target) throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        /*
         * スケールが0の場合。
         */
        NumberStringDecimal convertor = (NumberStringDecimal) target.newInstance().init(field, new Object[]{10, 0});

        // 入力データが空のバイト文字列
        try {
            convertor.convertOnRead(toBytes(""));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[]."));
        }

        // 入力データが0
        BigDecimal result = convertor.convertOnRead(toBytes("0"));
        assertThat(result, is(new BigDecimal("0")));

        // 入力データが1
        result = convertor.convertOnRead(toBytes("1"));
        assertThat(result, is(new BigDecimal("1")));

        // 入力データが整数
        result = convertor.convertOnRead(toBytes("12340"));
        assertThat(result, is(new BigDecimal("12340")));
        
        // 入力データが小数（スケールは無視される）
        result = convertor.convertOnRead(toBytes("123.4"));
        assertThat(result, is(new BigDecimal("123.4")));

        /*
         * スケールが3の場合。
         */
        convertor = (NumberStringDecimal) target.newInstance().init(field, new Object[]{10, 3});
        
        // 入力データが0
        result = convertor.convertOnRead(toBytes("0"));
        assertThat(result, is(new BigDecimal("0.000")));

        // 入力データが1
        result = convertor.convertOnRead(toBytes("1"));
        assertThat(result, is(new BigDecimal("0.001")));
        
        // 入力データが整数
        result = convertor.convertOnRead(toBytes("12340"));
        assertThat(result, is(new BigDecimal("12.340")));

        /*
         * スケールが-3の場合。
         */
        convertor = (NumberStringDecimal) target.newInstance().init(field, new Object[]{10, -3});

        // 入力データが整数
        result = convertor.convertOnRead(toBytes("123"));
        assertThat(result.toPlainString(), is("123000"));
        
    }
    
    /**
     * 読み込みテスト。パディング指定あり。
     */
    @Test
    public void testReadPadding() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        DataType<BigDecimal, byte[]> convertor = new NumberStringDecimal().init(field, new Object[]{10});

        // パディング（デフォルト）
        BigDecimal result = convertor.convertOnRead(toBytes("0000012345"));
        assertThat(result, is(new BigDecimal("12345")));
        
        // パディング（空白）
        field.setPaddingValue(" ");
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("     12345"));
        assertThat(result, is(new BigDecimal("12345")));

        // パディング（0）
        field.setPaddingValue("0");
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("0000012345"));
        assertThat(result, is(new BigDecimal("12345")));

        // パディング（X）
        field.setPaddingValue("X");
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("XXXXX12345"));
        assertThat(result, is(new BigDecimal("12345")));

        // パディング（x）
        field.setPaddingValue("x");
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("xxxxx00005"));
        assertThat(result, is(new BigDecimal("5")));

        // パディング（0）かつ、入力データが0
        field.setPaddingValue("0");
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("0"));
        assertThat(result, is(new BigDecimal("0")));

        // パディング（0）かつ、入力データが0000000000
        convertor = new NumberStringDecimal().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("0000000000"));
        assertThat(result, is(new BigDecimal("0")));
    }
    
    /**
     * トリム後およびパディング前の文字のバリエーションテスト。
     * <p/>
     * トリム後およびパディング前の最初の文字が符号の場合に例外がスローされる。
     */
    @Test
    public void testFirstCharVariation() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        DataType<BigDecimal, byte[]> convertor = new NumberStringDecimal().init(field, new Object[]{10});

        /*
         * トリム後の文字が符号の場合
         */
        // 先頭が1
        BigDecimal result = convertor.convertOnRead(toBytes("0000112345"));
        assertThat(result, is(new BigDecimal("112345")));
        // 先頭が2
        result = convertor.convertOnRead(toBytes("0000212345"));
        assertThat(result, is(new BigDecimal("212345")));
        // 先頭が3
        result = convertor.convertOnRead(toBytes("0000312345"));
        assertThat(result, is(new BigDecimal("312345")));
        // 先頭が4
        result = convertor.convertOnRead(toBytes("0000412345"));
        assertThat(result, is(new BigDecimal("412345")));
        // 先頭が5
        result = convertor.convertOnRead(toBytes("0000512345"));
        assertThat(result, is(new BigDecimal("512345")));
        // 先頭が6
        result = convertor.convertOnRead(toBytes("0000612345"));
        assertThat(result, is(new BigDecimal("612345")));
        // 先頭が7
        result = convertor.convertOnRead(toBytes("0000712345"));
        assertThat(result, is(new BigDecimal("712345")));
        // 先頭が8
        result = convertor.convertOnRead(toBytes("0000812345"));
        assertThat(result, is(new BigDecimal("812345")));
        // 先頭が9
        result = convertor.convertOnRead(toBytes("0000912345"));
        assertThat(result, is(new BigDecimal("912345")));

        // 入力データが文字列
        try {
            convertor.convertOnRead(toBytes("abc"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[abc]."));
        }

        //先頭が+
        try {
            result = convertor.convertOnRead(toBytes("0000+12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000+12340]."));
        }
        
        //先頭が-
        try {
            result = convertor.convertOnRead(toBytes("0000-12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000-12340]."));
        } 
        
        /*
         * パディング前の文字が符号の場合
         */
        // 先頭が1
        assertThat((new String(convertor.convertOnWrite("112345"), "ms932")), is("0000112345"));
        // 先頭が2
        assertThat((new String(convertor.convertOnWrite("212345"), "ms932")), is("0000212345"));
        // 先頭が3
        assertThat((new String(convertor.convertOnWrite("312345"), "ms932")), is("0000312345"));
        // 先頭が4
        assertThat((new String(convertor.convertOnWrite("412345"), "ms932")), is("0000412345"));
        // 先頭が5
        assertThat((new String(convertor.convertOnWrite("512345"), "ms932")), is("0000512345"));
        // 先頭が6
        assertThat((new String(convertor.convertOnWrite("612345"), "ms932")), is("0000612345"));
        // 先頭が7
        assertThat((new String(convertor.convertOnWrite("712345"), "ms932")), is("0000712345"));
        // 先頭が8
        assertThat((new String(convertor.convertOnWrite("812345"), "ms932")), is("0000812345"));
        // 先頭が9
        assertThat((new String(convertor.convertOnWrite("912345"), "ms932")), is("0000912345"));
        // 先頭が正の符号（符号は削除される）
        assertThat((new String(convertor.convertOnWrite("+12345"), "ms932")), is("0000012345"));

        
        // 先頭が負の符号（文字列）
        try {
            convertor.convertOnWrite("-12345");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must not be minus. parameter=[-12345]."));
        }

        // 先頭が負の符号（BigDecimal）
        try {
            convertor.convertOnWrite(BigDecimal.valueOf((long)-12345, 2));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must not be minus. parameter=[-123.45]."));
        }
    }
    
    /**
     * 書き込みテスト。
     */
    @Test
    public void testWrite() throws Exception {
        doWrite(NumberStringDecimal.class);
    }

    /**
     * 書き込み時のパディングに着目したテスト。
     */
    @Test
    public void testWriteAndPadding() {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

        // パディング文字指定なし(デフォルトの0が使用される。)
        DataType<BigDecimal, byte[]> dataType = new NumberStringDecimal().init(field, 10);
        assertThat(dataType.convertOnWrite("12345"), IsByte.is("0000012345"));

        // パディング文字指定：0
        field.setPaddingValue("0");
        dataType = new NumberStringDecimal().init(field, 10);
        assertThat(dataType.convertOnWrite("12345"), IsByte.is("0000012345"));

        // パディング文字指定：スペース
        field.setPaddingValue(" ");
        dataType = new NumberStringDecimal().init(field, 10);
        assertThat(dataType.convertOnWrite("12345"), IsByte.is("     12345"));

        // パディング文字指定：X
        field.setPaddingValue("X");
        dataType = new NumberStringDecimal().init(field, 10);
        assertThat(dataType.convertOnWrite("12345"), IsByte.is("XXXXX12345"));

        // パディング文字指定：9
        field.setPaddingValue("!");
        dataType = new NumberStringDecimal().init(field, 10);
        assertThat(dataType.convertOnWrite("12345"), IsByte.is("!!!!!12345"));

    }

    private static class IsByte extends TypeSafeMatcher<byte[]> {

        private String expected;

        private IsByte(String expected) {
            this.expected = expected;
        }
        public static IsByte is(String expected) {
            return new IsByte(expected);
        }

        @Override
        public boolean matchesSafely(byte[] bytes) {
            return Arrays.equals(bytes, expected.getBytes());
        }

        public void describeTo(Description description) {
            description.appendValue(expected.getBytes());
        }
    }
    
    /**
     * 書き込みテスト。（SignedSingleByteNumberTestでもこのメソッドのテストを行うため、publicメソッドとして切り出す）
     */
    public void doWrite(Class<? extends NumberStringDecimal> target) throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        DataType<BigDecimal, byte[]> convertor = target.newInstance().init(field, new Object[]{10});

        // 出力データがnull
        try {
            convertor.convertOnWrite(null);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must not be null."));
        }

        
        /*
         * 以降、出力データが文字列のパターン
         */
        
        // 出力データが空文字
        try {
            convertor.convertOnWrite("");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[]."));
        }
        
        // 出力データが文字列
        try {
            convertor.convertOnWrite("abc");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[abc]."));
        }

        // 出力データが不正な数値
        try {
            convertor.convertOnWrite("1.23.");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. parameter must be able to convert to BigDecimal. parameter=[1.23.]."));
        }

        // 出力データが0
        assertThat(new String(convertor.convertOnWrite("0"), "ms932"), is("0000000000"));

        // 出力データが1
        assertThat(new String(convertor.convertOnWrite("1"), "ms932"), is("0000000001"));
        
        // 出力データが整数
        convertor = target.newInstance().init(field, new Object[]{10, 3});
        assertThat(new String(convertor.convertOnWrite("12345"), "ms932"), is("012345.000"));

        
        // 出力データが小数
        convertor = target.newInstance().init(field, new Object[]{10, 2});
        assertThat(new String(convertor.convertOnWrite("1.23"), "ms932"), is("0000001.23"));

        // 出力データの桁数がバイト長と一致
        convertor = target.newInstance().init(field, new Object[]{10, 0});
        assertThat(new String(convertor.convertOnWrite("1234567890"), "ms932"), is("1234567890"));

        /*
         * 以降、出力データがBigDecimalのパターン
         */

        // 出力データが小数点1桁、小数点位置は1
        convertor = target.newInstance().init(field, new Object[]{10, 1});
        assertThat(new String(convertor.convertOnWrite(new BigDecimal("12.3")), "ms932"), is("00000012.3"));

        // 出力データが小数点1桁、小数点位置は2
        convertor = target.newInstance().init(field, new Object[]{10, 2});
        assertThat(new String(convertor.convertOnWrite(BigDecimal.valueOf((long)123, 1)), "ms932"), is("0000012.30"));
        
        // 出力データが小数点1桁、小数点位置は0（指定されたスケールが、出力データのスケールより小さい）
        convertor = target.newInstance().init(field, new Object[]{10, 0});
        try {
            convertor.convertOnWrite(new BigDecimal("1.23"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid scale was specified. specify scale must be greater than the parameter scale. specified scale=[0], parameter scale=[2], parameter=[1.23]."));
        }
        
        // 出力データは小数点1桁、小数点位置は-3
        convertor = new NumberStringDecimal().init(field, new Object[]{10, -3});
        assertThat(new String(convertor.convertOnWrite(new BigDecimal("123000")), "ms932"), is("0000000123"));


        // 出力データは小数点0桁、小数点位置は-4（桁落ちが発生するので例外で落ちる）
        convertor = new NumberStringDecimal().init(field, new Object[]{10, -4});
        assertThat(new String(convertor.convertOnWrite(new BigDecimal("123000")), "ms932"), is("00000012.3"));

    }

    /**
     * 小数点が不要の場合のテスト。
     */
    @Test
    public void testDecimalPoint() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

        // 出力データは小数点0桁、小数点位置は0
        NumberStringDecimal convertor = (NumberStringDecimal) new NumberStringDecimal().init(field, new Object[]{10, 0});
        ((NumberStringDecimal) convertor).setRequiredDecimalPoint(false);
        assertThat(new String(convertor.convertOnWrite(BigDecimal.valueOf((long)123, 0)), "ms932"), is("0000000123"));

        // 出力データは小数点0桁、小数点位置は1
        convertor = (NumberStringDecimal) new NumberStringDecimal().init(field, new Object[]{10, 1});
        ((NumberStringDecimal) convertor).setRequiredDecimalPoint(false);
        assertThat(new String(convertor.convertOnWrite(BigDecimal.valueOf((long)123, 0)), "ms932"), is("0000001230"));

        // 出力データは小数点0桁、小数点位置は-1
        convertor = (NumberStringDecimal) new NumberStringDecimal().init(field, new Object[]{10, 0});
        ((NumberStringDecimal) convertor).setRequiredDecimalPoint(false);
        try {
            convertor.convertOnWrite(BigDecimal.valueOf((long)123, 1));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid scale was specified. specify scale must be greater than the parameter scale. specified scale=[0], parameter scale=[1], parameter=[12.3]."));
        }

        // 出力データは小数点1桁、小数点位置は-3
        convertor = (NumberStringDecimal) new NumberStringDecimal().init(field, new Object[]{10, -3});
        ((NumberStringDecimal) convertor).setRequiredDecimalPoint(false);
        assertThat(new String(convertor.convertOnWrite(new BigDecimal("123000")), "ms932"), is("0000000123"));
        

        // 出力データは小数点0桁、小数点位置は-4（桁落ちが発生するので例外で落ちる）
        convertor = (NumberStringDecimal) new NumberStringDecimal().init(field, new Object[]{10, -4});
        ((NumberStringDecimal) convertor).setRequiredDecimalPoint(false);
        try {
            assertThat(new String(convertor.convertOnWrite(new BigDecimal("123000")), "ms932"), is("0000000123"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid scale was specified. scaled data should not have a decimal point. scale=[-4], scaled data=[12.3], write data=[123000]."));
        }
        
    }
    
    /**
     * スケールの数字のバリエーション。
     */
    @Test
    public void testInvalidScaleVariation() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

        // スケールがnullの場合、スケールは0として扱われる
        DataType<BigDecimal, byte[]> convertor = new NumberStringDecimal().init(field, new Object[]{10, null});
        BigDecimal result = convertor.convertOnRead(toBytes("12340"));
        assertThat(result, is(new BigDecimal("12340")));

        // スケールが空の場合、スケールは0として扱われる
        convertor = new NumberStringDecimal().init(field, new Object[]{10, ""});
        result = convertor.convertOnRead(toBytes("12340"));
        assertThat(result, is(new BigDecimal("12340")));

        // スケールがInteger型でない場合、例外がスローされる
        try {
            new NumberStringDecimal().init(field, new Object[]{10, "abc"}); // abc
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid parameter type was specified. 2nd parameter type must be Integer. parameter=[10, abc]. convertor=[NumberStringDecimal]."));
        }
        try {
            new NumberStringDecimal().init(field, new Object[]{10, "123"}); // 文字列の123
            fail();
        } catch(SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid parameter type was specified. 2nd parameter type must be Integer. parameter=[10, 123]. convertor=[NumberStringDecimal]."));
        }
    }


    /**
     * 不正なパディング文字が設定された場合のテスト。
     */
    @Test
    public void testInvalidPaddingStr() throws Exception {

        FieldDefinition field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("1");

        // パディング文字に1
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[1], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に2
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("2");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[2], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に3
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("3");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[3], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に4
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("4");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[4], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に5
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("5");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[5], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に6
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("6");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[6], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に7
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("7");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[7], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に8
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("8");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[8], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に9
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("9");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid padding character was specified. padding character must not be [1-9] pattern. padding character=[9], convertor=[NumberStringDecimal]."));
        }
        // パディング文字に全角
        field = (FieldDefinition) new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test").setPaddingValue("０");
        try {
            new NumberStringDecimal().init(field, new Object[]{10});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. the length of padding bytes must be '1', but was '2'. padding string=[０]."));
        }
    }

    /**
     * フォーマット定義ファイルを使用した読み込みテスト。
     */
    @Test
    public void testReadFormatFile() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  number  X9(10, "")   
        11  number2  SX9(10, 3)  
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        byte[] bytes = "0000123.45".getBytes("ms932");
        byte[] bytes2 = "0000012345".getBytes("ms932");
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.write(bytes2);
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).
        createFormatter(formatFile).setInputStream(source).initialize();
        
        DataRecord record = formatter.readRecord();
        
        assertEquals(2, record.size());
        assertEquals(new BigDecimal("123.45"),          record.get("number"));
        assertEquals(new BigDecimal("12.345"),          record.get("number2"));
        
        source.close();
        new File("record.dat").deleteOnExit();
    }
    

    
    /**
     * フォーマット定義ファイルを使用した書き込みテスト。
     */
    @Test
    public void testWriteFormatFile() throws Exception {
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 20

        # データレコード定義
        [Default]
        1  number  X9(10, 2)   
        11  number2  SX9(10, 4)    
        ***************************************************/
        formatFile.deleteOnExit();
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        

        OutputStream dest = new FileOutputStream("./record.dat", false);
        
        DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).
        createFormatter(formatFile).setOutputStream(dest).initialize();
        
        formatter.writeRecord(new DataRecord(){{
            put("number", "123.45");
            put("number2", String.valueOf(BigDecimal.valueOf(12345, 3)));
        }});
        
        dest.close();

        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        byte[] bytes = new byte[10];
        
        source.read(bytes);
        assertEquals("0000123.45", new String(bytes, "ms932"));

        source.read(bytes);
        assertEquals("00012.3450", new String(bytes, "ms932"));

        source.close();
        new File("record.dat").deleteOnExit();
    }

    /**
     * {@link NumberStringDecimal#trim(String)}のテスト。
     *
     * 空文字列をtrimした場合、空文字列が返却されること。
     */
    @Test
    public void testTrimEmptyString() {
        NumberStringDecimal sut = new NumberStringDecimal();
        assertThat(sut.trim(""), is(""));
    }

    /** 文字列をバイトに変換する */
    private byte[] toBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("ms932");
    }
    
}
