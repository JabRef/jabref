package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class GroupEditor extends SimpleEditor {

    private Optional<BibEntry> bibEntry;

    public GroupEditor(final Field field,
                       final SuggestionProvider<?> suggestionProvider,
                       final FieldCheckers fieldCheckers,
                       final GuiPreferences preferences,
                       final boolean isMultiLine,
                       final UndoManager undoManager,
                       final UndoAction undoAction,
                       final RedoAction redoAction) {
        super(field, suggestionProvider, fieldCheckers, preferences, isMultiLine, undoManager, undoAction, redoAction);

        this.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                List<String> draggedGroups = (List<String>) event.getDragboard().getContent(DragAndDropDataFormats.GROUP);
                if (bibEntry.isPresent() && draggedGroups.getFirst() != null) {
                    String newGroup = bibEntry.map(entry -> entry.getField(StandardField.GROUPS)
                                                                 .map(oldGroups -> oldGroups + (preferences.getBibEntryPreferences().getKeywordSeparator()) + (draggedGroups.getFirst()))
                                                                 .orElse(draggedGroups.getFirst()))
                                              .orElse(null);
                    bibEntry.map(entry -> entry.setField(StandardField.GROUPS, newGroup));
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        this.bibEntry = Optional.of(entry);
    }
}
