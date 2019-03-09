package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import org.jabref.gui.SaveOrderConfigDisplayView;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Preference tab for file sorting options.
 */
class ExportSortingPrefsTab extends Pane implements PrefsTab {

    private final SaveOrderConfigDisplayView exportOrderPanel;
    private final GridPane builder = new GridPane();

    public ExportSortingPrefsTab(JabRefPreferences prefs) {
        exportOrderPanel = new SaveOrderConfigDisplayView(prefs.loadExportSaveOrder());

        builder.add(exportOrderPanel, 1, 8);
        builder.add(new Line(), 2, 9);
    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        //empty
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
    public String getTabName() {
        return Localization.lang("Export sorting");
    }
}
