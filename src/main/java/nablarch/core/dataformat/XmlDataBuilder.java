package nablarch.core.dataformat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nablarch.core.dataformat.StructuredDataRecordFormatterSupport.StructuredDataDirective;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.util.StringUtil;

/**
 * XMLパーサー。<br>
 * この実装ではStAXを使用してXMLデータの構築を行います。
 *
 * @author TIS
 */
public class XmlDataBuilder extends StructuredDataEditorSupport implements StructuredDataBuilder {

    /** 作成対象XMLのバージョン */
    private static final String TARGET_XML_VERSION = "1.0";

    /** 属性あり要素のコンテンツ名(デフォルトはbody) */
    private String contentName = "body";

    /**
     * コンストラクタ
     */
    public XmlDataBuilder() {
        super();
    }

    /**
     * 属性ありコンテンツの要素名を設定する。
     *
     * @param contentName 属性ありコンテンツの要素名
     */
    public void setContentName(final String contentName) {
        this.contentName = contentName;
    }

    /**
     * XML文字列を作成します。
     *
     * @param map フラットマップ
     * @param layoutDef フォーマット定義
     * @param out XML文字列出力先ストリーム
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    public void buildData(Map<String, ?> map, LayoutDefinition layoutDef,
            OutputStream out) throws IOException, InvalidDataFormatException {
        RecordDefinition rd = layoutDef.getRecords().get(0);

        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            String textEncoding =
                    StructuredDataDirective.getTextEncoding(layoutDef.getDirective());
            XMLStreamWriter writer = factory
                    .createXMLStreamWriter(new OutputStreamWriter(out, textEncoding));

            writer.writeStartDocument(textEncoding, TARGET_XML_VERSION);
            writer.writeStartElement(rd.getTypeName());
            NestedKeys nestedKeys = new NestedKeys(map);
            buildXml("", map, layoutDef, rd, writer, nestedKeys);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();

        } catch (XMLStreamException e) {
            throw new InvalidDataFormatException(
                    String.format("invalid data found. [%s]", e.getMessage()), e);
        }
    }

    /**
     * XMLを構築します。
     *
     * @param currentKeyBase キー名ベース
     * @param map 出力対象マップ
     * @param ld フォーマット定義
     * @param rd レコードタイプ定義
     * @param writer XMLライタ
     * @param nestedKeys 部分キーの集合
     * @throws XMLStreamException XML出力に失敗した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    private void buildXml(String currentKeyBase, Map<String, ?> map,
            LayoutDefinition ld, RecordDefinition rd, XMLStreamWriter writer, NestedKeys nestedKeys)
            throws XMLStreamException, InvalidDataFormatException {

        // 属性が先頭になるように組み替え
        List<FieldDefinition> attrFdList = new ArrayList<FieldDefinition>();
        List<FieldDefinition> valueFdList = new ArrayList<FieldDefinition>();
        List<FieldDefinition> newFdList = new ArrayList<FieldDefinition>();
        for (FieldDefinition fd : rd.getFields()) {
            if (fd.isAttribute()) {
                attrFdList.add(fd);
            } else {
                valueFdList.add(fd);
            }
        }
        newFdList.addAll(attrFdList);
        newFdList.addAll(valueFdList);

        for (FieldDefinition fd : newFdList) {
            // マップ格納用のキー作成
            String mapKey = buildMapKey(currentKeyBase, fd.getName());
            // レコード定義を取得
            RecordDefinition nrd = ld.getRecordType(fd.getName());

            // 項目が配列の時
            if (fd.isArray()) {
                // レコード定義を取得
                if (fd.getName().equals(contentName)) {
                    // コンテンツを表す項目の場合は配列を許容しない
                    throw new InvalidDataFormatException("Array type can not be specified in the content."
                            + " parent name: " + currentKeyBase + ",field name: " + fd.getName());
                }
                if (nrd != null) {
                    // ObjectArray
                    writeObjectArray(writer, ld, nrd, fd, currentKeyBase, mapKey, map, nestedKeys);

                } else {
                    // StringArray
                    writeStringArray(writer, fd, currentKeyBase, mapKey, map);
                }
            } else {
                if (nrd != null) {
                    // Object
                    writeObject(writer, ld, nrd, fd, mapKey, map, currentKeyBase, nestedKeys);

                } else {
                    // Value
                    writeValue(writer, fd, mapKey, map, currentKeyBase);
                }
            }
        }
    }

    /**
     * オブジェクト配列の出力処理です
     * @param writer XMLライタ
     * @param ld フォーマット定義
     * @param nrd レコードタイプ定義
     * @param fd フィールド定義
     * @param currentKeyBase キー名ベース
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @param nestedKeys
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeObjectArray(XMLStreamWriter writer, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd,
                                  String currentKeyBase, String mapKey, Map<String, ?> map, NestedKeys nestedKeys) throws XMLStreamException {
        int objectCount = 0;
        for (int i = 0;; i++) {
            // 対象キーがひとつでも含まれていれば出力対象とする
            boolean isOut = false;
            for (FieldDefinition nfd : nrd.getFields()) {
                String tmpSubKey = FieldDefinitionUtil.normalizeWithNonWordChar(nfd.getName().replaceAll("@|\\[.*\\]", ""));
                String tmpkey = String.format("%s[%s].%s", mapKey, i, tmpSubKey);
                if (nestedKeys.contains(tmpkey)) {
                    isOut = true;
                    break;
                }
            }

            if (isOut) {
                writer.writeStartElement(fd.getName());
                buildXml(String.format("%s[%s]", mapKey, i), map, ld, nrd, writer, nestedKeys);
                writer.writeEndElement();
                objectCount++;
            } else {
                break;
            }
        }
        // 配列の長さチェック実施
        checkArrayLength(fd, objectCount, currentKeyBase);
    }

    /**
     * 文字列配列の出力処理です
     * @param writer XMLライタ
     * @param fd フィールド定義
     * @param currentKeyBase キー名ベース
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeStringArray(XMLStreamWriter writer, FieldDefinition fd, String currentKeyBase, String mapKey, Map<String, ?> map)
            throws XMLStreamException {
        if (map != null && map.get(mapKey) != null) {
            String[] arr = (String[]) map.get(mapKey);

            // 配列の長さチェック実施
            checkArrayLength(fd, arr.length, currentKeyBase);

            for (String arrayVal : arr) {
                Object writeVal = convertToFieldOnWrite(arrayVal, fd);
                CharacterStreamDataString dataType = (CharacterStreamDataString) fd.getDataType();
                // データタイプのコンバータを実行する
                String writeStringValue = dataType.convertOnWrite(writeVal);
                writer.writeStartElement(fd.getName());
                writer.writeCharacters(writeStringValue);
                writer.writeEndElement();
            }
        } else {
            checkIndispensable(currentKeyBase, fd, null);
        }
    }

    /**
     * オブジェクトの出力処理です
     * @param writer XMLライタ
     * @param ld フォーマット定義
     * @param nrd レコードタイプ定義
     * @param fd フィールド定義
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @param currentKeyBase キー名ベース
     * @param nestedKeys 部分キーの集合
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeObject(XMLStreamWriter writer, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd,
            String mapKey, Map<String, ?> map, String currentKeyBase, NestedKeys nestedKeys) throws XMLStreamException {
        // 属性チェック実施
        if (fd.isAttribute()) {
            throw new InvalidDataFormatException(String.format(
                    "BaseKey = %s,Field %s is Object but specified by Attribute",
                    currentKeyBase, fd.getName()));
        }

        boolean containsKey = (map != null && nestedKeys.contains(mapKey));
        if (containsKey) {
            // 子オブジェクトを出力
            writer.writeStartElement(fd.getName());
            buildXml(mapKey, map, ld, nrd, writer, nestedKeys);
            writer.writeEndElement();
        } else {
            // 必須チェック実施
            checkIndispensable(currentKeyBase, fd, null);
        }
    }

    /**
     * 値の出力処理です
     * @param writer XMLライタ
     * @param fd フィールド定義
     * @param mapKey マップキー
     * @param map 出力対象マップ
     * @param currentKeyBase キー名ベース
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeValue(XMLStreamWriter writer, FieldDefinition fd, String mapKey, Map<String, ?> map, String currentKeyBase)
        throws XMLStreamException {

        String writeStringVal = null;
        if (map != null) {
            String mapVal = map.get(mapKey) == null ? null : StringUtil.toString(map.get(mapKey));
            Object writeVal = convertToFieldOnWrite(mapVal, fd);
            if(map.containsKey(mapKey) || writeVal != null) {
                CharacterStreamDataString dataType = (CharacterStreamDataString) fd.getDataType();
                // データタイプのコンバータを実行する
                writeStringVal = dataType.convertOnWrite(writeVal);
            }
        }

        // 必須チェック実施
        checkIndispensable(currentKeyBase, fd, writeStringVal);

        if (map != null && map.containsKey(mapKey) || writeStringVal != null) {
            if (fd.isAttribute()) {
                writer.writeAttribute(fd.getName(), writeStringVal);
            } else if (fd.getName().equals(contentName)) {
                writer.writeCharacters(writeStringVal);
            } else {
                writer.writeStartElement(fd.getName());
                writer.writeCharacters(writeStringVal);
                writer.writeEndElement();
            }
        }
    }

    /**
     * ネストしたキーの集合を表すクラス。
     * 元のキー文字列とその部分からなる部分キーを持ち、
     * 与えられたキー文字列が、元のキーか部分キーのいずれかに合致するか判定する。
     *
     * 元のキー文字列が以下のようになっているとする。
     * - aaa[0].bbb[0].ccc
     *
     * この場合、
     * - aaa[0]
     * - aaa[0].bbb
     * - aaa[0].bbb[0]
     * の３つが部分キーとなる。
     *
     * この状態で、contains("aaa[0].bbb")と呼び出すと真が返却される。
     */
    static class NestedKeys {
        /** 元のMapのキーの集合 */
        private final Set<String> originalKeys;
        /** 生成された部分キーの集合 */
        private final Set<String> partialKeys;

        /**
         * Mapからインスタンスを生成する。
         * 与えられたMapがnullの場合、{@link #contains(String)}は常に偽となる。
         *
         * @param originalMap 元のMap
         */
        NestedKeys(Map<String, ?> originalMap) {
            this(originalMap != null ? originalMap.keySet() : Collections.<String>emptySet());
        }

        /**
         * 元のキーの集合からインスタンスを生成する。
         * @param originalKeys 元のキーの集合
         */
        NestedKeys(Set<String> originalKeys) {
            this.originalKeys = originalKeys;
            this.partialKeys = createAllPartialKeys(originalKeys);
        }

        /**
         * 指定されたキーが、部分キーのいずれがに一致する場合、または元のキーと一致するか判定する。
         *
         * @param key 判定対象のキー
         * @return 一致する場合、真
         */
        boolean contains(String key) {
            return partialKeys.contains(key) || originalKeys.contains(key);
        }


        private Set<String> createAllPartialKeys(Set<String> originalKeys) {
            Set<String> all = new HashSet<String>();
            for (String orig : originalKeys) {
                try {
                    all.addAll(createPartialKeys(orig));
                } catch (RuntimeException e) {
                    // キーの形式が不正である場合を想定(例:"a].bbb")
                    throw new IllegalArgumentException("failed to process key \"" + orig + "\"", e);
                }
            }
            return all;
        }

        /**
         * 与えられたキーから、その部分キーを生成する。
         *
         * @param originalKey 元のキー
         * @return 部分キー
         */
        private Set<String> createPartialKeys(String originalKey) {
            Set<String> partialKeys = new HashSet<String>();
            // ドット区切りで要素に分割する
            // "aaa[0].bbb[0].ccc" -> { "aaa[0]", "bbb[0]", "ccc" }
            String[] split = originalKey.split("\\.");
            StringBuilder sb = new StringBuilder(originalKey.length());
            boolean first = true;
            final int last = split.length - 1;   // 一番最後の要素
            final int last2 = last - 1;          // 最後から2番目の要素
            for (int i = 0; i < last; i++) {
                if (!first) {
                    sb.append(".");
                }
                first = false;
                sb.append(split[i]);
                String partialKey = sb.toString();
                partialKeys.add(partialKey);
                // 最後から2番めの要素に添字がある場合、添字を取り除いたものも部分キーとする
                if (i == last2 && partialKey.endsWith("]")) {
                    String keyWithoutIndex = partialKey.substring(0, partialKey.length() - 3);
                    partialKeys.add(keyWithoutIndex);
                }
            }
            return partialKeys;
        }
    }
}
