package nablarch.core.dataformat.convertor.value;

import java.util.Arrays;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * 出力データが未設定の場合に、デフォルト値を設定するコンバータ。
 * 入力時にはデフォルト値は使用せず、入力データをそのまま返却する。
 * 
 * @author Iwauo Tajima
 */
public class DefaultValue extends ValueConvertorSupport<Object, Object> {

    /** デフォルト値 */
    private Object defaultValue = null;
    
    /** {@inheritDoc} */
    public DefaultValue initialize(FieldDefinition field, Object... args) {
        super.initialize(field, args);
        
        if (args.length != 1) {
            throw new SyntaxErrorException(String.format(
                "parameter size was invalid. parameter size must be one, "
              + "but was [%s]. parameter=%s. convertor=[DefaultValue].", args.length, Arrays.toString(args))
            );
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(String.format(
                    "1st parameter was null. parameter=%s. convertor=[DefaultValue].", Arrays.toString(args))
                );
        }
        
        defaultValue = args[0];
        return this;
    }

    /** {@inheritDoc}
     * この実装では、入力時に、引数のオブジェクトをそのまま返却する。
     */
    public Object convertOnRead(Object data) {
        return data;
    }

    /** {@inheritDoc}
     * この実装では、出力時に、引数の値が未設定（null）の場合、デフォルト値を返却して返却する
     * @return 値が未設定の場合はデフォルト値、値が設定されている場合は引数のオブジェクト
     */
    public Object convertOnWrite(Object data) {
        return (data == null) ? defaultValue
                              : data;
    }
}
