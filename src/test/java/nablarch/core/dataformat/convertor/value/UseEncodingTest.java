package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link UseEncoding}のテストクラス。
 */
public class UseEncodingTest {

    private final UseEncoding sut = new UseEncoding();
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void initialize() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition();
        sut.initialize(fieldDefinition, "ms932");

        assertThat(fieldDefinition.getEncoding(), is(Charset.forName("ms932")));
    }

    @Test
    public void specifyEmptyOptions_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("parameter was not specified. parameter must be specified. convertor=[UseEncoding].");
        sut.initialize(new FieldDefinition());
    }

    @Test
    public void specifyNullToEncoding_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null. parameter=[null]. convertor=[UseEncoding].");
        sut.initialize(new FieldDefinition(), new Object[] {null});
    }

    @Test
    public void specifyNotStringToEncoding_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid parameter type was specified. parameter type must be 'String' but was: 'java.lang.StringBuilder'. parameter=[ms932]. convertor=[UseEncoding].");
        sut.initialize(new FieldDefinition(), new StringBuilder("ms932"));
    }

    @Test
    public void specifyNullToOptions_shouldThrowException() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("args must not be null");
        sut.initialize(new FieldDefinition(), null);
    }

    @Test
    public void write_shouldThrowUnsupported() throws Exception {
        expectedException.expect(UnsupportedOperationException.class);
        sut.convertOnWrite("");
    }

    @Test
    public void read_shouldThrowUnsupported() throws Exception {
        expectedException.expect(UnsupportedOperationException.class);
        sut.convertOnRead("");
    }
}