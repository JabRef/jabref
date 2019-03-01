package org.jabref.gui.preferences;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
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

    private final RadioButton exportInOriginalOrder;
    private final RadioButton exportInTableOrder;
    private final RadioButton exportInSpecifiedOrder;
    private final SaveOrderConfigDisplayView exportOrderPanel;
    private final GridPane builder = new GridPane();

    public ExportSortingPrefsTab(JabRefPreferences prefs) {
        // EXPORT SORT ORDER
        // create Components
        exportInOriginalOrder = new RadioButton(Localization.lang("Export entries in their original order"));
        exportInTableOrder = new RadioButton(Localization.lang("Export in current table sort order"));
        exportInSpecifiedOrder = new RadioButton(Localization.lang("Export entries ordered as specified"));

        final ToggleGroup group = new ToggleGroup();
        exportInOriginalOrder.setToggleGroup(group);
        exportInTableOrder.setToggleGroup(group);
        exportInSpecifiedOrder.setToggleGroup(group);

        exportOrderPanel = new SaveOrderConfigDisplayView(prefs.loadExportSaveOrder());

        Label exportSortOrder = new Label(Localization.lang("Export sort order"));
        exportSortOrder.getStyleClass().add("sectionHeader");
        // create GUI

        builder.add(exportOrderPanel.getJFXPanel(), 1, 8);
        builder.add(new Line(), 2, 9);

    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {

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
