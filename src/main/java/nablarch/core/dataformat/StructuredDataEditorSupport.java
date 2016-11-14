package nablarch.core.dataformat;

import java.util.Set;

import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;

/**
 * 構造化データを解析/構築する際に使用するクラス郡の抽象基底クラス。
 * <p/>
 * 各種データで共通的に使用する必須項目チェックや配列項目数のチェック、コンバータの呼び出し処理などを行います。
 *
 * @author TIS
 */
public abstract class StructuredDataEditorSupport {

    /**
     * 必須チェック用関数
     * <br>
     * チェック可否をTrueで強制的に実行
     *
     * @param baseKey      キー
     * @param fd           FieldDefinition
     * @param targetObject チェック対象オブジェクト
     * @throws InvalidDataFormatException targetObjectがNullで必須項目の場合
     */
    protected void checkIndispensable(String baseKey, FieldDefinition fd,
                                      Object targetObject) throws InvalidDataFormatException {
        checkRequired(baseKey, fd, targetObject, true);
    }

    /**
     * 必須チェック用の関数 <br>
     * targetObjectがNullで必須項目の場合は<br>
     * throwInvalidDataFormatExceptionを実行
     *
     * @param baseKey      キー
     * @param fd           FieldDefinition
     * @param targetObject チェック対象オブジェクト
     * @param checkTarget  チェック可否
     * @throws InvalidDataFormatException targetObjectがNullで必須項目の場合
     */
    protected void checkRequired(String baseKey, FieldDefinition fd,
                                 Object targetObject, boolean checkTarget)
            throws InvalidDataFormatException {

        if (!checkTarget) {
            return;
        }

        if (!fd.isRequired()) {
            return;
        }

        if (targetObject == null) {
            throw new InvalidDataFormatException(String.format(
                    "BaseKey = %s,Field %s is required", baseKey, fd.getName()));
        }
    }

    /**
     * 配列の長さチェックを実行します。
     *
     * @param fd           フィールド定義
     * @param actualLength 実際の長さ
     * @param baseKey      対象キー
     */
    protected void checkArrayLength(FieldDefinition fd, int actualLength,
                                    String baseKey) {
        if (actualLength < fd.getMinArraySize()
                || actualLength > fd.getMaxArraySize()) {
            String message = String
                    .format("Out of range array length BaseKey = %s,FieldName=%s:MinCount=%d:MaxCount=%d:Actual=%d",
                            baseKey, fd.getName(), fd.getMinArraySize(),
                            fd.getMaxArraySize(), actualLength);
            throw new InvalidDataFormatException(message);
        }
    }

    /**
     * Map(XML)に格納する際のKeyを作成し、返却します
     *
     * @param currentKeyBase Keyを作成する際のベース文字列(親フィールド名称)
     * @param fieldName      作成対象フィールド名称
     * @return 作成したKey
     */
    protected String buildMapKey(String currentKeyBase, String fieldName) {
        // Mapに格納する際のKeyを作成

        String normalizedField = FieldDefinitionUtil.normalizeWithNonWordChar(fieldName);

        if (StringUtil.isNullOrEmpty(currentKeyBase)) {
            return normalizedField;
        }

        String normalizedBase = FieldDefinitionUtil.normalizeWithNonWordChar(currentKeyBase);
        return Builder.concat(
                normalizedBase, ".", normalizedField);
    }

    /**
     * 読み込んだフィールド文字列をコンバータを用いてオブジェクトに変換し、返却する。
     *
     * @param fieldStr 読み込んだフィールド文字列
     * @param field    フィールド定義情報保持クラス
     * @return コンバートしたフィールドの内容
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object convertToFieldOnRead(String fieldStr,
                                          FieldDefinition field) {
        CharacterStreamDataString dataType = (CharacterStreamDataString) field
                .getDataType();

        Object value;
        try {
            // データタイプのコンバータを実行する
            value = dataType.convertOnRead(fieldStr);

            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) {
                value = convertor.convertOnRead(value);
            }
        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対して、フィールド名の情報を付与する
            throw e.setFieldName(field.getName());
        }
        return value;
    }

    /**
     * 読み込んだフィールド文字列をコンバータを用いてオブジェクトに変換し、返却する。
     *
     * @param fieldStr 読み込んだフィールド文字列
     * @param field    フィールド定義情報保持クラス
     * @return コンバートしたフィールドの内容
     */
    @SuppressWarnings("rawtypes")
    protected Object convertToFieldOnWrite(Object fieldStr,
                                           FieldDefinition field) {
        Object value;
        try {
            value = fieldStr;

            // コンバータを実行する
            for (ValueConvertor convertor : field.getConvertors()) {
                value = convertor.convertOnWrite(value);
            }

        } catch (InvalidDataFormatException e) {
            // コンバータで発生した例外に対して、フィールド名の情報を付与する
            throw e.setFieldName(field.getName());
        }
        return value;
    }

    /**
     * 当該フィールドのタイプ識別子がネストオブジェクト(OB)かどうか判定する。
     *
     * @param fieldDef 判定対象フィールドのフィールド定義
     * @return ネストオブジェクトである場合、真
     */
    protected boolean isObjectType(FieldDefinition fieldDef) {
        Set<String> converterNames = fieldDef.getConvertorSettingList().keySet();
        return converterNames.contains("OB");
    }
}
