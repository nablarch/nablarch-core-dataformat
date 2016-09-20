package nablarch.core.dataformat.convertor.value;

import java.util.regex.Pattern;

import nablarch.core.dataformat.InvalidDataFormatException;

/**
 * 符号付き数値コンバータ。 
 * <p>
 * {@link NumberString}を継承し、符号付き数値であるかどうかの形式チェックおよび変換を行う。
 * </p>
 * <p>
 * 本コンバータは可変長ファイルの数値フィールドを読み書きする場合に、{@link nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString}と組み合わせて使用することを想定している。<br/>
 * 固定長ファイルの数値フィールドを読み書きする場合は、{@link nablarch.core.dataformat.convertor.datatype.SignedNumberStringDecimal}を使用すること。
 * </p>
 * @author Masato Inoue
 */
public class SignedNumberString extends NumberString {

    /** 符号付き数値のパターン */
    private static final Pattern PATTERN = Pattern.compile("^[+-]?([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$");

    /**{@inheritDoc}
     * <p/>
     * {@link SignedNumberString}では、入出力データが符号付き数値であることのチェックを行う。
     */
    @Override
    protected void validateNumericString(String data) {
        if (!PATTERN.matcher(data).matches()) {
            throw new InvalidDataFormatException(
                    String.format(
          "invalid parameter format was specified. parameter format must be [%s]. value=[%s].", PATTERN, data));
        }
    }
}
