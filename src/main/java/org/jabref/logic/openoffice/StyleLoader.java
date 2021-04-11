package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.layout.LayoutFormatterPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StyleLoader {

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH =
        "/resource/openoffice/default_authoryear.jstyle";

    public static final String DEFAULT_NUMERICAL_STYLE_PATH =
        "/resource/openoffice/default_numerical.jstyle";

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

    public StyleLoader(OpenOfficePreferences openOfficePreferences,
                       LayoutFormatterPreferences formatterPreferences,
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
     * @return parse log. result.hasError() is false if the style is added, true otherwise.
     *         // was: True if the added style is valid, false otherwise
     */
    public OOBibStyleParser.ParseLog addStyleIfValid(String filename) {
        Objects.requireNonNull(filename);
        try {
            OOBibStyle newStyle = new OOBibStyle(new File(filename),
                                                 layoutFormatterPreferences,
                                                 encoding);

            OOBibStyleParser.ParseLog parseLog = newStyle.getParseLog();
            if ( parseLog == null ) {
                parseLog = new OOBibStyleParser.ParseLog();
                parseLog.error(filename, 0,
                               "OOBibStyle constructor returned with no parseLog");
            }

            if (externalStyles.contains(newStyle)) {
                LOGGER.info("External style file " + filename + " already existing.");
                parseLog.error(filename, 0,
                               "An external style file with the same content,"
                               + " including its path"
                               + " is already known (not adding)" );
                return parseLog;
            } else if (newStyle.isValid() && !parseLog.hasError()) {
                externalStyles.add(newStyle);
                storeExternalStyles();
                return parseLog;
            } else {
                String msg = String.format("Style with filename %s is invalid", filename);
                LOGGER.error(msg);
                parseLog.error(filename, 0, msg);
                return parseLog;
            }
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            String msg = "Cannot find external style file " + filename;
            LOGGER.info(msg, e);
            OOBibStyleParser.ParseLog parseLog = new  OOBibStyleParser.ParseLog();
            parseLog.error( filename, 0, msg );
            return parseLog;
        } catch (IOException e) {
            LOGGER.info("Problem reading external style file " + filename, e);
            OOBibStyleParser.ParseLog parseLog = new  OOBibStyleParser.ParseLog();
            String msg = "Problem (IOException) reading external style file " + filename;
            parseLog.error( filename, 0, msg );
            return parseLog;
        }
    }

    private void loadExternalStyles() {
        externalStyles.clear();
        // Read external lists
        List<String> lists = openOfficePreferences.getExternalStyles();
        for (String filename : lists) {
            try {
                OOBibStyle style = new OOBibStyle(new File(filename),
                                                  layoutFormatterPreferences,
                                                  encoding);
                if (style.isValid()) { // Problem!
                    externalStyles.add(style);
                } else {
                    LOGGER.error(String.format("Style with filename %s is invalid", filename));
                }
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find external style file " + filename, e);
            } catch (IOException e) {
                LOGGER.info("Problem reading external style file " + filename, e);
            }
        }
    }

    private void loadInternalStyles() {
        internalStyles.clear();
        for (String filename : internalStyleFiles) {
            try {
                internalStyles.add(new OOBibStyle(filename, layoutFormatterPreferences));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file " + filename, e);
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
