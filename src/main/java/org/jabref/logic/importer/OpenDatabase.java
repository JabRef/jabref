package org.jabref.logic.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDatabase {

    public static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabase.class);

    private OpenDatabase() {
    }

    /**
     * Load database (bib-file)
     *
     * @param name Name of the BIB-file to open
     * @return ParserResult which never is null
     */
    public static ParserResult loadDatabase(String name, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        File file = new File(name);
        LOGGER.info("Opening: " + name);

        if (!file.exists()) {
            ParserResult pr = ParserResult.fromErrorMessage(Localization.lang("File not found"));
            pr.setFile(file);

            LOGGER.error(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;
        }

        try {
            if (!FileBasedLock.waitForFileLock(file.toPath())) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return new ParserResult();
            }

            ParserResult pr = OpenDatabase.loadDatabase(file, importFormatPreferences, fileMonitor);
            pr.setFile(file);
            if (pr.hasWarnings()) {
                for (String aWarn : pr.warnings()) {
                    LOGGER.warn(aWarn);
                }
            }
            return pr;
        } catch (IOException ex) {
            ParserResult pr = ParserResult.fromError(ex);
            pr.setFile(file);
            LOGGER.error("Problem opening .bib-file", ex);
            return pr;
        }
    }

    /**
     * Opens a new database.
     */
    public static ParserResult loadDatabase(File fileToOpen, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor)
            throws IOException {
        ParserResult result = new BibtexImporter(importFormatPreferences, fileMonitor).importDatabase(fileToOpen.toPath(),
                importFormatPreferences.getEncoding());

        if (importFormatPreferences.isKeywordSyncEnabled()) {
            for (BibEntry entry : result.getDatabase().getEntries()) {
                SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, importFormatPreferences.getKeywordSeparator());
            }
            LOGGER.debug("Synchronized special fields based on keywords");
        }

        performLoadDatabaseMigrations(result);

        return result;
    }

    private static void performLoadDatabaseMigrations(ParserResult parserResult) {
        List<PostOpenMigration> postOpenMigrations = Collections.singletonList(new ConvertLegacyExplicitGroups());

        for (PostOpenMigration migration : postOpenMigrations) {
            migration.performMigration(parserResult);
        }
    }
}
