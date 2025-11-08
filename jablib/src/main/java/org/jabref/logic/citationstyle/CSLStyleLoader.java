package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the loading of CitationStyles from both internal resources and external files.
 */
public record CSLStyleLoader(
        OpenOfficePreferences openOfficePreferences) {
    public static final String DEFAULT_STYLE = "ieee.csl";

    private static final String STYLES_ROOT = "/csl-styles";
    private static final String CATALOG_PATH = "/citation-style-catalog.json";
    private static final List<CitationStyle> INTERNAL_STYLES = new ArrayList<>();
    private static final List<CitationStyle> EXTERNAL_STYLES = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleLoader.class);

    public CSLStyleLoader(@NonNull OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;
        loadExternalStyles();
    }

    /**
     * Returns a list of all available citation styles (both internal and external).
     */
    public static List<CitationStyle> getStyles() {
        List<CitationStyle> result = new ArrayList<>(INTERNAL_STYLES);
        result.addAll(EXTERNAL_STYLES);
        return result;
    }

    /**
     * Returns the default citation style which is currently set to {@link CSLStyleLoader#DEFAULT_STYLE}.
     */
    public static CitationStyle getDefaultStyle() {
        return INTERNAL_STYLES.stream()
                              .filter(style -> DEFAULT_STYLE.equals(style.getFilePath()))
                              .findFirst()
                              .orElseGet(() -> CSLStyleUtils.createCitationStyleFromFile(DEFAULT_STYLE)
                                                            .orElse(new CitationStyle("", "Empty", "Empty", false, false, false, "", true)));
    }

    /**
     * Loads the internal (built-in) CSL styles from the catalog generated at build-time.
     */
    public static void loadInternalStyles() {
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
                int styleCount = styleInfoList.size();
                for (Map<String, Object> info : styleInfoList) {
                    @NonNull
                    String path = (String) info.get("path");
                    @NonNull
                    String title = (String) info.get("title");
                    @Nullable
                    String shortTitle = (String) info.get("shortTitle");
                    if (shortTitle == null) {
                        LOGGER.error("JabRef added support of shortTitle in August, 2025. Please execute './gradlew jablib:clean jablib:build' to update the citation style cache.");
                        shortTitle = title;
                    }
                    boolean isNumeric = (boolean) info.get("isNumeric");
                    boolean hasBibliography = (boolean) info.get("hasBibliography");
                    boolean usesHangingIndent = (boolean) info.get("usesHangingIndent");

                    // We use these metadata and just load the content instead of re-parsing for them
                    // These are located in the resources directly; therefore it is enough to use the class itself for loading
                    try (InputStream styleStream = CSLStyleLoader.class.getResourceAsStream(STYLES_ROOT + "/" + path)) {
                        if (styleStream != null) {
                            String source = new String(styleStream.readAllBytes());
                            CitationStyle style = new CitationStyle(path, title, shortTitle, isNumeric, hasBibliography, usesHangingIndent, source, true);
                            INTERNAL_STYLES.add(style);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error loading style file: {}", path, e);
                        styleCount--;
                    }
                }
                LOGGER.info("Loaded {} CSL styles", styleCount);
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
        EXTERNAL_STYLES.clear();

        List<String> stylePaths = openOfficePreferences.getExternalCslStyles();
        for (String stylePath : stylePaths) {
            Optional<CitationStyle> style = CSLStyleUtils.createCitationStyleFromFile(stylePath);
            style.ifPresent(EXTERNAL_STYLES::add);
        }
    }

    /**
     * Adds a new external CSL style if it's valid.
     *
     * @return Optional containing the added CitationStyle if valid, empty otherwise
     */
    public Optional<CitationStyle> addStyleIfValid(@NonNull String stylePath) {
        Optional<CitationStyle> newStyleOptional = CSLStyleUtils.createCitationStyleFromFile(stylePath);
        if (newStyleOptional.isPresent()) {
            CitationStyle newStyle = newStyleOptional.get();

            EXTERNAL_STYLES.add(newStyle);
            storeExternalStyles();
            return newStyleOptional;
        }

        return Optional.empty();
    }

    /**
     * Stores the current list of external styles to preferences.
     */
    private void storeExternalStyles() {
        List<String> stylePaths = EXTERNAL_STYLES.stream()
                                                 .map(CitationStyle::getPath)
                                                 .toList();
        openOfficePreferences.setExternalCslStyles(stylePaths);
    }

    /**
     * Removes a style from the external styles list.
     *
     * @return true if the style was removed, false otherwise
     */
    public boolean removeStyle(@NonNull CitationStyle style) {
        if (!style.isInternalStyle()) {
            boolean result = EXTERNAL_STYLES.remove(style);
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
