package net.sf.jabref.logic.importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.AutoSaveUtil;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OpenDatabase {

    public static final Log LOGGER = LogFactory.getLog(OpenDatabase.class);


    /**
     * Load database (bib-file) or, if there exists, a newer autosave version, unless the flag is set to ignore the autosave
     *
     * @param name Name of the BIB-file to open
     * @param ignoreAutosave true if autosave version of the file should be ignored
     * @return ParserResult which never is null
     */

    public static ParserResult loadDatabaseOrAutoSave(String name, boolean ignoreAutosave, Charset defaultEncoding) {
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
                boolean autoSaveFound = AutoSaveUtil.newerAutoSaveExists(file);
                if (autoSaveFound) {
                    // We have found a newer autosave. Make a note of this, so it can be
                    // handled after startup:
                    ParserResult postp = new ParserResult(null, null, null);
                    postp.setPostponedAutosaveFound(true);
                    postp.setFile(file);
                    return postp;
                }
            }

            if (!FileBasedLock.waitForFileLock(file.toPath(), 10)) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return ParserResult.getNullResult();
            }

            ParserResult pr = OpenDatabase.loadDatabase(file, defaultEncoding);
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

    /**
     * Opens a new database.
     */
    public static ParserResult loadDatabase(File fileToOpen, Charset defaultEncoding) throws IOException {
        // Open and parse file
        ParserResult result = new BibtexImporter(ImportFormatPreferences.fromPreferences(Globals.prefs))
                .importDatabase(fileToOpen.toPath(), defaultEncoding);

        if (SpecialFieldsUtils.keywordSyncEnabled()) {
            for (BibEntry entry : result.getDatabase().getEntries()) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, null);
            }
            LOGGER.debug("Synchronized special fields based on keywords");
        }

        return result;
    }

}
