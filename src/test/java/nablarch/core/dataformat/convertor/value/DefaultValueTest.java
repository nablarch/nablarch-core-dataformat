package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * デフォルト値コンバータのテスト。
 * <p>
 * 観点：
 * 読み込み時にデフォルト値が無視されることのテスト、出力時にデフォルト値が正しく書き込まれることのテスト。
 *
 * @author Masato Inoue
 */
public class DefaultValueTest {


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final DefaultValue sut = new DefaultValue();


    @Test
    public void writeNull_shouldReturnDefaultValue() throws Exception {
        sut.initialize(new FieldDefinition(), "default value");
        assertThat(((String) sut.convertOnWrite(null)), is("default value"));
    }

    @Test
    public void writeEmptyString_shouldReturnEmptyString() throws Exception {
        sut.initialize(new FieldDefinition(), "default value");

        assertThat(((String) sut.convertOnWrite("")), is(""));
    }

    @Test
    public void writeZero_shouldReturnZero() throws Exception {
        sut.initialize(new FieldDefinition(), 999);
        assertThat((Integer) sut.convertOnWrite(0), is(0));
    }

    @Test
    public void readNull_shouldReturnNull() throws Exception {
        sut.initialize(new FieldDefinition(), "default value");
        assertThat(sut.convertOnRead(null), is(nullValue()));
    }

    @Test
    public void readEmpty_shouldReturnEmpty() throws Exception {
        sut.initialize(new FieldDefinition(), "default value");
        assertThat((String) sut.convertOnRead(""), is(""));
    }

    @Test
    public void readZero_shouldReturnZero() throws Exception {
        sut.initialize(new FieldDefinition(), 999);
        assertThat((Integer) sut.convertOnRead(0), is(0));
    }
    @Test
    public void testNotSpecifyOptions() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("parameter size was invalid");
        sut.initialize(new FieldDefinition());
    }

    @Test
    public void testSpecifyOptionsOverLimit() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("parameter size was invalid");
        sut.initialize(new FieldDefinition(), "1", "2");
    }

    @Test
    public void testSpecifyNullToOptions() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("args must not be null");
        sut.initialize(new FieldDefinition(), (Object[]) null);
    }

    @Test
    public void testSpecifyNullToFirstOption() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null.");
        sut.initialize(new FieldDefinition(), new Object[] {null});
    }
}
