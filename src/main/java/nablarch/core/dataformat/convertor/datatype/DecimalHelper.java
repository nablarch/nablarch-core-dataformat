package nablarch.core.dataformat.convertor.datatype;

import static nablarch.core.util.Builder.concat;

import java.math.BigDecimal;
import java.math.BigInteger;

import nablarch.core.dataformat.InvalidDataFormatException;

/**
 * {@link PackedDecimal}および{@link ZonedDecimal}のヘルパクラス。
 * <p/>
 * {@link DataType#convertOnWrite(Object)}メソッドの引数として渡される出力対象データをスケールなしのlong値に変換する責務を持つ。
 * @author Masato Inoue
 */
public final class DecimalHelper {

    /** コンストラクタ（private） */
    private DecimalHelper() { }
    
    /** 出力データの最大値（18桁） */
    private static final BigInteger MAX_NUMBER = new BigInteger("999999999999999999");

    /** 出力データの最小値（18桁） */
    private static final BigInteger MIN_NUMBER = new BigInteger("-999999999999999999");

    /** 正の符号 */
    private static final String PLUS_SIGN = "+";

    /**
     * 出力対象のデータをスケールなしのlong値に変換する。
     * <p/>
     * 具体的には、以下の順番で出力対象のデータを変換する。
     * <ol>
     * <li>出力対象のデータをBigDecimalに変換。</li>
     * <li>BigDecimalをスケールなしのBigIntegerに変換。</li>
     * <li>BigIntegerをlongに変換。</li>
     * </ol>
     * @param data 出力対象のデータ
     * @return 引数のBigDecimalを変換したスケールなしのlong値
     */
    public static long toUnscaledLongValue(Object data) {
        BigDecimal bigDecimal = toBigDecimal(data);
        BigInteger bigInteger = bigDecimal.unscaledValue();
        if (bigInteger.compareTo(MAX_NUMBER) == 1) {
            throwInvalidDigits(bigDecimal, bigInteger, true);
        }
        if (bigInteger.compareTo(MIN_NUMBER) == -1) {
            throwInvalidDigits(bigDecimal, bigInteger, false);
        }
        return bigInteger.longValue();
    }

    /**
     * 
     * 出力対象のデータをBigDecimalに変換する。
     * @param data 変換対象データ
     * @return 変換後のデータ
     */
    public static BigDecimal toBigDecimal(Object data) {
        return toBigDecimal(data, null);
    }
    
    /**
     * 出力対象のデータをBigDecimalに変換する。
     * @param data 変換対象データ
     * @param scale スケール
     * @return 変換後のデータ
     */
    public static BigDecimal toBigDecimal(Object data, Integer scale) {
        if (data == null || "".equals(data)) {
            if (scale != null) {
                return new BigDecimal(BigInteger.ZERO, scale);
            } else {
                return BigDecimal.ZERO;
            }
        }
        if (data instanceof BigDecimal) {
            return (BigDecimal) data;
        }
        try {
            String strData = data.toString();
            if (scale != null) {
                if (strData.startsWith(PLUS_SIGN)) {
                    // 正の符号が付与された文字列をそのままBigIntegerに変換すると例外がスローされるので、正の符号を削除する。
                    strData = strData.substring(PLUS_SIGN.length());
                }
                return new BigDecimal(new BigInteger(strData), scale);
            } else {
                return new BigDecimal(strData);
            }
        } catch (NumberFormatException e) {
            throw new InvalidDataFormatException(concat(
                    "invalid parameter was specified. parameter must be able to convert to BigDecimal. "
                  , "parameter=[", data, "]."), e);
        }
    }
    
    /**
     * BigIntegerの桁数が不正（19桁以上）な場合に例外をスローする。
     * @param bigDecimal 元のパラメータ
     * @param bigInteger スケールなしのパラメータ
     * @param positive 正数かどうか
     * @param unscaledValueEqualsOriginal
     */
    private static void throwInvalidDigits(BigDecimal bigDecimal, BigInteger bigInteger, boolean positive) {
        boolean unscaledValueEqualsOriginal = isUnscaledValueEqualsOriginal(bigDecimal,
                bigInteger);
        // 符号を除いた桁数を求める（負数の場合は桁数-1）
        int unscaledDigits = String.valueOf(bigInteger).length() - (positive ? 0 : 1);
        throw new InvalidDataFormatException(concat(
                "invalid parameter was specified. "
                , "the number of ", !unscaledValueEqualsOriginal ? "unscaled " : "", "parameter digits must be 18 or less, "
                , "but was '", unscaledDigits, "'. "
                , !unscaledValueEqualsOriginal ? concat("unscaled parameter=[", bigInteger , "], ") : ""
                , !unscaledValueEqualsOriginal ? "original " : "", "parameter=[", bigDecimal , "]"
        , "."));
    }


    /**
     * スケールありの値とスケールなし値が一致するかどうか。
     * @param bigDecimal 元のパラメータ
     * @param bigInteger スケールなしのパラメータ
     * @return スケールありの値とスケールなし値が一致する場合、true
     */
    private static boolean isUnscaledValueEqualsOriginal(BigDecimal bigDecimal, BigInteger bigInteger) {
        return bigDecimal.equals(new BigDecimal(bigInteger));
    }
}
