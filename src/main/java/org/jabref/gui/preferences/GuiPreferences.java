package org.jabref.gui.preferences;

import org.jabref.gui.entryeditor.EntryEditorPreferences;

public interface GuiPreferences extends org.jabref.logic.preferences.CliPreferences {
    EntryEditorPreferences getEntryEditorPreferences();
}
