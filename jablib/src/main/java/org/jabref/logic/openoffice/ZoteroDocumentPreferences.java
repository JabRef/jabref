package org.jabref.logic.openoffice;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.model.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@NullMarked
public final class ZoteroDocumentPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZoteroDocumentPreferences.class);

    private static final int MAX_LENGTH = 255;
    private static final String ZOTERO_PREF = "ZOTERO_PREF";
    private static final String CSL_PREF = "CSL_PREF";
    private static final String STYLE = "style";
    private static final String STYLE_ID = "id";
    private static final String EMPTY_XML_DOCUMENT_DATA = """
            <data data-version="3" zotero-version="">\
            <session id=""/>\
            <style id="" hasBibliography="0" bibliographyStyleHasBeenSet="0"/>\
            <prefs><pref name="fieldType" value="ReferenceMark"/></prefs>\
            </data>""";

    private ZoteroDocumentPreferences() {
    }

    public static Optional<CitationStyle> findCitationStyle(XTextDocument document, List<CitationStyle> availableStyles)
            throws WrappedTargetException {
        Optional<String> serializedData = readSerializedData(document);
        Optional<String> styleId = serializedData.flatMap(ZoteroDocumentPreferences::findStyleId);
        if (styleId.isEmpty()) {
            return Optional.empty();
        }

        String documentStyleId = styleId.get();
        for (CitationStyle availableStyle : availableStyles) {
            if (documentStyleId.equals(availableStyle.getStyleId())) {
                return Optional.of(availableStyle);
            }
        }

        return Optional.empty();
    }

    public static boolean writeCitationStyle(XTextDocument document, CitationStyle citationStyle)
            throws
            IllegalTypeException,
            NotRemoveableException,
            PropertyVetoException,
            WrappedTargetException {
        Optional<String> serializedData = readSerializedData(document);
        String updatedData;
        try {
            if (serializedData.isPresent()) {
                // update current preference with styleId
                updatedData = createOrUpdatePrefWithStyleId(serializedData.get(), citationStyle.getStyleId());
            } else {
                // create preference with styleId
                updatedData = createOrUpdatePrefWithStyleId(EMPTY_XML_DOCUMENT_DATA, citationStyle.getStyleId());
            }
            writeProperty(document, ZOTERO_PREF, updatedData);
        } catch (IOException e) {
            LOGGER.warn("Could not serialize Zotero document preferences", e);
            return false;
        }
        return true;
    }

    static Optional<String> findStyleId(String serializedData) {
        if (serializedData.isBlank()) {
            return Optional.empty();
        }

        try {
            Document document = buildXmlDocument(serializedData);
            Node styleNode = document.getElementsByTagName(STYLE).item(0);
            if (!(styleNode instanceof Element styleElement)) {
                return Optional.empty();
            }

            String attribute = styleElement.getAttribute(STYLE_ID);
            if (attribute.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(attribute);
        } catch (IOException e) {
            LOGGER.debug("Could not parse Zotero document style", e);
            return Optional.empty();
        }
    }

    private static String createOrUpdatePrefWithStyleId(String serializedData, String styleId) throws IOException {
        Document document = buildXmlDocument(serializedData);
        Element styleElement = getOrCreateXmlStyleElement(document);
        styleElement.setAttribute(STYLE_ID, styleId);
        return serializeXmlDocument(document);
    }

    private static Document buildXmlDocument(String serializedData) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(Reader.of(serializedData));
            return builder.parse(inputSource);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not parse Zotero XML document data", e);
        }
    }

    private static Element getOrCreateXmlStyleElement(Document document) throws IOException {
        Node styleNode = document.getElementsByTagName(STYLE).item(0);
        if (styleNode instanceof Element styleElement) {
            return styleElement;
        }

        Element dataElement = document.getDocumentElement();
        if (dataElement == null) {
            throw new IOException("Zotero XML document data has no root element");
        }

        Element styleElement = document.createElement(STYLE);
        dataElement.appendChild(styleElement);
        return styleElement;
    }

    private static String serializeXmlDocument(Document document) throws IOException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IOException("Could not serialize Zotero XML document data", e);
        }
    }

    private static Optional<String> readSerializedData(XTextDocument document)
            throws WrappedTargetException {
        Optional<String> serializedData = readChunkedProperty(document, ZOTERO_PREF);
        if (serializedData.isEmpty()) {
            serializedData = readChunkedProperty(document, CSL_PREF);
        }
        return serializedData;
    }

    private static Optional<String> readChunkedProperty(XTextDocument document, String propertyName)
            throws WrappedTargetException {
        List<String> chunks = new ArrayList<>();
        int chunkIndex = 1;

        while (true) {
            String chunkPropertyName = propertyName + "_" + chunkIndex;
            Optional<String> chunk = UnoUserDefinedProperty.getStringValue(document, chunkPropertyName);
            if (chunk.isEmpty()) {
                break;
            }
            chunks.add(chunk.orElseThrow());
            chunkIndex++;
        }

        if (chunks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("", chunks));
    }

    private static void writeProperty(XTextDocument document, String propertyName, String value)
            throws
            IllegalTypeException,
            NotRemoveableException,
            PropertyVetoException,
            WrappedTargetException {
        int chunkCount = Math.ceilDiv(value.length(), MAX_LENGTH);
        for (int chunkIndex = 1; chunkIndex <= chunkCount; chunkIndex++) {
            int start = (chunkIndex - 1) * MAX_LENGTH;
            int end = Math.min(chunkIndex * MAX_LENGTH, value.length());
            UnoUserDefinedProperty.setStringProperty(document, propertyName + "_" + chunkIndex, value.substring(start, end));
        }
        removeTrailingChunks(document, propertyName, chunkCount);
    }

    private static void removeTrailingChunks(XTextDocument document, String propertyName, int lastChunkIndex)
            throws NotRemoveableException {
        for (String existingProperty : UnoUserDefinedProperty.getListOfNames(document)) {
            Optional<Integer> chunkIndex = getChunkIndex(propertyName, existingProperty);
            if (chunkIndex.isEmpty()) {
                continue;
            }

            int index = chunkIndex.get();
            if (index > lastChunkIndex) {
                UnoUserDefinedProperty.removeIfExists(document, existingProperty);
            }
        }
    }

    private static Optional<Integer> getChunkIndex(String propertyName, String existingProperty) {
        String prefix = propertyName + "_";
        if (!existingProperty.startsWith(prefix)) {
            return Optional.empty();
        }

        try {
            int chunkIndex = Integer.parseInt(existingProperty.substring(prefix.length()));
            if (chunkIndex > 0) {
                return Optional.of(chunkIndex);
            }
        } catch (NumberFormatException _) {
            // Ignore similarly named custom properties.
        }
        return Optional.empty();
    }
}
