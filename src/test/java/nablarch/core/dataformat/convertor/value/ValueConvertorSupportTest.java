package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;

import nablarch.core.dataformat.FieldDefinition;

import org.junit.Assert;
import org.junit.Test;

/**
 * ValueConvertorSupportのgetterのテスト。
 * @author Masato Inoue
 */
public class ValueConvertorSupportTest {
    
    private ValueConvertorSupport sut = new ValueConvertorSupport() {
        @Override
        public Object convertOnRead(final Object data) {
            return null;
        }

        @Override
        public Object convertOnWrite(final Object data) {
            return null;
        }
    };
    
    @Test
    public void testGetter(){
        final FieldDefinition definition = new FieldDefinition();
        sut.initialize(definition);
        Assert.assertThat(sut.getField(), is(sameInstance(definition)));
    }
}
