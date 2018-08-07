package org.jabref.gui.preftabs;

import java.awt.BorderLayout;

import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;

import javax.swing.JPanel;

import org.jabref.gui.SaveOrderConfigDisplay;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;


/**
 * Preference tab for file sorting options.
 */
class ExportSortingPrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final RadioButton exportInOriginalOrder;
    private final RadioButton exportInTableOrder;
    private final RadioButton exportInSpecifiedOrder;
    private final SaveOrderConfigDisplay exportOrderPanel;


    public ExportSortingPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;

        GridPane builder = new GridPane();
        // EXPORT SORT ORDER
        // create Components
        exportInOriginalOrder = new RadioButton(Localization.lang("Export entries in their original order"));
        exportInTableOrder = new RadioButton(Localization.lang("Export in current table sort order"));
        exportInSpecifiedOrder = new RadioButton(Localization.lang("Export entries ordered as specified"));


        EventHandler<javafx.event.ActionEvent> listener =  new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                boolean selected = event.getSource() == exportInSpecifiedOrder;
                exportOrderPanel.setEnabled(selected);
            }
        };
        exportInOriginalOrder.setOnAction(listener);
        exportInTableOrder.setOnAction(listener);
        exportInSpecifiedOrder.setOnAction(listener);

        // create GUI
        builder.add(new Label(Localization.lang("Export sort order")),1,1);
        builder.add(new Separator(),2,1);
        builder.add(exportInOriginalOrder, 1,2);
        builder.add(new Line(),2,3);
        builder.add(exportInTableOrder, 1,4);
        builder.add(new Line(),2,5);
        builder.add(exportInSpecifiedOrder, 1,6);
        builder.add(new Line(),2,7);

        exportOrderPanel = new SaveOrderConfigDisplay();
        builder.add(exportOrderPanel.getJFXPanel(),2,8);
        builder.add(new Line(),2,9);

        // COMBINE EVERYTHING
        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER)) {
            exportInOriginalOrder.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
            exportInSpecifiedOrder.setSelected(true);
        } else {
            exportInTableOrder.setSelected(true);
        }

        boolean selected = prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER);
        exportOrderPanel.setEnabled(selected);
        exportOrderPanel.setSaveOrderConfig(prefs.loadExportSaveOrder());
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER, exportInOriginalOrder.isSelected());
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, exportInSpecifiedOrder.isSelected());

        prefs.storeExportSaveOrder(exportOrderPanel.getSaveOrderConfig());
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
