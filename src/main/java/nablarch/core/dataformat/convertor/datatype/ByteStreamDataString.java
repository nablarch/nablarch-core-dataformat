package nablarch.core.dataformat.convertor.datatype;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * バイトストリームで入出力する文字列のデータタイプ。
 * <p>
 * 本クラスは入力時にパディング、出力時にトリムを行う。
 * </p>
 * <p>
 * 本データタイプは、バイトデータをそのまま文字として読み込む。<br/>
 * 通常、バイト長の異なる文字が混在するフィールドを扱う場合や、UTF-8のファイルを読み書きする場合、
 * 全角文字のパディングに半角スペースを使用する場合に、本データタイプを使用する。
 * </p>
 * <p>
 * 本クラスを使用する場合、パディング/トリム文字として指定できるのはシングルバイト文字のみである。<br/>
 * また、デフォルトではパディング/トリム文字として半角スペースを使用するが、個別にパディング/トリム文字を指定することもできる。
 * </p>
 * <p>
 * 文字列のバイト長がシングルバイトで統一されているフィールドを読み書きする場合は{@link SingleByteCharacterString}、
 * ダブルバイトで統一されている場合は{@link DoubleByteCharacterString}を使用すること。
 * </p>
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class ByteStreamDataString extends ByteStreamDataSupport<String> {

    /** {@inheritDoc} */
    public ByteStreamDataString initialize(Object... args) {
        if (args == null) {
            throw new SyntaxErrorException(Builder.concat(
                    "initialize parameter was null. parameter must be specified. convertor=[", getClass().getSimpleName(), "]."));
        }
        if (args.length == 0) {
            throw new SyntaxErrorException(Builder.concat(
                    "parameter was not specified. parameter must be specified. convertor=[", getClass().getSimpleName(), "]."));
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(Builder.concat(
                    "1st parameter was null. parameter=", Arrays.toString(args), ". convertor=[", getClass().getSimpleName(), "]."));
        }
        if (!(args[0] instanceof Integer)) {
            throw new SyntaxErrorException(Builder.concat(
            "invalid parameter type was specified. 1st parameter must be an integer. ", 
            "parameter=", Arrays.toString(args), ". convertor=[", getClass().getSimpleName(), "]."));
        }
        setSize((Integer) args[0]);
        return this;
    }

    /** {@inheritDoc}
     * この実装では、入力時に、引数のバイト配列を1バイト文字に変換して返却する。
     * また、変換の際に指定された文字でトリムを行う。デフォルトのトリム文字として半角スペースを使用する。
     */
    @Override
    public String convertOnRead(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String str = null;
        try {
            str = new String(bytes, getField().getEncoding().name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can not happen.
        }
        char padChar = getPaddingStr().charAt(0);
        int chopPos = str.length() - 1;
        while ((chopPos >= 0) && (str.charAt(chopPos) == padChar)) {
            chopPos--;
        }
        return str.substring(0, chopPos + 1);
    }

    /** {@inheritDoc}
     * この実装では、出力時に、引数のオブジェクト（文字列など）をバイト配列に変換して返却する。
     * また、変換の際に指定された文字でパディングを行う。デフォルトのパディング文字として半角スペースを使用する。
     */
    @Override
    public byte[] convertOnWrite(Object data) {
        byte[] bytes = null;
        try {
            if (data == null) {
                bytes = new byte[0];
            } else {
                bytes = StringUtil.toString(data)
                                  .getBytes(getField().getEncoding().name());
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can not happen.
        }                      
        if (bytes.length > getSize()) {
            throw new InvalidDataFormatException(
                "invalid parameter was specified. "
              + "too large data. field size = '" + getSize()
              + "' data size = '"  + bytes.length  + "'."
              + " data: " + StringUtil.toString(data)
            );
        }
        ByteBuffer buff = ByteBuffer.wrap(new byte[getSize()]);
        buff.put(bytes);
        int padSize = (getSize() - bytes.length) / getPaddingBytes().length;
        for (int i = 0; i < padSize; i++) {
            buff.put(getPaddingBytes());
        }
        return buff.array();
    }
    

    // ------------------------------------------------------------- helper
    /**
     * パディング/トリム処理で使用する文字列を返却する。
     * パディング/トリム文字列が指定されていない場合、デフォルトで半角スペースを使用する。
     * @return パディング/トリム処理で使用する文字列
     */
    protected String getPaddingStr() {
        Object padding = getField().getPaddingValue();
        String paddingStr = (padding == null) ? getDefaultPaddingStr()
                                 : padding.toString();
        if (paddingStr.length() != 1) {
            throw new SyntaxErrorException(Builder.concat(
                    "invalid padding character was specified. "
                  , "Length of padding character must be '1', but was '", paddingStr.length(), "'. padding str = [", paddingStr, "]"));
        }
        return paddingStr;
    }
    
    /**
     * パディング/トリム処理で使用するデフォルトの文字を返却する。
     * <p>
     * 本メソッドをオーバーライドすることで、パディング/トリム処理で使用するデフォルトの文字列を変更できる。
     * </p>
     * @return パディング/トリム処理で使用するデフォルトの文字列
     */
    protected String getDefaultPaddingStr() {
        return " ";
    }
    
    /**
     * パディング/トリム文字として許容するバイト長を返却する。
     * <p/>
     * 本メソッドをオーバーライドすることで、パディング/トリム文字として許容するバイト長を変更できる。
     * @return パディング/トリム文字として許容するバイト長
     */
    protected int getPaddingCharLength() {
        return 1;
    }

    /**
     * パディングに使用する文字のバイト配列を返却する。
     * @return パディングに使用するバイトデータ
     */
    protected byte[] getPaddingBytes() {
        if (paddingBytes != null) {
            return paddingBytes;
        }
        // パディング文字はエンコーディングの都合上ここで初期化する。
        byte[] bytes = null;
        try {
            bytes = getPaddingStr().getBytes(
                getField().getEncoding().name()
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can not happen.
        }
        if (bytes.length != getPaddingCharLength()) {
            throw new SyntaxErrorException(
                "invalid parameter was specified. "
              + "the length of padding string must be "
              + getPaddingCharLength() + "."
              + " but specified one was " + bytes.length
              + " byte long."
            );
        }
        paddingBytes = bytes;
        return paddingBytes;
    }

    /** パディングに使用するバイトデータ */
    private byte[] paddingBytes = null;
    
    


}
