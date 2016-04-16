package net.sf.jabref.importer;

import java.io.File;
import java.nio.charset.Charset;

import net.sf.jabref.Globals;
import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileBasedLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AutosaveAwareDatabaseLoader {

    private static final Log LOGGER = LogFactory.getLog(AutosaveAwareDatabaseLoader.class);


    /**
     *
     * @param name Name of the bib-file to open
     * @param ignoreAutosave true if autosave version of the file should be ignored
     * @return ParserResult which never is null
     */

    public static ParserResult openBibFile(String name, boolean ignoreAutosave) {
        // String in OpenDatabaseAction.java
        LOGGER.info("Opening: " + name);
        File file = new File(name);
        if (!file.exists()) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            LOGGER.error(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;

        }
        try {

            if (!ignoreAutosave) {
                boolean autoSaveFound = AutoSaveManager.newerAutoSaveExists(file);
                if (autoSaveFound) {
                    // We have found a newer autosave. Make a note of this, so it can be
                    // handled after startup:
                    ParserResult postp = new ParserResult(null, null, null);
                    postp.setPostponedAutosaveFound(true);
                    postp.setFile(file);
                    return postp;
                }
            }

            if (!FileBasedLock.waitForFileLock(file, 10)) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return ParserResult.NULL_RESULT;
            }

            Charset encoding = Globals.prefs.getDefaultEncoding();
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            pr.setFile(file);
            if (pr.hasWarnings()) {
                for (String aWarn : pr.warnings()) {
                    LOGGER.warn(aWarn);
                }
            }
            return pr;
        } catch (Throwable ex) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            pr.setErrorMessage(ex.getMessage());
            LOGGER.info("Problem opening .bib-file", ex);
            return pr;
        }

    }

}
