package org.jabref.gui.preferences.externalstorages;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.CliPreferences.CredentialPersistence;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ExternalStoragesTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty citeDriveEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty rememberCiteDriveLoginProperty = new SimpleBooleanProperty();
    private final StringProperty citeDriveApiBaseUrlProperty = new SimpleStringProperty("");
    private final StringProperty citeDriveAppBaseUrlProperty = new SimpleStringProperty("");

    private final CiteDrivePreferences citeDrivePreferences;
    private final CliPreferences preferences;

    private final Validator apiBaseUrlValidator;
    private final Validator appBaseUrlValidator;

    public ExternalStoragesTabViewModel(CliPreferences preferences) {
        this.preferences = preferences;
        this.citeDrivePreferences = preferences.getCiteDrivePreferences();

        apiBaseUrlValidator = urlValidator(citeDriveApiBaseUrlProperty, Localization.lang("Server address"));
        appBaseUrlValidator = urlValidator(citeDriveAppBaseUrlProperty, Localization.lang("Web address"));
    }

    private static Validator urlValidator(StringProperty url, String fieldName) {
        return new FunctionBasedValidator<>(
                url,
                CiteDrivePreferences::isSecureUrl,
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("External storages"),
                        fieldName,
                        Localization.lang("Please enter a valid HTTPS URL"))));
    }

    @Override
    public void setValues() {
        citeDriveEnabledProperty.setValue(citeDrivePreferences.isEnabled());
        rememberCiteDriveLoginProperty.setValue(citeDrivePreferences.shouldPersistRefreshToken());
        citeDriveApiBaseUrlProperty.setValue(citeDrivePreferences.getApiBaseUrl());
        citeDriveAppBaseUrlProperty.setValue(citeDrivePreferences.getAppBaseUrl());
    }

    @Override
    public void storeSettings() {
        citeDrivePreferences.setEnabled(citeDriveEnabledProperty.getValue());
        citeDrivePreferences.setAppBaseUrl(citeDriveAppBaseUrlProperty.getValue().trim());
    }

    @Override
    public Optional<CliPreferences.CredentialSave> prepareCredentialSave() {
        return preferences.prepareCiteDriveTokenSave(
                citeDriveApiBaseUrlProperty.get().trim(),
                rememberCiteDriveLoginProperty.get() ? CredentialPersistence.SAVE : CredentialPersistence.SESSION_ONLY);
    }

    @Override
    public boolean validateSettings() {
        return apiBaseUrlValidator.getValidationStatus().isValid() && appBaseUrlValidator.getValidationStatus().isValid();
    }

    /// Back to JabRef's own CiteDrive addresses
    public void resetCiteDriveAddresses() {
        CiteDrivePreferences defaults = CiteDrivePreferences.getDefault();
        citeDriveApiBaseUrlProperty.setValue(defaults.getApiBaseUrl());
        citeDriveAppBaseUrlProperty.setValue(defaults.getAppBaseUrl());
    }

    public BooleanProperty citeDriveEnabledProperty() {
        return citeDriveEnabledProperty;
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
