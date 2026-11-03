package org.jabref.logic.citationstyle;

import java.io.Reader;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// CSL metadata shared by the runtime loader and the standalone catalog generator, without
/// depending on the desktop style hierarchy or its formatting engines.
@NullMarked
public record CSLStyleMetadata(String styleId,
                               String styleClass,
                               String title,
                               String shortTitle,
                               boolean isNumericStyle,
                               boolean hasBibliography,
                               boolean hasBibliographySortOrder,
                               boolean usesHangingIndent) {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleMetadata.class);
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    public static Optional<CSLStyleMetadata> parseStyleInfo(String filename, String content) {
        XMLStreamReader reader;
        try {
            reader = XML_INPUT_FACTORY.createXMLStreamReader(Reader.of(content));
        } catch (XMLStreamException exception) {
            LOGGER.error("Error opening CSL XML for file {}", filename, exception);
            return Optional.empty();
        }
        try {
            boolean inInfo = false;
            boolean hasBibliography = false;
            boolean hasBibliographySortOrder = false;
            boolean hasCitation = false;
            boolean usesHangingIndent = false;
            boolean inBibliography = false;
            String styleId = "";
            String styleClass = "";
            String title = "";
            boolean isNumericStyle = false;
            String shortTitle = "";
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "style" ->
                                styleClass = Optional.ofNullable(reader.getAttributeValue("", "class")).orElse("");
                        case "bibliography" -> {
                            hasBibliography = true;
                            inBibliography = true;
                            usesHangingIndent = "true".equals(reader.getAttributeValue("", "hanging-indent"));
                        }
                        case "sort" -> {
                            if (inBibliography) {
                                hasBibliographySortOrder = true;
                            }
                        }
                        case "citation" ->
                                hasCitation = true;
                        case "info" ->
                                inInfo = true;
                        case "id" -> {
                            if (inInfo) {
                                styleId = reader.getElementText();
                            }
                        }
                        case "title" -> {
                            if (inInfo) {
                                title = reader.getElementText();
                            }
                        }
                        case "title-short" -> {
                            if (inInfo) {
                                shortTitle = reader.getElementText();
                            }
                        }
                        case "category" ->
                                isNumericStyle = Optional.ofNullable(reader.getAttributeValue("", "citation-format"))
                                                         .map("numeric"::equals).orElse(isNumericStyle);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("info".equals(reader.getLocalName())) {
                        inInfo = false;
                    } else if ("bibliography".equals(reader.getLocalName())) {
                        inBibliography = false;
                    }
                }
            }
            if (hasCitation) {
                return Optional.of(new CSLStyleMetadata(styleId, styleClass, title, shortTitle, isNumericStyle,
                        hasBibliography, hasBibliographySortOrder, usesHangingIndent));
            }
            LOGGER.debug("No citation found for file {}", filename);
            return Optional.empty();
        } catch (XMLStreamException exception) {
            LOGGER.error("Error parsing CSL XML for file {}", filename, exception);
            return Optional.empty();
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException exception) {
                LOGGER.debug("Could not close CSL XML reader for file {}", filename, exception);
            }
        }
    }
}
