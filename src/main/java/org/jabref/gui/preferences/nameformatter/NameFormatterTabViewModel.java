package org.jabref.gui.preferences.nameformatter;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

public class NameFormatterTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<NameFormatterItemModel> formatterListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty addFormatterNameProperty = new SimpleStringProperty();
    private final StringProperty addFormatterStringProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final NameFormatterPreferences initialNameFormatterPreferences;

    NameFormatterTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialNameFormatterPreferences = preferences.getNameFormatterPreferences();
    }

    @Override
    public void setValues() {
        formatterListProperty.clear();
        List<String> names = initialNameFormatterPreferences.getNameFormatterKey();
        List<String> formats = initialNameFormatterPreferences.getNameFormatterValue();

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

        NameFormatterPreferences newNameFormatterPreferences = new NameFormatterPreferences(
                names,
                formats);
        preferences.storeNameFormatterPreferences(newNameFormatterPreferences);
    }

    public void addFormatter() {
        if (!StringUtil.isNullOrEmpty(addFormatterNameProperty.getValue()) &&
                !StringUtil.isNullOrEmpty(addFormatterStringProperty.getValue())) {

            formatterListProperty.add(new NameFormatterItemModel(
                    addFormatterNameProperty.getValue(), addFormatterStringProperty.getValue()));

            addFormatterNameProperty.setValue("");
            addFormatterStringProperty.setValue("");
        }
    }

    public void removeFormatter(NameFormatterItemModel formatter) {
        formatterListProperty.remove(formatter);
    }

    public ListProperty<NameFormatterItemModel> formatterListProperty() {
        return formatterListProperty;
    }

    public StringProperty addFormatterNameProperty() {
        return addFormatterNameProperty;
    }

    public StringProperty addFormatterStringProperty() {
        return addFormatterStringProperty;
    }
}
