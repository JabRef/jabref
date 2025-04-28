package org.jabref.gui.preferences;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.edit.CopyToPreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.frame.SidePanePreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.newentryunified.NewEntryUnifiedPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.push.PushToApplicationPreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.logic.preferences.CliPreferences;

public interface GuiPreferences extends CliPreferences {
    CopyToPreferences getCopyToPreferences();

    EntryEditorPreferences getEntryEditorPreferences();

    MergeDialogPreferences getMergeDialogPreferences();

    AutoCompletePreferences getAutoCompletePreferences();

    CoreGuiPreferences getGuiPreferences();

    WorkspacePreferences getWorkspacePreferences();

    UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences();

    ExternalApplicationsPreferences getExternalApplicationsPreferences();

    SidePanePreferences getSidePanePreferences();

    GroupsPreferences getGroupsPreferences();

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    PreviewPreferences getPreviewPreferences();

    PushToApplicationPreferences getPushToApplicationPreferences();

    NameDisplayPreferences getNameDisplayPreferences();

    MainTablePreferences getMainTablePreferences();

    ColumnPreferences getMainTableColumnPreferences();

    ColumnPreferences getSearchDialogColumnPreferences();

    KeyBindingRepository getKeyBindingRepository();

    NewEntryUnifiedPreferences getNewEntryUnifiedPreferences();
}
