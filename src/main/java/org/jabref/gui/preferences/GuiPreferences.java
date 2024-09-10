package org.jabref.gui.preferences;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;

public interface GuiPreferences extends org.jabref.logic.preferences.CliPreferences {
    EntryEditorPreferences getEntryEditorPreferences();

    MergeDialogPreferences getMergeDialogPreferences();

    AutoCompletePreferences getAutoCompletePreferences();

    CoreGuiPreferences getGuiPreferences();

    WorkspacePreferences getWorkspacePreferences();

    UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences();

    ExternalApplicationsPreferences getExternalApplicationsPreferences();
}
