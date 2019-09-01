package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

public class XmpPrivacyTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty xmpFilterEnabledProperty = new SimpleBooleanProperty();
    private final ListProperty<XmpPrivacyItemModel> filterListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> availableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Field> addFieldNameProperty = new SimpleObjectProperty<>();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    XmpPrivacyTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        xmpFilterEnabledProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER));

        filterListProperty.clear();
        List<XmpPrivacyItemModel> xmpExclusions = preferences.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS)
                .stream().map(name -> new XmpPrivacyItemModel(FieldFactory.parseField(name))).collect(Collectors.toList());
        filterListProperty.addAll(xmpExclusions);

        availableFieldsProperty.clear();
        availableFieldsProperty.addAll(FieldFactory.getCommonFields());
    }

    @Override
    public void storeSettings() {
        preferences.putBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER, xmpFilterEnabledProperty.getValue());
        preferences.putStringList(JabRefPreferences.XMP_PRIVACY_FILTERS, filterListProperty.getValue().stream()
                        .map(XmpPrivacyItemModel::getField)
                        .map(Field::getName)
                        .collect(Collectors.toList()));
    }

    public void addField() {
        if (addFieldNameProperty.getValue() == null) {
            return;
        }

        if (filterListProperty.getValue().stream().filter(item -> item.getField().equals(addFieldNameProperty.getValue())).findAny().isEmpty()) {
            filterListProperty.add(new XmpPrivacyItemModel(addFieldNameProperty.getValue()));
            addFieldNameProperty.setValue(null);
        }
    }

    public void removeFilter(XmpPrivacyItemModel filter) {
        filterListProperty.remove(filter);
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() { return new ArrayList<>(); }

    public BooleanProperty xmpFilterEnabledProperty() { return xmpFilterEnabledProperty; }

    public ListProperty<XmpPrivacyItemModel> filterListProperty() { return filterListProperty; }

    public ListProperty<Field> availableFieldsProperty() { return availableFieldsProperty; }

    public ObjectProperty<Field> addFieldNameProperty() { return addFieldNameProperty; }

    public String getFieldDisplayName(Field field) {
        if (field instanceof SpecialField) {
            return field.getName() + " (" + Localization.lang("Special") + ")";
        } else if (field instanceof IEEEField) {
            return field.getName() + " (" + Localization.lang("IEEE") + ")";
        } else if (field instanceof InternalField) {
            return field.getName() + " (" + Localization.lang("Internal") + ")";
        } else if (field instanceof UnknownField) {
            return field.getName() + " (" + Localization.lang("Custom") + ")";
        } else {
            return field.getName();
        }
    }
}
