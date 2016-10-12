package nablarch.core.dataformat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldDefinitionUtilTest {

    /**
     * 非単語文字区切りの文字列の変換バリエーションテスト
     */
    @Test
    public void testNormalizeWithNonWordChar() {
        testNormalizeWithNonWordChar(null, null);
        testNormalizeWithNonWordChar("", "");
        testNormalizeWithNonWordChar("a", "a");
        testNormalizeWithNonWordChar("ab", "ab");
        testNormalizeWithNonWordChar("aB", "a:b");
        testNormalizeWithNonWordChar("aBC", "a:b:c");
        testNormalizeWithNonWordChar("aBC", "A:B:C");
        testNormalizeWithNonWordChar("bC", ":b:c");
        testNormalizeWithNonWordChar("nsKey1.nsKey2", "ns:key1.ns:key2");
        testNormalizeWithNonWordChar("nsKey1[5].nsKey2", "ns:key1[5].ns:key2");
    }
    
    /**
     * 非単語文字区切りの文字列の変換バリエーションテストのサブメソッド
     * @param expected 期待値
     * @param actual 入力値
     */
    private void testNormalizeWithNonWordChar(String expected, String actual) {
        assertEquals(expected, FieldDefinitionUtil.normalizeWithNonWordChar(actual));
    }

    /**
     * 先頭大文字変換のバリエーションテスト
     */
    @Test
    public void testToUpperFirstChar() {
        assertEquals(null, FieldDefinitionUtil.toUpperFirstChar(null));    
        assertEquals("", FieldDefinitionUtil.toUpperFirstChar(""));    
        assertEquals("Abc", FieldDefinitionUtil.toUpperFirstChar("abc"));    
        assertEquals("ABC", FieldDefinitionUtil.toUpperFirstChar("ABC"));    
    }

    /**
     * 先頭小文字変換のバリエーションテスト
     */
    @Test
    public void testToLowerFirstChar() {
        assertEquals(null, FieldDefinitionUtil.toLowerFirstChar(null));    
        assertEquals("", FieldDefinitionUtil.toLowerFirstChar(""));    
        assertEquals("abc", FieldDefinitionUtil.toLowerFirstChar("abc"));    
        assertEquals("aBC", FieldDefinitionUtil.toLowerFirstChar("ABC"));    
    }

}
