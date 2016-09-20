package nablarch.core.dataformat.convertor.value;

import java.util.Arrays;

import nablarch.core.dataformat.CharacterReplacementManager;
import nablarch.core.dataformat.CharacterReplacementUtil;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;

/**
 * 入力時および出力時に寄せ字処理を行うコンバータ。
 * @author Masato Inoue
 */
public class CharacterReplacer extends ValueConvertorSupport<String, String> {
    
    /** 寄せ字タイプ */
    private String typeName;
    
    /** フィールド名*/
    private String fieldName;

    /** {@inheritDoc} */
    public CharacterReplacer initialize(FieldDefinition field, Object... args) {
        super.initialize(field, args);
        if (args.length != 1) {
            throw new SyntaxErrorException(String.format(
                    "parameter size was invalid. parameter size must be one, "
                  + "but was '%s'. parameter=%s, convertor=[CharacterReplacer].", args.length, Arrays.toString(args))
                );
        }
        if (args[0] == null) {
            throw new SyntaxErrorException(String.format(
                    "1st parameter was null. parameter=%s, convertor=[CharacterReplacer].", Arrays.toString(args))
                );
        }
        if (!(args[0] instanceof String)) {
            throw new SyntaxErrorException(
                    String.format(
                            "invalid parameter type was specified. parameter type must be 'String', but was '%s'. parameter=%s, convertor=[CharacterReplacer].",
                            args[0].getClass().getName(), Arrays.toString(args)));
        }
        typeName = (String) args[0];
        if (typeName.length() == 0) {
            throw new SyntaxErrorException(
                    String.format(
                            "parameter was empty. parameter must not be empty. parameter=%s, convertor=[CharacterReplacer].",
                            Arrays.toString(args)));
        }

        // フォーマット定義ファイルで指定されたタイプ名が、寄せ字タイプ名として定義されているかチェックする
        if (!CharacterReplacementManager.getInstance().containsReplacementType(
                typeName)) {
            throw new SyntaxErrorException(String.format(
                    "replacement type name was not found. value=[%s]. must specify defined replacement type name. convertor=[CharacterReplacer].", typeName));
        }
        
        // フォーマット定義ファイルで指定された文字エンコーディングが、寄せ字変換タイプ名に対応する文字エンコーディングと一致するかどうかチェックする
        if (!CharacterReplacementManager.getInstance()
                .checkReplacementTypeEncoding(typeName, field.getEncoding())) {
            throw new SyntaxErrorException(String.format(
                    "field encoding '%s' was invalid. field encoding must match the encoding that is defined as replacement type '%s'.",
                    field.getEncoding(), typeName));
        }
        
        fieldName = field.getName();
        
        return this;
    }

    /**
     * この実装では、入力時に、引数のオブジェクトをそのまま返却する。
     * @param data 入力時の寄せ字変換前の文字列
     * @return 入力時の寄せ字変換後の文字列
     */
    public String convertOnRead(String data) {
        if (data == null) {
            return null;
        }
        String result = CharacterReplacementManager.getInstance().replaceCharacter(typeName, data); 
        CharacterReplacementUtil.setResult(fieldName, data, result);
        return result;
    }

    /**
     * 出力文字列に対する寄せ字変換処理を行う。
     * @param data 出力時の寄せ字変換前の文字列
     * @return 出力時の寄せ字変換後の文字列
     */
    public String convertOnWrite(Object data) {
        if (data == null) {
            return null;
        }        
        if (!(data instanceof String)) {
            throw new InvalidDataFormatException(
                    "invalid parameter type was specified. parameter type must be 'java.lang.String'. type=[" + data.getClass() + "].");
        }
        return CharacterReplacementManager.getInstance().replaceCharacter(typeName, (String) data);
    }
    
}
