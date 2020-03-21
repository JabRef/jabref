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
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.JabRefPreferences;

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
    private final BooleanProperty enforceLegalKeysProperty = new SimpleBooleanProperty();
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
    private final JabRefPreferences preferences;

    private List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

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
        selectedLanguageProperty.setValue(preferences.getLanguage());

        encodingsListProperty.setValue(FXCollections.observableArrayList(Encodings.getCharsets()));
        selectedEncodingProperty.setValue(preferences.getDefaultEncoding());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        if (preferences.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            selectedBiblatexModeProperty.setValue(BibDatabaseMode.BIBLATEX);
        } else {
            selectedBiblatexModeProperty.setValue(BibDatabaseMode.BIBTEX);
        }

        inspectionWarningDuplicateProperty.setValue(preferences.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        confirmDeleteProperty.setValue(preferences.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        enforceLegalKeysProperty.setValue(preferences.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        allowIntegerEditionProperty.setValue(preferences.getBoolean(JabRefPreferences.ALLOW_INTEGER_EDITION_BIBTEX));
        memoryStickModeProperty.setValue(preferences.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        collectTelemetryProperty.setValue(preferences.shouldCollectTelemetry());
        showAdvancedHintsProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS));

        markOwnerProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_OWNER));
        markOwnerNameProperty.setValue(preferences.get(JabRefPreferences.DEFAULT_OWNER));
        markOwnerOverwriteProperty.setValue(preferences.getBoolean(JabRefPreferences.OVERWRITE_OWNER));

        markTimestampProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        markTimeStampFormatProperty.setValue(preferences.get(JabRefPreferences.TIME_STAMP_FORMAT));
        markTimeStampOverwriteProperty.setValue(preferences.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        markTimeStampFieldNameProperty.setValue(preferences.get(JabRefPreferences.TIME_STAMP_FIELD));
        updateTimeStampProperty.setValue(preferences.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
    }

    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != preferences.getLanguage()) {
            preferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        preferences.setDefaultEncoding(selectedEncodingProperty.getValue());
        preferences.putBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE, selectedBiblatexModeProperty.getValue() == BibDatabaseMode.BIBLATEX);

        preferences.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarningDuplicateProperty.getValue());
        preferences.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDeleteProperty.getValue());
        preferences.putBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY, enforceLegalKeysProperty.getValue());
        preferences.putBoolean(JabRefPreferences.ALLOW_INTEGER_EDITION_BIBTEX, allowIntegerEditionProperty.getValue());
        if (preferences.getBoolean(JabRefPreferences.MEMORY_STICK_MODE) && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }
        preferences.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStickModeProperty.getValue());
        preferences.setShouldCollectTelemetry(collectTelemetryProperty.getValue());
        preferences.putBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS, showAdvancedHintsProperty.getValue());

        preferences.putBoolean(JabRefPreferences.USE_OWNER, markOwnerProperty.getValue());
        preferences.put(JabRefPreferences.DEFAULT_OWNER, markOwnerNameProperty.getValue().trim());
        preferences.putBoolean(JabRefPreferences.OVERWRITE_OWNER, markOwnerOverwriteProperty.getValue());

        preferences.putBoolean(JabRefPreferences.USE_TIME_STAMP, markTimestampProperty.getValue());
        preferences.put(JabRefPreferences.TIME_STAMP_FORMAT, markTimeStampFormatProperty.getValue().trim());
        preferences.put(JabRefPreferences.TIME_STAMP_FIELD, markTimeStampFieldNameProperty.getValue().trim());
        preferences.putBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP, markTimeStampOverwriteProperty.getValue());
        preferences.putBoolean(JabRefPreferences.UPDATE_TIMESTAMP, updateTimeStampProperty.getValue());
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
    public List<String> getRestartWarnings() { return restartWarning; }

    // General

    public ListProperty<Language> languagesListProperty() { return this.languagesListProperty; }

    public ObjectProperty<Language> selectedLanguageProperty() { return this.selectedLanguageProperty; }

    public ListProperty<Charset> encodingsListProperty() { return this.encodingsListProperty; }

    public ObjectProperty<Charset> selectedEncodingProperty() { return this.selectedEncodingProperty; }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() { return this.bibliographyModeListProperty; }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() { return this.selectedBiblatexModeProperty; }

    public BooleanProperty inspectionWarningDuplicateProperty() { return this.inspectionWarningDuplicateProperty; }

    public BooleanProperty confirmDeleteProperty() { return this.confirmDeleteProperty; }

    public BooleanProperty memoryStickModeProperty() { return this.memoryStickModeProperty; }

    public BooleanProperty collectTelemetryProperty() { return this.collectTelemetryProperty; }

    public BooleanProperty enforceLegalKeysProperty() { return this.enforceLegalKeysProperty; }

    public BooleanProperty allowIntegerEditionProperty() { return this.allowIntegerEditionProperty; }

    public BooleanProperty showAdvancedHintsProperty() { return this.showAdvancedHintsProperty; }

    // Entry owner

    public BooleanProperty markOwnerProperty() { return this.markOwnerProperty; }

    public StringProperty markOwnerNameProperty() { return this.markOwnerNameProperty; }

    public BooleanProperty markOwnerOverwriteProperty() { return this.markOwnerOverwriteProperty; }

    // Time stamp

    public BooleanProperty markTimestampProperty() { return this.markTimestampProperty; }

    public StringProperty markTimeStampFormatProperty() { return this.markTimeStampFormatProperty; }

    public BooleanProperty markTimeStampOverwriteProperty() { return this.markTimeStampOverwriteProperty; }

    public StringProperty markTimeStampFieldNameProperty() { return this.markTimeStampFieldNameProperty; }

    public BooleanProperty updateTimeStampProperty() { return this.updateTimeStampProperty; }
}
