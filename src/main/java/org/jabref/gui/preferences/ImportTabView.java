package org.jabref.gui.preferences;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportTabView extends AbstractPreferenceTabView<ImportTabViewModel> implements PreferencesTab {

    public ImportTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new ImportTabViewModel(preferences);
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import");
    }
}
