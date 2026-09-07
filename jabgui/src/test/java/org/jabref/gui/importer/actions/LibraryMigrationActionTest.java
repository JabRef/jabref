package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.Set;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// [utest->req~import.legacy-library-migration~1]
class LibraryMigrationActionTest {

    private final CliPreferences preferences = mock(CliPreferences.class, RETURNS_DEEP_STUBS);

    @Test
    void currentFormatNeedsNoMigration() {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(new BibEntry().withField(StandardField.KEYWORDS, "foo, bar")));

        assertEquals(List.of(), LibraryMigrationAction.getNecessaryMigrations(parserResult, preferences));
    }

    @Test
    void onlyMigrationsWithSomethingToDoAreOffered() {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(
                new BibEntry().withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]"),
                new BibEntry().withField(StandardField.KEYWORDS, "foo, prio1")));

        List<Class<? extends PostOpenMigration>> offered = LibraryMigrationAction.getNecessaryMigrations(parserResult, preferences)
                                                                                 .stream()
                                                                                 .<Class<? extends PostOpenMigration>>map(PostOpenMigration::getClass)
                                                                                 .toList();

        assertEquals(List.of(ConvertMarkingToGroups.class, SpecialFieldsToSeparateFields.class), offered);
    }

    @Test
    void legacyGroupTreeIsDetectedFromMetaData() {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(new BibEntry()));
        parserResult.getMetaData().setGroupsInLegacyFormat(true);

        List<PostOpenMigration> offered = LibraryMigrationAction.getNecessaryMigrations(parserResult, preferences);

        assertEquals(1, offered.size());
        assertEquals(ConvertLegacyExplicitGroups.class, offered.getFirst().getClass());
    }
}
