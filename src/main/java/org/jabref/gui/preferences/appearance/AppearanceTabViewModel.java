package org.jabref.gui.preferences.appearance;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SpinnerValueFactory;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;

import com.jthemedetecor.OsThemeDetector;
import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AppearanceTabViewModel implements PreferenceTabViewModel {

    public static SpinnerValueFactory<Integer> fontSizeValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(9, Integer.MAX_VALUE);

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();
    private final BooleanProperty themeLightProperty = new SimpleBooleanProperty();
    private final BooleanProperty themeDarkProperty = new SimpleBooleanProperty();

    private final BooleanProperty automaticThemeDetectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty themeCustomProperty = new SimpleBooleanProperty();
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty();

    // Signal for when RadioButton (gui/preferences/appearance/AppearanceTab.java) of Automatic Detection is selected.
    // Explicitly set boolean value after button selection to avoid disappearing preferences.
    private final BooleanProperty flagWhenAutomaticButtonSelected = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final AppearancePreferences appearancePreferences;

    private final Validator fontSizeValidator;
    private final Validator customPathToThemeValidator;

    public AppearanceTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.appearancePreferences = preferences.getAppearancePreferences();

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

        customPathToThemeValidator = new FunctionBasedValidator<>(
                customPathToThemeProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Appearance"),
                        Localization.lang("Visual theme"),
                        Localization.lang("Please specify a css theme file."))));
    }

    @Override
    public void setValues() {
        fontOverrideProperty.setValue(appearancePreferences.shouldOverrideDefaultFontSize());
        fontSizeProperty.setValue(String.valueOf(appearancePreferences.getMainFontSize()));

        // The light theme is in fact the absence of any theme modifying 'base.css'. Another embedded theme like
        // 'dark.css', stored in the classpath, can be introduced in {@link org.jabref.gui.theme.Theme}.
        Theme currentTheme = appearancePreferences.getTheme();

        // Current theme is 'Light'
        if (currentTheme.getType() == Theme.Type.DEFAULT) {
            // Override themeLightProperty when autoThemeDetection enabled.
            if (appearancePreferences.automaticThemeDetectionFlag().getValue()) {
                automaticThemeDetectionProperty.setValue(true);
                themeLightProperty.setValue(false);
            } else {
                themeLightProperty.setValue(true);
                automaticThemeDetectionProperty.setValue(false);
            }
            themeDarkProperty.setValue(false);
            themeCustomProperty.setValue(false);
        } else if (currentTheme.getType() == Theme.Type.EMBEDDED) {
            // Override themeDarkProperty when autoThemeDetection enabled.
            if (appearancePreferences.automaticThemeDetectionFlag().getValue()) {
                automaticThemeDetectionProperty.setValue(true);
                themeDarkProperty.setValue(false);
            } else {
                themeDarkProperty.setValue(true);
                automaticThemeDetectionProperty.setValue(false);
            }
            themeLightProperty.setValue(false);
            themeCustomProperty.setValue(false);
        } else {
            themeLightProperty.setValue(false);
            themeDarkProperty.setValue(false);
            themeCustomProperty.setValue(true);
            automaticThemeDetectionProperty.setValue(false);
            customPathToThemeProperty.setValue(currentTheme.getName());
        }
    }

    @Override
    public void storeSettings() {
        appearancePreferences.setShouldOverrideDefaultFontSize(fontOverrideProperty.getValue());
        appearancePreferences.setMainFontSize(Integer.parseInt(fontSizeProperty.getValue()));

        if (themeLightProperty.getValue()) {
            appearancePreferences.setTheme(Theme.light());
            // Check if user has enabled automatic detection. If so, change the flag in appearancePreferences to true
            // so that it knows to set the auto detection radio button to checked.
            appearancePreferences.setAutomaticThemeDetectionFlag(flagWhenAutomaticButtonSelected.getValue());
        } else if (themeDarkProperty.getValue()) {
            appearancePreferences.setTheme(Theme.dark());
            // Check if user has enabled automatic detection. If so, change the flag in appearancePreferences to true
            // so that it knows to set the auto detection radio button to checked.
            appearancePreferences.setAutomaticThemeDetectionFlag(flagWhenAutomaticButtonSelected.getValue());
        } else if (themeCustomProperty.getValue()) {
            appearancePreferences.setTheme(Theme.custom(customPathToThemeProperty.getValue()));
        } else if (automaticThemeDetectionProperty.getValue()) {
            // When user enables Automatic Theme Detection from OS, change theme if need be and update flag in
            // appearancePreferences so that the preference is acknowledged.
            appearancePreferences.setTheme(detectOSThemePreference());
            appearancePreferences.setAutomaticThemeDetectionFlag(true);
        }
    }

    /**
     * @return Theme to change UI to or remain with.
     * Local detector of OS theme preference to change JabRef theme upon user pressing 'Save' after enabling
     * automatic detection in JabRef Appearance Preferences.
     */
    public Theme detectOSThemePreference() {
        final OsThemeDetector detector = OsThemeDetector.getDetector();
        final boolean isDarkThemeUsed = detector.isDark();
        if (isDarkThemeUsed) {
            return Theme.dark();
        } else {
            return Theme.light();
        }
    }

    public ValidationStatus fontSizeValidationStatus() {
        return fontSizeValidator.getValidationStatus();
    }

    public ValidationStatus customPathToThemeValidationStatus() {
        return customPathToThemeValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        CompositeValidator validator = new CompositeValidator();

        if (fontOverrideProperty.getValue()) {
            validator.addValidators(fontSizeValidator);
        }

        if (themeCustomProperty.getValue()) {
            validator.addValidators(customPathToThemeValidator);
        }

        ValidationStatus validationStatus = validator.getValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    public BooleanProperty fontOverrideProperty() {
        return fontOverrideProperty;
    }

    public StringProperty fontSizeProperty() {
        return fontSizeProperty;
    }

    public BooleanProperty themeLightProperty() {
        flagWhenAutomaticButtonSelected.setValue(false);
        return themeLightProperty;
    }

    public BooleanProperty themeDarkProperty() {
        flagWhenAutomaticButtonSelected.setValue(false);
        return themeDarkProperty;
    }

    public BooleanProperty customThemeProperty() {
        flagWhenAutomaticButtonSelected.setValue(false);
        return themeCustomProperty;
    }

    public BooleanProperty automaticDetectionProperty() {
        flagWhenAutomaticButtonSelected.setValue(true);
        return automaticThemeDetectionProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public void importCSSFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.getLastPreferencesExportPath()).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file ->
                customPathToThemeProperty.setValue(file.toAbsolutePath().toString()));
    }
}
