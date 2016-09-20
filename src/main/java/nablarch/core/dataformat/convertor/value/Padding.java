package nablarch.core.dataformat.convertor.value;

import java.util.Arrays;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * パディング/トリム処理で使用する値を設定する。
 * このコンバーターはフィールド設定を行うのみで、実際のパディング/トリム処理はデータタイプのコンバータに委譲する。
 * 
 * @author Iwauo Tajima
 * @see nablarch.core.dataformat.convertor.datatype.SingleByteCharacterString
 * @see nablarch.core.dataformat.convertor.datatype.DoubleByteCharacterString
 */
public class Padding extends ValueConvertorSupport<Object, Object> {

    /** {@inheritDoc}
     * パディング/トリム処理で使用する値をフィールド定義に設定する。
     */
    public Padding initialize(FieldDefinition field, Object... args) {
        super.initialize(field, args);
        if (args.length != 1) {
            throw new SyntaxErrorException(String.format(
                "parameter size was invalid. parameter size must be one, but was [%s]. parameter=%s. convertor=[Padding].", args.length, Arrays.toString(args))
            );
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(String.format(
                    "1st parameter was null. parameter=%s. convertor=[Padding].", Arrays.toString(args))
                );
        }
        field.setPaddingValue(args[0]);
        return null;
    }

    /** {@inheritDoc}
     * データタイプのコンバータに処理を委譲するので、本メソッドは使用されない。
     */
    public Object convertOnRead(Object data) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     * データタイプのコンバータに処理を委譲するので、本メソッドは使用されない。
     */
    public Object convertOnWrite(Object data) {
        throw new UnsupportedOperationException();
    }
}
