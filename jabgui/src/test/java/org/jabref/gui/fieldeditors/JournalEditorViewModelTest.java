package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalEditorViewModelTest {

    private final JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
    private final UndoManager undoManager = new UndoManager();
    private BibEntry entry;
    private JournalEditorViewModel viewModel;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Journal of Foo");

        FieldCheckers fieldCheckers = mock(FieldCheckers.class);
        when(fieldCheckers.getForField(StandardField.JOURNAL)).thenReturn(java.util.List.of());

        viewModel = new JournalEditorViewModel(
                StandardField.JOURNAL,
                mock(SuggestionProvider.class),
                repository,
                fieldCheckers,
                mock(TaskExecutor.class),
                mock(DialogService.class),
                mock(GuiPreferences.class),
                undoManager);
        viewModel.bindToEntry(entry);
    }

    @Test
    void togglingTheAbbreviationCanBeUndone() {
        when(repository.getNextAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));

        viewModel.toggleAbbreviation();
        assertEquals("J. Foo", entry.getField(StandardField.JOURNAL).orElseThrow());

        assertTrue(undoManager.canUndo());
        undoManager.undo();
        assertEquals("Journal of Foo", entry.getField(StandardField.JOURNAL).orElseThrow());
    }

    @Test
    void togglingWithoutAMatchingAbbreviationChangesNothing() {
        when(repository.getNextAbbreviation("Journal of Foo")).thenReturn(Optional.empty());

        viewModel.toggleAbbreviation();

        assertEquals("Journal of Foo", entry.getField(StandardField.JOURNAL).orElseThrow());
        assertFalse(undoManager.canUndo());
    }
}
