package nablarch.core.dataformat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.util.Builder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XMLパーサー。<br>
 * この実装ではDOMを使用してXMLデータの解析を行います。
 *
 * @author TIS
 */
public class XmlDataParser extends StructuredDataEditorSupport implements StructuredDataParser {

    /** 属性あり要素のコンテンツ名(デフォルトはbody) */
    private String contentName = "body";

    /**
     * フラットマップを作成します。
     *
     * @param xml XML文字列
     * @param layoutDef フォーマット定義
     * @return フラットマップ
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合
     * @throws InvalidDataFormatException 読み込んだデータがフォーマット定義に違反している場合
     */
    public Map<String, ?> parseData(InputStream xml, LayoutDefinition layoutDef)
            throws IOException, InvalidDataFormatException {

        Map<String, Object> record = new HashMap<String, Object>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xml);

            RecordDefinition recordDef = layoutDef.getRecords().get(0);
            String typeName = recordDef.getTypeName();
            NodeList nodeList = doc.getElementsByTagName(recordDef.getTypeName());
            if (nodeList.getLength() == 0) {
                throw new InvalidDataFormatException("expected node [" + typeName + "] not found.");
            }
            Element rootNode = (Element) nodeList.item(0);
            makeMap("", record, layoutDef, recordDef, rootNode);
        } catch (SAXException e) {
            throw new InvalidDataFormatException(
                    String.format("invalid data found. [%s]", e.getMessage()), e);
        } catch (ParserConfigurationException e) {
            throw new InvalidDataFormatException(
                    String.format("invalid data found. [%s]", e.getMessage()), e);
        }
        return record;
    }

    /**
     * フラットMap作成処理</br>
     * XMLを解析したDOMから、キーで階層構造を表現した１階層のMapを作成します。
     *
     * @param currentKeyBase キー名ベース
     * @param outMap 出力対象マップ
     * @param layoutDef フォーマット定義
     * @param recordDef レコードタイプ定義
     * @param parent 親ノード
     */
    private void makeMap(String currentKeyBase, Map<String, Object> outMap,
            LayoutDefinition layoutDef, RecordDefinition recordDef, Element parent) {

        for (FieldDefinition fieldDef : recordDef.getFields()) {
            if (fieldDef.isFiller()) {
                continue;
            }
            // Mapに格納する際のKeyを作成
            String fieldName = fieldDef.getName();
            String mapKey = buildMapKey(currentKeyBase, fieldName);

            if (fieldDef.isArray()) {

                if (fieldName.equals(contentName)) {
                    throw new InvalidDataFormatException("Array type can not be specified in the content."
                            + " parent name: " + currentKeyBase + ",field name: " + fieldName);
                }

                // 子ノード
                NodeList childNodes = parent.getElementsByTagName(fieldName);

                // Listの長さチェック実行
                checkArrayLength(fieldDef, childNodes.getLength(), currentKeyBase);

                if (isObjectType(fieldDef)) {
                    // オブジェクト配列
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Element child = (Element) childNodes.item(i);
                        String nextKeyBase = Builder.concat(mapKey, "[", i, "]");
                        RecordDefinition nextRecordDef = layoutDef.getRecordType(fieldName);
                        makeMap(nextKeyBase, outMap, layoutDef, nextRecordDef, child);
                    }
                    // 要素数の情報を付与
                    outMap.put(mapKey + "Size", Integer.toString(childNodes.getLength()));
                } else {
                    // 文字列配列
                    String[] array = toStringArray(childNodes);
                    outMap.put(mapKey, array);
                }
            } else {
                if (isObjectType(fieldDef)) {
                    // 属性チェック実施
                    if (fieldDef.isAttribute()) {
                        throw new InvalidDataFormatException(String.format(
                                "BaseKey = %s,Field %s is Object but specified by Attribute",
                                currentKeyBase, fieldName));
                    }

                    // Object
                    Element child = getChildElement(fieldName, parent);

                    // 必須チェック実行
                    checkIndispensable(currentKeyBase, fieldDef, child);

                    RecordDefinition next = layoutDef.getRecordType(fieldName);
                    if (child != null) {
                        makeMap(mapKey, outMap, layoutDef, next, child);
                    }
                } else {
                    String childNodeVal;
                    if (fieldDef.isAttribute()) {
                        // 属性
                        Node attr = parent.getAttributes().getNamedItem(fieldName);
                        childNodeVal = toString(attr);
                    } else if (fieldName.equals(contentName)) {
                        // コンテンツ
                        childNodeVal = parent.getFirstChild() == null ? null : toString(parent);
                    } else {
                        // ノード
                        Element child = getChildElement(fieldName, parent);
                        childNodeVal = toString(child);
                    }
                    Object convertedValue = convertToFieldOnRead(childNodeVal, fieldDef);

                    // 必須チェック実行
                    checkIndispensable(currentKeyBase, fieldDef, convertedValue);

                    outMap.put(mapKey, convertedValue);
                }
            }
        }
    }


    /**
     * 子ノードを取得します。
     *
     * @param targetNodeName 取得対象となるノード名
     * @param parent 取得元の親ノード
     * @return 子ノード
     */
    private Element getChildElement(String targetNodeName, Element parent) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE
                    && targetNodeName.equals(item.getNodeName())) {
                return (Element) item;
            }
        }
        return null;
    }

    /**
     * ノードを文字列に変換する。
     *
     * @param node 変換対象となるノード
     * @return 返還後の文字列（引数がnullの場合はnullが返却される）
     */
    private String toString(Node node) {
        return node == null ? null : node.getTextContent();
    }

    /**
     * NodeListを文字列配列に変換する。
     * @param nodeList 変換対象となるNodeList
     * @return 返還後の文字列配列
     */
    private String[] toStringArray(NodeList nodeList) {
        // 文字列配列
        int len = nodeList.getLength();
        String[] array = new String[len];
        for (int i = 0; i < len; i++) {
            array[i] = nodeList.item(i).getTextContent();
        }
        return array;
    }

    /**
     * 属性あり要素のコンテンツ名を設定する。
     *
     * @param contentName 属性あり要素のコンテンツ名
     */
    public void setContentName(String contentName) {
        this.contentName = contentName;
    }
}
