package net.sf.jabref.openoffice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.logic.journals.JournalAbbreviationRepository;

public class StyleLoader {

    private static final Log LOGGER = LogFactory.getLog(StyleLoader.class);

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    private final List<String> internalStyleFiles = Arrays.asList(DEFAULT_AUTHORYEAR_STYLE_PATH,
            DEFAULT_NUMERICAL_STYLE_PATH);

    private final JournalAbbreviationRepository repository;
    private final OpenOfficePreferences preferences;
    private final Charset encoding;

    private final List<OOBibStyle> internalStyles = new ArrayList<>();
    private final List<OOBibStyle> externalStyles = new ArrayList<>();


    public StyleLoader(OpenOfficePreferences preferences, JournalAbbreviationRepository repository, Charset encoding) {
        this.repository = repository;
        this.preferences = preferences;
        this.encoding = encoding;
        update();
    }

    public void update() {
        loadInternalStyles();
        loadExternalStyles();
    }

    public List<OOBibStyle> getStyles() {
        List<OOBibStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    public void addStyle(String filename) {
        try {
            OOBibStyle newStyle = new OOBibStyle(new File(filename), repository, encoding);
            if (externalStyles.contains(newStyle)) {
                LOGGER.info("External style file " + filename + " already existing.");
            } else {
                externalStyles.add(newStyle);
                storeExternalStyles();
            }
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find external style file " + filename, e);
        } catch (IOException e) {
            LOGGER.info("Problem reading external style file " + filename, e);
        }

    }

    private void loadExternalStyles() {
        externalStyles.clear();
        // Read external lists
        List<String> lists = preferences.getExternalStyles();
        for (String filename : lists) {
            try {
                externalStyles.add(new OOBibStyle(new File(filename), repository, encoding));
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
                internalStyles.add(new OOBibStyle(filename, repository));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file " + filename, e);
            }
        }
    }

    private void storeExternalStyles() {
        List<String> filenames = new ArrayList<>(externalStyles.size());
        for (OOBibStyle style : externalStyles) {
            filenames.add(style.getFile().getPath());
        }
        preferences.setExternalStyles(filenames);
    }

    public boolean removeStyle(OOBibStyle style) {
        if (!style.isFromResource()) {
            boolean result = externalStyles.remove(style);
            storeExternalStyles();
            return result;
        }
        return false;
    }

    public OOBibStyle getUsedStyle() {
        String filename = preferences.getCurrentStyle();
        if (filename != null) {
            for (OOBibStyle style : getStyles()) {
                if (filename.equals(style.getPath())) {
                    return style;
                }
            }
        }
        // Pick the first internal
        preferences.setCurrentStyle(internalStyles.get(0).getPath());
        return internalStyles.get(0);
    }
}
