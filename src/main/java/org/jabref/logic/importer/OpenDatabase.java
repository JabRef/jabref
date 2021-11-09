package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabase.class);

    private OpenDatabase() {
    }

    /**
     * Load database (bib-file)
     *
     * @param fileToOpen Name of the BIB-file to open
     * @return ParserResult which never is null
     */
    public static ParserResult loadDatabase(Path fileToOpen, GeneralPreferences generalPreferences, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor)
            throws IOException {
        ParserResult result = new BibtexImporter(importFormatPreferences, fileMonitor).importDatabase(fileToOpen,
                generalPreferences.getDefaultEncoding());

        performLoadDatabaseMigrations(result, importFormatPreferences.getKeywordSeparator());

        return result;
    }

    private static void performLoadDatabaseMigrations(ParserResult parserResult, Character keywordDelimited) {
        List<PostOpenMigration> postOpenMigrations = Arrays.asList(
                new ConvertLegacyExplicitGroups(),
                new ConvertMarkingToGroups(),
                new SpecialFieldsToSeparateFields(keywordDelimited)
        );

        for (PostOpenMigration migration : postOpenMigrations) {
            migration.performMigration(parserResult);
        }
    }
}
