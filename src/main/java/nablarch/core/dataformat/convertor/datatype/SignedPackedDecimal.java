package nablarch.core.dataformat.convertor.datatype;

import nablarch.core.dataformat.FieldDefinition;

/**
 * 符号付きパック10進数のデータタイプ。
 * <p>
 * 入力時にはバイト配列（符号付きパック10進数）をBigDecimal型に変換し、
 * 出力時にはオブジェクト（BigDecimalや文字列型の数値など）をバイト配列（符号付きパック10進数）に変換して返却する。
 * </p>
 * @see PackedDecimal
 * @author Iwauo Tajima
 */
public class SignedPackedDecimal extends PackedDecimal {

    /** {@inheritDoc} */
    @Override
    public SignedPackedDecimal init(FieldDefinition field, Object... args) {
        super.init(field, args);
        if (args.length == 4) {
            setPackSignNibblePositive(Integer.parseInt((String) args[2],
                    16));
            setPackSignNibbleNegative(Integer.parseInt((String) args[3],
                    16));
        }
        setSigned(true);
        return this;
    }
}
