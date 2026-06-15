package org.jabref.gui.entryeditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntryEditorTab extends Tab {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditorTab.class);

    /// The entry currently being edited in the editor. The entry editor keeps this in sync for every tab
    /// (not only the focused one) so that {@link #shouldShow()} can react to entry and entry-type changes.
    private final ObjectProperty<@Nullable BibEntry> currentEntry = new SimpleObjectProperty<>();

    /// The entry (and its type) the tab content was last built for. Used to rebuild the content lazily on
    /// focus when the entry or its type changed. Kept separate from {@link #currentEntry} so that pushing a
    /// new entry into the property does not suppress the lazy rebuild in {@link #notifyAboutFocus(BibEntry)}.
    private @Nullable BibEntry boundEntry;
    private @Nullable EntryType boundEntryType;

    public ObjectProperty<@Nullable BibEntry> currentEntryProperty() {
        return currentEntry;
    }

    public @Nullable BibEntry getCurrentEntry() {
        return currentEntry.get();
    }

    /// Whether this tab should be shown for the current entry. The entry editor observes this value and
    /// adds or removes the tab accordingly. Implementations derive it from {@link #currentEntryProperty()}
    /// and any relevant preference (or other) observables, so the editor re-renders without being told.
    public abstract ObservableValue<Boolean> shouldShow();

    /// Updates the view with the contents of the given entry.
    protected abstract void bindToEntry(BibEntry entry);

    /// The tab just got the focus. Override this method if you want to perform a special action on focus (like selecting
    /// the first field in the editor)
    protected void handleFocus() {
        // Do nothing by default
    }

    /// Notifies the tab that it got focus and should display the given entry.
    public void notifyAboutFocus(BibEntry entry) {
        currentEntry.set(entry);
        if (!entry.equals(boundEntry) || !entry.getType().equals(boundEntryType)) {
            // TODO: Shouldn't "bindToEntry" called when changing the entry?
            LOGGER.trace("Tab got focus with different entry (or entry type) {}", entry);
            LOGGER.trace("Different entry: {}", !entry.equals(boundEntry));
            LOGGER.trace("Different entry type: {}", !entry.getType().equals(boundEntryType));
            boundEntry = entry;
            boundEntryType = entry.getType();
            bindToEntry(entry);
        }
        handleFocus();
    }
}
