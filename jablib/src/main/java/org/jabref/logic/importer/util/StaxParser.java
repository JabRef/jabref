package org.jabref.logic.importer.util;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class StaxParser {

    /**
     * Extracts the XML content inside the first
     * encountered parent tag, including tag elements,
     * attributes, namespace, prefix and contained text
     *
     * @param reader the stream reader
     * @return Returns the inner XML content
     */
    public static String getXMLContent(XMLStreamReader reader) throws XMLStreamException {
        // skip over START DOCUMENT event
        while (reader.getEventType() == XMLStreamConstants.START_DOCUMENT && reader.hasNext()) {
            reader.next();
        }

        StringBuilder content = new StringBuilder();

        String parentTag = reader.getLocalName();
        int depth = 1;
        content.append(getXMLStartTag(reader, true));

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();

                if (tagName.equals(parentTag)) {
                    // nested tag of same type
                    depth++;
                }

                // append the start tag
                content.append(getXMLStartTag(reader, false));
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();

                // append the end tag
                content.append(getXMLEndTag(reader));

                if (tagName.equals(parentTag)) {
                    depth--;

                    if (depth == 0) {
                        // reached the closing tag of the first parent tag
                        break;
                    }
                }
            } else if (event == XMLStreamConstants.CHARACTERS) {
                content.append(getXMLText(reader));
            } else if (event == XMLStreamConstants.CDATA) {
                content.append(getXMLCData(reader));
            } else if (event == XMLStreamConstants.COMMENT) {
                content.append(getXMLComment(reader));
            } else if (event == XMLStreamConstants.PROCESSING_INSTRUCTION) {
                content.append(getXMLProcessingInstruction(reader));
            } else if (event == XMLStreamConstants.SPACE || event == XMLStreamConstants.ENTITY_REFERENCE) {
                content.append(getXMLText(reader));
            }
        }

        return content.toString().trim();
    }

    private static String getXMLStartTag(XMLStreamReader reader, boolean addNamespaceURI) {
        StringBuilder startTag = new StringBuilder();

        String prefix = reader.getPrefix();

        startTag.append("<")
                .append(prefix != null && !prefix.isBlank() ? prefix + ":" : "")
                .append(reader.getName().getLocalPart());

        String namespaceURI = reader.getNamespaceURI();
        if (addNamespaceURI && namespaceURI != null) {
            startTag.append(" xmlns")
                    .append(prefix != null && !prefix.isBlank() ? ":" + prefix : "")
                    .append("=\"")
                    .append(namespaceURI)
                    .append("\"");
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            startTag.append(" ").append(reader.getAttributeLocalName(i)).append("=\"").append(reader.getAttributeValue(i)).append("\"");
        }

        if (reader.isEndElement()) {
            startTag.append("/");
        }

        startTag.append(">");
        return startTag.toString();
    }

    private static String getXMLEndTag(XMLStreamReader reader) {
        StringBuilder endTag = new StringBuilder();
        String prefix = reader.getPrefix();

        endTag.append("</")
              .append(prefix != null && !prefix.isBlank() ? prefix + ":" : "")
              .append(reader.getName().getLocalPart())
              .append(">");

        return endTag.toString();
    }

    private static String getXMLCData(XMLStreamReader reader) {
        return "<![CDATA[" + reader.getText() + "]]>";
    }

    private static String getXMLComment(XMLStreamReader reader) {
        return "<!--" + reader.getText() + "-->";
    }

    private static String getXMLProcessingInstruction(XMLStreamReader reader) {
        return "<?" + reader.getPITarget() + " " + reader.getPIData() + "?>";
    }

    private static String getXMLText(XMLStreamReader reader) {
        return reader.getText().trim();
    }
}
