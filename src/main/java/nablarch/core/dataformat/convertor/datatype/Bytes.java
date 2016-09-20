package nablarch.core.dataformat.convertor.datatype;

import java.util.Arrays;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * バイト配列のデータタイプ。
 * <p>
 * 入力時にはバイト配列に対して何もせずそのまま返却し、
 * 出力時にはオブジェクトをバイト配列に変換して返却する。
 * </p>
 * @author Iwauo Tajima
 */
public class Bytes extends ByteStreamDataSupport<byte[]> {
    
    /** {@inheritDoc} */
    public Bytes initialize(Object... args) {
        if (args.length == 0) {
            throw new SyntaxErrorException("parameter was not specified. parameter must be specified. convertor=[Bytes].");
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(String.format(
                    "1st parameter was null. parameter=%s. convertor=[Bytes].", Arrays.toString(args))
                );
        }
        if (!(args[0] instanceof Integer)) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid parameter type was specified. parameter type must be 'Integer' but was: '%s'. parameter=%s. convertor=[Bytes].",
                            args[0].getClass().getName(), Arrays.toString(args)));
        }
        setSize((Integer) args[0]);
        return this;
    }

    /** {@inheritDoc}
     * この実装では、入力時に、引数のバイト配列をそのまま返却する。
     */
    @Override
    public byte[] convertOnRead(byte[] data) {
        return data;
    }

    /** {@inheritDoc}
     * この実装では、出力時に、引数のオブジェクトをバイト配列に変換して返却する。
     */
    @Override
    public byte[] convertOnWrite(Object data) {
        if (data == null) {
            throw new InvalidDataFormatException(
                    "invalid parameter was specified. parameter must be not null.");
        }
        if (!(data instanceof byte[])) {
            throw new InvalidDataFormatException("invalid parameter type was specified. "
                    + "parameter must be a byte array.").setFieldName(getField().getName());
        }
        return (byte[]) data;
    }

}
