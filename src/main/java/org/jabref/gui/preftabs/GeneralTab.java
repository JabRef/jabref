package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import javax.swing.JPanel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;



import static org.jabref.logic.l10n.Languages.LANGUAGES;

class GeneralTab extends JPanel implements PrefsTab {


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

    private final TextField timeStampFormat;
    private final TextField timeStampField;
    private final JabRefPreferences prefs;

    private ObservableList<String> options = FXCollections.observableArrayList(LANGUAGES.keySet().toArray(new String[LANGUAGES.keySet().size()]));
    private final ComboBox<? extends String> language = new ComboBox<>(options);
    private final ComboBox<Charset> encodings;
    private final ComboBox<BibDatabaseMode> biblatexMode;
    private final DialogService dialogService;

    public GeneralTab(DialogService dialogService, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.dialogService = dialogService;
        setLayout(new BorderLayout());

        biblatexMode = new ComboBox<>(FXCollections.observableArrayList(BibDatabaseMode.values()));


        memoryStick = new CheckBox(Localization.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        useOwner = new CheckBox(Localization.lang("Mark new entries with owner name") + ':');
        updateTimeStamp = new CheckBox(Localization.lang("Update timestamp on modification"));
        useTimeStamp = new CheckBox(Localization.lang("Mark new entries with addition date") + ". "
                + Localization.lang("Date format") + ':');
        if (!useTimeStamp.isSelected()) {
            updateTimeStamp.setDisable(true);
        }
        useTimeStamp.setOnAction(e->updateTimeStamp.setDisable(!useTimeStamp.isSelected()));
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


        GridPane builder = new GridPane();
        builder.add(new Label(Localization.lang("General")),1,1);
        builder.add(new Separator(),2,1);
        builder.add(new Line(),2,2);
        builder.add(inspectionWarnDupli, 2,3);
        builder.add(new Line(),2,4);
        builder.add(confirmDelete, 2,5);
        builder.add(new Line(),2,6);
        builder.add(enforceLegalKeys, 2,7);
        builder.add(new Line(),2,8);
        builder.add(memoryStick, 2,9);

        // Create a new panel with its own FormLayout for the last items:
        builder.add(useOwner, 2,10);
        builder.add(defOwnerField,3,10);
        builder.add(overwriteOwner,4,10);

        Button help = new Button("Help");
        help.setOnAction(event -> new HelpAction(HelpFile.OWNER).getHelpButton().doClick());
        builder.add(help,5,10);


        builder.add(useTimeStamp, 2,13);
        builder.add(timeStampFormat,3,13);
        builder.add(overwriteTimeStamp,4,13);
        builder.add(new Label(Localization.lang("Field name") + ':'),6,13);
        builder.add(timeStampField,7,13);

        Button help1 = new Button("Help");
        help1.setOnAction(event -> new HelpAction(HelpFile.TIMESTAMP).getHelpButton().doClick());
        builder.add(help1,8,13);


        builder.add(updateTimeStamp, 2,14);
        builder.add(new Line(),2,15);

        builder.add(shouldCollectTelemetry,2,15);
        builder.add(new Line(),2,16);
        Label lab;
        lab = new Label(Localization.lang("Language") + ':');
        builder.add(lab, 1,17);
        builder.add(language,2,17);
        builder.add(new Line(),2,18);
        lab = new Label(Localization.lang("Default encoding") + ':');
        builder.add(lab, 1,19);
        builder.add(encodings,2,19);

        builder.add(new Label(Localization.lang("Default bibliography mode")),1,20);

        builder.add(biblatexMode,2,21);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {
        useOwner.setSelected(prefs.getBoolean(JabRefPreferences.USE_OWNER));
        overwriteOwner.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER));
        useTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        overwriteTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        updateTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
        updateTimeStamp.setSelected(useTimeStamp.isSelected());
        enforceLegalKeys.setSelected(prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        shouldCollectTelemetry.setSelected(prefs.shouldCollectTelemetry());
        memoryStick.setSelected(prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        confirmDelete.setSelected(prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        defOwnerField.setText(prefs.get(JabRefPreferences.DEFAULT_OWNER));
        timeStampFormat.setText(prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        timeStampField.setText(prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        inspectionWarnDupli.setSelected(prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            biblatexMode.setValue(BibDatabaseMode.BIBLATEX);
        } else {
            biblatexMode.setValue(BibDatabaseMode.BIBTEX);
        }

        Charset enc = Globals.prefs.getDefaultEncoding();
        encodings.setValue(enc);

        String oldLan = prefs.get(JabRefPreferences.LANGUAGE);

        // Language choice
        int ilk = 0;
        for (String lan : LANGUAGES.values()) {
            if (lan.equals(oldLan)) {
                language.setVisibleRowCount(ilk);
            }
            ilk++;
        }

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

            DefaultTaskExecutor.runInJavaFXThread(()->dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef.")));
        }
        prefs.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStick.isSelected());
        prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDelete.isSelected());
        prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarnDupli.isSelected());
        String owner = defOwnerField.getText().trim();
        prefs.put(JabRefPreferences.DEFAULT_OWNER, owner);
        prefs.put(JabRefPreferences.TIME_STAMP_FORMAT, timeStampFormat.getText().trim());
        prefs.put(JabRefPreferences.TIME_STAMP_FIELD, timeStampField.getText().trim());
        // Update name of the time stamp field based on preferences
        InternalBibtexFields.updateTimeStampField(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        prefs.setDefaultEncoding((Charset) encodings.getItems());
        prefs.putBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE, biblatexMode.getItems().equals(BibDatabaseMode.BIBLATEX));

        if (!LANGUAGES.get(language.getItems().toString()).equals(prefs.get(JabRefPreferences.LANGUAGE))) {
            prefs.put(JabRefPreferences.LANGUAGE, LANGUAGES.get(language.getItems().toString()));
            Localization.setLanguage(LANGUAGES.get(language.getItems().toString()));
            // Update any defaults that might be language dependent:
            Globals.prefs.setLanguageDependentDefaultValues();
            // Warn about restart needed:

            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showWarningDialogAndWait(Localization.lang("Changed language settings"),
                    Localization.lang("You have changed the language setting.")
                            .concat(" ")
                            .concat(Localization.lang("You must restart JabRef for this to come into effect."))));
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
