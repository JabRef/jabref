package org.jabref.gui.preferences;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.remote.JabRefMessageHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.RemoteUtil;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import kong.unirest.UnirestException;

public class NetworkTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty remoteServerProperty = new SimpleBooleanProperty();
    private final StringProperty remotePortProperty = new SimpleStringProperty("");
    private final BooleanProperty proxyUseProperty = new SimpleBooleanProperty();
    private final StringProperty proxyHostnameProperty = new SimpleStringProperty("");
    private final StringProperty proxyPortProperty = new SimpleStringProperty("");
    private final BooleanProperty proxyUseAuthenticationProperty = new SimpleBooleanProperty();
    private final StringProperty proxyUsernameProperty = new SimpleStringProperty("");
    private final StringProperty proxyPasswordProperty = new SimpleStringProperty("");

    private final Validator remotePortValidator;
    private final Validator proxyHostnameValidator;
    private final Validator proxyPortValidator;
    private final Validator proxyUsernameValidator;
    private final Validator proxyPasswordValidator;

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final RemotePreferences initialRemotePreferences;
    private final ProxyPreferences initialProxyPreferences;

    private final List<String> restartWarning = new ArrayList<>();

    public NetworkTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialRemotePreferences = preferences.getRemotePreferences();
        this.initialProxyPreferences = preferences.getProxyPreferences();

        remotePortValidator = new FunctionBasedValidator<>(
                remotePortProperty,
                input -> {
                    try {
                        int portNumber = Integer.parseInt(remotePortProperty().getValue());
                        return RemoteUtil.isUserPort(portNumber);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Network"),
                        Localization.lang("Remote operation"),
                        Localization.lang("You must enter an integer value in the interval 1025-65535"))));

        proxyHostnameValidator = new FunctionBasedValidator<>(
                proxyHostnameProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Network"),
                        Localization.lang("Proxy configuration"),
                        Localization.lang("Please specify a hostname"))));

        proxyPortValidator = new FunctionBasedValidator<>(
                proxyPortProperty,
                input -> getPortAsInt(input).isPresent(),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Network"),
                        Localization.lang("Proxy configuration"),
                        Localization.lang("Please specify a port"))));

        proxyUsernameValidator = new FunctionBasedValidator<>(
                proxyUsernameProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Network"),
                        Localization.lang("Proxy configuration"),
                        Localization.lang("Please specify a username"))));

        proxyPasswordValidator = new FunctionBasedValidator<>(
                proxyPasswordProperty,
                input -> input.length() > 0,
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Network"),
                        Localization.lang("Proxy configuration"),
                        Localization.lang("Please specify a password"))));
    }

    public void setValues() {
        remoteServerProperty.setValue(initialRemotePreferences.useRemoteServer());
        remotePortProperty.setValue(String.valueOf(initialRemotePreferences.getPort()));

        setProxyValues();
    }

    private void setProxyValues() {
        proxyUseProperty.setValue(initialProxyPreferences.isUseProxy());
        proxyHostnameProperty.setValue(initialProxyPreferences.getHostname());
        proxyPortProperty.setValue(initialProxyPreferences.getPort());
        proxyUseAuthenticationProperty.setValue(initialProxyPreferences.isUseAuthentication());
        proxyUsernameProperty.setValue(initialProxyPreferences.getUsername());
        proxyPasswordProperty.setValue(initialProxyPreferences.getPassword());
    }

    public void storeSettings() {
        storeRemoteSettings();
        storeProxySettings();
    }

    private void storeRemoteSettings() {
        RemotePreferences newRemotePreferences = new RemotePreferences(
                initialRemotePreferences.getPort(),
                remoteServerProperty.getValue()
        );

        getPortAsInt(remotePortProperty.getValue()).ifPresent(newPort -> {
            if (initialRemotePreferences.isDifferentPort(newPort)) {
                newRemotePreferences.setPort(newPort);

                if (newRemotePreferences.useRemoteServer()) {
                    restartWarning.add(Localization.lang("Remote server port") + ": " + newPort);
                }
            }
        });

        if (newRemotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), initialRemotePreferences.getPort());
        } else {
            Globals.REMOTE_LISTENER.stop();
        }

        preferences.storeRemotePreferences(newRemotePreferences);
    }

    private void storeProxySettings() {
        ProxyPreferences newProxyPreferences = new ProxyPreferences(
                proxyUseProperty.getValue(),
                proxyHostnameProperty.getValue().trim(),
                proxyPortProperty.getValue().trim(),
                proxyUseAuthenticationProperty.getValue(),
                proxyUsernameProperty.getValue().trim(),
                proxyPasswordProperty.getValue()
        );

        if (!newProxyPreferences.equals(initialProxyPreferences)) {
            ProxyRegisterer.register(newProxyPreferences);
        }
        preferences.storeProxyPreferences(newProxyPreferences);
    }

    private Optional<Integer> getPortAsInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public ValidationStatus remotePortValidationStatus() {
        return remotePortValidator.getValidationStatus();
    }

    public ValidationStatus proxyHostnameValidationStatus() {
        return proxyHostnameValidator.getValidationStatus();
    }

    public ValidationStatus proxyPortValidationStatus() {
        return proxyPortValidator.getValidationStatus();
    }

    public ValidationStatus proxyUsernameValidationStatus() {
        return proxyUsernameValidator.getValidationStatus();
    }

    public ValidationStatus proxyPasswordValidationStatus() {
        return proxyPasswordValidator.getValidationStatus();
    }

    public boolean validateSettings() {
        CompositeValidator validator = new CompositeValidator();

        if (remoteServerProperty.getValue()) {
            validator.addValidators(remotePortValidator);
        }

        if (proxyUseProperty.getValue()) {
            validator.addValidators(proxyHostnameValidator);
            validator.addValidators(proxyPortValidator);

            if (proxyUseAuthenticationProperty.getValue()) {
                validator.addValidators(proxyUsernameValidator);
                validator.addValidators(proxyPasswordValidator);
            }
        }

        ValidationStatus validationStatus = validator.getValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    /**
     * Check the connection by using the given url. Used for validating the http proxy.
     * The checking result will be appear when request finished.
     * The checking result could be either success or fail, if fail, the cause will be displayed.
     */
    public void checkConnection() {
        final String connectionSuccessText = Localization.lang("Connection successful!");
        final String connectionFailedText = Localization.lang("Connection failed!");
        final String dialogTitle = Localization.lang("Check Proxy Setting");

        final String testUrl = "http://jabref.org";

        // Workaround for testing, since the URLDownload uses stored proxy settings, see
        // preferences.storeProxyPreferences(...) below.
        storeProxySettings();

        URLDownload urlDownload;
        try {
            urlDownload = new URLDownload(testUrl);
            if (urlDownload.canBeReached()) {
                dialogService.showInformationDialogAndWait(dialogTitle, connectionSuccessText);
            } else {
                dialogService.showErrorDialogAndWait(dialogTitle, connectionFailedText);
            }
        } catch (MalformedURLException e) {
            // Why would that happen? Because one of developers inserted a failing url in testUrl...
        } catch (UnirestException e) {
            dialogService.showErrorDialogAndWait(dialogTitle, connectionFailedText);
        }

        preferences.storeProxyPreferences(initialProxyPreferences);
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    public BooleanProperty remoteServerProperty() {
        return remoteServerProperty;
    }

    public StringProperty remotePortProperty() {
        return remotePortProperty;
    }

    public BooleanProperty proxyUseProperty() {
        return proxyUseProperty;
    }

    public StringProperty proxyHostnameProperty() {
        return proxyHostnameProperty;
    }

    public StringProperty proxyPortProperty() {
        return proxyPortProperty;
    }

    public BooleanProperty proxyUseAuthenticationProperty() {
        return proxyUseAuthenticationProperty;
    }

    public StringProperty proxyUsernameProperty() {
        return proxyUsernameProperty;
    }

    public StringProperty proxyPasswordProperty() {
        return proxyPasswordProperty;
    }
}
