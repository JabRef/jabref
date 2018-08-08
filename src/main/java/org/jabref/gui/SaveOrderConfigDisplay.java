package org.jabref.gui;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.SaveOrderConfig;


public class SaveOrderConfigDisplay {

    private GridPane panel;
    private JPanel jPanel;
    private ComboBox<String> savePriSort;
    private ComboBox<String> saveSecSort;
    private ComboBox<String> saveTerSort;
    private CheckBox savePriDesc;
    private CheckBox saveSecDesc;
    private CheckBox saveTerDesc;

    private JComboBox<String> savePriSort1;
    private JComboBox<String> saveSecSort1;
    private JComboBox<String> saveTerSort1;
    private JCheckBox savePriDesc1;
    private JCheckBox saveSecDesc1;
    private JCheckBox saveTerDesc1;


    public SaveOrderConfigDisplay() {
        init();
    }

    private void init() {
        List<String> fieldNames = InternalBibtexFields.getAllPublicFieldNames();
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        savePriSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));
        savePriSort.setEditable(true);
        saveSecSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));
        saveSecSort.setEditable(true);
        saveTerSort = new ComboBox<>(FXCollections.observableArrayList(allPlusKey));
        saveTerSort.setEditable(true);

        savePriDesc = new CheckBox(Localization.lang("Descending"));
        saveSecDesc = new CheckBox(Localization.lang("Descending"));
        saveTerDesc = new CheckBox(Localization.lang("Descending"));

        GridPane builder = new GridPane();
        builder.add(new Label(Localization.lang("    Primary sort criterion ")),1,1);
        builder.add(savePriSort,2,1);
        builder.add(savePriDesc,3,1);

        builder.add(new Label(Localization.lang("Secondary sort criterion ")),1,2);
        builder.add(saveSecSort,2,2);
        builder.add(saveSecDesc,3,2);

        builder.add(new Label(Localization.lang("    Tertiary sort criterion ")),1, 3);
        builder.add(saveTerSort,2,3);
        builder.add(saveTerDesc,3,3);
        panel = builder;

        savePriSort1 = new JComboBox<>(allPlusKey);
        savePriSort1.setEditable(true);
        saveSecSort1 = new JComboBox<>(allPlusKey);
        saveSecSort1.setEditable(true);
        saveTerSort1 = new JComboBox<>(allPlusKey);
        saveTerSort1.setEditable(true);
        savePriDesc1 = new JCheckBox(Localization.lang("Descending"));
        saveSecDesc1 = new JCheckBox(Localization.lang("Descending"));
        saveTerDesc1 = new JCheckBox(Localization.lang("Descending"));
        FormLayout layout = new FormLayout("right:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref",
                "pref, 2dlu, pref, 2dlu, pref");
        FormBuilder builder1 = FormBuilder.create().layout(layout);
        builder1.add(Localization.lang("Primary sort criterion")).xy(1, 1);
        builder1.add(savePriSort1).xy(3, 1);
        builder1.add(savePriDesc1).xy(5, 1);
        builder1.add(Localization.lang("Secondary sort criterion")).xy(1, 3);
        builder1.add(saveSecSort1).xy(3, 3);
        builder1.add(saveSecDesc1).xy(5, 3);
        builder1.add(Localization.lang("Tertiary sort criterion")).xy(1, 5);
        builder1.add(saveTerSort1).xy(3, 5);
        builder1.add(saveTerDesc1).xy(5, 5);
        jPanel = builder1.build();
    }

    public Node getJFXPanel() {
        return panel;
    }

    public JPanel getPanel() {
        return jPanel;
    }

    public void setEnabled(boolean enabled) {
        savePriSort.setDisable(!enabled);
        savePriDesc.setDisable(!enabled);
        saveSecSort.setDisable(!enabled);
        saveSecDesc.setDisable(!enabled);
        saveTerSort.setDisable(!enabled);
        saveTerDesc.setDisable(!enabled);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        Objects.requireNonNull(saveOrderConfig);

        savePriSort.setValue(saveOrderConfig.sortCriteria[0].field);
        savePriDesc.setSelected(saveOrderConfig.sortCriteria[0].descending);
        saveSecSort.setValue(saveOrderConfig.sortCriteria[1].field);
        saveSecDesc.setSelected(saveOrderConfig.sortCriteria[1].descending);
        saveTerSort.setValue(saveOrderConfig.sortCriteria[2].field);
        saveTerDesc.setSelected(saveOrderConfig.sortCriteria[2].descending);

    }

    public SaveOrderConfig getSaveOrderConfig() {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig();
        saveOrderConfig.sortCriteria[0].field = getSelectedItemAsLowerCaseTrim(savePriSort);
        saveOrderConfig.sortCriteria[0].descending = savePriDesc.isSelected();
        saveOrderConfig.sortCriteria[1].field = getSelectedItemAsLowerCaseTrim(saveSecSort);
        saveOrderConfig.sortCriteria[1].descending = saveSecDesc.isSelected();
        saveOrderConfig.sortCriteria[2].field = getSelectedItemAsLowerCaseTrim(saveTerSort);
        saveOrderConfig.sortCriteria[2].descending = saveTerDesc.isSelected();

        return saveOrderConfig;
    }

    private String getSelectedItemAsLowerCaseTrim(ComboBox<String> sortBox) {
        return sortBox.getValue().toLowerCase(Locale.ROOT).trim();
    }
}
