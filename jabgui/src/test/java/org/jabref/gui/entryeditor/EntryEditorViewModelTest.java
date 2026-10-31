package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class EntryEditorViewModelTest {

    private final EntryEditorPreferences entryEditorPreferences = EntryEditorPreferences.getDefault();
    private final EntryEditorTabFactory tabFactory = mock(EntryEditorTabFactory.class);

    @Test
    void savedCustomTabRebuildsTabsWithoutRestart() {
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
        // The view model subscribes to the configured tab models in its constructor.
        new EntryEditorViewModel(
                new JabRefGuiStateManager(),
                preferences,
                mock(TaskExecutor.class),
                mock(DialogService.class),
                mock(JournalAbbreviationRepository.class),
                () -> mock(LibraryTab.class),
                tabFactory);
        List<EntryEditorTabModel> tabModels = new ArrayList<>(entryEditorPreferences.getTabModels());
        tabModels.add(new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("HALid")));

        entryEditorPreferences.getTabModels().setAll(tabModels);

        verify(tabFactory).createTabs();
    }
}
