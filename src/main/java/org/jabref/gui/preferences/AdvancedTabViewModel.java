package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.remote.JabRefMessageHandler;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.RemoteUtil;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AdvancedTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty remoteServerProperty = new SimpleBooleanProperty();
    private final StringProperty remotePortProperty = new SimpleStringProperty("");
    private final BooleanProperty useIEEELatexAbbreviationsProperty = new SimpleBooleanProperty();
    private final BooleanProperty useCaseKeeperProperty = new SimpleBooleanProperty();
    private final BooleanProperty useUnitFormatterProperty = new SimpleBooleanProperty();
    private final BooleanProperty proxyUseProperty = new SimpleBooleanProperty();
    private final StringProperty proxyHostnameProperty = new SimpleStringProperty("");
    private final StringProperty proxyPortProperty = new SimpleStringProperty("");
    private final BooleanProperty proxyUseAuthenticationProperty = new SimpleBooleanProperty();
    private final StringProperty proxyUsernameProperty = new SimpleStringProperty("");
    private final StringProperty proxyPasswordProperty = new SimpleStringProperty("");

    private Validator remotePortValidator;
    private Validator proxyHostnameValidator;
    private Validator proxyPortValidator;
    private Validator proxyUsernameValidator;
    private Validator proxyPasswordValidator;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final RemotePreferences remotePreferences;
    private final ProxyPreferences proxyPreferences;

    private List<String> restartWarning = new ArrayList<>();

    public AdvancedTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.remotePreferences = preferences.getRemotePreferences();
        this.proxyPreferences = preferences.getProxyPreferences();

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
                        Localization.lang("Advanced"),
                        Localization.lang("Remote operation"),
                        Localization.lang("You must enter an integer value in the interval 1025-65535"))));

        proxyHostnameValidator = new FunctionBasedValidator<>(
                proxyHostnameProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Advanced"),
                        Localization.lang("Network"),
                        Localization.lang("Please specify a hostname"))));

        proxyPortValidator = new FunctionBasedValidator<>(
                proxyPortProperty,
                input -> getPortAsInt(input).isPresent(),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Advanced"),
                        Localization.lang("Network"),
                        Localization.lang("Please specify a port"))));

        proxyUsernameValidator = new FunctionBasedValidator<>(
                proxyUsernameProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Advanced"),
                        Localization.lang("Network"),
                        Localization.lang("Please specify a username"))));

        proxyPasswordValidator = new FunctionBasedValidator<>(
                proxyPasswordProperty,
                input -> input.length() > 0,
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Advanced"),
                        Localization.lang("Network"),
                        Localization.lang("Please specify a password"))));
    }

    public void setValues() {
        remoteServerProperty.setValue(remotePreferences.useRemoteServer());
        remotePortProperty.setValue(String.valueOf(remotePreferences.getPort()));

        useIEEELatexAbbreviationsProperty.setValue(preferences.getJournalAbbreviationPreferences().useIEEEAbbreviations());

        useCaseKeeperProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH));
        useUnitFormatterProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH));

        proxyUseProperty.setValue(proxyPreferences.isUseProxy());
        proxyHostnameProperty.setValue(proxyPreferences.getHostname());
        proxyPortProperty.setValue(proxyPreferences.getPort());
        proxyUseAuthenticationProperty.setValue(proxyPreferences.isUseAuthentication());
        proxyUsernameProperty.setValue(proxyPreferences.getUsername());
        proxyPasswordProperty.setValue(proxyPreferences.getPassword());
    }

    public void storeSettings() {
        storeRemoteSettings();

        JournalAbbreviationPreferences journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
        if (journalAbbreviationPreferences.useIEEEAbbreviations() != useIEEELatexAbbreviationsProperty.getValue()) {
            journalAbbreviationPreferences.setUseIEEEAbbreviations(useIEEELatexAbbreviationsProperty.getValue());
            preferences.storeJournalAbbreviationPreferences(journalAbbreviationPreferences);
            Globals.journalAbbreviationLoader.update(journalAbbreviationPreferences);
        }

        preferences.putBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH, useCaseKeeperProperty.getValue());
        preferences.putBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH, useUnitFormatterProperty.getValue());

        storeProxySettings();
    }

    private void storeRemoteSettings() {
        RemotePreferences newRemotePreferences = new RemotePreferences(
                remotePreferences.getPort(),
                remoteServerProperty.getValue()
        );

        getPortAsInt(remotePortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentPort(newPort)) {
                newRemotePreferences.setPort(newPort);

                if (newRemotePreferences.useRemoteServer()) {
                    restartWarning.add(Localization.lang("Remote server port") + ": " + newPort);
                }
            }
        });

        if (newRemotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), remotePreferences.getPort());
        } else {
            Globals.REMOTE_LISTENER.stop();
        }

        preferences.setRemotePreferences(newRemotePreferences);
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

        if (!newProxyPreferences.equals(proxyPreferences)) {
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

    public ValidationStatus remotePortValidationStatus() { return remotePortValidator.getValidationStatus(); }

    public ValidationStatus proxyHostnameValidationStatus() { return proxyHostnameValidator.getValidationStatus(); }

    public ValidationStatus proxyPortValidationStatus() { return proxyPortValidator.getValidationStatus(); }

    public ValidationStatus proxyUsernameValidationStatus() { return proxyUsernameValidator.getValidationStatus(); }

    public ValidationStatus proxyPasswordValidationStatus() { return proxyPasswordValidator.getValidationStatus(); }

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

    @Override
    public List<String> getRestartWarnings() { return restartWarning; }

    public BooleanProperty remoteServerProperty() { return remoteServerProperty; }

    public StringProperty remotePortProperty() { return remotePortProperty; }

    public BooleanProperty useIEEELatexAbbreviationsProperty() { return useIEEELatexAbbreviationsProperty; }

    public BooleanProperty useCaseKeeperProperty() { return useCaseKeeperProperty; }

    public BooleanProperty useUnitFormatterProperty() { return useUnitFormatterProperty; }

    public BooleanProperty proxyUseProperty() { return proxyUseProperty; }

    public StringProperty proxyHostnameProperty() { return proxyHostnameProperty; }

    public StringProperty proxyPortProperty() { return proxyPortProperty; }

    public BooleanProperty proxyUseAuthenticationProperty() { return proxyUseAuthenticationProperty; }

    public StringProperty proxyUsernameProperty() { return proxyUsernameProperty; }

    public StringProperty proxyPasswordProperty() { return proxyPasswordProperty; }
}
