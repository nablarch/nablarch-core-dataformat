package nablarch.core.dataformat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            buildXml("", map, layoutDef, rd, writer);
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
     * @throws XMLStreamException XML出力に失敗した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    private void buildXml(String currentKeyBase, Map<String, ?> map,
            LayoutDefinition ld, RecordDefinition rd, XMLStreamWriter writer) 
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
                    writeObjectArray(writer, ld, nrd, fd, currentKeyBase, mapKey, map);
                    
                } else {
                    // StringArray
                    writeStringArray(writer, fd, currentKeyBase, mapKey, map);
                }
            } else {
                if (nrd != null) {
                    // Object
                    writeObject(writer, ld, nrd, fd, mapKey, map, currentKeyBase);
                    
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
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeObjectArray(XMLStreamWriter writer, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd, 
            String currentKeyBase, String mapKey, Map<String, ?> map) throws XMLStreamException {
        int objectCount = 0;
        for (int i = 0;; i++) {
            // 対象キーがひとつでも含まれていれば出力対象とする
            boolean isOut = false;
            for (FieldDefinition nfd : nrd.getFields()) {
                String tmpSubKey = FieldDefinitionUtil.normalizeWithNonWordChar(nfd.getName().replaceAll("@|\\[.*\\]", ""));
                String tmpkey = String.format("%s[%s].%s", mapKey, i, tmpSubKey);
                if (startsWithKey(map, tmpkey)) {
                    isOut = true;
                    break;
                }
            }

            if (isOut) {
                writer.writeStartElement(fd.getName());
                buildXml(String.format("%s[%s]", mapKey, i), map, ld, nrd, writer);
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
     * Map内に指定のキーから始まっている要素があるかどうか
     * @param map Map
     * @param prefix キー
     * @return 存在している場合はtrue
     */
    private boolean startsWithKey(final Map<String, ?> map, final String prefix) {
        if (map == null) {
            return false;
        }

        final String withSeparator = prefix + '.';
        final String withArray = prefix + '[';
        for (final String key : map.keySet()) {
            if (key.equals(prefix) || key.startsWith(withSeparator) || key.startsWith(withArray)) {
                return true;
            }
        }
        return false;
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
        if (map != null && map.containsKey(mapKey)) {
            String[] arr = (String[]) map.get(mapKey);

            // 配列の長さチェック実施
            checkArrayLength(fd, arr.length, currentKeyBase);

            for (int i = 0; i < arr.length; i++) {
                writer.writeStartElement(fd.getName());
                writer.writeCharacters(arr[i]);
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
     * @throws XMLStreamException XML出力に失敗した場合
     */
    private void writeObject(XMLStreamWriter writer, LayoutDefinition ld, RecordDefinition nrd, FieldDefinition fd, 
            String mapKey, Map<String, ?> map, String currentKeyBase) throws XMLStreamException {
        // 属性チェック実施
        if (fd.isAttribute()) {
            throw new InvalidDataFormatException(String.format(
                    "BaseKey = %s,Field %s is Object but specified by Attribute",
                    currentKeyBase, fd.getName()));
        }
        
        // 必須チェック実施
        boolean containsKey = false;
        if (map != null) {
            for (String key : map.keySet()) {
                if (key.startsWith(mapKey)) {
                    containsKey = true;
                    break;
                }
            }
        }
        if (!containsKey) {
            checkIndispensable(currentKeyBase, fd, null);
        }

        if (containsKey) {
            // 子オブジェクトを出力
            writer.writeStartElement(fd.getName());
            buildXml(mapKey, map, ld, nrd, writer);
            writer.writeEndElement();
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

        Object writeVal = null;
        if (map != null) {
            String mapVal = map.get(mapKey) == null ? null : StringUtil.toString(map.get(mapKey));
            writeVal = convertToFieldOnWrite(mapVal, fd);
        }

        // 必須チェック実施
        checkIndispensable(currentKeyBase, fd, writeVal);

        if (map != null && map.containsKey(mapKey) || writeVal != null) {
            CharacterStreamDataString dataType = (CharacterStreamDataString) fd.getDataType();
            // データタイプのコンバータを実行する
            writeVal = dataType.convertOnWrite(writeVal);
            if (fd.isAttribute()) {
                writer.writeAttribute(fd.getName(), StringUtil.toString(writeVal));
            } else if (fd.getName().equals(contentName)) {
                writer.writeCharacters(StringUtil.toString(writeVal));
            } else {
                writer.writeStartElement(fd.getName());
                writer.writeCharacters(StringUtil.toString(writeVal));
                writer.writeEndElement();
            }
        }
    }
}
