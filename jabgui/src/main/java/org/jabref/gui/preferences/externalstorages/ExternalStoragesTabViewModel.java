package org.jabref.gui.preferences.externalstorages;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.util.URLUtil;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ExternalStoragesTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty rememberCiteDriveLoginProperty = new SimpleBooleanProperty();
    private final StringProperty citeDriveApiBaseUrlProperty = new SimpleStringProperty("");
    private final StringProperty citeDriveAppBaseUrlProperty = new SimpleStringProperty("");

    private final CiteDrivePreferences citeDrivePreferences;

    private final Validator apiBaseUrlValidator;
    private final Validator appBaseUrlValidator;

    public ExternalStoragesTabViewModel(CiteDrivePreferences citeDrivePreferences) {
        this.citeDrivePreferences = citeDrivePreferences;

        apiBaseUrlValidator = urlValidator(citeDriveApiBaseUrlProperty, Localization.lang("Server address"));
        appBaseUrlValidator = urlValidator(citeDriveAppBaseUrlProperty, Localization.lang("Web address"));
    }

    private static Validator urlValidator(StringProperty url, String fieldName) {
        return new FunctionBasedValidator<>(
                url,
                URLUtil::isValidHttpUrl,
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("External storages"),
                        fieldName,
                        Localization.lang("Please enter a valid URL"))));
    }

    @Override
    public void setValues() {
        rememberCiteDriveLoginProperty.setValue(citeDrivePreferences.shouldPersistRefreshToken());
        citeDriveApiBaseUrlProperty.setValue(citeDrivePreferences.getApiBaseUrl());
        citeDriveAppBaseUrlProperty.setValue(citeDrivePreferences.getAppBaseUrl());
    }

    @Override
    public void storeSettings() {
        citeDrivePreferences.setPersistRefreshToken(rememberCiteDriveLoginProperty.getValue());
        citeDrivePreferences.setApiBaseUrl(citeDriveApiBaseUrlProperty.getValue().trim());
        citeDrivePreferences.setAppBaseUrl(citeDriveAppBaseUrlProperty.getValue().trim());
    }

    @Override
    public boolean validateSettings() {
        return apiBaseUrlValidator.getValidationStatus().isValid() && appBaseUrlValidator.getValidationStatus().isValid();
    }

    public BooleanProperty rememberCiteDriveLoginProperty() {
        return rememberCiteDriveLoginProperty;
    }

    public StringProperty citeDriveApiBaseUrlProperty() {
        return citeDriveApiBaseUrlProperty;
    }

    public StringProperty citeDriveAppBaseUrlProperty() {
        return citeDriveAppBaseUrlProperty;
    }

    public ValidationStatus apiBaseUrlValidationStatus() {
        return apiBaseUrlValidator.getValidationStatus();
    }

    public ValidationStatus appBaseUrlValidationStatus() {
        return appBaseUrlValidator.getValidationStatus();
    }
}
