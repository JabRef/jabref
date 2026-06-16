package org.jabref.gui.entryeditor;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

public class EntryEditorViewModel extends AbstractViewModel {

    private final ObjectProperty<@Nullable BibEntry> currentlyEditedEntry = new SimpleObjectProperty<>();
    private final StateManager stateManager;
    private final EntryEditorPreferences entryEditorPreferences;

    public EntryEditorViewModel(StateManager stateManager, EntryEditorPreferences entryEditorPreferences) {
        this.stateManager = stateManager;
        this.entryEditorPreferences = entryEditorPreferences;

        // [impl->req~entry-editor.keep-showing~1] — when selection becomes empty, keep the old entry showing
        stateManager.getSelectedEntries().addListener((InvalidationListener) _ -> {
            if (!stateManager.getSelectedEntries().isEmpty()) {
                currentlyEditedEntry.set(stateManager.getSelectedEntries().getFirst());
            }
        });
    }

    public ObjectProperty<@Nullable BibEntry> currentlyEditedEntryProperty() {
        return currentlyEditedEntry;
    }

    public @Nullable BibEntry getCurrentlyEditedEntry() {
        return currentlyEditedEntry.get();
    }

    public ObservableList<EntryEditorTabModel> getTabModels() {
        return entryEditorPreferences.getTabModels();
    }
}
