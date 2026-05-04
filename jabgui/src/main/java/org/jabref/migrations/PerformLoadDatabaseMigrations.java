package org.jabref.migrations;

import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.ParserResult;

public class PerformLoadDatabaseMigrations {

    // FIXME: Should be called somewhere
    // Typical call: performLoadDatabaseMigrations(result, importFormatPreferences.bibEntryPreferences().getKeywordSeparator());
    public static void performLoadDatabaseMigrations(ParserResult parserResult,
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
}
