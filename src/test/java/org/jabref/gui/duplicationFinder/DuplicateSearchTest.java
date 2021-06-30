package org.jabref.gui.duplicationFinder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class DuplicateSearchTest {

    private final DialogService dialogService = spy(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final CountingUndoManager undoManager = mock(CountingUndoManager.class);

    private DuplicateSearch duplicateSearch;
    private BibEntry entry1;

    @BeforeEach
    void setupDuplicateSearchInstance() {
        entry1 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, "A tale from the trenches")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development")
                .withCitationKey("Chattopadhyay2020");

        when(jabRefFrame.getCurrentLibraryTab()).thenReturn(libraryTab);
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        duplicateSearch = new DuplicateSearch(jabRefFrame, dialogService, stateManager);
    }

    @Test
    public void executeWithNoEntries() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));
        when(bibDatabaseContext.getEntries()).thenReturn(Collections.emptyList());

        duplicateSearch.execute();
        verify(dialogService, times(1)).notify(Localization.lang("Searching for duplicates..."));
    }

    @Test
    public void executeWithOneEntry() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));
        when(bibDatabaseContext.getEntries()).thenReturn(Collections.singletonList(entry1));

        duplicateSearch.execute();
        verify(dialogService, times(1)).notify(Localization.lang("Searching for duplicates..."));
    }

    @Test
    public void executeWithNoDuplicates() {
        BibEntry entry2 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Tale S Sastad and Karl Thomas Hjelmervik")
                .withField(StandardField.TITLE, "Synthesizing Realistic, High-Resolution Anti-Submarine Sonar Data\n")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.1109/OCEANSKOBE.2018.8558837")
                .withCitationKey("Sastad2018");

        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));
        when(bibDatabaseContext.getEntries()).thenReturn(Arrays.asList(entry1, entry2));
        when(bibDatabaseContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(libraryTab.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        when(libraryTab.getDatabase()).thenReturn(mock(BibDatabase.class));
        when(libraryTab.getUndoManager()).thenReturn(undoManager);
        when(undoManager.addEdit(mock(NamedCompound.class))).thenReturn(true);

        duplicateSearch.execute();
        verify(dialogService, times(1)).notify(Localization.lang("Searching for duplicates..."));
        verify(dialogService, times(1)).notify(Localization.lang("Duplicates found") + ": " + String.valueOf(0) + ' '
                + Localization.lang("pairs processed") + ": " + String.valueOf(0));
    }
}
