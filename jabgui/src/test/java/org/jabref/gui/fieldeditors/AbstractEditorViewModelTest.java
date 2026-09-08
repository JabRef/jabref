package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/// The editor normalises CRLF for its comparison, because the text control uses `\n` throughout.
/// What it records has to be what the entry holds, or the change describes a state the library was
/// never in — and a change that does not match the library refuses to apply, which showed up as
/// typing doing nothing at all in a library loaded from a CRLF file.
@NullMarked
class AbstractEditorViewModelTest {

    @Test
    void editingAFieldHoldingCrlfReachesTheEntry() {
        BibEntry entry = new BibEntry().withField(StandardField.ABSTRACT, "line one\r\nline two");
        HeadlessGuiUndoManager journal = new HeadlessGuiUndoManager();
        AbstractEditorViewModel editor = new AbstractEditorViewModel(
                StandardField.ABSTRACT, mock(SuggestionProvider.class), mock(FieldCheckers.class), journal);
        editor.bindToEntry(entry);

        editor.textProperty().set("line one\nline two!");

        assertEquals(Optional.of("line one\nline two!"), entry.getField(StandardField.ABSTRACT));
        assertTrue(journal.canUndo(), "the edit was not recorded");

        journal.undo();
        assertEquals(Optional.of("line one\r\nline two"), entry.getField(StandardField.ABSTRACT),
                "undo did not restore the value the entry actually held");
    }
}
