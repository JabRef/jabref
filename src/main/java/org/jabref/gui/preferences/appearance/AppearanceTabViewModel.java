package org.jabref.gui.preferences.appearance;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SpinnerValueFactory;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.TelemetryPreferences;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AppearanceTabViewModel implements PreferenceTabViewModel {
    protected enum ThemeTypes {
        LIGHT(Localization.lang("Light")),
        DARK(Localization.lang("Dark")),
        CUSTOM(Localization.lang("Custom..."));

        private final String displayName;

        ThemeTypes(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    protected static SpinnerValueFactory<Integer> fontSizeValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(9, Integer.MAX_VALUE);

    private final ReadOnlyListProperty<Language> languagesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Language.values()));;
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();

    private final ReadOnlyListProperty<ThemeTypes> themesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemeTypes.values()));;
    private final ObjectProperty<ThemeTypes> selectedThemeProperty = new SimpleObjectProperty<>();
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty();

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();
    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();

    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final AppearancePreferences appearancePreferences;
    private final TelemetryPreferences telemetryPreferences;

    private final Validator fontSizeValidator;
    private final Validator customPathToThemeValidator;

    private final List<String> restartWarning = new ArrayList<>();

    public AppearanceTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.appearancePreferences = preferences.getAppearancePreferences();
        this.telemetryPreferences = preferences.getTelemetryPreferences();

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
        selectedLanguageProperty.setValue(appearancePreferences.getLanguage());

        // The light theme is in fact the absence of any theme modifying 'base.css'. Another embedded theme like
        // 'dark.css', stored in the classpath, can be introduced in {@link org.jabref.gui.theme.Theme}.
        switch (appearancePreferences.getTheme().getType()) {
            case DEFAULT -> selectedThemeProperty.setValue(ThemeTypes.LIGHT);
            case EMBEDDED -> selectedThemeProperty.setValue(ThemeTypes.DARK);
            case CUSTOM -> {
                selectedThemeProperty.setValue(ThemeTypes.CUSTOM);
                customPathToThemeProperty.setValue(appearancePreferences.getTheme().getName());
            }
        }

        fontOverrideProperty.setValue(appearancePreferences.shouldOverrideDefaultFontSize());
        fontSizeProperty.setValue(String.valueOf(appearancePreferences.getMainFontSize()));

        openLastStartupProperty.setValue(appearancePreferences.shouldOpenLastEdited());
        showAdvancedHintsProperty.setValue(appearancePreferences.shouldShowAdvancedHints());
        inspectionWarningDuplicateProperty.setValue(appearancePreferences.shouldWarnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(appearancePreferences.shouldConfirmDelete());

        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
    }

    @Override
    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != appearancePreferences.getLanguage()) {
            appearancePreferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        appearancePreferences.setShouldOverrideDefaultFontSize(fontOverrideProperty.getValue());
        appearancePreferences.setMainFontSize(Integer.parseInt(fontSizeProperty.getValue()));

        switch (selectedThemeProperty.get()) {
            case LIGHT -> appearancePreferences.setTheme(Theme.light());
            case DARK -> appearancePreferences.setTheme(Theme.dark());
            case CUSTOM -> appearancePreferences.setTheme(Theme.custom(customPathToThemeProperty.getValue()));
        }

        appearancePreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        appearancePreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());
        appearancePreferences.setWarnAboutDuplicatesInInspection(inspectionWarningDuplicateProperty.getValue());
        appearancePreferences.setConfirmDelete(confirmDeleteProperty.getValue());

        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());
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

        if (selectedThemeProperty.getValue() == ThemeTypes.CUSTOM) {
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

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    public ReadOnlyListProperty<Language> languagesListProperty() {
        return this.languagesListProperty;
    }

    public ObjectProperty<Language> selectedLanguageProperty() {
        return this.selectedLanguageProperty;
    }

    public ReadOnlyListProperty<ThemeTypes> themesListProperty() {
        return this.themesListProperty;
    }

    public ObjectProperty<ThemeTypes> selectedThemeProperty() {
        return this.selectedThemeProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public void importCSSFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.getInternalPreferences().getLastPreferencesExportPath()).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file ->
                customPathToThemeProperty.setValue(file.toAbsolutePath().toString()));
    }

    public BooleanProperty fontOverrideProperty() {
        return fontOverrideProperty;
    }

    public StringProperty fontSizeProperty() {
        return fontSizeProperty;
    }

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return this.showAdvancedHintsProperty;
    }

    public BooleanProperty inspectionWarningDuplicateProperty() {
        return this.inspectionWarningDuplicateProperty;
    }

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }
}
