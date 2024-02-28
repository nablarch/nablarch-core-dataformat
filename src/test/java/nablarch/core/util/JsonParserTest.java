package nablarch.core.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("serial")
public class JsonParserTest {

    /**
     * 階層パターンを網羅したJSONデータのテストです。
     */
    @Test
    public void testParseAllPatternOfStratumJson() throws Exception {

        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", "value2");
            put("key3", new ArrayList<Object>() {{
                add("value3-1");
                add("value3-2");
            }});

            put("Class1", new HashMap<String, Object>() {{
                put("ClassKey1", "Class1Value1");
                put("ClassKey2", "Class1Value2");
                put("ClassKey3", "3");
            }});

            put("Class2", new HashMap<String, Object>() {{
                put("Class2Key1", "Class2Value1");
                put("Class2Key2", "Class2Value2");
            }});

            put("Class3", new HashMap<String, Object>() {{
                put("Class3Key1", "Class3Value1");
                put("Class3Key2", "Class3Value2");
                put("Class3Key3", new ArrayList<Object>() {{
                    add("Class3Value3-1");
                    add("Class3Value3-2");
                    add("Class3Value3-3");
                    add("Class3Value3-4");
                }});
            }});

            put("Class4", new HashMap<String, Object>() {{
                put("Class4Key1", "Class4Value1");
                put("Class41", new HashMap<String, Object>() {{
                    put("Class41Key1", "Class4-1Value1");
                    put("Class41Key2", "Class4-1Value2");
                    put("Class41Key3", "Class4-1Value3");
                }});
                put("Class42", new HashMap<String, Object>() {{
                    put("Class42Key1", "Class4-2Value1");
                    put("Class42Key2", "Class4-2Value2");
                    put("Class42Key3", "Class4-2Value3");
                    put("Class421", new ArrayList<Object>() {{
                        add(new HashMap<String, Object>() {{
                            put("Class421Key1", "Class4-2-1[0]Value1");
                            put("Class421Key2", "Class4-2-1[0]Value2");
                        }});
                        add(new HashMap<String, Object>() {{
                            put("Class421Key1", "Class4-2-1[1]Value1");
                            put("Class421Key2", "Class4-2-1[1]Value2");
                        }});
                    }});
                }});
            }});
        }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testParseAllPatternOfStratumJson.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * データ型網羅
     */
    @Test
    public void testAllDataType() throws Exception {

        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("string_half", "value");
            put("string_wide", "バリュー");
            put("string_mix", "value バリュー");
            put("string_array", new ArrayList<Object>() {{
                add("array1");
                add("array2");
                add("array3");
            }});
            put("integer1", "123");
            put("integer2", "-123");
            put("float1", "123.456");
            put("float2", "-123.456");
            put("int_exp1", "123e10");
            put("int_exp2", "123e+10");
            put("int_exp3", "123e-10");
            put("int_exp4", "-123e10");
            put("int_exp5", "-123e+10");
            put("int_exp6", "-123e-10");
            put("flo_exp1", "123.456e10");
            put("flo_exp2", "123.456e+10");
            put("flo_exp3", "123.456e-10");
            put("flo_exp4", "-123.456e10");
            put("flo_exp5", "-123.456e+10");
            put("flo_exp6", "-123.456e-10");
            put("numeric_array", new ArrayList<Object>() {{
                add("123");
                add("456");
                add("789");
            }});
            put("boolean_true", "true");
            put("boolean_false", "false");
            put("boolean_array", new ArrayList<Object>() {{
                add("true");
                add("false");
                add("true");
            }});
            put("null", null);
            put("object", new HashMap<String, Object>() {{
                put("obj_string", "obj_value");
            }});
            put("object_array", new ArrayList<Object>() {{
                add(new HashMap<String, Object>() {{
                    put("obj_arr_str1", "obj_arr_val11");
                    put("obj_arr_str2", "obj_arr_val12");
                }});
                add(new HashMap<String, Object>() {{
                    put("obj_arr_str1", "obj_arr_val21");
                    put("obj_arr_str2", "obj_arr_val22");
                }});
            }});
        }};

        final InputStream stream = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testAllDataType.json");

        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(stream));
        assertThat(result, is(expectedMap));
    }

    /**
     * データ型エラー
     */
    @Test
    public void testDataTypeError() throws Exception {
        try {
            new JsonParser().parse("{\"not_literal\":abc}");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), e.getMessage(), containsString("found invalid token:abc"));
        }
    }

    /**
     * オブジェクト形式で始まっていない
     */
    @Test
    public void testNotStartsWithObject() throws Exception {

        try {
            new JsonParser().parse("\"key\":\"value\"}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("JSON data must starts with '{'"));
        }
    }

    /**
     * オブジェクト形式で終わっていない
     */
    @Test
    public void testNotEndsWithObject() throws Exception {
        try {
            new JsonParser().parse("{\"key\":\"value\"");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("JSON data must ends with '}'"));
        }
    }

    /**
     * オブジェクトの開始位置が不正
     */
    @Test
    public void testInvalidObjectStart() throws Exception {

        try {
            new JsonParser().parse("{{\"key\", \"value\"}}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("incorrect object starting position"));
        }
    }

    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd1() throws Exception {

        try {
            new JsonParser().parse("{\"key1\":}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("incorrect object ending position"));
        }
    }

    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd2() throws Exception {
        try {
            new JsonParser().parse("{}}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("object ending but current object is null"));
        }
    }

    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd3() throws Exception {
        try {
            new JsonParser().parse("{},\"key\":\"value\"}   ");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("current object is null. token:[\"key\"]"));
        }
    }

    /**
     * 配列の開始位置が不正
     * JSONの仕様では二重配列にした場合など、配列の前に":"がないパターンも許容されるが
     * フォーマット定義ファイルの仕様上、項目には名前をつけるため、配列の前には必ず":"がくる必要がある。
     * 以下のような構造はJSON上はOKだが、汎用データフォーマット機能上はNG
     * {"object":[[1,2],[3,4]]}
     * 以下のようにする必要がある。
     * {"object":[{"array1":[1,2]},{"array2":[3,4]}]}
     */
    @Test
    public void testInvalidArrayStart() throws Exception {

        try {
            new JsonParser().parse("{[\"key\", \"value\"]}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("array is need start after :"));
        }
    }

    /**
     * 配列の終了位置が不正
     */
    @Test
    public void testInvalidArrayEnd() throws Exception {
        try {
            new JsonParser().parse("{]}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("array end detected, but not started"));
        }
    }

    /**
     * 空のオブジェクト
     */
    @Test
    public void testEmptyObject() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>();

        Map<String, ?> result = new JsonParser().parse("{}");
        assertEquals(expectedMap, result);
    }

    /**
     * 空の配列
     */
    @Test
    public void testEmptyArray() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("key1", new ArrayList<Object>());
        }};

        Map<String, ?> result = new JsonParser().parse("{\"key1\":[]}");
        assertEquals(expectedMap, result);
    }

    /**
     * キーが文字列以外
     */
    @Test
    public void testKeyIsNotString() throws Exception {

        try {
            new JsonParser().parse("{1:\"value\"}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("key is not string"));
        }
    }

    /**
     * 値が存在しない
     */
    @Test
    public void testNoValue() throws Exception {
        try {
            new JsonParser().parse("{\"key1\":, \"key2\":\"value2\"}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("value is requires"));
        }
    }

    /**
     * エスケープシーケンスが単体で存在する場合、エスケープシーケンスとして解析される
     */
    @Test
    public void testSingleUnescape() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("quotationMark"   , "\"");
                    put("reverseSolidus"  , "\\");
                    put("escapedSolidus"  , "/");
                    put("backspace"       , "\b");
                    put("formFeed"        , "\f");
                    put("lineFeed"        , "\n");
                    put("carriageReturn"  , "\r");
                    put("tab"             , "\t");
                    put("unescapedSolidus", "/");
                    put("codePoint"       , "a");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testSingleUnescape.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * コードポイントを使用したエスケープシーケンスが単体で存在する場合、エスケープシーケンスとして解析される
     */
    @Test
    public void testSingleUnescapeCodepoint() throws Exception {

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("unicodeQuotationMark" , "\"");
                    put("unicodeReverseSolidus", "\\");
                    put("unicodeSolidus"       , "/");
                    put("unicodeBackspace"     , "\b");
                    put("unicodeFormFeed"      , "\f");
                    put("unicodeLineFeed"      , "\n");
                    put("unicodeCarriageReturn", "\r");
                    put("unicodeTab"           , "\t");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testSingleUnescapeCodepoint.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * エスケープシーケンスが連続する場合、それぞれがエスケープシーケンスとして解析される。
     * 1文字目のエスケープシーケンスはどれでも違いはないため、代表値として固有の処理がある「/"」と「//」を使用する。
     */
    @Test
    public void testMultipleUnescape() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    /*
                     * 以下、テストケースのキーに用いてる略称
                     * QM → quotationMark
                     * RS → reverseSolidusj
                     * ES → escapedSolidus
                     * BS → backspace
                     * FF → formFeed
                     * LF → lineFeed
                     * CR → carriageReturn
                     * TB → tab
                     * US → unescapedSolidus
                     * CP → codePoint
                     * */
                    put("QM_QM", "\"\"");
                    put("QM_RS", "\"\\");
                    put("QM_ES", "\"/");
                    put("QM_BS", "\"\b");
                    put("QM_FF", "\"\f");
                    put("QM_LF", "\"\n");
                    put("QM_CR", "\"\r");
                    put("QM_TB", "\"\t");
                    put("QM_US", "\"/");
                    put("QM_CP", "\"a");

                    put("RS_QM", "\\\"");
                    put("RS_RS", "\\\\");
                    put("RS_ES", "\\/");
                    put("RS_BS", "\\\b");
                    put("RS_FF", "\\\f");
                    put("RS_LF", "\\\n");
                    put("RS_CR", "\\\r");
                    put("RS_TB", "\\\t");
                    put("RS_US", "\\/");
                    put("RS_CP", "\\a");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testMultipleUnescape.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * エスケープシーケンスの後に文字がある場合、エスケープシーケンスと文字として解析されること
     */
    @Test
    public void testEscapeSequenceAndChar() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("quotationMark"   , "\"a");
                    put("reverseSolidus"  , "\\a");
                    put("escapedSolidus"  , "/a");
                    put("backspace"       , "\ba");
                    put("formFeed"        , "\fa");
                    put("lineFeed"        , "\na");
                    put("carriageReturn"  , "\ra");
                    put("tab"             , "\ta");
                    put("unescapedSolidus", "/a");
                    put("codePoint"       , "aa");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testEscapeSequenceAndChar.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * 文字の後にエスケープシーケンスがある場合、エスケープシーケンスと文字として解析されること
     */
    @Test
    public void testCharAndEscapeSequence() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("quotationMark"   , "a\"");
                    put("reverseSolidus"  , "a\\");
                    put("escapedSolidus"  , "a/");
                    put("backspace"       , "a\b");
                    put("formFeed"        , "a\f");
                    put("lineFeed"        , "a\n");
                    put("carriageReturn"  , "a\r");
                    put("tab"             , "a\t");
                    put("unescapedSolidus", "a/");
                    put("codePoint"       , "aa");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testCharAndEscapeSequence.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * 文字と文字の間にエスケープシーケンスがある場合、エスケープシーケンスと文字として解析されること
     */
    @Test
    public void testCharAndEscapeSequenceAndChar() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("quotationMark"   , "a\"a");
                    put("reverseSolidus"  , "a\\a");
                    put("escapedSolidus"  , "a/a");
                    put("backspace"       , "a\ba");
                    put("formFeed"        , "a\fa");
                    put("lineFeed"        , "a\na");
                    put("carriageReturn"  , "a\ra");
                    put("tab"             , "a\ta");
                    put("unescapedSolidus", "a/a");
                    put("codePoint"       , "aaa");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testCharAndEscapeSequenceAndChar.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }


    /**
     * エスケープしたバックスラッシュと文字が組み合わせた場合、文字がエスケープシーケンスとして解析されないこと
     */
    @Test
    public void testUnescapeWithbfnrt() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("b", "\\b");
                    put("f", "\\f");
                    put("n", "\\n");
                    put("r", "\\r");
                    put("t", "\\t");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testUnescapeWithbfnrt.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * エスケープしたバックスラッシュとコードポイントを組み合わせた場合、エスケープシーケンスとして解析されないこと
     */
    @Test
    public void testUnescapeCodepointWithuxxxx() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("uxxxx", "\\u005C");
                }};

        Map<String, ?> result = new JsonParser().parse("{\"uxxxx\":\"\\\\u005C\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * アンエスケープ処理(例外処理)
     * エスケープ対象外の文字をエスケープしようとした場合にパースが失敗することを確認する。
     */
    @Test
    public void testUnescapeError() throws Exception {
        try {
            new JsonParser().parse("{\"key0\":\"\\a\"}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("found invalid json format :"));
        }
    }

    /**
     * コードポイントアンエスケープ処理(例外処理)
     */
    @Test
    public void testUnescapeCodepointError() throws Exception {

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testUnescapeCodepointError.json");
        try {

            new JsonParser().parse(readAll(resource));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("found invalid unicode string :"));
        }
    }

    /**
     * ホワイトスペースがどこに入力されていても、パースできることを確認する。
     */
    @Test
    public void testWhiteSpace() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("noWhiteSpace", "a");
                    put("fullWhiteSpace", "a");
                    put("fullTab", "a");
                    put("fullNewLine", "a");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testWhiteSpace.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * 空文字列
     */
    @Test
    public void testEmptyString() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("key1", "");
                }};

        Map<String, ?> result = new JsonParser().parse("{\"key1\":\"\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * カンマなし
     */
    @Test
    public void testNoComma1() throws Exception {
        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testNoComma1.json");

        try {
            new JsonParser().parse(readAll(resource));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("last token is not separator. token:[\"key2\"]"));
        }
    }

    /**
     * カンマなし
     */
    @Test
    public void testNoComma2() throws Exception {

        try {
            new JsonParser().parse("{\"key1\":\"value1\" \"key2\":\"value2\"}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("last token is not separator. token:[\"key2\"]"));
        }
    }

    /**
     * タブインデント
     */
    @Test
    public void testTabIndent() throws Exception {

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }};


        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testTabIndent.json");
        Map<String, ?> result = new JsonParser().parse(readAll(resource));
        assertEquals(expectedMap, result);
    }

    /**
     * 入れ子の配列のテスト
     * 配列の中にオブジェクトの配列を持ち、その後ろに項目を持つパターンと
     * 配列の中に配列が連続で並ぶパターン、3重配列のパターンをテスト。
     */
    @Test
    public void testNestedArrayParse() throws Exception {

        //期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("NestedArray", new ArrayList<Object>() {{
                        add(new HashMap<String, Object>() {{
                            put("key1", "value1");
                            put("NestedArray1", new ArrayList<Object>() {{
                                add(new HashMap<String, Object>() {{
                                    put("NAKey11", "NAValue11");
                                }});
                                add(new HashMap<String, Object>() {{
                                    put("NAKey12", "NAValue12");
                                }});
                            }});
                            put("key2", "value2");
                            put("NestedArray2", new ArrayList<String>() {{
                                add("NAValue2");
                            }});
                            put("NestedArray3", new ArrayList<Object>() {{
                                add(new HashMap<String, Object>() {{
                                    put("NestedArray4", new ArrayList<String>() {{
                                        add("NAValue4");
                                    }});
                                }});
                            }});
                        }});
                    }});
                }};
        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testNestedArrayParse.json");
        Map<String, ?> result = new JsonParser().parse(readAll(resource));
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ場合のテスト
     * {"{":"{"}
     */
    @Test
    public void testOnlyObjectStartValue() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("{", "{");
        }};
        Map<String, ?> result = new JsonParser().parse("{\"{\":\"{\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ場合のテスト
     * {"}":"}"}
     */
    @Test
    public void testOnlyObjectEndValue() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("}", "}");
        }};
        Map<String, ?> result = new JsonParser().parse("{\"}\":\"}\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ場合のテスト
     * {"[":"["}
     */
    @Test
    public void testOnlyArrayStartValue() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("[", "[");
        }};
        Map<String, ?> result = new JsonParser().parse("{\"[\":\"[\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ場合のテスト
     * {"]":"]"}
     */
    @Test
    public void testOnlyArrayEndValue() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("]", "]");
        }};
        Map<String, ?> result = new JsonParser().parse("{\"]\":\"]\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ場合のテスト
     * {":":":"}
     */
    @Test
    public void testOnlyColonValue() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put(":", ":");
        }};
        Map<String, ?> result = new JsonParser().parse("{\":\":\":\"}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ配列のテスト
     * {"{":["{"]}
     */
    @Test
    public void testOnlyObjectStartValueInArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("{", new ArrayList<String>() {{
                add("{");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"{\":[\"{\"]}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ配列のテスト
     * {"}":["}"]}
     */
    @Test
    public void testOnlyObjectEndValueInArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("}", new ArrayList<String>() {{
                add("}");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"}\":[\"}\"]}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ配列のテスト
     * {"[":["["]}
     */
    @Test
    public void testOnlyArrayStartValueInArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("[", new ArrayList<String>() {{
                add("[");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"[\":[\"[\"]}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ配列のテスト
     * {"]":["]"]}
     */
    @Test
    public void testOnlyArrayEndValueInArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("]", new ArrayList<String>() {{
                add("]");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"]\":[\"]\"]}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ配列のテスト
     * {":":[":"]}
     */
    @Test
    public void testOnlyColonValueInArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put(":", new ArrayList<String>() {{
                add(":");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\":\":[\":\"]}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ子オブジェクトのテスト
     * {"{":{"{":"{"}}
     */
    @Test
    public void testOnlyObjectStartValueInNestedObject() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("{", new HashMap<String, Object>() {{
                put("{", "{");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"{\":{\"{\":\"{\"}}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ子オブジェクトのテスト
     * {"}":{"}":"}"}}
     */
    @Test
    public void testOnlyObjectEndValueInNestedObject() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("}", new HashMap<String, Object>() {{
                put("}", "}");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"}\":{\"}\":\"}\"}}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ子オブジェクトのテスト
     * {"[":{"[":"["}}
     */
    @Test
    public void testOnlyArrayStartValueInNestedObject() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("[", new HashMap<String, Object>() {{
                put("[", "[");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"[\":{\"[\":\"[\"}}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ子オブジェクトのテスト
     * {"]":{"]":"]"}}
     */
    @Test
    public void testOnlyArrayEndValueInNestedObject() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("]", new HashMap<String, Object>() {{
                put("]", "]");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\"]\":{\"]\":\"]\"}}");
        assertEquals(expectedMap, result);
    }

    /**
     * セパレータだけを値にもつ子オブジェクトのテスト
     * {":":{":":":"}}
     */
    @Test
    public void testOnlyColonValueInNestedArray() {
        HashMap<String, Object> expectedMap = new HashMap<String, Object>() {{
            put(":", new HashMap<String, Object>() {{
                put(":", ":");
            }});
        }};
        Map<String, ?> result = new JsonParser().parse("{\":\":{\":\":\":\"}}");
        assertEquals(expectedMap, result);
    }

    private String readAll(InputStream stream) throws Exception {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
        try {
            final StringBuilder builder = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                builder.append(line);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }
}
