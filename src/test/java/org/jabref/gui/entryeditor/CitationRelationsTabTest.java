package org.jabref.gui.entryeditor;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CitationRelationsTabTest {

    private CitationRelationsTab tab;
    private BibEntry bibEntry;
    private BibEntry relatedEntry;

    private EntryEditorPreferences preferencesMock;
    private UndoManager undoMock;
    private BibDatabaseContext databaseContext;
    private DialogService diagService;
    private StateManager stateManager;
    private FileUpdateMonitor fileUpdateMonitor;
    private PreferencesService preferencesService;
    private LibraryTab libraryTab;

    @BeforeEach
    void setup() {
        databaseContext = new BibDatabaseContext();
        preferencesMock = mock(EntryEditorPreferences.class);
        diagService = mock(DialogService.class);
        undoMock = mock(UndoManager.class);
        stateManager = mock(StateManager.class);
        fileUpdateMonitor = mock(FileUpdateMonitor.class);
        preferencesService = mock(PreferencesService.class);
        libraryTab = mock(LibraryTab.class);

        bibEntry = new BibEntry();
        bibEntry.setField(StandardField.DOI, "12.3456/1");
        relatedEntry = new BibEntry();
        databaseContext.getDatabase().insertEntries(bibEntry, relatedEntry);

        tab = new CitationRelationsTab(preferencesMock,diagService,databaseContext,undoMock,stateManager,fileUpdateMonitor,preferencesService,libraryTab);
    }

    @Test
    void doiExistsTest() {
        assertTrue(tab.doiExists("12.3456/1"));
        assertFalse(tab.doiExists("34.5678/1"));
    }

    @Test
    void getEntryByDoiTest() {
        assertEquals(tab.getEntryByDOI("12.3456/1"), bibEntry);
        assertNull(tab.getEntryByDOI("76.6679/20"));
    }

    @Test
    void filterNonExistingTest() {

    }
}
