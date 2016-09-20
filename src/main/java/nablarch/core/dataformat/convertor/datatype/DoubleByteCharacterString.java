package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.util.Builder;
import nablarch.core.util.annotation.Published;

/**
 * ダブルバイト文字列のデータタイプ。
 * <p>
 * 入力時にはバイトデータを文字列に変換し、
 * 出力時にはオブジェクトをバイトデータに変換して返却する。
 * </p>
 * <p>
 * 本クラスを使用する場合、パディング/トリム文字として指定できるのはダブルバイト文字のみである。<br/>
 * また、デフォルトではパディング/トリム文字として全角スペースを使用するが、個別にパディング/トリム文字を指定することもできる。
 * </p>
 * <p>
 * 本クラスは、ファイルの文字コードがShift_JISやMS932の場合に、
 * 全角文字（ダブルバイト文字）フィールドの入出力に使用することを想定している。<br/>
 * ただし、全角文字であることのバリデーションを行うわけではないので、実際にはシングルバイト文字や3バイト以上の文字の読み書きも行われる。<br/>
 * 全角文字のバリデーションについては、別途、業務アクションなどで行うこと。
 * </p>
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class DoubleByteCharacterString extends ByteStreamDataString {

    /** {@inheritDoc} */
    @Override
    public ByteStreamDataString initialize(Object... args) {
        super.initialize(args);
        if (getSize() % 2 != 0) {
            throw new SyntaxErrorException(Builder.concat(
                    "invalid field size was specified. the length of DoubleByteCharacter data field must be a even number. ", 
                    "field size=[", getSize(), "]. convertor=[", getClass().getName(), "]."));
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    protected String getDefaultPaddingStr() {
        return "　";
    }
    
    /** {@inheritDoc} */
    @Override
    public int getPaddingCharLength() {
        return 2;
    }

}
