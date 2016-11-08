package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@lins SignedNumberString}の固定長テスト。
 * @author Masato Inoue
 */
public class SignedNumberStringDecimalTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * 初期化時にnullが渡されたときのテスト。
     */
    @Test
    public void testInitializeNull() {
        SignedNumberStringDecimal datatype = new SignedNumberStringDecimal();

        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, hoge]. convertor=[SignedNumberStringDecimal].");

        datatype.initialize(null, "hoge");
    }

    /**
     * 空文字を読み込む場合のテスト。
     */
    @Test
    public void testReadEmpty() {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});

        assertThat(convertor.convertOnRead(""), is(new BigDecimal("0")));
    }
    /**
     * null, 空文字を書き込む場合のテスト。
     */
    @Test
    public void testWriteNullOrEmpty() {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});

        assertThat(convertor.convertOnWrite(null), is("0000000000".getBytes()));
        assertThat(convertor.convertOnWrite(""), is("0000000000".getBytes()));
    }


    /**
     * 符号位置固定かつ符号非必須の場合の読み込みテスト。トリムが正常に行われることを確認。
     */
    @Test
    public void testReadFixSignPosition() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        
        /*
         * 符号が非必須の場合。
         */
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""}); // 符号位置は固定、符号は非必須
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(false);

        // パディング文字が0。符号の右にパディング文字
        BigDecimal result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("+000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("-000012340"));
        assertThat(result, is(new BigDecimal("-12340")));


        // 正の符号として'+'、負の符号として'-'以外の文字は使用できない（拡張しない限り）
        try {
            result = convertor.convertOnRead(toBytes("■00012340")); 
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]?0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[■00012340]."));
        }
        try {
            result = convertor.convertOnRead(toBytes("▲00012340")); 
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]?0*[0-9]+(\\.[0-9]*[0-9])?]. parameter=[▲00012340]."));
        }
        
        // パディング文字が空白。符号の右にパディング文字
        field.setPaddingValue(" ");
        convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""}); 
        result = convertor.convertOnRead(toBytes("     12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("+    12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("-    12340"));
        assertThat(result, is(new BigDecimal("-12340")));

        // パディング文字が0。符号の左にパディング文字
        field.setPaddingValue("0");
        result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        try {
            result = convertor.convertOnRead(toBytes("0000+12340")); // 符号位置固定の場合、トリム後の文字の先頭に数値以外の文字（符号など）が含まれてはいけない
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]? *[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000+12340]."));
        }
        try {
            result = convertor.convertOnRead(toBytes("0000-12340")); // 符号位置固定の場合、トリム後の文字の先頭に数値以外の文字（符号など）が含まれてはいけない
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]? *[0-9]+(\\.[0-9]*[0-9])?]. parameter=[0000-12340]."));
        }
        try {
            result = convertor.convertOnRead(toBytes("+000+12340")); // 符号位置固定の場合、トリム後の文字の先頭に数値以外の文字（符号など）が含まれてはいけない
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]? *[0-9]+(\\.[0-9]*[0-9])?]. parameter=[+000+12340]."));
        }
        try {
            result = convertor.convertOnRead(toBytes("-000-12340")); // 符号位置固定の場合、トリム後の文字の先頭に数値以外の文字（符号など）が含まれてはいけない
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-]? *[0-9]+(\\.[0-9]*[0-9])?]. parameter=[-000-12340]."));
        }
        
        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, ""});
        result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("■00012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("▲00012340"));
        assertThat(result, is(new BigDecimal("-12340")));
        
        
        // 符号位置が固定かどうかのフラグと、正の符号が必須かどうかのフラグが空文字の場合、符号位置が固定、符号が非必須として動作することの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, ""});
        result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
    }
    
    /**
     * 符号位置固定かつ符号必須の場合の読み込みテスト。トリムが正常に行われることを確認。
     */
    @Test
    public void testReadFixSignPositionRequiredSign() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        field.setPaddingValue(" ");
        /*
         * 符号が必須の場合の動作確認テスト
         */
        SignedNumberStringDecimal convertor =  new SignedNumberStringDecimal();
        convertor.init(field, 10, "", true, true);
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(true);

        //     +12340（符号あり）
        BigDecimal result = convertor.convertOnRead(toBytes("+     12340"));
        assertThat(result, is(new BigDecimal("12340")));
        //     -12340（符号あり）
        result = convertor.convertOnRead(toBytes("-     12340"));
        assertThat(result, is(new BigDecimal("-12340")));
        //      12340（符号なし）
        try {
            result = convertor.convertOnRead(toBytes("     12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [[+-] *[0-9]+(\\.[0-9]*[0-9])?]. parameter=[     12340]."));
        }
               
    }

    /**
     * 符号位置非固定かつ符号非必須の場合の読み込みテスト。トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNonFixSignPosition() throws Exception {
        
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        
        /*
         * 符号が非必須の場合。
         */
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, "", false, false}); // 符号位置は非固定、符号は非必須
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(false);
        
        // パディング文字が0。符号の左にパディング文字
        BigDecimal result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("0000+12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("0000-12340"));
        assertThat(result, is(new BigDecimal("-12340")));
        
        // パディング文字が空白。符号の左にパディング文字
        field.setPaddingValue(" ");        
        convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});         
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(false);
        result = convertor.convertOnRead(toBytes("     12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("    +12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("    -12340"));
        assertThat(result, is(new BigDecimal("-12340")));
        
        // パディング文字が0。符号の右にパディング文字
        result = convertor.convertOnRead(toBytes("0000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("+000012340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("-000012340"));
        assertThat(result, is(new BigDecimal("-12340")));
        
        // パディング文字が空白。符号の右にパディング文字
        field.setPaddingValue(" ");
        result = convertor.convertOnRead(toBytes("     12340"));
        assertThat(result, is(new BigDecimal("12340")));
        try {
            result = convertor.convertOnRead(toBytes("+    12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [ *[+-]?[0-9]+(\\.[0-9]*[0-9])?]. parameter=[+    12340]."));
        }
        try {
            result = convertor.convertOnRead(toBytes("-    12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [ *[+-]?[0-9]+(\\.[0-9]*[0-9])?]. parameter=[-    12340]."));
            
        }
        

        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10});
        result = convertor.convertOnRead(toBytes("     12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("■   12340"));
        assertThat(result, is(new BigDecimal("12340")));
        result = convertor.convertOnRead(toBytes("▲   12340"));
        assertThat(result, is(new BigDecimal("-12340")));
    }

    /**
     * 符号位置非固定かつ符号必須の場合の読み込みテスト。トリムが正常に行われることを確認。
     */
    @Test
    public void testReadNonFixSignPositionRequiredSign() throws Exception {
        
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        field.setPaddingValue(" ");
        
        /*
         * 符号が必須の場合。
         */
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, "", false, true}); // 符号位置は非固定、符号は必須
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(true);
  
        //     +12340（符号あり）
        BigDecimal result = convertor.convertOnRead(toBytes("     +12340"));
        assertThat(result, is(new BigDecimal("12340")));
        //     -12340（符号あり）
        result = convertor.convertOnRead(toBytes("     -12340"));
        assertThat(result, is(new BigDecimal("-12340")));
        //      12340（符号なし）
        try {
            result = convertor.convertOnRead(toBytes("     12340"));
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter format was specified. parameter format must be [ *[+-][0-9]+(\\.[0-9]*[0-9])?]. parameter=[     12340]."));
        }
    }
    
    /**
     * 符号位置固定かつ符号非必須の場合の書き込みテスト。出力データは符号あり数値。
     */
    @Test
    public void testWriteFixSignPosition() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(false);
        
        // 出力データが符号有り正数
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("0000001234"));

        // 出力データが符号有り負数
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("-000001234"));

        // 出力データがBigDecimal
        assertThat(new String(convertor.convertOnWrite(new BigDecimal("-1234")), "ms932"), is("-000001234"));

        // 出力データが符号有り負数（小数点付き）
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimal().init(field, new Object[]{10, 2, true, false});
        assertThat(new String(convertor.convertOnWrite("-1234.56"), "ms932"), is("-001234.56"));

        // 出力データが符号有り負数（小数点付き）
        field.setPaddingValue(" ");
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimal().init(field, new Object[]{10, 2, true, false});
        assertThat(new String(convertor.convertOnWrite("-1234.56"), "ms932"), is("-  1234.56"));


        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        field.setPaddingValue("0");
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, 0, true, false});
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("0000001234"));
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("▲00001234"));
    }
    
    /**
     * 符号位置固定かつ符号必須の場合の書き込みテスト。入力データは符号あり数値。
     */
    @Test
    public void testWriteFixSignPositionRequiredSign() throws Exception {
        
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(true);

        // 出力データが符号なし正数
        assertThat(new String(convertor.convertOnWrite("1234"), "ms932"), is("+000001234"));
        
        // 出力データが符号有り正数
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("+000001234"));

        // 出力データが符号有り負数
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("-000001234"));
        
        
        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, "", true, true});
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(true);
        
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("■00001234"));
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("▲00001234"));
    }
    
    /**
     * 符号位置非固定かつ符号非必須の場合の書き込みテスト。入力データは符号あり数値。
     */
    @Test
    public void testWriteNonFixSignPosition() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        
        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(false);

        // 出力データが符号なし正数
        assertThat(new String(convertor.convertOnWrite("1234"), "ms932"), is("0000001234"));
        
        // 出力データが符号有り正数
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("0000001234"));

        // 出力データが符号有り負数
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("00000-1234"));

        // 出力データが符号有り正数（小数点付き）
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimal().init(field, new Object[]{10, 2});
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(false);
        assertThat(new String(convertor.convertOnWrite("-1234.56"), "ms932"), is("00-1234.56"));

        
        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(false);
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("0000001234"));
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("0000▲1234"));
    }
    
    /**
     * 符号位置非固定かつ符号非必須の場合の書き込みテスト。入力データは符号あり数値。
     */
    @Test
    public void testWriteNonFixSignPositionRequiredSign() throws Exception {

        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});
        convertor.setRequiredDecimalPoint(true);
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(true);

        // 出力データが符号なし正数
        assertThat(new String(convertor.convertOnWrite("1234"), "ms932"), is("00000+1234"));
        
        // 出力データが符号有り正数
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("00000+1234"));

        // 出力データが符号有り負数
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("00000-1234"));

        
        // クラスを拡張すれば、正の符号が■、負の符号が▲の場合にも対応できることの確認
        convertor = (SignedNumberStringDecimal) new SignedNumberStringDecimalExtends().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(false);
        convertor.setRequiredPlusSign(true);
        assertThat(new String(convertor.convertOnWrite("+1234"), "ms932"), is("0000■1234"));
        assertThat(new String(convertor.convertOnWrite("-1234"), "ms932"), is("0000▲1234"));
    }

    /**
     * 出力時にパラメータがnull, 空文字の場合のテスト。
     */
    @Test
    public void testWriteNull() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");

        SignedNumberStringDecimal convertor = (SignedNumberStringDecimal)new SignedNumberStringDecimal().init(field, new Object[]{10, ""});
        convertor.setFixedSignPosition(true);
        convertor.setRequiredPlusSign(false);

        // null の場合
        assertThat(convertor.convertOnWrite(null), is("0000000000".getBytes()));

        // 空文字の場合
        assertThat(convertor.convertOnWrite(""), is("0000000000".getBytes()));
    }

    /**
     * バイト長が不正な場合のテスト。
     */
    @Test
    public void testInvalidByteLength() throws Exception {
        FieldDefinition field = new FieldDefinition().setEncoding(Charset.forName("ms932")).setName("test");
        // 初期化パラメータが存在しない場合、例外がスローされる
        try {
            new SignedNumberStringDecimal().init(field, new Object[]{});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("parameter was not specified. parameter must be specified. convertor=[SignedNumberStringDecimal]."));
        }
        // バイト長がnullの場合例外がスローされる
        try {
            new SignedNumberStringDecimal().init(field, new Object[]{null});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("1st parameter was null. parameter=[null]. convertor=[SignedNumberStringDecimal]."));
        }
        // バイト長が数値でない場合例外がスローされる
        try {
            new SignedNumberStringDecimal().init(field, new Object[]{"abc"});
            fail();
        } catch (SyntaxErrorException e) {
            assertThat(e.getMessage(), is("invalid parameter type was specified. 1st parameter must be Integer. parameter=[abc]. convertor=[SignedNumberStringDecimal]."));
        }

        DataType<BigDecimal, byte[]> init = new SignedNumberStringDecimal().init(field, new Object[]{10});
        try {
            init.convertOnWrite("12345678901");
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), is("invalid parameter was specified. too large data. field size = '10' data size = '11'. data: 12345678901"));
        }
    }
    
    /**
     * フォーマット定義ファイルを使用したディレクティブの読み込みテスト。
     */
    @Test
    public void testReadFormatFileDirective() throws Exception {
        
        /*
         * ディレクティブを省略した場合
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        


        DataRecord record = doTestReadFormatFileDirective(formatFile, "0000012345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));

        
        /*
         * ディレクティブを以下のとおり設定した場合
         * required-decimal-point: true
         * fixed-sign-position: true
         * required-plus-sign: false
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10
        # 小数点の要否     
        required-decimal-point: false
        # 符号位置の固定/非固定
        fixed-sign-position: true
        # 正の符号の必須/非必須
        required-plus-sign: false
        
        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        record = doTestReadFormatFileDirective(formatFile, "0000012345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));
        record = doTestReadFormatFileDirective(formatFile, "-000012345");
        assertEquals(new BigDecimal("-123.45"),          record.get("signedNumber"));
        

        /*
         * ディレクティブを以下のとおり設定した場合
         * required-decimal-point: true
         * fixed-sign-position: true
         * required-plus-sign: true
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10
        # 小数点の要否     
        required-decimal-point: false
        # 符号位置の固定/非固定
        fixed-sign-position: true
        # 正の符号の必須/非必須
        required-plus-sign: true
        
        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        record = doTestReadFormatFileDirective(formatFile, "+000012345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));
         

        /*
         * ディレクティブを以下のとおり設定した場合
         * required-decimal-point: true
         * fixed-sign-position: false
         * required-plus-sign: false
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10
        # 小数点の要否     
        required-decimal-point: false
        # 符号位置の固定/非固定
        fixed-sign-position: false
        # 正の符号の必須/非必須
        required-plus-sign: false
        
        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        record = doTestReadFormatFileDirective(formatFile, "0000012345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));
        record = doTestReadFormatFileDirective(formatFile, "0000-12345");
        assertEquals(new BigDecimal("-123.45"),          record.get("signedNumber"));
        

        /*
         * ディレクティブを以下のとおり設定した場合
         * required-decimal-point: true
         * fixed-sign-position: false
         * required-plus-sign: true
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10
        # 小数点の要否     
        required-decimal-point: false
        # 符号位置の固定/非固定
        fixed-sign-position: false
        # 正の符号の必須/非必須
        required-plus-sign: true
        
        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        record = doTestReadFormatFileDirective(formatFile, "0000+12345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));
         
        formatFile.delete();
        
        
        /*
         * ディレクティブを以下のとおり設定した場合
         * required-decimal-point: false
         * fixed-sign-position: true
         * required-plus-sign: false
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 各レコードの長さ
        record-length: 10
        # 小数点の要否     
        required-decimal-point: false
        # 符号位置の固定/非固定
        fixed-sign-position: true
        # 正の符号の必須/非必須
        required-plus-sign: false
        
        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 2)    # 
        ***************************************************/
        
        record = doTestReadFormatFileDirective(formatFile, "0000012345");
        assertEquals(new BigDecimal("123.45"),          record.get("signedNumber"));
        record = doTestReadFormatFileDirective(formatFile, "-000012345");
        assertEquals(new BigDecimal("-123.45"),          record.get("signedNumber"));
        
        
    }

    private DataRecord doTestReadFormatFileDirective(File formatFile, String value) throws FileNotFoundException,
            IOException {
        
        byte[] bytes = value.getBytes("ms932");
        
        OutputStream dest = new FileOutputStream("./record.dat", false);
        dest.write(bytes);
        dest.close();
        
        InputStream source = new BufferedInputStream(
                new FileInputStream("record.dat"));

        DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).
        createFormatter(formatFile).setInputStream(source).initialize();
        
        DataRecord record = formatter.readRecord();
        source.close();
        new File("record.dat").delete();
        return record;
    }

    /**
     * 書き込み時にパラメータがnullの場合、デフォルト値を出力するテスト。
     */
    @Test
    public void testWriteDefault() throws Exception {

        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
         # ファイルタイプ
         file-type:    "Fixed"
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "ms932"

         # 各レコードの長さ
         record-length: 10

         # データレコード定義
         [Default]
         1  signedNumber SX9(10, 3)   123
         ***************************************************/

        FilePathSetting.getInstance().addBasePathSetting("input", "file:./")
                .addBasePathSetting("format", "file:./")
                .addFileExtensions("format", "fmt");

        OutputStream dest = new FileOutputStream("./record.dat", false);
        DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).
                createFormatter(formatFile).setOutputStream(dest).initialize();
        formatter.writeRecord(new DataRecord(){{
            put("signedNumber", null);
        }});
        dest.close();
        InputStream source = new BufferedInputStream(new FileInputStream("record.dat"));
        byte[] bytes = new byte[10];

        source.read(bytes);

        source.close();

        assertEquals("000123.000", new String(bytes, "ms932"));
    }

    /**
     * フォーマット定義ファイルを使用したディレクティブの書き込みテスト。
     */
    @Test
    public void testWriteFormatFileDirective() throws Exception {
        
        /*
         * ディレクティブを省略した場合
         */
        File formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 3)    # 
        ***************************************************/
        
        FilePathSetting.getInstance().addBasePathSetting("input",  "file:./")
                                 .addBasePathSetting("format", "file:./")
                                 .addFileExtensions("format", "fmt");
        
        byte[] bytes = doDirectiveTest(formatFile, 12345);
        
        assertEquals("000012.345", new String(bytes, "ms932"));

        
        /*
         * ディレクティブを以下のとおり設定した場合
         * fixed-sign-position: true
         * required-plus-sign: false
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 符号位置の固定/非固定
        fixed-sign-position: true
        # 正の符号の必須/非必須
        required-plus-sign: false
        
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 3)    # 
        ***************************************************/
        
        bytes = doDirectiveTest(formatFile, 12345);
        assertEquals("000012.345", new String(bytes, "ms932"));
        bytes = doDirectiveTest(formatFile, -12345);
        assertEquals("-00012.345", new String(bytes, "ms932"));
        
        
        /*
         * ディレクティブを以下のとおり設定した場合
         * fixed-sign-position: true
         * fixed-sign-position: true
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 符号位置の固定/非固定
        fixed-sign-position: true
        # 正の符号の必須/非必須
        required-plus-sign: true
        
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 3)    # 
        ***************************************************/

        bytes = doDirectiveTest(formatFile, 12345);
        assertEquals("+00012.345", new String(bytes, "ms932"));

        
        /*
         * ディレクティブを以下のとおり設定した場合
         * fixed-sign-position: false
         * fixed-sign-position: false
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 符号位置の固定/非固定
        fixed-sign-position: false
        # 正の符号の必須/非必須
        required-plus-sign: false
        
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 3)    # 
        ***************************************************/

        bytes = doDirectiveTest(formatFile, 12345);
        assertEquals("000012.345", new String(bytes, "ms932"));
        bytes = doDirectiveTest(formatFile, -12345);
        assertEquals("000-12.345", new String(bytes, "ms932"));

        
        /*
         * ディレクティブを以下のとおり設定した場合
         * fixed-sign-position: false
         * fixed-sign-position: true
         */
        formatFile = Hereis.file("./format.fmt");
        /**********************************************
        # ファイルタイプ
        file-type:    "Fixed"
        # 文字列型フィールドの文字エンコーディング
        text-encoding: "ms932"
        # 符号位置の固定/非固定
        fixed-sign-position: false
        # 正の符号の必須/非必須
        required-plus-sign: true
        
        # 各レコードの長さ
        record-length: 10

        # データレコード定義
        [Default]
        1  signedNumber SX9(10, 3)    # 
        ***************************************************/

        bytes = doDirectiveTest(formatFile, 12345);
        assertEquals("000+12.345", new String(bytes, "ms932"));
        

        formatFile.delete();
    }
    
    /**
     * フォーマッタを使用してデータを書き出し、またその書き出したデータをバイト配列として読み込む。
     */
    private byte[] doDirectiveTest(File formatFile, final int value) throws Exception {
        OutputStream dest = new FileOutputStream("./record.dat", false);
        DataRecordFormatter formatter = FormatterFactory.getInstance().setCacheLayoutFileDefinition(false).
        createFormatter(formatFile).setOutputStream(dest).initialize();
        formatter.writeRecord(new DataRecord(){{
            put("signedNumber", String.valueOf(BigDecimal.valueOf(value, 3)));
        }});
        dest.close();
        InputStream source = new BufferedInputStream(new FileInputStream("record.dat"));
        byte[] bytes = new byte[10];

        source.read(bytes);
        
        source.close();
        
        return bytes;
    }

    /**
     * 符号として'■'および'▲'を使用する拡張クラス。
     * @author Masato Inoue
     */
    private class SignedNumberStringDecimalExtends extends SignedNumberStringDecimal {
        @Override
        protected String getPlusSign() {
            return "■";
        }
        @Override
        protected String getMinusSign() {
            return "▲";
        }
    }

    /** 文字列をバイトに変換する */
    private byte[] toBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("ms932");
    }
    
}
