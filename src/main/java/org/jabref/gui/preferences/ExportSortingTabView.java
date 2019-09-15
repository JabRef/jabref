package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Label;

import org.jabref.gui.SaveOrderConfigDisplayView;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class ExportSortingTabView extends AbstractPreferenceTabView implements PreferencesTab {

    private final SaveOrderConfigDisplayView exportOrderPanel;

    public ExportSortingTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        Label title = new Label(Localization.lang("Export sorting"));
        title.getStyleClass().add("titleHeader");

        exportOrderPanel = new SaveOrderConfigDisplayView();
        exportOrderPanel.setValues(preferences.loadExportSaveOrder());

        this.setWidth(650.0);
        this.setSpacing(10.0);
        this.getChildren().addAll(title, exportOrderPanel);
    }

    @Override
    public String getTabName() { return Localization.lang("Export sorting"); }

    @Override
    public void setValues() {
        exportOrderPanel.setValues(preferences.loadExportSaveOrder());
    }

    @Override
    public void storeSettings() {
        exportOrderPanel.storeConfig();
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }
}
