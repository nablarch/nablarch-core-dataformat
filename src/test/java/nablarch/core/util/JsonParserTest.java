package nablarch.core.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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
     * アンエスケープ処理
     */
    @Test
    public void testUnescape() throws Exception {
        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("key0", "/");
                    put("key1", "a\b a\f a\n a\r a\t");
                    put("key2", "a\\b a\\f a\\n a\\r a\\t");
                    put("key3", "a\\\b a\\\f a\\\n a\\\r a\\\t");
                    put("key4", "a\\\\b a\\\\f a\\\\n a\\\\r a\\\\t");
                    put("key5", "a\" a\\ a/");
                    put("key6", "a\\\" a\\\\ a\\/");
                    put("key7", "a\\\\\" a\\\\\\ a\\\\/");
                    put("key8", "\"foo\" isn't \"bar\". specials: \b\r\n\f\t\\/");
                    put("key9", "\"\\\b\f\n\r\t");
                }};

        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testUnescape.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
        assertThat(result, is(expectedMap));
    }

    /**
     * アンエスケープ処理(例外処理)
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
     * コードポイントアンエスケープ処理
     */
    @Test
    public void testUnescapeCodepoint() throws Exception {

        // 期待結果Map
        Map<String, Object> expectedMap =
                new HashMap<String, Object>() {{
                    put("key1", "あいうえお\\u1234あ");
                }};

        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(
                "{\"key1\":\"\\u3042い\\u3046え\\u304a\\\\u1234\\u3042\"}");
        assertThat(result, is(expectedMap));
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

    /**
     * 入れ子の配列のテスト
     */
    @Test
    public void testNestedArrayParse() throws Exception {
        final InputStream resource = FileUtil.getResource(
                "classpath:nablarch/core/util/JsonParserTest/testNestedArrayParse.json");
        Map<String, Object> result = (Map<String, Object>) new JsonParser().parse(readAll(resource));
    }
}
