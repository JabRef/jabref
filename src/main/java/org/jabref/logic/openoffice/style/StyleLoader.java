package org.jabref.logic.openoffice.style;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StyleLoader {

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleLoader.class);

    // All internal styles
    private final List<String> internalStyleFiles = Arrays.asList(DEFAULT_AUTHORYEAR_STYLE_PATH,
            DEFAULT_NUMERICAL_STYLE_PATH);

    private final OpenOfficePreferences openOfficePreferences;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final Charset encoding;

    // Lists of the internal
    // and external styles
    private final List<OOBibStyle> internalStyles = new ArrayList<>();
    private final List<OOBibStyle> externalStyles = new ArrayList<>();

    public StyleLoader(OpenOfficePreferences openOfficePreferences, LayoutFormatterPreferences formatterPreferences,
                       Charset encoding) {
        this.openOfficePreferences = Objects.requireNonNull(openOfficePreferences);
        this.layoutFormatterPreferences = Objects.requireNonNull(formatterPreferences);
        this.encoding = Objects.requireNonNull(encoding);
        loadInternalStyles();
        loadExternalStyles();
    }

    public List<OOBibStyle> getStyles() {
        List<OOBibStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    /**
     * Adds the given style to the list of styles
     *
     * @param filename The filename of the style
     * @return True if the added style is valid, false otherwise
     */
    public boolean addStyleIfValid(String filename) {
        Objects.requireNonNull(filename);
        try {
            OOBibStyle newStyle = new OOBibStyle(new File(filename), layoutFormatterPreferences, encoding);
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
        List<String> lists = openOfficePreferences.getExternalStyles();
        for (String filename : lists) {
            try {
                OOBibStyle style = new OOBibStyle(new File(filename), layoutFormatterPreferences, encoding);
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
                internalStyles.add(new OOBibStyle(filename, layoutFormatterPreferences));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file {}", filename, e);
            }
        }
    }

    private void storeExternalStyles() {
        List<String> filenames = new ArrayList<>(externalStyles.size());
        for (OOBibStyle style : externalStyles) {
            filenames.add(style.getPath());
        }
        openOfficePreferences.setExternalStyles(filenames);
    }

    public boolean removeStyle(OOBibStyle style) {
        Objects.requireNonNull(style);
        if (!style.isInternalStyle()) {
            boolean result = externalStyles.remove(style);
            storeExternalStyles();
            return result;
        }
        return false;
    }

    public OOBibStyle getUsedStyle() {
        String filename = openOfficePreferences.getCurrentStyle();
        if (filename != null) {
            for (OOBibStyle style : getStyles()) {
                if (filename.equals(style.getPath())) {
                    return style;
                }
            }
        }
        // Pick the first internal
        openOfficePreferences.setCurrentStyle(internalStyles.get(0).getPath());
        return internalStyles.get(0);
    }
}
