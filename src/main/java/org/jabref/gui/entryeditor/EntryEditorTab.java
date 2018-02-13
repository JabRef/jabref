package org.jabref.gui.entryeditor;

import javafx.scene.control.Tab;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.model.entry.BibEntry;

public abstract class EntryEditorTab extends Tab {

    protected BibEntry currentEntry;

    /**
     * Needed to track for which type of entry this tab was build and to rebuild it if the type changes
     */
    private String currentEntryType = "";

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
        if (!entry.equals(currentEntry) || !currentEntryType.equals(entry.getType())) {
            currentEntry = entry;
            currentEntryType = entry.getType();
            DefaultTaskExecutor.runInJavaFXThread(() -> bindToEntry(entry));
        }
        handleFocus();
    }

}
