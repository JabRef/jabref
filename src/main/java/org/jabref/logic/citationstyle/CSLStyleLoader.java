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

import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the loading of CitationStyles from both internal resources and external files.
 */
public class CSLStyleLoader {
    public static final String DEFAULT_STYLE = "ieee.csl";

    private static final String STYLES_ROOT = "/csl-styles";
    private static final String CATALOG_PATH = "/citation-style-catalog.json";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final List<CitationStyle> INTERNAL_STYLES = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleLoader.class);

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
        loadInternalStyles();
    }

    private final OpenOfficePreferences openOfficePreferences;
    private final List<CitationStyle> externalStyles = new ArrayList<>();

    /**
     * Style information record (title, isNumericStyle) pair for a citation style.
     */
    public record StyleInfo(String title, boolean isNumericStyle) {
    }

    public CSLStyleLoader(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = Objects.requireNonNull(openOfficePreferences);
        loadExternalStyles();
    }

    /**
     * Returns a list of all available citation styles (both internal and external).
     */
    public List<CitationStyle> getStyles() {
        List<CitationStyle> result = new ArrayList<>(INTERNAL_STYLES);
        result.addAll(externalStyles);
        return result;
    }

    /**
     * Returns the default citation style which is currently set to {@link CSLStyleLoader#DEFAULT_STYLE}.
     */
    public static CitationStyle getDefaultStyle() {
        return INTERNAL_STYLES.stream()
                              .filter(style -> DEFAULT_STYLE.equals(style.getFilePath()))
                              .findFirst()
                              .orElseGet(() -> createCitationStyleFromFile(DEFAULT_STYLE)
                                      .orElse(new CitationStyle("", "Empty", false, "", true)));
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
     * Loads the internal (built-in) CSL styles from the catalog generated at build-time.
     */
    private static void loadInternalStyles() {
        INTERNAL_STYLES.clear();

        try (InputStream is = CSLStyleLoader.class.getResourceAsStream(CATALOG_PATH)) {
            if (is == null) {
                LOGGER.error("Could not find citation style catalog");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> styleInfoList = mapper.readValue(is,
                    new TypeReference<>() {
                    });

            if (!styleInfoList.isEmpty()) {
                for (Map<String, Object> info : styleInfoList) {
                    String path = (String) info.get("path");
                    String title = (String) info.get("title");
                    boolean isNumeric = (boolean) info.get("isNumeric");

                    // We use these metadata and just load the content instead of re-parsing for them
                    try (InputStream styleStream = CSLStyleLoader.class.getResourceAsStream(STYLES_ROOT + "/" + path)) {
                        if (styleStream != null) {
                            String source = new String(styleStream.readAllBytes());
                            CitationStyle style = new CitationStyle(path, title, isNumeric, source, true);
                            INTERNAL_STYLES.add(style);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error loading style file: {}", path, e);
                    }
                }
            } else {
                LOGGER.error("Citation style catalog is empty");
            }
        } catch (IOException e) {
            LOGGER.error("Error loading citation style catalog", e);
        }
    }

    /**
     * Loads external CSL styles from the preferences.
     */
    private void loadExternalStyles() {
        externalStyles.clear();

        // Read external lists
        List<String> stylePaths = openOfficePreferences.getExternalCslStyles();
        for (String stylePath : stylePaths) {
            try {
                Optional<CitationStyle> style = createCitationStyleFromFile(stylePath);
                style.ifPresent(externalStyles::add);
            } catch (Exception e) {
                LOGGER.info("Problem reading external style file {}", stylePath, e);
            }
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
        try (InputStream inputStream = CSLStyleLoader.class.getResourceAsStream(internalFile)) {
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

    /**
     * Adds a new external CSL style if it's valid.
     *
     * @return Optional containing the added CitationStyle if valid, empty otherwise
     */
    public Optional<CitationStyle> addStyleIfValid(String stylePath) {
        Objects.requireNonNull(stylePath);

        Optional<CitationStyle> newStyleOptional = createCitationStyleFromFile(stylePath);
        if (newStyleOptional.isPresent()) {
            CitationStyle newStyle = newStyleOptional.get();

            // Add it to the list and save to preferences
            externalStyles.add(newStyle);
            storeExternalStyles();
            return newStyleOptional;
        }

        return Optional.empty();
    }

    /**
     * Stores the current list of external styles to preferences.
     */
    private void storeExternalStyles() {
        List<String> stylePaths = externalStyles.stream()
                                                .map(CitationStyle::getPath)
                                                .toList();
        openOfficePreferences.setExternalCslStyles(stylePaths);
    }

    /**
     * Removes a style from the external styles list.
     *
     * @return true if the style was removed, false otherwise
     */
    public boolean removeStyle(CitationStyle style) {
        Objects.requireNonNull(style);
        if (!style.isInternalStyle()) {
            boolean result = externalStyles.remove(style);
            storeExternalStyles();
            return result;
        }
        return false;
    }

    public static List<CitationStyle> getInternalStyles() {
        if (INTERNAL_STYLES.isEmpty()) {
            loadInternalStyles();
        }
        return List.copyOf(INTERNAL_STYLES);
    }
}
