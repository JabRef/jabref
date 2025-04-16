package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.StandardFileType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself
 */
@AllowedToUseClassGetResource("org.jabref.logic.citationstyle.CitationStyle.discoverCitationStyles reads the whole path to discover all available styles. Should be converted to a build-time job.")
public class CitationStyle implements OOStyle {

    public static final String DEFAULT = "/ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyle.class);
    private static final String STYLES_ROOT = "/csl-styles";
    private static final String CATALOG_PATH = "/citation-style-catalog.json";
    private static final List<CitationStyle> STYLES = new ArrayList<>();
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    // Currently, we have support for only one alphanumeric style, so we hardcode it
    private static final String ALPHANUMERIC_STYLE = "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690";

    private final String filePath;
    private final String title;
    private final boolean isNumericStyle;
    private final String source;

    public CitationStyle(String filename, String title, boolean isNumericStyle, String source) {
        this.filePath = Objects.requireNonNull(filename);
        this.title = Objects.requireNonNull(title);
        this.isNumericStyle = isNumericStyle;
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates an CitationStyle instance out of the style string
     */
    private static Optional<CitationStyle> createCitationStyleFromSource(final InputStream source, final String filename) {
        try {
            String content = new String(source.readAllBytes());

            Optional<StyleInfo> styleInfo = parseStyleInfo(filename, content);
            return styleInfo.map(info -> new CitationStyle(filename, info.title(), info.isNumericStyle(), content));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    public record StyleInfo(String title, boolean isNumericStyle) {
    }

    @VisibleForTesting
    public static Optional<StyleInfo> parseStyleInfo(String filename, String content) {
        FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);

        try {
            XMLStreamReader reader = FACTORY.createXMLStreamReader(new StringReader(content));

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

    private static String stripInvalidProlog(String source) {
        int startIndex = source.indexOf("<");
        if (startIndex > 0) {
            return source.substring(startIndex);
        } else {
            return source;
        }
    }

    /**
     * Loads the CitationStyle from the given file
     */
    public static Optional<CitationStyle> createCitationStyleFromFile(final String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: {}", styleFile);
            return Optional.empty();
        }

        // Check if this is an absolute path (external file)
        Path filePath = Path.of(styleFile);
        if (filePath.isAbsolute() && Files.exists(filePath)) {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                return createCitationStyleFromSource(inputStream, styleFile);
            } catch (IOException e) {
                LOGGER.error("Error reading source file", e);
                return Optional.empty();
            }
        }

        // If not an absolute path, treat as internal resource
        String internalFile = STYLES_ROOT + (styleFile.startsWith("/") ? "" : "/") + styleFile;
        try (InputStream inputStream = CitationStyle.class.getResourceAsStream(internalFile)) {
            if (inputStream == null) {
                LOGGER.error("Could not find file: {}", styleFile);
                return Optional.empty();
            }
            return createCitationStyleFromSource(inputStream, styleFile);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return Optional.empty();
    }

    /**
     * Provides the default citation style which is currently IEEE
     *
     * @return default citation style
     */
    public static CitationStyle getDefault() {
        return createCitationStyleFromFile(DEFAULT).orElse(new CitationStyle("", "Empty", false, ""));
    }

    /**
     * Provides the citation styles that come with JabRef.
     * The list of styles is determined via a build-time task that exports "citation-style-catalog.json"
     * Note: If we're in a context where OpenOfficePreferences is available, we should use CSLStyleLoader instead of this method.
     * This method is kept for backward compatibility (for Previews and tests, where preferences aren't involved).
     *
     * @return list of available citation styles
     */
    public static List<CitationStyle> discoverCitationStyles() {
        if (!STYLES.isEmpty()) {
            return STYLES;
        }

        try (InputStream is = CitationStyle.class.getResourceAsStream(CATALOG_PATH)) {
            if (is == null) {
                LOGGER.error("Could not find citation style catalog");
                return List.of();
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> styleInfoList = mapper.readValue(is,
                    new TypeReference<>() {
                    });

            for (Map<String, Object> info : styleInfoList) {
                String path = (String) info.get("path");
                Optional<CitationStyle> styleOpt = createCitationStyleFromFile(path);
                styleOpt.ifPresent(STYLES::add);
            }

            return STYLES;
        } catch (IOException e) {
            LOGGER.error("Error loading citation style catalog", e);
            return List.of();
        }
    }

    /**
     * Checks if the given style file is a CitationStyle
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    public String getTitle() {
        return title;
    }

    public boolean isNumericStyle() {
        return isNumericStyle;
    }

    /**
     * Currently, we have support for one alphanumeric CSL style.
     * There is no tag or field in .csl style files that can be parsed to determine if it is an alphanumeric style.
     * Thus, to determine alphanumeric nature, we currently manually check for equality with "DIN 1505-2".
     */
    public boolean isAlphanumericStyle() {
        return ALPHANUMERIC_STYLE.equals(this.title);
    }

    public String getSource() {
        return source;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        CitationStyle other = (CitationStyle) o;
        return Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public boolean isInternalStyle() {
        // If the path is an absolute path, it's an external style
        return !Path.of(filePath).isAbsolute();
    }

    @Override
    public String getPath() {
        return getFilePath();
    }
}
