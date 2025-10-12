package org.jabref.logic.openoffice.style;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JStyleLoader {

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    private static final Logger LOGGER = LoggerFactory.getLogger(JStyleLoader.class);

    // All internal styles
    private final List<String> internalStyleFiles = List.of(DEFAULT_AUTHORYEAR_STYLE_PATH, DEFAULT_NUMERICAL_STYLE_PATH);

    private final OpenOfficePreferences openOfficePreferences;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    // Lists of the internal
    // and external styles
    private final List<JStyle> internalStyles = new ArrayList<>();
    private final List<JStyle> externalStyles = new ArrayList<>();

    public JStyleLoader(@NonNull OpenOfficePreferences openOfficePreferences,
                        @NonNull LayoutFormatterPreferences formatterPreferences,
                        @NonNull JournalAbbreviationRepository abbreviationRepository) {
        this.openOfficePreferences = openOfficePreferences;
        this.layoutFormatterPreferences = formatterPreferences;
        this.abbreviationRepository = abbreviationRepository;
        loadInternalStyles();
        loadExternalStyles();
    }

    public List<JStyle> getStyles() {
        List<JStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    /**
     * Adds the given style to the list of styles
     *
     * @param filename The filename of the style
     * @return True if the added style is valid, false otherwise
     */
    public boolean addStyleIfValid(@NonNull Path filename) {
        try {
            JStyle newStyle = new JStyle(filename, layoutFormatterPreferences, abbreviationRepository);
            if (externalStyles.contains(newStyle)) {
                LOGGER.info("External style file {} already existing.", filename);
            } else if (newStyle.isValid()) {
                externalStyles.add(newStyle);
                storeExternalStyles();
                return true;
            } else {
                LOGGER.error("Style with filename {} is invalid", filename);
            }
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find external style file {}", filename, e);
        } catch (IOException e) {
            LOGGER.info("Problem reading external style file {}", filename, e);
        }
        return false;
    }

    private void loadExternalStyles() {
        externalStyles.clear();
        // Read external lists
        List<String> lists = openOfficePreferences.getExternalJStyles();
        for (String filename : lists) {
            try {
                JStyle style = new JStyle(Path.of(filename), layoutFormatterPreferences, abbreviationRepository);
                if (style.isValid()) { // Problem!
                    externalStyles.add(style);
                } else {
                    LOGGER.error("Style with filename {} is invalid", filename);
                }
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find external style file {}", filename);
            } catch (IOException e) {
                LOGGER.info("Problem reading external style file {}", filename, e);
            }
        }
    }

    private void loadInternalStyles() {
        internalStyles.clear();
        for (String filename : internalStyleFiles) {
            try {
                internalStyles.add(new JStyle(filename, layoutFormatterPreferences, abbreviationRepository));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file {}", filename, e);
            }
        }
    }

    private void storeExternalStyles() {
        List<String> filenames = new ArrayList<>(externalStyles.size());
        for (JStyle style : externalStyles) {
            filenames.add(style.getPath());
        }
        openOfficePreferences.setExternalJStyles(filenames);
    }

    public boolean removeStyle(@NonNull JStyle style) {
        if (!style.isInternalStyle()) {
            boolean result = externalStyles.remove(style);
            storeExternalStyles();
            return result;
        }
        return false;
    }

    public JStyle getUsedJstyle() {
        String filename = openOfficePreferences.getCurrentJStyle();
        if (filename != null) {
            for (JStyle style : getStyles()) {
                if (filename.equals(style.getPath())) {
                    return style;
                }
            }
        }
        // Pick the first internal
        openOfficePreferences.setCurrentJStyle(internalStyles.getFirst().getPath());
        return internalStyles.getFirst();
    }
}
