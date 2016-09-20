package nablarch.core.dataformat.convertor;

import java.util.Map;

import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.dataformat.convertor.value.ValueConvertor;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.CaseInsensitiveMap;

/**
 * コンバータの生成を行う抽象基底ファクトリクラス。
 * 
 * コンバータ名とコンバータ実装クラスの対応表をもとに、コンバータを生成する。
 * @author Masato Inoue
 */
public abstract class ConvertorFactorySupport {

    // ---------------------------------------------------------- structure
    /** コンバータ名とコンバータ実装クラスの対応表 */
    private Map<String, Class<?>> convertorTable = null;
    
    /**
     * コンバータ名とコンバータ実装クラスの対応表を取得する。
     * @return コンバータ名とコンバータ実装クラスの対応表
     */
    @Published(tag = "architect")
    public Map<String, Class<?>> getConvertorTable() {
        return convertorTable;
    }
    
    /**
     * コンストラクタ。
     */
    @Published(tag = "architect")
    public ConvertorFactorySupport() {
        convertorTable = getDefaultConvertorTable();
    }
    
    /**
     * デフォルトのコンバータ名とコンバータ実装クラスの対応表を取得する。
     * @return デフォルトのコンバータ名とコンバータ実装クラスの対応表
     */
    @Published(tag = "architect")
    protected abstract Map<String, Class<?>> getDefaultConvertorTable();

    // -------------------------------------------------------- factory api
    /**
     * 引数で指定されたデータタイプ名に対応するコンバータを生成する。
     * @param <F> 変換前の値の型
     * @param <T> 変換後の値の型
     * @param typeName データタイプ名
     * @param field    フィールド定義
     * @param args     データタイプのパラメータ
     * @return 生成されたコンバータ
     */
    @SuppressWarnings("unchecked")
    public <T, F> DataType<T, F> typeOf(String typeName, FieldDefinition field,
            Object... args) {
        return (DataType<T, F>) typeOfWithoutInit(typeName, field, args).init(field, args);
    }

    /**
     * 引数で指定されたデータタイプ名に対応するコンバータを生成する。
     * @param <F> 変換前の値の型
     * @param <T> 変換後の値の型
     * @param typeName データタイプ名
     * @param field    フィールド定義
     * @param args     データタイプのパラメータ
     * @return 生成されたコンバータ
     */
    @SuppressWarnings("unchecked")
    public <T, F> DataType<T, F> typeOfWithoutInit(String typeName, FieldDefinition field,
            Object... args) {

        Class<?> clazz = getConvertorTable().get(typeName);
        if (clazz == null) {
            throw new SyntaxErrorException(
                "unknown data type name was specified. input data type name=[" + typeName + "]"
            );
        }
        if (!DataType.class.isAssignableFrom(clazz)) {
            throw new SyntaxErrorException(String.format(
                    "the convertor corresponding to the data type name was not a DataType class. input data type name=[%s]. convertor class=[%s]."
                    , typeName, clazz.getName()));
        }
        try {
            return ((DataType<T, F>) clazz.newInstance());
        } catch (InstantiationException e) {
            throw new IllegalStateException(String.format(
                    "data type convertor could not be instantiated. data type name=[%s]. convertor class=[%s]."
                    , typeName, clazz.getName()), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "data type convertor could not be instantiated. data type name=[%s]. convertor class=[%s]."
                    , typeName, clazz.getName()), e);
        } 
    }
    
    /**
     * 引数で指定されたコンバータ名に対応するコンバータを生成する。
     * @param convertorName コンバータ名
     * @param field         フィールド定義
     * @param args          コンバータのパラメータ
     * @return 生成されたコンバータ
     */
    @SuppressWarnings("rawtypes")
    public ValueConvertor<?, ?> convertorOf(String          convertorName,
                                            FieldDefinition field,
                                            Object...       args) {
         
        Class<?> clazz = getConvertorTable().get(convertorName);
        if (clazz == null) {
            throw new SyntaxErrorException(
                    "unknown value convertor name was specified. input value convertor name=[" + convertorName + "]."
                );            
        }
        if (!ValueConvertor.class.isAssignableFrom(clazz)) {
            throw new SyntaxErrorException(String.format(
                    "the convertor corresponding to the data type name was not a ValueConvertor class. input value convertor name=[%s]. convertor class=[%s]."
                    , convertorName, clazz.getName()));
        }
        try {
            return ((ValueConvertor) clazz.newInstance()).initialize(field, args);
        } catch (InstantiationException e) {
            throw new IllegalStateException(String.format(
                    "value convertor could not be instantiated. value convertor name=[%s]. convertor class=[%s]."
                    , convertorName, clazz.getName()), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "value convertor could not be instantiated. value convertor name=[%s]. convertor class=[%s]."
                    , convertorName, clazz.getName()), e);
        } 
    }

    /**
     * コンバータ名とコンバータ実装クラスの対応表を設定する。
     * @param table コンバータ名とコンバータ実装クラスの対応表
     * @return このオブジェクト自体
     * @throws ClassNotFoundException
     *              指定されたクラスが存在しなかった場合、もしくは、
     *              指定されたクラスが ValueConvertor を実装していなかった場合
     */
    @SuppressWarnings("rawtypes")
    @Published(tag = "architect")
    public ConvertorFactorySupport
    setConvertorTable(Map<String, String> table) throws ClassNotFoundException {
        Map<String, Class<?>> convertorTable = new CaseInsensitiveMap<Class<?>>();
        
        for (Map.Entry<String, String> entry : table.entrySet()) {
            String className = entry.getValue();
            Class convertor = Class.forName(className);
            convertorTable.put(entry.getKey(), convertor);
            if (!(DataType.class.isAssignableFrom(convertor) || ValueConvertor.class
                    .isAssignableFrom(convertor))) {
                throw new ClassNotFoundException(
                        "invalid class was specified. class name must convertor class. class=["
                                + className + "].");
            }
        }
        this.convertorTable = convertorTable;
        return this;
    }

}
