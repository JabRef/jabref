package org.jabref.gui.preferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class ImportTabView extends AbstractPreferenceTabView<ImportTabViewModel> implements PreferencesTab {

    public ImportTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        this.viewModel = new ImportTabViewModel(dialogService, preferences);
    }

    @Override
    public String getTabName() { return Localization.lang("Import"); }
}
