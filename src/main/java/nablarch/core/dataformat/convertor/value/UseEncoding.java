package nablarch.core.dataformat.convertor.value;

import java.nio.charset.Charset;
import java.util.Arrays;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * フィールドの文字エンコーディングを指定する コンバータ。
 * このコンバーターはフィールドの設定処理のみを行うのみで、実際のエンコーディング処理は、データタイプのコンバータに委譲する。
 * 
 * @author Iwauo Tajima
 */
public class UseEncoding extends ValueConvertorSupport<Object, Object> {
    
    /** {@inheritDoc}
     * この実装では、フィールドの文字エンコーディグの設定をおこなう。
     * データタイプのコンバータに処理を委譲するので、実際の変換処理の中でこのコンバータが呼ばれることはない。
     */
    public UseEncoding initialize(FieldDefinition field, Object... args) {
        super.initialize(field, args);
        
        if (args.length == 0) {
            throw new SyntaxErrorException(
                    String.format("parameter was not specified. parameter must be specified. convertor=[UseEncoding]."));
        }        
        if (args[0] == null) {
            throw new SyntaxErrorException(String.format(
                    "1st parameter was null. parameter=%s. convertor=[UseEncoding].", Arrays.toString(args))
                );
        }
        if (!(args[0] instanceof String)) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid parameter type was specified. parameter type must be 'String' but was: '%s'. parameter=%s. convertor=[UseEncoding].",
                            args[0].getClass().getName(), Arrays.toString(args)));
        }
        
        field.setEncoding(Charset.forName(args[0].toString()));
        return null;
    }

    /** {@inheritDoc}
     * データタイプのコンバータに処理を委譲するので、このメソッドは使用されない。
     */
    public Object convertOnRead(Object data) {
        throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc}
     * データタイプのコンバータに処理を委譲するので、このメソッドは使用されない。
     */
    public Object convertOnWrite(Object data) {
        throw new UnsupportedOperationException();
    }
}
