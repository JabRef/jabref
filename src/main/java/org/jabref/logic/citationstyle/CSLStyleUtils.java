package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling Citation Style Language (CSL) files.
 * Contains shared functionality used by both runtime ({@link CSLStyleLoader}) and build-time ({@link CitationStyleCatalogGenerator}) components.
 */
public class CSLStyleUtils {
    private static final String STYLES_ROOT = "/csl-styles";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleUtils.class);

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    private CSLStyleUtils() {
        // prevent instantiation
    }

    /**
     * Style information record (title, isNumericStyle) pair for a citation style.
     */
    public record StyleInfo(String title, boolean isNumericStyle) {
    }

    /**
     * Parses the style information from a style content using StAX.
     *
     * @param filename The filename of the style (for logging)
     * @param content The XML content of the style
     * @return Optional containing the StyleInfo if valid, empty otherwise
     */
    public static Optional<StyleInfo> parseStyleInfo(String filename, String content) {
        try {
            XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(content));

            boolean inInfo = false;
            boolean hasBibliography = false;
            String title = "";
            boolean isNumericStyle = false;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();

                    switch (elementName) {
                        case "bibliography" -> hasBibliography = true;
                        case "info" -> inInfo = true;
                        case "title" -> {
                            if (inInfo) {
                                title = reader.getElementText();
                            }
                        }
                        case "category" -> {
                            String citationFormat = reader.getAttributeValue(null, "citation-format");
                            if (citationFormat != null) {
                                isNumericStyle = "numeric".equals(citationFormat);
                            }
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("info".equals(reader.getLocalName())) {
                        inInfo = false;
                    }
                }
            }

            if (hasBibliography && title != null) {
                return Optional.of(new StyleInfo(title, isNumericStyle));
            } else {
                LOGGER.debug("No valid title or bibliography found for file {}", filename);
                return Optional.empty();
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Error parsing XML for file {}: {}", filename, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Creates a CitationStyle from a file path.
     *
     * @param styleFile Path to the CSL file
     * @return Optional containing the CitationStyle if valid, empty otherwise
     */
    public static Optional<CitationStyle> createCitationStyleFromFile(String styleFile) {
        if (!CitationStyle.isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: {}", styleFile);
            return Optional.empty();
        }

        // Check if this is an absolute path (external file)
        Path filePath = Path.of(styleFile);
        if (filePath.isAbsolute() && Files.exists(filePath)) {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                return createCitationStyleFromSource(inputStream, styleFile, false);
            } catch (IOException e) {
                LOGGER.error("Error reading source file", e);
                return Optional.empty();
            }
        }

        // If not an absolute path, treat as internal resource
        String internalFile = STYLES_ROOT + (styleFile.startsWith("/") ? "" : "/") + styleFile;
        try (InputStream inputStream = CSLStyleUtils.class.getResourceAsStream(internalFile)) {
            if (inputStream == null) {
                LOGGER.error("Could not find file: {}", styleFile);
                return Optional.empty();
            }
            return createCitationStyleFromSource(inputStream, styleFile, true);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return Optional.empty();
    }

    /**
     * Creates a CitationStyle from the input stream.
     *
     * @return Optional containing the CitationStyle if valid, empty otherwise
     */
    private static Optional<CitationStyle> createCitationStyleFromSource(InputStream source, String filename, boolean isInternal) {
        try {
            String content = new String(source.readAllBytes());

            Optional<StyleInfo> styleInfo = parseStyleInfo(filename, content);
            return styleInfo.map(info -> new CitationStyle(filename, info.title(), info.isNumericStyle(), content, isInternal));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }
}
