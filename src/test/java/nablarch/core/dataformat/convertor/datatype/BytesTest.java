package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * バイト型コンバータ{@link Bytes}のテスト。
 *
 * @author TIS
 */
public class BytesTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Bytes sut = new Bytes();

    /**
     * 初期化時にnullをわたすと例外がスローされること。
     */
    @Test
    public void testInitializeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("initialize parameter was null. parameter must be specified. convertor=[Bytes].");

        sut.initialize(null);
    }

    /**
     * 初期化時の第一引数（バイト長）に0を設定すると例外がスローされること。
     */
    @Test
    public void testInitializeSizeZero() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter was specified. 1st parameter must be positive number, but was [0].");

        sut.initialize(0, null);
    }

    /**
     * 初期化時の第一引数（バイト長）に負数を設定すると例外がスローされること。
     */
    @Test
    public void testInitializeSizeNegative() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("invalid parameter was specified. 1st parameter must be positive number, but was [-1].");

        sut.initialize(-1, null);
    }

    /**
     * 初期化時の第一引数（バイト長）にnullを設定すると例外がスローされること。
     */
    @Test
    public void testInitializeSizeNull() {
        exception.expect(SyntaxErrorException.class);
        exception.expectMessage("1st parameter was null. parameter=[null, null]. convertor=[Bytes].");

        sut.initialize(null, null);
    }

    /**
     * 正常に入力できること。
     */
    @Test
    public void testReadNormal() {
        sut.init(new FieldDefinition(), 3);

        assertThat(sut.convertOnRead("abc".getBytes()), is("abc".getBytes()));
    }

    /**
     * 正常に出力できること。
     */
    @Test
    public void testWriteNormal() {
        sut.init(new FieldDefinition(), 3);

        assertThat(sut.convertOnWrite("cba".getBytes()), is("cba".getBytes()));
    }

    /**
     * 出力時にパラメータがnullのテスト。
     */
    @Test
    public void testWriteParameterNull() {
        sut.init(new FieldDefinition(), 1);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter must be not null.");

        sut.convertOnWrite(null);
    }

    /**
     * 出力時に出力対象のバイト長が多い場合のテスト。
     */
    @Test
    public void testWriteOverLength() {
        sut.init(new FieldDefinition(), 3);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter length = [4], expected = [3].");

        sut.convertOnWrite("abcd".getBytes());
    }

    /**
     * 出力時に出力対象のバイト長が少ない場合のテスト。
     */
    @Test
    public void testWriteUnderLength() {
        sut.init(new FieldDefinition(), 3);

        exception.expect(InvalidDataFormatException.class);
        exception.expectMessage("invalid parameter was specified. parameter length = [2], expected = [3].");

        sut.convertOnWrite("ab".getBytes());
    }

    /**
     * 出力時に出力対象がバイト配列でない場合のテスト。
     */
    @Test
    public void testWriteNotByteArray() {
        sut.init(new FieldDefinition().setName("bytes"), 3);

        exception.expect(allOf(
                instanceOf(InvalidDataFormatException.class),
                hasProperty("message", is(Matchers.containsString("invalid parameter type was specified. parameter must be a byte array."))),
                hasProperty("fieldName", is("bytes"))
        ));

        sut.convertOnWrite("abc");
    }

    /**
     * {@link DataType#removePadding}のテスト。
     * {@link Bytes}はトリム・パディングをしない。
     */
    @Test
    public void testRemovePadding() {
        sut.init(new FieldDefinition(), 5);

        byte[] data = new byte[] {
            0x00, 0x01, 0x02, 0x03, 0x04
        };

        assertThat(sut.removePadding(data), is(data));
    }
}
