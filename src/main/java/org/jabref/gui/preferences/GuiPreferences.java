package org.jabref.gui.preferences;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;

public interface GuiPreferences extends org.jabref.logic.preferences.CliPreferences {
    EntryEditorPreferences getEntryEditorPreferences();

    MergeDialogPreferences getMergeDialogPreferences();
}
