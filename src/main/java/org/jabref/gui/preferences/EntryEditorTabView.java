package org.jabref.gui.preferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class EntryEditorTabView extends AbstractPreferenceTabView<EntryEditorTabViewModel> implements PreferencesTab {

    public EntryEditorTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        this.viewModel = new EntryEditorTabViewModel(dialogService, preferences);
    }

    @Override
    public String getTabName() { return Localization.lang("Entry editor"); }
}
