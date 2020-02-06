package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AppearanceTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();
    private final BooleanProperty themeLightProperty = new SimpleBooleanProperty();
    private final BooleanProperty themeDarkProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    private Validator fontSizeValidator;

    private List<String> restartWarnings = new ArrayList<>();

    public AppearanceTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        fontSizeValidator = new FunctionBasedValidator<>(
                fontSizeProperty,
                input -> {
                    try {
                        return Integer.parseInt(fontSizeProperty().getValue()) > 8;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Appearance"),
                        Localization.lang("Font settings"),
                        Localization.lang("You must enter an integer value higher than 8."))));
    }

    @Override
    public void setValues() {
        fontOverrideProperty.setValue(preferences.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE));
        fontSizeProperty.setValue(String.valueOf(preferences.getInt(JabRefPreferences.MAIN_FONT_SIZE)));

        switch (preferences.get(JabRefPreferences.FX_THEME)) {
            case ThemeLoader.DARK_CSS:
                themeLightProperty.setValue(false);
                themeDarkProperty.setValue(true);
                break;
            case ThemeLoader.MAIN_CSS:
            default:
                themeLightProperty.setValue(true);
                themeDarkProperty.setValue(false);
        }
    }

    @Override
    public void storeSettings() {
        if (preferences.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE) != fontOverrideProperty.getValue()) {
            restartWarnings.add(Localization.lang("Override font settings"));
            preferences.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE, fontOverrideProperty.getValue());
        }

        int newFontSize = Integer.parseInt(fontSizeProperty.getValue());
        if (preferences.getInt(JabRefPreferences.MAIN_FONT_SIZE) != newFontSize) {
            restartWarnings.add(Localization.lang("Override font size"));
            preferences.putInt(JabRefPreferences.MAIN_FONT_SIZE, newFontSize);
        }

        if (themeLightProperty.getValue() && !preferences.get(JabRefPreferences.FX_THEME).equals(ThemeLoader.MAIN_CSS)) {
            restartWarnings.add(Localization.lang("Theme changed to light theme."));
            preferences.put(JabRefPreferences.FX_THEME, ThemeLoader.MAIN_CSS);
        } else if (themeDarkProperty.getValue() && !preferences.get(JabRefPreferences.FX_THEME).equals(ThemeLoader.DARK_CSS)) {
            restartWarnings.add(Localization.lang("Theme changed to dark theme."));
            preferences.put(JabRefPreferences.FX_THEME, ThemeLoader.DARK_CSS);
        }
    }

    public ValidationStatus fontSizeValidationStatus() { return fontSizeValidator.getValidationStatus(); }

    @Override
    public boolean validateSettings() {
        if (fontOverrideProperty.getValue() && !fontSizeValidator.getValidationStatus().isValid()) {
            fontSizeValidator.getValidationStatus().getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() { return restartWarnings; }

    public BooleanProperty fontOverrideProperty() { return fontOverrideProperty; }

    public StringProperty fontSizeProperty() { return fontSizeProperty; }

    public BooleanProperty themeLightProperty() { return themeLightProperty; }

    public BooleanProperty themeDarkProperty() { return themeDarkProperty; }

}
