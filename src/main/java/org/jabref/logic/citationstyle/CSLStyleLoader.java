package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the loading of CitationStyles from both internal resources and external files.
 */
public class CSLStyleLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleLoader.class);
    private static final String CATALOG_PATH = "/citation-style-catalog.json";

    private final OpenOfficePreferences openOfficePreferences;

    // Lists of the internal and external styles
    private final List<CitationStyle> internalStyles = new ArrayList<>();
    private final List<CitationStyle> externalStyles = new ArrayList<>();

    public CSLStyleLoader(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = Objects.requireNonNull(openOfficePreferences);
        loadInternalStyles();
        loadExternalStyles();
    }

    /**
     * Returns a list of all available citation styles (both internal and external).
     */
    public List<CitationStyle> getStyles() {
        List<CitationStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    /**
     * Loads the internal (built-in) CSL styles from the catalog generated at build time.
     */
    private void loadInternalStyles() {
        internalStyles.clear();

        try (InputStream is = CSLStyleLoader.class.getResourceAsStream(CATALOG_PATH)) {
            if (is == null) {
                LOGGER.error("Could not find citation style catalog");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> styleInfoList = mapper.readValue(is,
                    new TypeReference<>() {
                    });

            for (Map<String, Object> info : styleInfoList) {
                String path = (String) info.get("path");
                Optional<CitationStyle> styleOpt = CitationStyle.createCitationStyleFromFile(path);
                styleOpt.ifPresent(internalStyles::add);
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
        List<String> stylePaths = openOfficePreferences.getExternalCitationStyles();
        for (String stylePath : stylePaths) {
            try {
                Optional<CitationStyle> style = createFromExternalFile(stylePath);
                if (style.isPresent()) {
                    externalStyles.add(style.get());
                } else {
                    LOGGER.error("Style with path {} is invalid", stylePath);
                }
            } catch (Exception e) {
                LOGGER.info("Problem reading external style file {}", stylePath, e);
            }
        }
    }

    /**
     * Creates a CitationStyle from an external file.
     *
     * @param filePath Path to the external CSL file
     * @return Optional containing the CitationStyle if valid, empty otherwise
     */
    private Optional<CitationStyle> createFromExternalFile(String filePath) {
        if (!CitationStyle.isCitationStyleFile(filePath)) {
            LOGGER.error("Can only load citation style files: {}", filePath);
            return Optional.empty();
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            LOGGER.error("External style file does not exist: {}", filePath);
            return Optional.empty();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            Optional<CitationStyle> styleOpt = createCitationStyleFromSource(inputStream, filePath);

            // Return a style with the full path for external files
            if (styleOpt.isPresent()) {
                CitationStyle style = styleOpt.get();
                return Optional.of(new ExternalCitationStyle(filePath, style.getTitle(),
                        style.isNumericStyle(), style.getSource()));
            }

            return Optional.empty();
        } catch (IOException e) {
            LOGGER.error("Error reading external style file: {}", filePath, e);
            return Optional.empty();
        }
    }

    /**
     * Creates a CitationStyle from the input stream.
     */
    private Optional<CitationStyle> createCitationStyleFromSource(InputStream source, String filename) {
        try {
            String content = new String(source.readAllBytes());

            Optional<CitationStyle.StyleInfo> styleInfo = CitationStyle.parseStyleInfo(filename, content);
            return styleInfo.map(info -> new CitationStyle(filename, info.title(), info.isNumericStyle(), content));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    /**
     * Adds a new external CSL style if it's valid.
     *
     * @param stylePath Path to the CSL style file
     * @return Optional containing the added CitationStyle if valid, empty otherwise
     */
    public Optional<CitationStyle> addStyleIfValid(String stylePath) {
        Objects.requireNonNull(stylePath);

        Optional<CitationStyle> newStyleOptional = createFromExternalFile(stylePath);
        if (newStyleOptional.isPresent()) {
            CitationStyle newStyle = newStyleOptional.get();

            // Check if it already exists
            if (externalStyles.stream().anyMatch(style -> style.getPath().equals(stylePath))) {
                LOGGER.info("External style file {} already exists.", stylePath);
                return newStyleOptional; // Return it anyway since it's valid
            }

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
                                                .collect(Collectors.toList());
        openOfficePreferences.setExternalCitationStyles(stylePaths);
    }

    /**
     * Removes a style from the external styles list.
     *
     * @param style The style to remove
     * @return True if the style was removed, false otherwise
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

    /**
     * Extension of CitationStyle that represents an external style file.
     */
    private static class ExternalCitationStyle extends CitationStyle {

        public ExternalCitationStyle(String filename, String title, boolean isNumericStyle, String source) {
            super(filename, title, isNumericStyle, source);
        }

        @Override
        public boolean isInternalStyle() {
            return false;
        }
    }
}
