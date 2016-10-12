package nablarch.core.util;

import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "key1":"value1",
            "key2":"value2",
            "key3":["value3-1","value3-2"],
            "Class1":{
                "ClassKey1":"Class1Value1",
                "ClassKey2":"Class1Value2",
                "ClassKey3":"3"
            },
            "Class2":{
                "Class2Key1":"Class2Value1",
                "Class2Key2":"Class2Value2"
            },
            "Class3":{
                "Class3Key1":"Class3Value1",
                "Class3Key2":"Class3Value2",
                "Class3Key3":["Class3Value3-1","Class3Value3-2","Class3Value3-3","Class3Value3-4"]
            },
            "Class4":{
                "Class4Key1":"Class4Value1",
                "Class41":{
                    "Class41Key1":"Class4-1Value1",
                    "Class41Key2":"Class4-1Value2",
                    "Class41Key3":"Class4-1Value3"
                },
                "Class42":{
                    "Class42Key1":"Class4-2Value1",
                    "Class42Key2":"Class4-2Value2",
                    "Class42Key3":"Class4-2Value3",
                    "Class421":[{
                        "Class421Key1":"Class4-2-1[0]Value1",
                        "Class421Key2":"Class4-2-1[0]Value2"
                        },{
                        "Class421Key1":"Class4-2-1[1]Value1",
                        "Class421Key2":"Class4-2-1[1]Value2"
                        }]
                }
            }
        }
        *********************************************/
        
        // 期待結果Map
		Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", "value2");
            put("key3", new ArrayList<Object>() {{
                add("value3-1");
                add("value3-2");
            }});
            
            put( "Class1", new HashMap<String, Object>() {{
                put("ClassKey1", "Class1Value1");
                put("ClassKey2", "Class1Value2");
                put("ClassKey3", "3");
            }});
            
            put( "Class2", new HashMap<String, Object>() {{
                put("Class2Key1", "Class2Value1");
                put("Class2Key2", "Class2Value2");
            }});
            
            put( "Class3", new HashMap<String, Object>() {{
                put("Class3Key1", "Class3Value1");
                put("Class3Key2", "Class3Value2");
                put("Class3Key3", new ArrayList<Object>() {{
                    add("Class3Value3-1");
                    add("Class3Value3-2");
                    add("Class3Value3-3");
                    add("Class3Value3-4");
                }});
            }});
            
            put( "Class4", new HashMap<String, Object>() {{
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
                    put( "Class421", new ArrayList<Object>() {{
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

        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    
    /**
     * データ型網羅
     */
    @Test
    public void testAllDataType() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "string_half":"value",
            "string_wide":"バリュー",
            "string_mix":"value バリュー",
            "string_array":["array1", "array2", "array3"],
            "integer1":123,
            "integer2":-123,
            "float1":123.456,
            "float2":-123.456,
            "int_exp1":123e10,
            "int_exp2":123e+10,
            "int_exp3":123e-10,
            "int_exp4":-123e10,
            "int_exp5":-123e+10,
            "int_exp6":-123e-10,
            "flo_exp1":123.456e10,
            "flo_exp2":123.456e+10,
            "flo_exp3":123.456e-10,
            "flo_exp4":-123.456e10,
            "flo_exp5":-123.456e+10,
            "flo_exp6":-123.456e-10,
            "numeric_array":[123, 456, 789],
            "boolean_true":true,
            "boolean_false":false,
            "boolean_array":[true, false, true],
            "null":null,
            "object":{
                    "obj_string":"obj_value"
            },
            "object_array":[
                {
                    "obj_arr_str1":"obj_arr_val11",
                    "obj_arr_str2":"obj_arr_val12"
                },
                {
                    "obj_arr_str1":"obj_arr_val21",
                    "obj_arr_str2":"obj_arr_val22"
                }
            ]
        }
        *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put("string_half", "value");
            put("string_wide", "バリュー");
            put("string_mix", "value バリュー");
            put("string_array", new ArrayList<Object>(){{
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
            put("numeric_array", new ArrayList<Object>(){{
                add("123");
                add("456");
                add("789");
            }});
            put("boolean_true", "true");
            put("boolean_false", "false");
            put("boolean_array", new ArrayList<Object>(){{
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

        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    /**
     * データ型エラー
     */
    @Test
    public void testDataTypeError() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "not_literal":abc
        }
        *********************************************/

        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("found invalid token:abc"));
        }
    }
    
    /**
     * オブジェクト形式で始まっていない
     */
    @Test
    public void testNotStartsWithObject() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
            "key":"value"
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("JSON data must starts with '{'"));
        }
    }
    
    /**
     * オブジェクト形式で終わっていない
     */
    @Test
    public void testNotEndsWithObject() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "key":"value"
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("JSON data must ends with '}'"));
        }
    }
    
    /**
     * オブジェクトの開始位置が不正
     */
    @Test
    public void testInvalidObjectStart() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            {"key", "value"}
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("incorrect object starting position"));
        }
    }
    
    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd1() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key1":
        }   
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("incorrect object ending position"));
        }
    }
    
    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd2() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          }
        }   
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("object ending but current object is null"));
        }
    }
    
    /**
     * オブジェクトの終了位置が不正
     */
    @Test
    public void testInvalidObjectEnd3() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
        },"key":"value"}   
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("current object is null. token:[\"key\"]"));
        }
    }
    
    /**
     * 配列の開始位置が不正
     */
    @Test
    public void testInvalidArrayStart() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            ["key", "value"]
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("array is need start after :"));
        }
    }
    
    /**
     * 配列の終了位置が不正
     */
    @Test
    public void testInvalidArrayEnd() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            ]
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("array end detected, but not started"));
        }
    }
    
    /**
     * 空のオブジェクト
     */
    @Test
    public void testEmptyObject() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {}
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>();
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    /**
     * 空の配列
     */
    @Test
    public void testEmptyArray() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "key1":[]
        }
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = new HashMap<String, Object>(){{
            put("key1", new ArrayList<Object>());
        }};
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    /**
     * キーが文字列以外
     */
    @Test
    public void testKeyIsNotString() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            1:"value"
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("key is not string"));
        }
    }

    /**
     * 値が存在しない
     */
    @Test
    public void testNoValue() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
            "key1":,
            "key2":"value2"
        }
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("value is requires"));
        }
    }
    
    /**
     * アンエスケープ処理
     */
    @Test
    public void testUnescape() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key0":"\/",
          "key1":"a\b a\f a\n a\r a\t",
          "key2":"a\\b a\\f a\\n a\\r a\\t",
          "key3":"a\\\b a\\\f a\\\n a\\\r a\\\t",
          "key4":"a\\\\b a\\\\f a\\\\n a\\\\r a\\\\t",
          "key5":"a\" a\\ a/",
          "key6":"a\\\" a\\\\ a\\/",
          "key7":"a\\\\\" a\\\\\\ a\\\\/",
          "key8":"\"foo\" isn't \"bar\". specials: \b\r\n\f\t\\/",
          "key9":"\"\\\b\f\n\r\t"
        }   
         *********************************************/
        
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
                }}
        ;
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    /**
     * アンエスケープ処理(例外処理)
     */
    @Test
    public void testUnescapeError() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key0":"\a"
        }
         *********************************************/

        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("found invalid json format :"));
        }
    }

    
    
    
    /**
     * コードポイントアンエスケープ処理
     */
    @Test
    public void testUnescapeCodepoint() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key1":"\u3042い\u3046え\u304a\\u1234\u3042"
        }   
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "あいうえお\\u1234あ");
                }}
        ;
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }

    /**
     * コードポイントアンエスケープ処理(例外処理)
     */
    @Test
    public void testUnescapeCodepointError() throws Exception {
        // 変換対象データ
        String u1 = "\\";
        String u2 = "u";
        String u3 = "3";
        String u4 = "0";
        String u5 = "4";
        String u6 = "a";
        String json = Hereis.string(u1, u2, u3, u4, u5, u6);
        /*******************************************
        {
          "key1":"\u3042い\u3046え\u304a${u1}${u2}${u3}${u4}${u5}"
        }
         *********************************************/

        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("found invalid unicode string :"));
        }
    }
    
    /**
     * 空文字列
     */
    @Test
    public void testEmptyString() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key1":""
        }   
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "");
                }}
        ;
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
    /**
     * カンマなし
     */
    @Test
    public void testNoComma1() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key1":"value1"
          "key2":"value2"
        }   
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("last token is not separator. token:[\"key2\"]"));
        }
    }
    
    /**
     * カンマなし
     */
    @Test
    public void testNoComma2() throws Exception {
        // 変換対象データ
        String json = Hereis.string();
        /*******************************************
        {
          "key1":"value1" "key2":"value2"
        }   
         *********************************************/
        
        try {
            new JsonParser().parse(json);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("last token is not separator. token:[\"key2\"]"));
        }
    }
    
    /**
     * タブインデント
     */
    @Test
    public void testTabIndent() throws Exception {
        // 変換対象データ
        String indent = "\t";
        String json = Hereis.string(indent);
        /*******************************************
        {
        ${indent}"key1":"value1",
        ${indent}"key2":"value2"
        }   
         *********************************************/
        
        // 期待結果Map
        Map<String, Object> expectedMap = 
                new HashMap<String, Object>() {{
                    put("key1", "value1");
                    put("key2", "value2");
                }}
        ;
        
        Map<String, ?> result = new JsonParser().parse(json);
        assertEquals(expectedMap, result);
    }
    
}
