package net.sf.jabref.logic.util.io;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Currently used for debugging only
 */
public class XMLUtil {
    private static final Log LOGGER = LogFactory.getLog(XMLUtil.class);

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

}
