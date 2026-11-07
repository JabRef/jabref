package org.jabref.gui.importer.actions;

import java.util.List;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddGroupImportEntriesActionTest {

    private final AddGroupImportEntriesAction action = new AddGroupImportEntriesAction();
    private final BibDatabaseContext databaseContext = new BibDatabaseContext();

    private CliPreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getLibraryPreferences().shouldAddImportedEntries()).thenReturn(true);
        when(preferences.getLibraryPreferences().getAddImportedEntriesGroupName()).thenReturn("Imported entries");
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    private List<String> groupNames() {
        return databaseContext.getMetaData().getGroups()
                              .map(root -> root.getChildren().stream().map(GroupTreeNode::getName).toList())
                              .orElse(List.of());
    }

    /// A library without the group gets it, as the first child of the root.
    @Test
    void theGroupIsCreatedWhenItIsMissing() {
        assertTrue(action.addImportedEntriesGroupIfNeeded(databaseContext, preferences));

        assertEquals(List.of("Imported entries"), groupNames());
    }

    /// A library that already has the group is left alone, so that importing twice does not build a
    /// second one.
    @Test
    void aLibraryThatAlreadyHasTheGroupIsLeftAlone() {
        action.addImportedEntriesGroupIfNeeded(databaseContext, preferences);

        assertFalse(action.addImportedEntriesGroupIfNeeded(databaseContext, preferences));

        assertEquals(List.of("Imported entries"), groupNames());
    }

    /// Nothing is written when the preference is off.
    @Test
    void nothingIsWrittenWhenTheFeatureIsOff() {
        when(preferences.getLibraryPreferences().shouldAddImportedEntries()).thenReturn(false);

        assertFalse(action.addImportedEntriesGroupIfNeeded(databaseContext, preferences));

        assertEquals(List.of(), groupNames());
    }
}
