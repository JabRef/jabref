package org.jabref.gui.libraryproperties.general;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralPropertiesViewModelTest {

    @Test
    void leavesKeywordSeparatorUnsetWhenLibraryUsesGlobalFallback() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        GeneralPropertiesViewModel viewModel = new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, mock(UndoManager.class));

        viewModel.setValues();
        viewModel.storeSettings(databaseContext.getMetaData());

        assertEquals("", viewModel.keywordSeparatorProperty().get());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getKeywordSeparator());
    }

    /// The separator and the group definitions the migration rewrites are metadata, which the
    /// dialog records as a snapshot; the entries it rewrites are not, so they are recorded here.
    @Test
    void changingTheSeparatorMigratesTheEntriesAndRecordsOnlyThose() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "topic, subtopic");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        root.addSubgroup(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ',', true));
        databaseContext.getMetaData().setGroups(root);
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        JabRefUndoManager undoManager = new JabRefUndoManager();
        GeneralPropertiesViewModel viewModel = new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, undoManager);

        viewModel.setValues();
        viewModel.keywordSeparatorProperty().set(";");
        viewModel.storeSettings(databaseContext.getMetaData());

        assertEquals(Optional.of(';'), databaseContext.getMetaData().getKeywordSeparator());
        assertEquals(Optional.of("topic; subtopic"), entry.getField(StandardField.KEYWORDS));
        assertEquals(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ';', true),
                root.getChildren().getFirst().getGroup());

        undoManager.undo();

        assertEquals(Optional.of("topic, subtopic"), entry.getField(StandardField.KEYWORDS));
        assertFalse(undoManager.canUndo(), "the group and the separator are the metadata snapshot's business");
    }
}
