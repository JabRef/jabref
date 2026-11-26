package org.jabref.gui.preferences.xmp;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.field.Field;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class XmpPrivacyTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty xmpFilterEnabledProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> xmpFilterListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final DialogService dialogService;
    private final XmpPreferences xmpPreferences;

    private final Validator xmpFilterListValidator;

    XmpPrivacyTabViewModel(DialogService dialogService, XmpPreferences xmpPreferences) {
        this.dialogService = dialogService;
        this.xmpPreferences = xmpPreferences;

        xmpFilterListValidator = new FunctionBasedValidator<>(
                xmpFilterListProperty,
                input -> !input.isEmpty(),
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("XMP metadata"),
                        Localization.lang("Filter List"),
                        Localization.lang("List must not be empty."))));
    }

    @Override
    public void setValues() {
        xmpFilterEnabledProperty.setValue(xmpPreferences.shouldUseXmpPrivacyFilter());

        xmpFilterListProperty.clear();
        xmpFilterListProperty.addAll(xmpPreferences.getXmpPrivacyFilter());
    }

    @Override
    public void storeSettings() {
        xmpPreferences.setUseXmpPrivacyFilter(xmpFilterEnabledProperty.getValue());
        xmpPreferences.getXmpPrivacyFilter().clear();
        xmpPreferences.getXmpPrivacyFilter().addAll(xmpFilterListProperty.getValue());
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
}
