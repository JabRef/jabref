package org.jabref.gui.entryeditor;

import javafx.scene.control.Tab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

public abstract class EntryEditorTab extends Tab {

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
            currentEntry = entry;
            currentEntryType = entry.getType();
            bindToEntry(entry);
        }
        handleFocus();
    }

    /**
     * Switch to next Preview style - should be overriden if a EntryEditorTab is actually showing a preview
     */
    protected void nextPreviewStyle() {
        // do nothing by default
    }

    /**
     * Switch to previous Preview style - should be overriden if a EntryEditorTab is actually showing a preview
     */
    protected void previousPreviewStyle() {
        // do nothing by default
    }
}
