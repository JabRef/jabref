package org.jabref.gui;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.SaveOrderConfig;

public class SaveOrderConfigDisplay {

    private GridPane panel;
    private ComboBox<String> savePriSort;
    private ComboBox<String> saveSecSort;
    private ComboBox<String> saveTerSort;
    private CheckBox savePriDesc;
    private CheckBox saveSecDesc;
    private CheckBox saveTerDesc;


    public SaveOrderConfigDisplay() {
        init();
    }

    private void init() {
        List<String> fieldNames = InternalBibtexFields.getAllPublicFieldNames();
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        savePriSort = new ComboBox<>(FXCollections.observableArrayList(fieldNames));
        savePriSort.setEditable(true);
        saveSecSort = new ComboBox<>(FXCollections.observableArrayList(fieldNames));
        saveSecSort.setEditable(true);
        saveTerSort = new ComboBox<>(FXCollections.observableArrayList(fieldNames));
        saveTerSort.setEditable(true);

        savePriDesc = new CheckBox(Localization.lang("Descending"));
        saveSecDesc = new CheckBox(Localization.lang("Descending"));
        saveTerDesc = new CheckBox(Localization.lang("Descending"));

        Font font = new Font(10);
        GridPane builder = new GridPane();
        Label primarySortCriterion = new Label(Localization.lang("Primary sort criterion"));
        primarySortCriterion.setFont(font);
        builder.add(primarySortCriterion, 1, 1);
        builder.add(savePriSort, 2, 1);
        builder.add(savePriDesc, 3, 1);

        Label secondarySortCriterion = new Label(Localization.lang("Secondary sort criterion"));
        secondarySortCriterion.setFont(font);
        builder.add(secondarySortCriterion, 1, 2);
        builder.add(saveSecSort, 2, 2);
        builder.add(saveSecDesc, 3, 2);

        Label tertiarySortCriterion = new Label(Localization.lang("Tertiary sort criterion"));
        tertiarySortCriterion.setFont(font);
        builder.add(tertiarySortCriterion, 1, 3);
        builder.add(saveTerSort, 2, 3);
        builder.add(saveTerDesc, 3, 3);
        panel = builder;
    }

    public Node getJFXPanel() {
        return panel;
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
