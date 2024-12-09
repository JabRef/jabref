package org.jabref.gui.entryeditor;

import javafx.scene.control.Tab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntryEditorTab extends Tab {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditorTab.class);

    protected BibEntry currentEntry;

    /**
     * Needed to track for which type of entry this tab was build and to rebuild it if the type changes
     */
    private EntryType currentEntryType;

    /**
     * Decide whether to show this tab for the given entry.
     */
    public abstract boolean shouldShow(BibEntry entry);

    /**
     * Updates the view with the contents of the given entry.
     */
    protected abstract void bindToEntry(BibEntry entry);

    /**
     * The tab just got the focus. Override this method if you want to perform a special action on focus (like selecting
     * the first field in the editor)
     */
    protected void handleFocus() {
        // Do nothing by default
    }

    /**
     * Notifies the tab that it got focus and should display the given entry.
     */
    public void notifyAboutFocus(BibEntry entry) {
        if (!entry.equals(currentEntry) || !entry.getType().equals(currentEntryType)) {
            // TODO: Shouldn't "bindToEntry" called when changing the entry?
            LOGGER.trace("Tab got focus with different entry (or entry type) {}", entry);
            LOGGER.trace("Different entry: {}", entry.equals(currentEntry));
            LOGGER.trace("Different entry type: {}", !entry.getType().equals(currentEntryType));
            currentEntry = entry;
            currentEntryType = entry.getType();
            bindToEntry(entry);
        }
        handleFocus();
    }
}
