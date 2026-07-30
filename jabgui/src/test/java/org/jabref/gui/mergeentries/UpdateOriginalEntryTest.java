package org.jabref.gui.mergeentries;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UpdateOriginalEntryTest {

    @Test
    void emptyMergedEntryDoesNotModifyOriginalEntry() {
        BibEntry originalEntry = new BibEntry()
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.YEAR, "2024");

        DialogService dialogService = mock(DialogService.class);
        UndoManager undoManager = mock(UndoManager.class);

        UpdateOriginalEntry updateOriginalEntry = new UpdateOriginalEntry(
                originalEntry,
                Optional.of(new BibEntry()),
                Optional.empty(),
                dialogService,
                undoManager
        );

        updateOriginalEntry.update();

        assertEquals(Optional.of("Original title"), originalEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2024"), originalEntry.getField(StandardField.YEAR));
        verify(dialogService).notify(Localization.lang("Canceled merging entries"));
        verifyNoInteractions(undoManager);
    }

    @Test
    void canceledMergeDoesNotModifyOriginalEntry() {
        BibEntry originalEntry = new BibEntry()
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.YEAR, "2024");

        DialogService dialogService = mock(DialogService.class);
        UndoManager undoManager = mock(UndoManager.class);

        UpdateOriginalEntry updateOriginalEntry = new UpdateOriginalEntry(
                originalEntry,
                Optional.empty(),
                Optional.empty(),
                dialogService,
                undoManager
        );

        updateOriginalEntry.update();

        assertEquals(Optional.of("Original title"), originalEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2024"), originalEntry.getField(StandardField.YEAR));
        verify(dialogService).notify(Localization.lang("Canceled merging entries"));
        verifyNoInteractions(undoManager);
    }
}
