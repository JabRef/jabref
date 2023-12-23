package org.jabref.gui.importer;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewEntryActionTest {

    private NewEntryAction newEntryAction;
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
    private final DialogService dialogService = spy(DialogService.class);
    private final PreferencesService preferencesService = mock(PreferencesService.class);
    private final StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    public void setUp() {
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        newEntryAction = new NewEntryAction(() -> libraryTab, dialogService, preferencesService, stateManager);
    }

    @Test
    public void testExecuteOnSuccessWithFixedType() {
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(() -> libraryTab, type, dialogService, preferencesService, stateManager);
        when(tabContainer.getLibraryTabs()).thenReturn(List.of(libraryTab));

        newEntryAction.execute();
        verify(libraryTab, times(1)).insertEntry(new BibEntry(type));
    }
}
