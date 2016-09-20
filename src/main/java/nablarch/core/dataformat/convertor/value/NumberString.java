package nablarch.core.dataformat.convertor.value;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.util.StringUtil;

/**
 * 符号なし数値コンバータ。 
 * <p>
 * 入力時に、文字列データを数値型に変換し、出力時に、数値型のデータを文字列に変換する。
 * </p>
 * <p>
 * 本コンバータは可変長ファイルの数値フィールドを読み書きする場合に、{@link nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString}と組み合わせて使用することを想定している。<br/>
 * 固定長ファイルの数値フィールドを読み書きする場合は、{@link nablarch.core.dataformat.convertor.datatype.NumberStringDecimal}を使用すること。
 * </p>
 * <p>
 * <b>形式チェック</b><br/><br/>
 * 入力処理の場合は、引数の文字列に対して符号なし数値であるかどうかの形式チェックを行う。<br/>
 * 出力処理の場合は、引数のオブジェクトを文字列に変換し、その文字列が符号なし数値であることのチェックを行う。
 * </p>
 * <p>
 * <b>null値の扱い</b><br/><br/>
 * 入力処理の引数にnullまたは空文字が渡された場合、nullを返却する。<br/>
 * 出力処理の引数にnullが渡された場合、空文字を出力する。
 * </p>
 * @author Masato Inoue
 */
public class NumberString extends ValueConvertorSupport<Object, String> {

    /** 符号なし数値のパターン */
    private static final Pattern PATTERN = Pattern.compile("^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$");

    /** 空文字 */
    private static final String EMPTY = "";

    /**
     * {@inheritDoc}
     * <p>
     * {@link NumberString}では、入力時に、引数の文字列が符号なし数値であることのチェックを行い、
     * 問題がなければ、BigDecimalに変換して返却する。
     * </p>
     * <p>
     * 引数がnullまたは空文字の場合、nullを返却する。
     * </p>
     */
    public BigDecimal convertOnRead(String data) {
        if (StringUtil.isNullOrEmpty(data)) {
            return null;
        }
        validateNumericString(data);
        return new BigDecimal(data);
    }

    /**{@inheritDoc}
     * <p/>
     * この実装では、出力時に、引数のオブジェクトを文字列に変換し、その文字列が符号なし数値であることのチェックを行い、
     * 問題がなければ返却する。
     * <p>
     * 引数として許容する型は以下のとおりである。
     * <ul>
     * <li>java.lang.Number</li>
     * <li>java.lang.String</li>
     * </ul>
     * </p>
     * <p>
     * 引数がnullの場合、空文字を返却する。
     * </p>
     */
    public String convertOnWrite(Object data) {
        
        if (data == null) {
            return EMPTY;
        }

        String dataStr;
        if (data instanceof String) {
            dataStr = (String) data;
        } else if (data instanceof Number) {
            dataStr = StringUtil.toString(data);
        } else {
            throw new InvalidDataFormatException("invalid parameter type was specified. parameter must be java.lang.Number class or number string.");
        }
        validateNumericString(dataStr);
        return dataStr;
    }

    /**
     * 入出力データが数値文字列であるかどうかのチェックを行う。
     * <p/>
     * {@link NumberString}では、入出力データが符号なし数値であることのチェックを行う。
     * @param data 入出力されるデータ
     */
    protected void validateNumericString(String data) {
        if (!PATTERN.matcher(data).matches()) {
            throw new InvalidDataFormatException(String.format(
                    "invalid parameter format was specified. parameter format must be [%s]. value=[%s].", PATTERN, data));
        }
    }
}
