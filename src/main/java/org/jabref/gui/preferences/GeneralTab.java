package org.jabref.gui.preferences;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import static javafx.beans.binding.Bindings.not;

class GeneralTab extends Pane implements PrefsTab {

    private final CheckBox useOwner;
    private final CheckBox overwriteOwner;
    private final CheckBox enforceLegalKeys;
    private final CheckBox shouldCollectTelemetry;
    private final CheckBox confirmDelete;
    private final CheckBox memoryStick;
    private final CheckBox inspectionWarnDupli;
    private final CheckBox useTimeStamp;
    private final CheckBox updateTimeStamp;
    private final CheckBox overwriteTimeStamp;
    private final TextField defOwnerField;
    private final GridPane builder = new GridPane();

    private final TextField timeStampFormat;
    private final TextField timeStampField;
    private final JabRefPreferences prefs;

    private final ComboBox<Language> languageSelection = new ComboBox<>();
    private final ComboBox<Charset> encodings;
    private final ComboBox<BibDatabaseMode> biblatexMode;
    private final DialogService dialogService;

    public GeneralTab(DialogService dialogService, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.dialogService = dialogService;
        builder.setVgap(7);

        ActionFactory factory = new ActionFactory(prefs.getKeyBindingRepository());

        biblatexMode = new ComboBox<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
        memoryStick = new CheckBox(Localization.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        useOwner = new CheckBox(Localization.lang("Mark new entries with owner name") + ':');
        updateTimeStamp = new CheckBox(Localization.lang("Update timestamp on modification"));
        useTimeStamp = new CheckBox(Localization.lang("Mark new entries with addition date") + ". "
                + Localization.lang("Date format") + ':');
        updateTimeStamp.disableProperty().bind(not(useTimeStamp.selectedProperty()));
        overwriteOwner = new CheckBox(Localization.lang("Overwrite"));
        overwriteTimeStamp = new CheckBox(Localization.lang("If a pasted or imported entry already has the field set, overwrite."));
        enforceLegalKeys = new CheckBox(Localization.lang("Enforce legal characters in BibTeX keys"));
        confirmDelete = new CheckBox(Localization.lang("Show confirmation dialog when deleting entries"));
        defOwnerField = new TextField();
        timeStampFormat = new TextField();
        timeStampField = new TextField();
        inspectionWarnDupli = new CheckBox(Localization.lang("Warn about unresolved duplicates when closing inspection window"));
        shouldCollectTelemetry = new CheckBox(Localization.lang("Collect and share telemetry data to help improve JabRef."));
        encodings = new ComboBox<>(FXCollections.observableArrayList(Encodings.ENCODINGS));

        Label general = new Label(Localization.lang("General"));
        general.getStyleClass().add("sectionHeader");
        builder.add(general, 1, 1);
        builder.add(inspectionWarnDupli, 1, 3);
        builder.add(confirmDelete, 1, 5);
        builder.add(enforceLegalKeys, 1, 7);
        builder.add(memoryStick, 1, 9);

        // Owner name
        HBox ownerBox = new HBox();
        ownerBox.setAlignment(Pos.CENTER_LEFT);
        ownerBox.setSpacing(7);
        Button helpOwner = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.OWNER));
        ownerBox.getChildren().addAll(useOwner, defOwnerField, overwriteOwner, helpOwner);
        builder.add(ownerBox, 1, 10);

        builder.add(useTimeStamp, 1, 14);
        builder.add(timeStampFormat, 1, 16);
        builder.add(overwriteTimeStamp, 1, 17);
        Label fieldName = new Label(Localization.lang("Field name") + ':');
        builder.add(fieldName, 1, 19);
        builder.add(timeStampField, 1, 21);

        Button helpTimestamp = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.TIMESTAMP));
        builder.add(helpTimestamp, 1, 22);
        builder.add(updateTimeStamp, 1, 23);
        builder.add(shouldCollectTelemetry, 1, 25);

        // Language configuration
        HBox languageBox = new HBox();
        languageBox.setSpacing(115);
        languageBox.setAlignment(Pos.CENTER_LEFT);
        Label languageLabel = new Label(Localization.lang("Language") + ':');
        languageSelection.setItems(FXCollections.observableArrayList(Language.values()));
        new ViewModelListCellFactory<Language>()
                .withText(Language::getDisplayName)
                .install(languageSelection);
        languageBox.getChildren().addAll(languageLabel, languageSelection);
        builder.add(languageBox, 1, 27);

        // Encoding configuration
        HBox encodingBox = new HBox();
        encodingBox.setSpacing(68);
        encodingBox.setAlignment(Pos.CENTER_LEFT);
        Label defaultEncoding = new Label(Localization.lang("Default encoding") + ':');
        encodingBox.getChildren().addAll(defaultEncoding, encodings);
        builder.add(encodingBox, 1, 28);

        // Bibliography mode configuration
        HBox biblioBox = new HBox();
        biblioBox.setSpacing(10);
        biblioBox.setAlignment(Pos.CENTER_LEFT);
        Label defaultBibliographyMode = new Label(Localization.lang("Default bibliography mode"));
        biblioBox.getChildren().addAll(defaultBibliographyMode, biblatexMode);
        builder.add(biblioBox, 1, 29);
    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        useOwner.setSelected(prefs.getBoolean(JabRefPreferences.USE_OWNER));
        overwriteOwner.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER));
        useTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        overwriteTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        updateTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
        enforceLegalKeys.setSelected(prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        shouldCollectTelemetry.setSelected(prefs.shouldCollectTelemetry());
        memoryStick.setSelected(prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        confirmDelete.setSelected(prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        defOwnerField.setText(prefs.get(JabRefPreferences.DEFAULT_OWNER));
        timeStampFormat.setText(prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        timeStampField.setText(prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        inspectionWarnDupli.setSelected(prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        if (prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            biblatexMode.setValue(BibDatabaseMode.BIBLATEX);
        } else {
            biblatexMode.setValue(BibDatabaseMode.BIBTEX);
        }
        encodings.setValue(prefs.getDefaultEncoding());
        languageSelection.setValue(prefs.getLanguage());
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.USE_OWNER, useOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_OWNER, overwriteOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.USE_TIME_STAMP, useTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP, overwriteTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.UPDATE_TIMESTAMP, updateTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY, enforceLegalKeys.isSelected());
        prefs.setShouldCollectTelemetry(shouldCollectTelemetry.isSelected());
        if (prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE) && !memoryStick.isSelected()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }
        prefs.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStick.isSelected());
        prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDelete.isSelected());
        prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarnDupli.isSelected());
        String owner = defOwnerField.getText().trim();
        prefs.put(JabRefPreferences.DEFAULT_OWNER, owner);
        prefs.put(JabRefPreferences.TIME_STAMP_FORMAT, timeStampFormat.getText().trim());
        prefs.put(JabRefPreferences.TIME_STAMP_FIELD, timeStampField.getText().trim());
        // Update name of the time stamp field based on preferences
        InternalBibtexFields.updateTimeStampField(prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        prefs.setDefaultEncoding(encodings.getValue());
        prefs.putBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE, biblatexMode.getValue() == BibDatabaseMode.BIBLATEX);

        if (languageSelection.getValue() != prefs.getLanguage()) {
            prefs.setLanguage(languageSelection.getValue());
            Localization.setLanguage(languageSelection.getValue());

            // Warn about restart needed:
            dialogService.showWarningDialogAndWait(Localization.lang("Changed language settings"),
                    Localization.lang("You have changed the language setting.")
                                .concat(" ")
                                .concat(Localization.lang("You must restart JabRef for this to come into effect.")));
        }
    }

    @Override
    public boolean validateSettings() {
        try {
            // Test if date format is legal:
            DateTimeFormatter.ofPattern(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Invalid date format"),
                    Localization.lang("The chosen date format for new entries is not valid")));

            return false;
        }
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }
}
