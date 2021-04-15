package org.jabref.gui.importer;

import org.jabref.gui.*;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class NewEntryActionTest {

    private NewEntryAction newEntryAction;
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private DialogService dialogService = spy(DialogService.class);
    private PreferencesService preferencesService = mock(PreferencesService.class);
    private StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    public void setUp() {
        when(jabRefFrame.getCurrentLibraryTab()).thenReturn(libraryTab);
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        newEntryAction = new NewEntryAction(jabRefFrame, dialogService, preferencesService, stateManager);
    }

    @Test
    public void testExecuteIfNoBasePanel() {
        when(jabRefFrame.getBasePanelCount()).thenReturn(0);

        newEntryAction.execute();
        verify(libraryTab, times(0)).insertEntry(any(BibEntry.class));
        verify(dialogService, times(0)).showCustomDialogAndWait(any(EntryTypeView.class));
    }

    @Test
    public void testExecuteWithNoSelectedTypeFromDialog() {
        when(jabRefFrame.getBasePanelCount()).thenReturn(1);
        when(dialogService.showCustomDialogAndWait(any(EntryTypeView.class))).thenReturn(null);

        newEntryAction.execute();
        verify(dialogService, times(1)).showCustomDialogAndWait(any(EntryTypeView.class));
        verify(libraryTab, times(0)).insertEntry(any(BibEntry.class));
    }

    @Test
    public void testExecuteOnSuccessWithSelectedTypeFromDialog() {
        when(jabRefFrame.getBasePanelCount()).thenReturn(1);
        when(dialogService.showCustomDialogAndWait(any(EntryTypeView.class))).thenReturn(java.util.Optional.of(StandardEntryType.Article));

        newEntryAction.execute();
        verify(dialogService, times(1)).showCustomDialogAndWait(any(EntryTypeView.class));
        verify(libraryTab, times(1)).insertEntry(any(BibEntry.class));
    }

    @Test
    public void testExecuteOnSuccessWithFixedType() {
        newEntryAction = new NewEntryAction(jabRefFrame, StandardEntryType.Article, dialogService, preferencesService, stateManager);
        when(jabRefFrame.getBasePanelCount()).thenReturn(1);

        newEntryAction.execute();
        verify(dialogService, times(0)).showCustomDialogAndWait(any(EntryTypeView.class));
        verify(libraryTab, times(1)).insertEntry(any(BibEntry.class));
    }
}
