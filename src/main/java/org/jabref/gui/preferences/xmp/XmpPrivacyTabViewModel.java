package org.jabref.gui.preferences.xmp;

import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class XmpPrivacyTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty xmpFilterEnabledProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> xmpFilterListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> availableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Field> addFieldProperty = new SimpleObjectProperty<>();

    private final DialogService dialogService;
    private final XmpPreferences xmpPreferences;

    private final Validator xmpFilterListValidator;

    XmpPrivacyTabViewModel(DialogService dialogService, XmpPreferences xmpPreferences) {
        this.dialogService = dialogService;
        this.xmpPreferences = xmpPreferences;

        xmpFilterListValidator = new FunctionBasedValidator<>(
                xmpFilterListProperty,
                input -> input.size() > 0,
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("XMP metadata"),
                        Localization.lang("Filter List"),
                        Localization.lang("List must not be empty."))));
    }

    @Override
    public void setValues() {
        xmpFilterEnabledProperty.setValue(xmpPreferences.shouldUseXmpPrivacyFilter());

        xmpFilterListProperty.clear();
        xmpFilterListProperty.addAll(xmpPreferences.getXmpPrivacyFilter());

        availableFieldsProperty.clear();
        availableFieldsProperty.addAll(FieldFactory.getCommonFields());
        availableFieldsProperty.sort((Comparator.comparing(Field::getDisplayName)));
    }

    @Override
    public void storeSettings() {
        xmpPreferences.setUseXmpPrivacyFilter(xmpFilterEnabledProperty.getValue());
        xmpPreferences.getXmpPrivacyFilter().clear();
        xmpPreferences.getXmpPrivacyFilter().addAll(xmpFilterListProperty.getValue());
    }

    public void addField() {
        if (addFieldProperty.getValue() == null) {
            return;
        }

        if (xmpFilterListProperty.getValue().stream().filter(item -> item.equals(addFieldProperty.getValue())).findAny().isEmpty()) {
            xmpFilterListProperty.add(addFieldProperty.getValue());
            addFieldProperty.setValue(null);
        }
    }

    public void removeFilter(Field filter) {
        xmpFilterListProperty.remove(filter);
    }

    public ValidationStatus xmpFilterListValidationStatus() {
        return xmpFilterListValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus validationStatus = xmpFilterListValidationStatus();
        if (xmpFilterEnabledProperty.getValue() && !validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    public BooleanProperty xmpFilterEnabledProperty() {
        return xmpFilterEnabledProperty;
    }

    public ListProperty<Field> filterListProperty() {
        return xmpFilterListProperty;
    }

    public ListProperty<Field> availableFieldsProperty() {
        return availableFieldsProperty;
    }

    public ObjectProperty<Field> addFieldNameProperty() {
        return addFieldProperty;
    }
}
