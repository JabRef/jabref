package org.jabref.logic.util.io;

import java.io.StringWriter;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.RandomAccess;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Currently used for debugging only
 */
public class XMLUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtil.class);

    private XMLUtil() {
    }

    /**
     * Prints out the document to standard out. Used to generate files for test cases.
     */
    public static void printDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, result);
            System.out.println(writer);
        } catch (TransformerException ex) {
            LOGGER.error("", ex);
        }
    }

    public static List<Node> asList(NodeList n) {
        return n.getLength() == 0 ? Collections.emptyList() : new NodeListWrapper(n);
    }

    /**
     * Gets the content of a subnode.
     * For example,
     * <item>
     *     <nodeName>content</nodeName>
     * </item>
     */
    public static Optional<String> getNodeContent(Node item, String nodeName) {
        if (item.getNodeType() != Node.ELEMENT_NODE) {
            return Optional.empty();
        }

        NodeList metadata = ((Element) item).getElementsByTagName(nodeName);
        if (metadata.getLength() == 1) {
            return Optional.ofNullable(metadata.item(0).getTextContent());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the content of an attribute.
     * For example,
     * <item attributeName="content" />
     */
    public static Optional<String> getAttributeContent(Node item, String attributeName) {
        NamedNodeMap attributes = item.getAttributes();
        return Optional.ofNullable(attributes.getNamedItem(attributeName)).map(Node::getTextContent);
    }

    /**
     * Gets a list of subnodes with the specified tag name.
     * For example,
     * <item>
     *     <node>first hit</node>
     *     <node>second hit</node>
     * </item>
     */
    public static List<Node> getNodesByName(Node item, String nodeName) {
        if (item.getNodeType() != Node.ELEMENT_NODE) {
            return Collections.emptyList();
        }
        NodeList nodes = ((Element) item).getElementsByTagName(nodeName);
        return asList(nodes);
    }

    /**
     * Gets a the first subnode with the specified tag name.
     * For example,
     * <item>
     *     <node>hit</node>
     *     <node>second hit, but not returned</node>
     * </item>
     */
    public static Optional<Node> getNode(Node item, String nodeName) {
        return getNodesByName(item, nodeName).stream().findFirst();
    }

    // Wrapper to make NodeList iterable,
    // taken from <a href="http://stackoverflow.com/questions/19589231/can-i-iterate-through-a-nodelist-using-for-each-in-java">StackOverflow Answer</a>.
    private static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {

        private final NodeList list;

        NodeListWrapper(NodeList list) {
            this.list = list;
        }

        @Override
        public Node get(int index) {
            return list.item(index);
        }

        @Override
        public int size() {
            return list.getLength();
        }
    }
}
