package nablarch.core.dataformat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * データレコードのテスト
 * 
 * 観点：
 * DataRecord持つ変換メソッドのテストを網羅する。
 * 
 * @author Iwauo Tajima
 */
public class DataRecordTest {

    @Test
    public void testConversion() {
        DataRecord record = new DataRecord();
        
        record.put("stringData", "0123456789");
        record.put("stringArrayData", new String[]{"0123456789", "9876543210"});
        record.put("digitData",  BigDecimal.valueOf(123456789));
        record.put("binaryData", new byte[] { 0x30, 0x31, 0x32, 0x33, 0x34});
        record.put("nullData",   null);
        
        // 文字列データの変換
        assertEquals(record.get("stringData"), "0123456789");
        assertEquals(record.getString("stringData"), "0123456789");
        assertEquals(record.getBigDecimal("stringData"), BigDecimal.valueOf(123456789));
        try {
            record.getBytes("stringData");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }
        
        // 文字列配列データはget()かgetStringArray()しか意味がない。
        assertArrayEquals((String[])record.get("stringArrayData"), new String[]{"0123456789", "9876543210"});
        assertArrayEquals(record.getStringArray("stringArrayData"), new String[]{"0123456789", "9876543210"});
        try {
            record.getBytes("stringArrayData");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }
        
        // 数値データの変換
        assertEquals(record.get("digitData"), BigDecimal.valueOf(123456789));
        assertEquals(record.getString("digitData"), "123456789");
        assertEquals(record.getBigDecimal("digitData"), BigDecimal.valueOf(123456789));
        try {
            record.getBytes("digitData");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }
        
        // バイトデータはget()かgetByte()しか意味がない。
        for (int i=0; i<5; i++) {
            assertEquals((byte) (0x30 + i), ((byte[])record.get("binaryData"))[i]);
        }
        
        for (int i=0; i<5; i++) {
            assertEquals((byte) (0x30 + i), record.getBytes("binaryData")[i]);
        }
        
        try {
            record.getBigDecimal("binaryData");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof NumberFormatException);
        }
        assertTrue(record.getString("binaryData").startsWith("[B@")); //ヒープアドレス表現
        
        // nullはnull
        assertNull(record.get("nullData"));
        assertNull(record.getString("nullData"));
        assertNull(record.getStringArray("nullData"));
        assertNull(record.getBigDecimal("nullData"));
        assertNull(record.getBytes("nullData"));

    }
    
    /**
     * putの引数がnullまたは空文字
     */
    @Test
    public void testIllegalEntry() {
        DataRecord record = new DataRecord();
        try {
            record.put(null, "hoge");
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        try {
            record.put("", "hoge");
            fail();
        } catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testBigDecimalData() throws Exception {
        final DataRecord record = new DataRecord();

        final BigDecimal input = new BigDecimal("0.0000000001");
        record.put("decimal", input);

        assertThat(record.getString("decimal"), CoreMatchers.is("0.0000000001"));
        assertThat(record.get("decimal"), CoreMatchers.<Object>is(input));
        assertThat(record.getBigDecimal("decimal"), CoreMatchers.is(input));
    }
}
