package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.util.StandardFileType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling Citation Style Language (CSL) files.
 * Contains shared functionality used by both runtime ({@link CSLStyleLoader}) and build-time ({@link org.jabref.generators.CitationStyleCatalogGenerator}) components.
 */
public final class CSLStyleUtils {
    private static final String STYLES_ROOT = "/csl-styles";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleUtils.class);

    /**
     * Style information record (title, numeric nature, has bibliography specification, bibliography uses hanging indent) for a citation style.
     */
    public record StyleInfo(String title, String shortTitle, boolean isNumericStyle, boolean hasBibliography, boolean usesHangingIndent) {
    }

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    private CSLStyleUtils() {
        // prevent instantiation
    }

    /**
     * Checks if the given style file is a CitationStyle based on its extension
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    /**
     * Creates a CitationStyle from a file path.
     *
     * @param styleFile Path to the CSL file
     * @return Optional containing the CitationStyle if valid, empty otherwise
     */
    public static Optional<CitationStyle> createCitationStyleFromFile(String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Not a .csl style file: {}", styleFile);
            return Optional.empty();
        }

        // Check if absolute path (meaning: external CSL file) - and exists
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
            return styleInfo.map(info -> new CitationStyle(
                    filename,
                    info.title(),
                    info.shortTitle(),
                    info.isNumericStyle(),
                    info.hasBibliography(),
                    info.usesHangingIndent(),
                    content,
                    isInternal));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    /**
     * Parses the style information from a style content using StAX.
     *
     * @param filename The filename of the style (for logging)
     * @param content  The XML content of the style
     * @return Optional containing the StyleInfo if valid, empty otherwise
     */
    public static Optional<StyleInfo> parseStyleInfo(String filename, String content) {
        try {
            XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(Reader.of(content));

            boolean inInfo = false;
            boolean hasBibliography = false;
            boolean hasCitation = false;
            boolean usesHangingIndent = false;
            String title = "";
            boolean isNumericStyle = false;
            String shortTitle = "";

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();

                    switch (elementName) {
                        case "bibliography" -> {
                            hasBibliography = true;
                            String hangingIndent = reader.getAttributeValue(null, "hanging-indent");
                            usesHangingIndent = "true".equals(hangingIndent);
                        }
                        case "citation" ->
                                hasCitation = true;
                        case "info" ->
                                inInfo = true;
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

            if (hasCitation && title != null) {
                return Optional.of(new StyleInfo(title, shortTitle, isNumericStyle, hasBibliography, usesHangingIndent));
            } else {
                LOGGER.debug("No valid title or citation found for file {}", filename);
                return Optional.empty();
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Error parsing XML for file {}: {}", filename, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
