package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.testutils.JavaFxExtension;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// [utest->req~import.legacy-library-migration~1]
@NullMarked
@ExtendWith(JavaFxExtension.class)
class LibraryMigrationActionTest {

    private final CliPreferences preferences = mock(CliPreferences.class, RETURNS_DEEP_STUBS);
    private final DialogService dialogService = mock(DialogService.class);

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
    void declinedMigrationsAreNotOfferedAgain() {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(new BibEntry().withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]")));
        parserResult.getMetaData().setSkippedMigrations(List.of("markings"));

        assertEquals(List.of(), LibraryMigrationAction.getNecessaryMigrations(parserResult, preferences));
    }

    @Test
    void mandatoryMigrationIgnoresStoredSkip() {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(new BibEntry()));
        parserResult.getMetaData().setGroupsInLegacyFormat(true);
        parserResult.getMetaData().setSkippedMigrations(List.of("legacyGroups"));

        List<PostOpenMigration> offered = LibraryMigrationAction.getNecessaryMigrations(parserResult, preferences);

        assertEquals(List.of(ConvertLegacyExplicitGroups.class), offered.stream().map(PostOpenMigration::getClass).toList());
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

    @Test
    void migrateRunsSelectedAndRemembersDeselected() {
        BibEntry marked = new BibEntry().withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]");
        BibEntry prioritized = new BibEntry().withField(StandardField.KEYWORDS, "prio1");
        ParserResult parserResult = parserResultNeedingAllMigrations(marked, prioritized);
        answerDialog(true, 1);

        new LibraryMigrationAction().performAction(parserResult, dialogService, preferences);

        assertFalse(parserResult.getMetaData().isGroupsInLegacyFormat());
        assertTrue(marked.hasField(InternalField.MARKED_INTERNAL));
        assertEquals(Optional.empty(), prioritized.getField(StandardField.KEYWORDS));
        assertEquals(List.of("markings"), parserResult.getMetaData().getSkippedMigrations());
        assertTrue(parserResult.getChangedOnMigration());
    }

    @Test
    void keepAsIsRunsOnlyMandatoryMigrations() {
        BibEntry marked = new BibEntry().withField(InternalField.MARKED_INTERNAL, "[Nicolas:6]");
        BibEntry prioritized = new BibEntry().withField(StandardField.KEYWORDS, "prio1");
        ParserResult parserResult = parserResultNeedingAllMigrations(marked, prioritized);
        answerDialog(false, -1);

        new LibraryMigrationAction().performAction(parserResult, dialogService, preferences);

        assertFalse(parserResult.getMetaData().isGroupsInLegacyFormat());
        assertTrue(marked.hasField(InternalField.MARKED_INTERNAL));
        assertEquals(Optional.of("prio1"), prioritized.getField(StandardField.KEYWORDS));
        assertEquals(List.of("markings", "specialFieldsInKeywords"), parserResult.getMetaData().getSkippedMigrations());
        assertTrue(parserResult.getChangedOnMigration());
    }

    private ParserResult parserResultNeedingAllMigrations(BibEntry... entries) {
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult parserResult = new ParserResult(Set.of(entries));
        parserResult.getMetaData().setGroupsInLegacyFormat(true);
        return parserResult;
    }

    /// Simulates the user: deselects the check box at `deselectedIndex` (order: legacy groups, markings, special fields), then closes the dialog
    private void answerDialog(boolean migrate, int deselectedIndex) {
        when(dialogService.showCustomDialogAndWait(anyString(), any(DialogPane.class), any(ButtonType[].class))).thenAnswer(invocation -> {
            DialogPane pane = invocation.getArgument(1);
            List<CheckBox> checkBoxes = ((VBox) pane.getContent()).getChildren().stream()
                                                                  .filter(CheckBox.class::isInstance)
                                                                  .map(CheckBox.class::cast)
                                                                  .toList();
            assertEquals(3, checkBoxes.size());
            if (deselectedIndex >= 0) {
                checkBoxes.get(deselectedIndex).setSelected(false);
            }
            return migrate ? Optional.of(invocation.<ButtonType>getArgument(2)) : Optional.empty();
        });
    }
}
