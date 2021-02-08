package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;
import org.jabref.migrations.TimeStampToDateAddAndModify;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabase.class);

    private OpenDatabase() {
    }

    /**
     * Load database (bib-file)
     *
     * @param name Name of the BIB-file to open
     * @return ParserResult which never is null
     * @deprecated use {@link #loadDatabase(Path, ImportFormatPreferences, TimestampPreferences, FileUpdateMonitor)} instead
     */
    @Deprecated
    public static ParserResult loadDatabase(String name, ImportFormatPreferences importFormatPreferences, TimestampPreferences timestampPreferences, FileUpdateMonitor fileMonitor) {
        LOGGER.debug("Opening: " + name);
        Path file = Path.of(name);

        if (!Files.exists(file)) {
            ParserResult pr = ParserResult.fromErrorMessage(Localization.lang("File not found"));
            pr.setFile(file.toFile());

            LOGGER.error(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;
        }

        try {
            return OpenDatabase.loadDatabase(file, importFormatPreferences, timestampPreferences, fileMonitor);
        } catch (IOException ex) {
            ParserResult pr = ParserResult.fromError(ex);
            pr.setFile(file.toFile());
            LOGGER.error("Problem opening .bib-file", ex);
            return pr;
        }
    }

    /**
     * Opens a new database.
     */
    public static ParserResult loadDatabase(Path fileToOpen, ImportFormatPreferences importFormatPreferences, TimestampPreferences timestampPreferences, FileUpdateMonitor fileMonitor)
            throws IOException {
        ParserResult result = new BibtexImporter(importFormatPreferences, fileMonitor).importDatabase(fileToOpen,
                importFormatPreferences.getEncoding());

        performLoadDatabaseMigrations(result, timestampPreferences, importFormatPreferences.getKeywordSeparator());

        return result;
    }

    private static void performLoadDatabaseMigrations(ParserResult parserResult, TimestampPreferences timestampPreferences, Character keywordDelimited) {
        List<PostOpenMigration> postOpenMigrations = Arrays.asList(
                new ConvertLegacyExplicitGroups(),
                new ConvertMarkingToGroups(),
                new TimeStampToDateAddAndModify(timestampPreferences),
                new SpecialFieldsToSeparateFields(keywordDelimited)
        );

        for (PostOpenMigration migration : postOpenMigrations) {
            migration.performMigration(parserResult);
        }
    }
}
