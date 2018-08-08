package org.jabref.gui.preftabs;

import java.awt.BorderLayout;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.externalfiletype.ExternalFileTypeEditor;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationSettings;
import org.jabref.gui.push.PushToApplicationSettingsDialog;
import org.jabref.gui.push.PushToApplications;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

class ExternalTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final TextField emailSubject;
    private final TextField citeCommand;
    private final CheckBox openFoldersOfAttachedFiles;

    private final RadioButton defaultConsole;
    private final RadioButton executeConsole;
    private final TextField consoleCommand;
    private final Button browseButton;

    private final RadioButton adobeAcrobatReader;
    private final RadioButton sumatraReader;
    private final TextField adobeAcrobatReaderPath;
    private final TextField sumatraReaderPath;

    public ExternalTab(JabRefFrame frame, PreferencesDialog prefsDiag, JabRefPreferences prefs) {
        this.prefs = prefs;

        setLayout(new BorderLayout());

        Button editFileTypes = new Button(Localization.lang("Manage external file types"));
        citeCommand = new TextField();
        editFileTypes.setOnAction(e->ExternalFileTypeEditor.getAction());

        defaultConsole = new RadioButton(Localization.lang("Use default terminal emulator"));
        executeConsole = new RadioButton(Localization.lang("Execute command") + ":");
        consoleCommand = new TextField();
        browseButton = new Button(Localization.lang("Browse"));

        adobeAcrobatReader = new RadioButton(Localization.lang("Adobe Acrobat Reader"));
        adobeAcrobatReaderPath = new TextField();
        Button browseAdobeAcrobatReader = new Button(Localization.lang("Browse"));

        sumatraReader = new RadioButton(Localization.lang("Sumatra Reader"));
        sumatraReaderPath = new TextField();
        Button browseSumatraReader = new Button(Localization.lang("Browse"));

        Label commandDescription = new Label(Localization.lang("Note: Use the placeholder %0 for the location of the opened library file.", "%DIR"));

        defaultConsole.setOnAction(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        executeConsole.setOnAction(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        browseButton.setOnAction(e -> showConsoleChooser());

        browseAdobeAcrobatReader.setOnAction(e -> showAdobeChooser());

        GridPane consoleOptionPanel = new GridPane();
        consoleOptionPanel.add(defaultConsole, 1,1);
        consoleOptionPanel.add(executeConsole, 1,2);
        consoleOptionPanel.add(consoleCommand, 2,2);
        consoleOptionPanel.add(browseButton, 3,2);
        consoleOptionPanel.add(commandDescription, 2,3);

        GridPane pdfOptionPanel = new GridPane();
        pdfOptionPanel.add(adobeAcrobatReader, 1,1);
        pdfOptionPanel.add(adobeAcrobatReaderPath, 2,1);
        pdfOptionPanel.add(browseAdobeAcrobatReader, 3,1);

        if (OS.WINDOWS) {
            browseSumatraReader.setOnAction(e -> showSumatraChooser());
            pdfOptionPanel.add(sumatraReader, 1,2);
            pdfOptionPanel.add(sumatraReaderPath, 2,2);
            pdfOptionPanel.add(browseSumatraReader, 3,2);
        }

        GridPane builder = new GridPane();

        builder.add(new Label(Localization.lang("Sending of emails")),1,1);
        Label lab = new Label(Localization.lang("    Subject for sending an email with references").concat(":"));
        builder.add(lab,1,2);
        emailSubject = new TextField();
        builder.add(emailSubject,2,2);
        openFoldersOfAttachedFiles = new CheckBox(Localization.lang("Automatically open folders of attached files"));
        builder.add(openFoldersOfAttachedFiles,1,3);


        builder.add(new Label(Localization.lang("External programs")),1,4);


        GridPane butpan = new GridPane();
        int index = 0;
        for (PushToApplication pushToApplication : frame.getPushApplications().getApplications()) {
            addSettingsButton(pushToApplication, butpan, index);
            index++;
        }

        builder.add(butpan,1,5);


        lab = new Label(Localization.lang("Cite command") + ':');
        builder.add(lab,1,6);
        builder.add(citeCommand,2,6);


        builder.add(editFileTypes,1,7);

        builder.add(new Label(Localization.lang("Open console")),1,8);

        builder.add(consoleOptionPanel,1,9);


        builder.add(new Label(Localization.lang("Open PDF")),1,10);

        builder.add(pdfOptionPanel,1,11);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void addSettingsButton(final PushToApplication application, GridPane panel, int index) {
        PushToApplicationSettings settings = PushToApplications.getSettings(application);
        Button button = new Button(Localization.lang("Settings for %0", application.getApplicationName()));
        button.setOnAction(e -> PushToApplicationSettingsDialog.showSettingsDialog(null, settings));
        if (index % 2 == 0){
            panel.add(button,1,index/2+1);
        }
        else {
            panel.add(button,2,index/2+1);
        }
    }

    @Override
    public void setValues() {

        emailSubject.setText(prefs.get(JabRefPreferences.EMAIL_SUBJECT));
        openFoldersOfAttachedFiles.setSelected(prefs.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        citeCommand.setText(prefs.get(JabRefPreferences.CITE_COMMAND));

        defaultConsole.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));
        executeConsole.setSelected(!Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));

        consoleCommand.setText(Globals.prefs.get(JabRefPreferences.CONSOLE_COMMAND));

        adobeAcrobatReaderPath.setText(Globals.prefs.get(JabRefPreferences.ADOBE_ACROBAT_COMMAND));
        if (OS.WINDOWS) {
            sumatraReaderPath.setText(Globals.prefs.get(JabRefPreferences.SUMATRA_PDF_COMMAND));

            if (Globals.prefs.get(JabRefPreferences.USE_PDF_READER).equals(adobeAcrobatReaderPath.getText())) {
                adobeAcrobatReader.setSelected(true);
            } else if (Globals.prefs.get(JabRefPreferences.USE_PDF_READER).equals(sumatraReaderPath.getText())) {
                sumatraReader.setSelected(true);
            }
        }

        updateExecuteConsoleButtonAndFieldEnabledState();
    }

    @Override
    public void storeSettings() {
        prefs.put(JabRefPreferences.EMAIL_SUBJECT, emailSubject.getText());
        prefs.putBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, openFoldersOfAttachedFiles.isSelected());
        prefs.put(JabRefPreferences.CITE_COMMAND, citeCommand.getText());
        prefs.putBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION, defaultConsole.isSelected());
        prefs.put(JabRefPreferences.CONSOLE_COMMAND, consoleCommand.getText());
        prefs.put(JabRefPreferences.ADOBE_ACROBAT_COMMAND, adobeAcrobatReaderPath.getText());
        if (OS.WINDOWS) {
            prefs.put(JabRefPreferences.SUMATRA_PDF_COMMAND, sumatraReaderPath.getText());
        }
        readerSelected();
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    private void updateExecuteConsoleButtonAndFieldEnabledState() {
        browseButton.setDisable(!executeConsole.isSelected());
        consoleCommand.setDisable(!executeConsole.isSelected());
    }

    private void showConsoleChooser() {
        JFileChooser consoleChooser = new JFileChooser();
        int answer = consoleChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            consoleCommand.setText(consoleChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void showAdobeChooser() {
        JFileChooser adobeChooser = new JFileChooser();
        int answer = adobeChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            adobeAcrobatReaderPath.setText(adobeChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void showSumatraChooser() {
        JFileChooser adobeChooser = new JFileChooser();
        int answer = adobeChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            sumatraReaderPath.setText(adobeChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void readerSelected() {
        if (adobeAcrobatReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, adobeAcrobatReaderPath.getText());
        } else if (sumatraReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, sumatraReaderPath.getText());
        }
    }
}
