package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Abstract base class for tag-based field editors.
 * Provides common functionality for editors that display field values as tags.
 */
public abstract class TagsEditor extends HBox implements FieldEditorFX {

    protected final Field field;
    protected final SuggestionProvider<?> suggestionProvider;
    protected final FieldCheckers fieldCheckers;
    protected final UndoManager undoManager;

    public TagsEditor(Field field,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers,
                      UndoManager undoManager) {
        this.field = field;
        this.suggestionProvider = suggestionProvider;
        this.fieldCheckers = fieldCheckers;
        this.undoManager = undoManager;
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public double getWeight() {
        return 2;
    }

    @Override
    public abstract void bindToEntry(BibEntry entry);
}
