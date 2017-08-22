package org.jabref.gui.entryeditor;

import javafx.scene.control.Tab;

public abstract class EntryEditorTab extends Tab {

    /**
     * Used for lazy-loading of the tab content.
     */
    protected boolean isInitialized = false;

    public abstract boolean shouldShow();

    public void requestFocus() {

    }

    /**
     * This method is called when the user focuses this tab.
     */
    public void notifyAboutFocus() {
        if (!isInitialized) {
            initialize();
            isInitialized = true;
        }
    }

    protected abstract void initialize();
}
