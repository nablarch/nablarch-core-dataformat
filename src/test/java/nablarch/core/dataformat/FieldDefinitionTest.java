package nablarch.core.dataformat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@link FieldDefinition}のテスト。
 * @author Masato Inoue
 */
public class FieldDefinitionTest {

    @Test
    public void testConversion() {
        FieldDefinition definition = new FieldDefinition();
        try {
            definition.getSize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("data type was not set. data type must be set before run this method.", e.getMessage());
        }
    }
}
