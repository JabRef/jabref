package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.DirectoryMonitor;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntryEditorTabFactoryTest {

    private EntryEditorTabFactory tabFactory;
    private EntryEditorPreferences entryEditorPreferences;
    private AiPreferences aiPreferences;

    @BeforeEach
    void setUp() {
        entryEditorPreferences = EntryEditorPreferences.getDefault();
        aiPreferences = AiPreferences.getDefault();

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
        when(preferences.getAiPreferences()).thenReturn(aiPreferences);
        when(preferences.getPreviewPreferences()).thenReturn(PreviewPreferences.getDefault());

        tabFactory = new EntryEditorTabFactory(
                mock(PreviewPanel.class),
                mock(UndoAction.class),
                mock(RedoAction.class),
                mock(BuildInfo.class),
                mock(DialogService.class),
                mock(TaskExecutor.class),
                preferences,
                mock(StateManager.class),
                mock(FileUpdateMonitor.class),
                mock(DirectoryMonitor.class),
                mock(CountingUndoManager.class),
                mock(BibEntryTypesManager.class),
                mock(JournalAbbreviationRepository.class),
                mock(KeyBindingRepository.class),
                mock(SearchCitationsRelationsService.class));
    }

    @ParameterizedTest
    @EnumSource(value = EntryEditorTabModel.BuiltIn.class, names = {"AI_SUMMARY", "AI_CHAT"})
    void aiTabHiddenWhenAiFeaturesCurrentlyDisabled(EntryEditorTabModel.BuiltIn aiTabType) {
        entryEditorPreferences.setTabVisible(aiTabType, true);
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        assertFalse(tabFactory.preferenceDrivenVisibilityFor(aiTabType, entryEditorPreferences).getValue());
    }

    @ParameterizedTest
    @EnumSource(value = EntryEditorTabModel.BuiltIn.class, names = {"AI_SUMMARY", "AI_CHAT"})
    void aiTabVisibleWhenAiFeaturesCurrentlyEnabledAndUserTabToggleOn(EntryEditorTabModel.BuiltIn aiTabType) {
        entryEditorPreferences.setTabVisible(aiTabType, true);
        aiPreferences.setAiFeaturesEnabledCurrently(true);

        assertTrue(tabFactory.preferenceDrivenVisibilityFor(aiTabType, entryEditorPreferences).getValue());
    }

    @ParameterizedTest
    @EnumSource(value = EntryEditorTabModel.BuiltIn.class, names = {"AI_SUMMARY", "AI_CHAT"})
    void aiTabHiddenWhenUserTabToggleOffEvenIfAiFeaturesCurrentlyEnabled(EntryEditorTabModel.BuiltIn aiTabType) {
        entryEditorPreferences.setTabVisible(aiTabType, false);
        aiPreferences.setAiFeaturesEnabledCurrently(true);

        assertFalse(tabFactory.preferenceDrivenVisibilityFor(aiTabType, entryEditorPreferences).getValue());
    }

    @Test
    void unrelatedTabIgnoresAiFeaturesCurrentlyDisabled() {
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        assertTrue(tabFactory.preferenceDrivenVisibilityFor(EntryEditorTabModel.BuiltIn.ALL_FIELDS, entryEditorPreferences).getValue());
    }
}
