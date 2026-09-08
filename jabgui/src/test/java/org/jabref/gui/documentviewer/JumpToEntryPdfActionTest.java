package org.jabref.gui.documentviewer;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// [utest->feat~ai.chat.jump-to-entry-pdf~1]
@NullMarked
class JumpToEntryPdfActionTest {

    private StateManager stateManager;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        stateManager = mock(StateManager.class);
        dialogService = mock(DialogService.class);
        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList());
    }

    @Test
    void parseUrlWithCitationKeyAndPage() {
        Optional<JumpToEntryPdfAction.EntryCitationUrl> result =
                JumpToEntryPdfAction.parseUrl("entry://Smith2024/12");

        assertTrue(result.isPresent());
        assertEquals("Smith2024", result.get().citationKey());
        assertEquals(Optional.of(12), result.get().pageNumber());
    }

    @Test
    void parseUrlWithCitationKeyOnly() {
        Optional<JumpToEntryPdfAction.EntryCitationUrl> result =
                JumpToEntryPdfAction.parseUrl("entry://Smith2024");

        assertTrue(result.isPresent());
        assertEquals("Smith2024", result.get().citationKey());
        assertEquals(Optional.empty(), result.get().pageNumber());
    }

    @Test
    void parseUrlWithUnderscoreInCitationKey() {
        Optional<JumpToEntryPdfAction.EntryCitationUrl> result =
                JumpToEntryPdfAction.parseUrl("entry://Smith_2024/5");

        assertTrue(result.isPresent());
        assertEquals("Smith_2024", result.get().citationKey());
        assertEquals(Optional.of(5), result.get().pageNumber());
    }

    @Test
    void parseUrlWithColonInCitationKey() {
        Optional<JumpToEntryPdfAction.EntryCitationUrl> result =
                JumpToEntryPdfAction.parseUrl("entry://Heyl:2023aa/3");

        assertTrue(result.isPresent());
        assertEquals("Heyl:2023aa", result.get().citationKey());
        assertEquals(Optional.of(3), result.get().pageNumber());
    }

    @Test
    void parseUrlWithInvalidSchemeReturnsEmpty() {
        assertTrue(JumpToEntryPdfAction.parseUrl("https://example.com/paper.pdf").isEmpty());
    }

    @Test
    void parseUrlWithNullOrBlankReturnsEmpty() {
        assertTrue(JumpToEntryPdfAction.parseUrl(null).isEmpty());
        assertTrue(JumpToEntryPdfAction.parseUrl("").isEmpty());
        assertTrue(JumpToEntryPdfAction.parseUrl("   ").isEmpty());
    }

    @Test
    void executeWithNoLibraryOpenNotifiesUser() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        JumpToEntryPdfAction action = new JumpToEntryPdfAction("entry://Key1", stateManager, dialogService);
        action.execute();

        verify(dialogService).notify(Localization.lang("No library open"));
    }

    @Test
    void executeWithEntryNotFoundNotifiesUser() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        JumpToEntryPdfAction action = new JumpToEntryPdfAction("entry://MissingKey", stateManager, dialogService);
        action.execute();

        verify(dialogService).notify(Localization.lang("Citation key '%0' to select not found in open libraries.", "MissingKey"));
    }

    @Test
    void executeWithEntryHavingNoPdfFilesNotifiesUser() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry().withCitationKey("Key1");
        database.insertEntry(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        LibraryTab activeTab = mock(LibraryTab.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.ofNullable(activeTab));

        JumpToEntryPdfAction action = new JumpToEntryPdfAction("entry://Key1", stateManager, dialogService);
        action.execute();

        verify(activeTab).clearAndSelect(entry);
        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }

    @Test
    void executeWithEntryHavingNonPdfFileNotifiesUser() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry().withCitationKey("Key1");
        entry.setFiles(List.of(new LinkedFile("Notes", "notes.txt", "TXT")));
        database.insertEntry(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        LibraryTab activeTab = mock(LibraryTab.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.ofNullable(activeTab));

        JumpToEntryPdfAction action = new JumpToEntryPdfAction("entry://Key1", stateManager, dialogService);
        action.execute();

        verify(activeTab).clearAndSelect(entry);
        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }
}
