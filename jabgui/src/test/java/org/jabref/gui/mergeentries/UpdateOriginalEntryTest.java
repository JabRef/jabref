package org.jabref.gui.mergeentries;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateOriginalEntryTest {

    private static final String EDIT_NAME = "Merge entry with information";
    private static final String SUCCESS_MESSAGE = "Updated entry with merged information";

    private BibEntry originalEntry;
    private DialogService dialogService;
    private CountingUndoManager undoManager;

    @BeforeEach
    void setUp() {
        originalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.YEAR, "2020");
        dialogService = mock(DialogService.class);
        undoManager = mock(CountingUndoManager.class);
    }

    @Test
    void updateAppliesMergedFieldsToOriginalEntry() {
        BibEntry mergedEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Merged title")
                .withField(StandardField.ISSN, "0378-5955");

        new UpdateOriginalEntry(originalEntry, Optional.of(mergedEntry), dialogService, undoManager, EDIT_NAME, SUCCESS_MESSAGE)
                .update();

        assertEquals(StandardEntryType.Book, originalEntry.getType());
        assertEquals(Optional.of("Merged title"), originalEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("0378-5955"), originalEntry.getField(StandardField.ISSN));
        assertTrue(originalEntry.getField(StandardField.YEAR).isEmpty());
        verify(undoManager).addEdit(any());
        verify(dialogService).notify(SUCCESS_MESSAGE);
    }

    @Test
    void updateKeepsOriginalFieldsWhenMergedEntryIsEffectivelyEmpty() {
        BibEntry mergedEntry = new BibEntry(StandardEntryType.Article);

        new UpdateOriginalEntry(originalEntry, Optional.of(mergedEntry), dialogService, undoManager, EDIT_NAME, SUCCESS_MESSAGE)
                .update();

        assertEquals(Optional.of("Original title"), originalEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2020"), originalEntry.getField(StandardField.YEAR));
        verify(undoManager, never()).addEdit(any());
        verify(dialogService).notify("No information added");
    }

    @Test
    void updateAppliesTypeOnlyMerge() {
        BibEntry mergedEntry = new BibEntry(StandardEntryType.Book);
        BibEntry originalTypeOnlyEntry = new BibEntry(StandardEntryType.Article);

        new UpdateOriginalEntry(originalTypeOnlyEntry, Optional.of(mergedEntry), dialogService, undoManager, EDIT_NAME, SUCCESS_MESSAGE)
                .update();

        assertEquals(StandardEntryType.Book, originalTypeOnlyEntry.getType());
        verify(undoManager).addEdit(any());
        verify(dialogService).notify(SUCCESS_MESSAGE);
    }

    @Test
    void updateTreatsMissingMergedEntryAsCanceled() {
        new UpdateOriginalEntry(originalEntry, Optional.empty(), dialogService, undoManager, EDIT_NAME, SUCCESS_MESSAGE)
                .update();

        assertEquals(Optional.of("Original title"), originalEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2020"), originalEntry.getField(StandardField.YEAR));
        verify(undoManager, never()).addEdit(any());
        verify(dialogService).notify("Canceled merging entries");
    }
}
