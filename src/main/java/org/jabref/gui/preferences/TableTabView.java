package org.jabref.gui.preferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class TableTabView extends AbstractPreferenceTabView<TableTabViewModel> implements PreferencesTab {

    public TableTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        this.viewModel = new TableTabViewModel(dialogService, preferences);
    }

    @Override
    public String getTabName() { return Localization.lang("Entry table"); }
}
