package org.jabref.gui.documentviewer;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.documentviewer.JumpToEntryPdfAction.EntryLink;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void parseRelativeLinkWithPage() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Smith2024", Optional.empty(), Optional.of(12))),
                JumpToEntryPdfAction.parseUrl("entries/Smith2024#page=12"));
    }

    @Test
    void parseRelativeLinkWithPathPage() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Queiroz2026", Optional.empty(), Optional.of(5))),
                JumpToEntryPdfAction.parseUrl("entries/Queiroz2026/5"));
    }

    @Test
    void parseRelativeLinkWithFilesAndPathPage() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Queiroz2026", Optional.of(1), Optional.of(5))),
                JumpToEntryPdfAction.parseUrl("entries/Queiroz2026/files/1/5"));
    }

    @Test
    void parseRelativeLinkWithPageKeyword() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Queiroz2026", Optional.empty(), Optional.of(5))),
                JumpToEntryPdfAction.parseUrl("entries/Queiroz2026/page/5"));
    }

    @Test
    void parseRelativeLinkWithBareNumberFragment() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Queiroz2026", Optional.empty(), Optional.of(5))),
                JumpToEntryPdfAction.parseUrl("entries/Queiroz2026#5"));
    }

    @Test
    void isEntryUrlRecognizesEntryLinks() {
        assertTrue(JumpToEntryPdfAction.isEntryUrl("entries/Queiroz2026/5"));
        assertTrue(JumpToEntryPdfAction.isEntryUrl("/entries/Queiroz2026"));
        assertTrue(JumpToEntryPdfAction.isEntryUrl("jabref://libraries/lib/entries/Key"));
        assertFalse(JumpToEntryPdfAction.isEntryUrl("https://example.com"));
        assertFalse(JumpToEntryPdfAction.isEntryUrl(""));
        assertFalse(JumpToEntryPdfAction.isEntryUrl(null));
    }

    @Test
    void parseRelativeLinkWithoutPage() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Smith2024", Optional.empty(), Optional.empty())),
                JumpToEntryPdfAction.parseUrl("entries/Smith2024"));
    }

    @Test
    void parseAbsoluteLinkWithLibraryFileAndPage() {
        assertEquals(Optional.of(new EntryLink(Optional.of("Chocolate.bib-1a2b3c4d"), "Heyl:2023aa", Optional.of(2), Optional.of(3))),
                JumpToEntryPdfAction.parseUrl("jabref://libraries/Chocolate.bib-1a2b3c4d/entries/Heyl:2023aa/files/2#page=3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Smith_2024", "Heyl:2023aa", "smith.2024", "Sm+ith", "M%C3%BCller2020"})
    void parseKeepsCitationKeyCharacters(String key) {
        String expected = "M%C3%BCller2020".equals(key) ? "Müller2020" : key;
        assertEquals(Optional.of(expected), JumpToEntryPdfAction.parseUrl("entries/" + key).map(EntryLink::citationKey));
    }

    @Test
    void parseIgnoresInvalidPageAndFileIndex() {
        assertEquals(Optional.of(new EntryLink(Optional.empty(), "Key", Optional.empty(), Optional.empty())),
                JumpToEntryPdfAction.parseUrl("entries/Key/files/0#page=abc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/paper.pdf", "entry://Smith2024/5", "jabref://entries/Smith2024", "jabref://libraries/id/groups/Foo", "entries/Key/foo", "libraries/id", "", "   "})
    void parseRejectsOtherLinks(String url) {
        assertEquals(Optional.empty(), JumpToEntryPdfAction.parseUrl(url));
    }

    @Test
    void parseRejectsNull() {
        assertEquals(Optional.empty(), JumpToEntryPdfAction.parseUrl(null));
    }

    @Test
    void executeWithInvalidUrlNotifiesUser() {
        new JumpToEntryPdfAction("invalid-url", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("Invalid URL"));
    }

    @Test
    void executeWithNoLibraryOpenNotifiesUser() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        new JumpToEntryPdfAction("entries/Key1", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("No library open"));
    }

    @Test
    void executeWithUnknownLibraryIdNotifiesUser() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext()));

        new JumpToEntryPdfAction("jabref://libraries/unknown/entries/Key1", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("No library open"));
    }

    @Test
    void executeWithEntryNotFoundNotifiesUser() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext()));

        new JumpToEntryPdfAction("entries/MissingKey", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("Citation key '%0' to select not found in open libraries.", "MissingKey"));
    }

    @Test
    void executeWithEntryHavingNoPdfFilesNotifiesUser() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(new BibEntry().withCitationKey("Key1"));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));

        new JumpToEntryPdfAction("entries/Key1", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }

    @Test
    void executeWithEntryHavingNonPdfFileNotifiesUser() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(new BibEntry().withCitationKey("Key1").withFiles(List.of(new LinkedFile("Notes", "notes.txt", "TXT"))));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));

        new JumpToEntryPdfAction("entries/Key1", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }

    @Test
    void executeWithFileIndexOutOfRangeFallsBackToFirstPdfNotification() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(new BibEntry().withCitationKey("Key1").withFiles(List.of(new LinkedFile("Notes", "notes.txt", "TXT"))));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));

        new JumpToEntryPdfAction("entries/Key1/files/5", stateManager, dialogService).execute();

        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }

    @Test
    void executeSelectsEntryInStateManager() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry().withCitationKey("Key1");
        database.insertEntry(entry);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));

        new JumpToEntryPdfAction("entries/Key1", stateManager, dialogService).execute();

        verify(stateManager).setSelectedEntries(List.of(entry));
    }
}
