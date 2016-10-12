package nablarch.core.dataformat;

import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * レイアウト定義ファイルのパーサのテストクラス。
 * 
 * 観点：
 * 正常系はフォーマッタクラスで確認しているので、ここではレイアウト定義ファイルをパースする際の異常系テストを網羅する。
 * 可変長、固定長に関連するディレクティブの妥当性検証などは、本クラスではなく、フォーマッタクラスのテストで行う。
 * （パーサは可変長、固定長に依存しない作りになっているので）
 * 
 * @author Masato Inoue
 *
 */
public class LayoutFileParserTest {

    
    /**
     * 差分定義でベースタイプが存在しない場合
     */
    @Test
    public void testInvalidDiffDefinition(){

        File formatFile = Hereis.file("./format.fmt");
        /***********************************************************
        #
        # 共通定義部分 
        #
        file-type:    "Fixed"
        text-encoding:    "ms932" # 文字列型フィールドの文字エンコーディング
        record-length:     120    # 各レコードの長さ
       
        [Classifier] # レコードタイプ識別フィールド定義
        1   dataKbn   X(1)   # データ区分              
        113 withEdi   X(1)   # EDI情報使用フラグ
                             #    Y: EDIあり、N: なし


        [DataWithEDI] # データレコード (EDI情報あり)
          dataKbn  = "2"
          withEdi  = "Y"
        1    dataKbn       X(1)  "2"     # データ区分
        2    FIcode        X(4)          # 振込先金融機関コード
        6    FIname        X(15)         # 振込先金融機関名称
        21   officeCode    X(3)          # 振込先営業所コード
        24   officeName    X(15)         # 振込先営業所名
        39  ?tegataNum     X(4)  "9999"  # (手形交換所番号:未使用)
        43   syumoku       X(1)          # 預金種目
        44   accountNum    X(7)          # 口座番号
        51   recipientName X(30)         # 受取人名
        81   amount        X(10)         # 振込金額
        91   isNew         X(1)          # 新規コード
        92   ediInfo       X(20)         # EDI情報
        112  transferType  X(1)          # 振込区分
        113  withEdi       X(1)  "Y"     # EDI情報使用フラグ
        114 ?unused        X(7)  pad("0")# (未使用領域)            


        [DataWithoutEDI] < [error]  # データレコード (EDI情報なし)
          dataKbn = "2"                   #   EDI情報なしの場合、振込人情報を
          withEdi = "N"                   #   EDI情報の代わりに付記する。
        92   userCode1     X(10)      # ユーザコード1
        102  userCode2     X(10)      # ユーザコード2
        113  withEdi       X(1)  "N"  # EDI情報使用フラグ
        ************************************************************/
        formatFile.deleteOnExit();

        LayoutFileParser parser = new LayoutFileParser("./format.fmt");
        try {
            parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("undefined base record type name was specified. type name=[error]"));
        }
    }
    
    /**
     * 不正なリテラルの形式。
     */
    @Test
    public void testInvalidLiteral(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X (NU)   # 不正な形式!!
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        try {
            parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL "));
        }
    }
    
    /**
     * 不正なフィールド定義の形式。
     */
    @Test
    public void testInvalidFieldDefinitionFormat(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X abc"      # 不正な形式!!
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        try {
            parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL "));
        }
    }
    
    /**
     * 不正なディレクティブの形式。
     */
    @Test
    public void testInvalidDirectiveFormat(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        text-encoding:    abc    # 不正な形式!!
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        try {
            parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("encountered unexpected token. allowed token types are: NUMBER STRING_LITERAL BINARY_LITERAL "));
        }
    }
    
    /**
     * 空のパラメータの場合に、パース時に例外がスローされないこと。
     * （このような場合、コンバータのinitializeメソッドで例外がスローされる）
     */
    @Test
    public void testRPAREN(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X ()   # 空のパラメータ
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        LayoutDefinition definition = parser.parse();
    }
    
    
    /**
     * 空のパラメータの場合に、パース時に例外がスローされないこと。
     * （このような場合、コンバータのinitializeメソッドで例外がスローされる）
     */
    @Test
    public void testTokenType(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        record-separator: "\r\n" # CRLFで改行
        field-separator:  ","    # カンマ区切り
        
        [Books]
        error                    # 不正な定義
        1   Title      X ()       # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        try {
            LayoutDefinition definition = parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(true);
        }
    }
    

    /**
     * \bがエスケープ文字として認識されることのテスト。
     */
    @Test
    public void testEscapeB(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        record-separator: "\b"   # \bを設定
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X ()       # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      X Number   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        LayoutFileParser parser = new LayoutFileParser("./test.fmt");
        LayoutDefinition definition = parser.parse();
        String separator = (String)definition.getDirective().get("record-separator");
        assertEquals("\b", separator);
    }
    
    
    /**
     * レイアウト定義ファイルが存在しない場合のテスト。
     */
    public void testNotExistLayoutFile(){
        try {
            LayoutFileParser parser = new LayoutFileParser(
                    "notExistLayoutFile.fmt");
            parser.parse();
            fail();
        } catch (IllegalArgumentException e) {
            assertSame(FileNotFoundException.class, e.getCause().getClass());
            assertTrue(true);
        }
        
    }
    
    /**
     * フォーマット定義ファイルにデータタイプの記述がなくデフォルト値の記述のみがあった場合。
     */
    @Test
    public void testDefaultValueOnly(){
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************
        file-type:        "Variable"
        text-encoding:    "ms932"# MS932
        field-separator:  ","    # カンマ区切り
        
        [Books]
        1   Title      X          # タイトル
        2   Publisher  X          # 出版社
        3   Authors    X          # 著者
        4   Price      "aaa"   # 価格
        *****************************************/
        formatFile.deleteOnExit();
        
        try {
            LayoutFileParser parser = new LayoutFileParser("./test.fmt");
            parser.parse();
            fail();
        } catch (SyntaxErrorException e) {
            assertTrue(e.getMessage().contains("data type format was invalid. value=[\"aaa\"]. valid format=[[a-zA-Z_$][a-zA-Z0-9_$]*]."));
        }
    }
    
}
