package org.jabref.gui.preferences;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class GeneralTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<Language> languagesListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();
    private final ListProperty<Charset> encodingsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>();
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty memoryStickModeProperty = new SimpleBooleanProperty();
    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();
    private final BooleanProperty allowIntegerEditionProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();
    private final BooleanProperty markOwnerProperty = new SimpleBooleanProperty();
    private final StringProperty markOwnerNameProperty = new SimpleStringProperty("");
    private final BooleanProperty markOwnerOverwriteProperty = new SimpleBooleanProperty();
    private final BooleanProperty markTimestampProperty = new SimpleBooleanProperty();
    private final StringProperty markTimeStampFormatProperty = new SimpleStringProperty("");
    private final BooleanProperty markTimeStampOverwriteProperty = new SimpleBooleanProperty();
    private final StringProperty markTimeStampFieldNameProperty = new SimpleStringProperty("");
    private final BooleanProperty updateTimeStampProperty = new SimpleBooleanProperty();

    private Validator markTimeStampFormatValidator;

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final GeneralPreferences initialGeneralPreferences;
    private final OwnerPreferences initialOwnerPreferences;
    private final TimestampPreferences initialTimestampPreferences;

    private List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.initialGeneralPreferences = preferencesService.getGeneralPreferences();
        this.initialOwnerPreferences = preferencesService.getOwnerPreferences();
        this.initialTimestampPreferences = preferencesService.getTimestampPreferences();

        markTimeStampFormatValidator = new FunctionBasedValidator<>(
                markTimeStampFormatProperty,
                input -> {
                    try {
                        DateTimeFormatter.ofPattern(markTimeStampFormatProperty.getValue());
                    } catch (IllegalArgumentException exception) {
                        return false;
                    }
                    return true;
                },
                ValidationMessage.error(String.format("%s > %s > %s %n %n %s",
                        Localization.lang("General"),
                        Localization.lang("Time stamp"),
                        Localization.lang("Date format"),
                        Localization.lang("Invalid date format")
                        )
                )
        );
    }

    public void setValues() {
        languagesListProperty.setValue(FXCollections.observableArrayList(Language.values()));
        selectedLanguageProperty.setValue(preferencesService.getLanguage());

        encodingsListProperty.setValue(FXCollections.observableArrayList(Encodings.getCharsets()));
        selectedEncodingProperty.setValue(initialGeneralPreferences.getDefaultEncoding());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(initialGeneralPreferences.getDefaultBibDatabaseMode());

        inspectionWarningDuplicateProperty.setValue(initialGeneralPreferences.isWarnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(initialGeneralPreferences.shouldConfirmDelete());
        allowIntegerEditionProperty.setValue(initialGeneralPreferences.shouldAllowIntegerEditionBibtex());
        memoryStickModeProperty.setValue(initialGeneralPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(preferencesService.shouldCollectTelemetry());
        showAdvancedHintsProperty.setValue(initialGeneralPreferences.shouldShowAdvancedHints());

        markOwnerProperty.setValue(initialOwnerPreferences.isUseOwner());
        markOwnerNameProperty.setValue(initialOwnerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(initialOwnerPreferences.isOverwriteOwner());

        markTimestampProperty.setValue(initialTimestampPreferences.isUseTimestamps());
        markTimeStampFormatProperty.setValue(initialTimestampPreferences.getTimestampFormat());
        markTimeStampOverwriteProperty.setValue(initialTimestampPreferences.isOverwriteTimestamp());
        markTimeStampFieldNameProperty.setValue(initialTimestampPreferences.getTimestampField().getName());
        updateTimeStampProperty.setValue(initialTimestampPreferences.isUpdateTimestamp());
    }

    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != preferencesService.getLanguage()) {
            preferencesService.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        if (initialGeneralPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        preferencesService.storeGeneralPreferences(new GeneralPreferences(
                selectedEncodingProperty.getValue(),
                selectedBiblatexModeProperty.getValue(),
                inspectionWarningDuplicateProperty.getValue(),
                confirmDeleteProperty.getValue(),
                allowIntegerEditionProperty.getValue(),
                memoryStickModeProperty.getValue(),
                collectTelemetryProperty.getValue(),
                showAdvancedHintsProperty.getValue()));

        preferencesService.storeOwnerPreferences(new OwnerPreferences(
                markOwnerProperty.getValue(),
                markOwnerNameProperty.getValue().trim(),
                markOwnerOverwriteProperty.getValue()));

        preferencesService.storeTimestampPreferences(new TimestampPreferences(
                markTimestampProperty.getValue(),
                updateTimeStampProperty.getValue(),
                FieldFactory.parseField(markTimeStampFieldNameProperty.getValue().trim()),
                markTimeStampFormatProperty.getValue().trim(),
                markTimeStampOverwriteProperty.getValue()));
    }

    public ValidationStatus markTimeStampFormatValidationStatus() {
        return markTimeStampFormatValidator.getValidationStatus();
    }

    public boolean validateSettings() {
        ValidationStatus validationStatus = markTimeStampFormatValidationStatus();
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

    // General

    public ListProperty<Language> languagesListProperty() {
        return this.languagesListProperty;
    }

    public ObjectProperty<Language> selectedLanguageProperty() {
        return this.selectedLanguageProperty;
    }

    public ListProperty<Charset> encodingsListProperty() {
        return this.encodingsListProperty;
    }

    public ObjectProperty<Charset> selectedEncodingProperty() {
        return this.selectedEncodingProperty;
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public BooleanProperty inspectionWarningDuplicateProperty() {
        return this.inspectionWarningDuplicateProperty;
    }

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty memoryStickModeProperty() {
        return this.memoryStickModeProperty;
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }

    public BooleanProperty allowIntegerEditionProperty() {
        return this.allowIntegerEditionProperty;
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return this.showAdvancedHintsProperty;
    }

    // Entry owner

    public BooleanProperty markOwnerProperty() {
        return this.markOwnerProperty;
    }

    public StringProperty markOwnerNameProperty() {
        return this.markOwnerNameProperty;
    }

    public BooleanProperty markOwnerOverwriteProperty() {
        return this.markOwnerOverwriteProperty;
    }

    // Time stamp

    public BooleanProperty markTimestampProperty() {
        return this.markTimestampProperty;
    }

    public StringProperty markTimeStampFormatProperty() {
        return this.markTimeStampFormatProperty;
    }

    public BooleanProperty markTimeStampOverwriteProperty() {
        return this.markTimeStampOverwriteProperty;
    }

    public StringProperty markTimeStampFieldNameProperty() {
        return this.markTimeStampFieldNameProperty;
    }

    public BooleanProperty updateTimeStampProperty() {
        return this.updateTimeStampProperty;
    }
}
