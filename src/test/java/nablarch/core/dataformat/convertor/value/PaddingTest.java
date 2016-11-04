package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * パディングコンバータのテストケース。
 * <p>
 * 観点：
 * パディング処理は、実際の処理を委譲するDoubleByteCharacterTestクラス、SingleByteCharacterTestクラスのテストで確認するので、
 * ここでは異常系の網羅のみ行う。
 *
 * @author Masato Inoue
 */
public class PaddingTest {

    private final Padding sut = new Padding();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void initialize() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition();
        sut.initialize(fieldDefinition, "A");

        assertThat((String) fieldDefinition.getPaddingValue(), is("A"));
    }

    @Test
    public void specifyTooManyOptions_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "parameter size was invalid. parameter size must be one, but was [2]. parameter=[1, 2]. convertor=[Padding].");

        sut.initialize(new FieldDefinition(), "1", "2");
    }

    @Test
    public void specifyEmptyOptions_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage(
                "parameter size was invalid. parameter size must be one, but was [0]. parameter=[]. convertor=[Padding].");
        sut.initialize(new FieldDefinition());
    }

    @Test
    public void specifyNullToPaddingChar_shouldThrowException() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null. parameter=[null]. convertor=[Padding].");

        sut.initialize(new FieldDefinition(), new Object[] {null});
    }

    @Test
    public void specifyNullOptions_shouldThrowException() throws Exception {
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
