package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class OpenDatabase {

    private OpenDatabase() {
    }

    /**
     * Load database (bib-file)
     *
     * @param fileToOpen Name of the BIB-file to open
     * @return ParserResult which never is null
     */
    public static ParserResult loadDatabase(Path fileToOpen, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor)
            throws IOException {
        ParserResult result = new BibtexImporter(importFormatPreferences, fileMonitor).importDatabase(fileToOpen);

        performLoadDatabaseMigrations(result, importFormatPreferences.bibEntryPreferences().getKeywordSeparator());

        return result;
    }

    private static void performLoadDatabaseMigrations(ParserResult parserResult,
                                                      Character keywordDelimited) {
        List<PostOpenMigration> postOpenMigrations = Arrays.asList(
                new ConvertLegacyExplicitGroups(),
                new ConvertMarkingToGroups(),
                new SpecialFieldsToSeparateFields(keywordDelimited)
        );

        for (PostOpenMigration migration : postOpenMigrations) {
            migration.performMigration(parserResult);
        }
    }

    public static void openSharedDatabase(ParserResult parserResult,
                                          JabRefFrame frame,
                                          DialogService dialogService,
                                          PreferencesService preferencesService,
                                          StateManager stateManager,
                                          BibEntryTypesManager entryTypesManager,
                                          FileUpdateMonitor fileUpdateMonitor,
                                          UndoManager undoManager)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException, NotASharedDatabaseException {
        try {
            new SharedDatabaseUIManager(
                    frame,
                    dialogService,
                    preferencesService,
                    stateManager,
                    entryTypesManager,
                    fileUpdateMonitor,
                    undoManager)
                    .openSharedDatabaseFromParserResult(parserResult);
        } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                NotASharedDatabaseException e) {
            parserResult.getDatabaseContext().clearDatabasePath(); // do not open the original file
            parserResult.getDatabase().clearSharedDatabaseID();

            throw e;
        }
    }
}
