package nablarch.core.dataformat;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.hamcrest.CoreMatchers;

import nablarch.core.util.FileUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * レイアウト定義ファイルのパーサのテストクラス。
 * <p>
 * 観点：
 * 正常系はフォーマッタクラスで確認しているので、ここではレイアウト定義ファイルをパースする際の異常系テストを網羅する。
 * 可変長、固定長に関連するディレクティブの妥当性検証などは、本クラスではなく、フォーマッタクラスのテストで行う。
 * （パーサは可変長、固定長に依存しない作りになっているので）
 *
 * @author Masato Inoue
 */
public class LayoutFileParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * 差分定義でベースタイプが存在しない場合
     */
    @Test
    public void testInvalidDiffDefinition() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testInvalidDiffDefinition.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("undefined base record type name was specified. type name=[error]");
        parser.parse();
    }

    /**
     * 不正なリテラルの形式。
     */
    @Test
    public void testInvalidLiteral() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testInvalidLiteral.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL ");
        parser.parse();
    }

    /**
     * 不正なフィールド定義の形式。
     */
    @Test
    public void testInvalidFieldDefinitionFormat() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testInvalidFieldDefinitionFormat.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL ");
        parser.parse();
    }

    /**
     * 不正なディレクティブの形式。
     */
    @Test
    public void testInvalidDirectiveFormat() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testInvalidDirectiveFormat.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL ");
        parser.parse();
    }

    /**
     * 空のパラメータの場合に、パース時に例外がスローされないこと。
     * （このような場合、コンバータのinitializeメソッドで例外がスローされる）
     */
    @Test
    public void testRPAREN() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testRPAREN.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());
        LayoutDefinition definition = parser.parse();
    }

    /**
     * 空のパラメータの場合に、パース時に例外がスローされないこと。
     * （このような場合、コンバータのinitializeメソッドで例外がスローされる）
     */
    @Test
    public void testTokenType() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testTokenType.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        parser.parse();
    }

    /**
     * \bがエスケープ文字として認識されることのテスト。
     */
    @Test
    public void testEscapeB() throws Exception {
        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testEscapeB.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());
        LayoutDefinition definition = parser.parse();
        String separator = (String) definition.getDirective()
                                              .get("record-separator");

        assertThat(separator, is("\b"));
    }

    /**
     * レイアウト定義ファイルが存在しない場合のテスト。
     */
    @Test
    public void testNotExistLayoutFile() {

        LayoutFileParser parser = new LayoutFileParser(
                "notExistLayoutFile.fmt");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectCause(CoreMatchers.<Throwable>instanceOf(FileNotFoundException.class));
        parser.parse();

    }

    /**
     * フォーマット定義ファイルにデータタイプの記述がなくデフォルト値の記述のみがあった場合。
     */
    @Test
    public void testDefaultValueOnly() throws Exception {

        final URL url = FileUtil.getResourceURL(
                "classpath:nablarch/core/dataformat/LayoutFileParserTest/testDefaultValueOnly.fmt");

        LayoutFileParser parser = new LayoutFileParser(new File(url.toURI()).getAbsolutePath());

        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "data type format was invalid. value=[\"aaa\"]. valid format=[[a-zA-Z_$][a-zA-Z0-9_$]*].");
        parser.parse();
    }
}
