package net.sf.jabref.openoffice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;

public class StyleLoader {

    private static final Log LOGGER = LogFactory.getLog(StyleLoader.class);

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    private final JournalAbbreviationRepository repository;
    private final OpenOfficePreferences preferences;

    private final List<String> internalStyleFiles = Arrays.asList(DEFAULT_AUTHORYEAR_STYLE_PATH,
            DEFAULT_NUMERICAL_STYLE_PATH);

    private final List<OOBibStyle> internalStyles = new ArrayList<>();
    private final List<OOBibStyle> externalStyles = new ArrayList<>();


    public StyleLoader(OpenOfficePreferences preferences, JournalAbbreviationRepository repository) {
        this.repository = repository;
        this.preferences = preferences;
    }

    /**
     * Read the style file. Record the last modified time of the file.
     * @throws Exception
     */
    public OOBibStyle readStyleFile(boolean useDefaultAuthoryearStyle, boolean useDefaultNumericalStyle,
            String styleFile) throws IOException {
        if (useDefaultAuthoryearStyle) {
            return new OOBibStyle(DEFAULT_AUTHORYEAR_STYLE_PATH, repository);
        } else if (useDefaultNumericalStyle) {
            return new OOBibStyle(DEFAULT_NUMERICAL_STYLE_PATH, repository);
        } else {
            return new OOBibStyle(new File(styleFile), repository,
                    Globals.prefs.getDefaultEncoding());
        }
    }

    public void update() {
        readInternalStyleFiles();
        readExternalStyleFiles();
    }

    public List<OOBibStyle> getStyles() {
        List<OOBibStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    public void addStyleFile(String filename) {
        try {
            OOBibStyle newStyle = new OOBibStyle(new File(filename), repository, Globals.prefs.getDefaultEncoding());
            if (externalStyles.contains(newStyle)) {
                LOGGER.info("External style file " + filename + " already existing.");
            } else {
                externalStyles.add(newStyle);
                storeExternalStyleFiles();
            }
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find external style file " + filename, e);
        } catch (IOException e) {
            LOGGER.info("Problem reading external style file " + filename, e);
        }

    }

    private void readExternalStyleFiles() {
        externalStyles.clear();
        // Read external lists
        List<String> lists = preferences.getExternalStyleFiles();
        for (String filename : lists) {
            try {
                externalStyles.add(new OOBibStyle(new File(filename), repository, Globals.prefs.getDefaultEncoding()));
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find external style file " + filename, e);
            } catch (IOException e) {
                LOGGER.info("Problem reading external style file " + filename, e);
            }
        }
    }

    private void readInternalStyleFiles() {
        internalStyles.clear();
        for (String filename : internalStyleFiles) {
            try {
                internalStyles.add(new OOBibStyle(filename, repository));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file " + filename, e);
            }
        }
    }

    private void storeExternalStyleFiles() {
        List<String> filenames = new ArrayList<>(externalStyles.size());
        for (OOBibStyle style : externalStyles) {
            filenames.add(style.getFile().getAbsolutePath());
        }
        preferences.setExternalStyleFiles(filenames);
    }
}
