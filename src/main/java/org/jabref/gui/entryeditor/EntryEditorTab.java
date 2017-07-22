package org.jabref.gui.entryeditor;

import javafx.scene.control.Tab;
import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

public abstract class EntryEditorTab extends Tab {

    /**
     * Used for lazy-loading of the tab content.
     */
    private boolean isInitialized = false;

    public EntryEditorTab () {
        this.setStyle("-fx-font-size: " + Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE) + "pt;");
    }

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
