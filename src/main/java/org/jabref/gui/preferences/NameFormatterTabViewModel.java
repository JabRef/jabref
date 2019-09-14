package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

public class NameFormatterTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<NameFormatterItemModel> formatterListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty addFormatterNameProperty = new SimpleStringProperty();
    private final StringProperty addFormatterStringProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    NameFormatterTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        formatterListProperty.clear();
        List<String> names = preferences.getStringList(JabRefPreferences.NAME_FORMATER_KEY);
        List<String> formats = preferences.getStringList(JabRefPreferences.NAME_FORMATTER_VALUE);

        for (int i = 0; i < names.size(); i++) {
            if (i < formats.size()) {
                formatterListProperty.add(new NameFormatterItemModel(names.get(i), formats.get(i)));
            } else {
                formatterListProperty.add(new NameFormatterItemModel(names.get(i)));
            }
        }
    }

    @Override
    public void storeSettings() {
        formatterListProperty.removeIf(formatter -> formatter.getName().isEmpty());

        List<String> names = new ArrayList<>(formatterListProperty.size());
        List<String> formats = new ArrayList<>(formatterListProperty.size());
        for (NameFormatterItemModel formatterListItem : formatterListProperty) {
            names.add(formatterListItem.getName());
            formats.add(formatterListItem.getFormat());
        }

        preferences.putStringList(JabRefPreferences.NAME_FORMATER_KEY, names);
        preferences.putStringList(JabRefPreferences.NAME_FORMATTER_VALUE, formats);
    }

    public void addFormatter() {
        if (!StringUtil.isNullOrEmpty(addFormatterNameProperty.getValue()) &&
                !StringUtil.isNullOrEmpty(addFormatterStringProperty.getValue())) {

            NameFormatterItemModel newFormatter = new NameFormatterItemModel(
                    addFormatterNameProperty.getValue(), addFormatterStringProperty.getValue());

            addFormatterNameProperty.setValue("");
            addFormatterStringProperty.setValue("");
            formatterListProperty.add(newFormatter);
        }
    }

    public void removeFormatter(NameFormatterItemModel formatter) { formatterListProperty.remove(formatter); }

    @Override
    public boolean validateSettings() { return true; }

    @Override
    public List<String> getRestartWarnings() { return new ArrayList<>(); }

    public ListProperty<NameFormatterItemModel> formatterListProperty() { return formatterListProperty; }

    public StringProperty addFormatterNameProperty() { return addFormatterNameProperty; }

    public StringProperty addFormatterStringProperty() { return addFormatterStringProperty; }
}
