package org.jabref.gui.preferences.customization;

import java.io.IOException;
import java.net.HttpURLConnection;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.SpringerApiKeyPreferences;
import org.jabref.preferences.PreferencesService;

public class CustomizationTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");
    private final BooleanProperty useCustomSpringerKeyProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomSpringerKeyNameProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final DOIPreferences initialDOIPreferences;
    private final SpringerApiKeyPreferences initialSpringerApiKeyPreferences;

    public CustomizationTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.initialDOIPreferences = preferencesService.getDOIPreferences();
        this.initialSpringerApiKeyPreferences = preferencesService.getSpringerAPIKeyPreferences();
    }

    /**
     * Reads user settings
     */
    @Override
    public void setValues() {
        useCustomDOIProperty.setValue(initialDOIPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(initialDOIPreferences.getDefaultBaseURI());
        useCustomSpringerKeyProperty.setValue(initialSpringerApiKeyPreferences.isUseCustom());
        useCustomSpringerKeyNameProperty.setValue(initialSpringerApiKeyPreferences.getDefaultApiKey());
    }

    /**
     * Stores user settings
     */
    @Override
    public void storeSettings() {
        preferencesService.storeDOIPreferences(new DOIPreferences(
                useCustomDOIProperty.getValue(),
                useCustomDOINameProperty.getValue().trim()));
        preferencesService.storeCustomSpringerKeyPreferences(new SpringerApiKeyPreferences(
                useCustomSpringerKeyProperty.getValue(),
                useCustomSpringerKeyNameProperty.getValue().trim()));
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    /**
     * Gets CustomSpringerKeyProperty
     *
     * @return BooleanProperty
     */
    public BooleanProperty useCustomSpringerKeyProperty() {
        return this.useCustomSpringerKeyProperty;
    }

    /**
     * Get CustomSpringerKeyNameProperty
     *
     * @return StringProperty
     */
    public StringProperty useCustomSpringerKeyNameProperty() {
        return this.useCustomSpringerKeyNameProperty;
    }

    /**
     * Check the connection by using the given springer api. Used for validating the springer api key.
     * The checking result will be appear when request finished.
     */
    public void checkSpringerApiKey() {
        final String connectionSuccessText = Localization.lang("Connection successful!");
        final String connectionFailedText = Localization.lang("Connection failed!");
        final String dialogTitle = Localization.lang("Check Springer API Key Setting");

        final String springerApiKey = useCustomSpringerKeyNameProperty.getValue().trim();
        boolean valid = true;

        if (useCustomSpringerKeyProperty.getValue()) {
            if(!springerApiKey.isEmpty()){
                final String testUrl = "https://api.springernature.com/meta/v1/json?q=doi:10.1007/s11276-008-0131-4s=1&p=1&api_key="
                        + springerApiKey;
                URLDownload urlDownload;
                try {
                    urlDownload = new URLDownload(testUrl);
                    // The HEAD request cannot be used because its response is always 404.
                    int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                    valid = statusCode >= 200 && statusCode < 300;
                } catch (IOException | kong.unirest.UnirestException e) {
                    valid = false;
                }
            }else{
                valid = false;
            }
        }

        if(valid){
            dialogService.showInformationDialogAndWait(dialogTitle, connectionSuccessText);
        }else{
            dialogService.showErrorDialogAndWait(dialogTitle, connectionFailedText);
        }
    }
}
