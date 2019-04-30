package org.jabref.gui.preferences;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.EditExternalFileTypesAction;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationSettings;
import org.jabref.gui.push.PushToApplicationSettingsDialog;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

class ExternalTab implements PrefsTab {

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

    private final RadioButton defaultFileBrowser;
    private final RadioButton executeFileBrowser;
    private final TextField fileBrowserCommand;
    private final Button fileBrowserButton;

    private final GridPane builder = new GridPane();
    private final DialogService dialogService;
    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().build();

    public ExternalTab(JabRefFrame frame, PreferencesDialog prefsDiag, JabRefPreferences prefs) {
        this.prefs = prefs;
        dialogService = frame.getDialogService();
        builder.setVgap(7);

        Button editFileTypes = new Button(Localization.lang("Manage external file types"));
        citeCommand = new TextField();
        editFileTypes.setOnAction(e -> new EditExternalFileTypesAction().execute());
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

        defaultFileBrowser = new RadioButton(Localization.lang("Use default file browser"));
        executeFileBrowser = new RadioButton(Localization.lang("Execute Command"));
        fileBrowserCommand = new TextField();
        fileBrowserButton = new Button(Localization.lang("Browser"));

        Label commandDescription = new Label(Localization.lang("Note: Use the placeholder %0 for the location of the opened library file.", "%DIR"));
        defaultConsole.setOnAction(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        executeConsole.setOnAction(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        browseButton.setOnAction(e -> showConsoleChooser());

        fileBrowserButton.disableProperty().bind(executeFileBrowser.selectedProperty().not());
        fileBrowserCommand.disableProperty().bind(executeFileBrowser.selectedProperty().not());
        fileBrowserButton.setOnAction(e -> showFileBrowserCommandChooser());

        browseAdobeAcrobatReader.setOnAction(e -> showAdobeChooser());

        GridPane consoleOptionPanel = new GridPane();
        final ToggleGroup consoleGroup = new ToggleGroup();
        defaultConsole.setToggleGroup(consoleGroup);
        executeConsole.setToggleGroup(consoleGroup);
        consoleOptionPanel.add(defaultConsole, 1, 1);
        consoleOptionPanel.add(executeConsole, 1, 2);
        consoleOptionPanel.add(consoleCommand, 2, 2);
        consoleOptionPanel.add(browseButton, 3, 2);
        consoleOptionPanel.add(commandDescription, 2, 3);

        GridPane pdfOptionPanel = new GridPane();
        final ToggleGroup pdfReaderGroup = new ToggleGroup();
        pdfOptionPanel.add(adobeAcrobatReader, 1, 1);
        pdfOptionPanel.add(adobeAcrobatReaderPath, 2, 1);
        adobeAcrobatReader.setToggleGroup(pdfReaderGroup);
        pdfOptionPanel.add(browseAdobeAcrobatReader, 3, 1);

        Label fileBrowserCommandDescription = new Label(Localization.lang("Note: Use the placeholder %0 for the location of the opened library file.", "%DIR"));
        GridPane fileBrowserOptionPanel = new GridPane();
        final ToggleGroup fileBrowserGroup = new ToggleGroup();
        defaultFileBrowser.setToggleGroup(fileBrowserGroup);
        executeFileBrowser.setToggleGroup(fileBrowserGroup);
        fileBrowserOptionPanel.add(defaultFileBrowser, 1, 1);
        fileBrowserOptionPanel.add(executeFileBrowser, 1, 2);
        fileBrowserOptionPanel.add(fileBrowserCommand, 2, 2);
        fileBrowserOptionPanel.add(fileBrowserButton, 3, 2);
        fileBrowserOptionPanel.add(fileBrowserCommandDescription, 2, 3);

        if (OS.WINDOWS) {
            browseSumatraReader.setOnAction(e -> showSumatraChooser());
            pdfOptionPanel.add(sumatraReader, 1, 2);
            sumatraReader.setToggleGroup(pdfReaderGroup);
            pdfOptionPanel.add(sumatraReaderPath, 2, 2);
            pdfOptionPanel.add(browseSumatraReader, 3, 2);
        }

        // Sending of emails title
        Label sendingOfEmails = new Label(Localization.lang("Sending of emails"));
        sendingOfEmails.getStyleClass().add("sectionHeader");
        builder.add(sendingOfEmails, 1, 1);

        // Sending of emails configuration
        HBox sendRefMailBox = new HBox();
        sendRefMailBox.setSpacing(8);
        sendRefMailBox.setAlignment(Pos.CENTER_LEFT);
        Label subject = new Label(Localization.lang("Subject for sending an email with references").concat(":"));
        builder.add(subject, 1, 2);
        openFoldersOfAttachedFiles = new CheckBox(Localization.lang("Automatically open folders of attached files"));
        emailSubject = new TextField();
        sendRefMailBox.getChildren().setAll(openFoldersOfAttachedFiles, emailSubject);
        builder.add(sendRefMailBox, 1, 3);

        builder.add(new Separator(), 1, 7);

        // External programs title
        Label externalPrograms = new Label(Localization.lang("External programs"));
        externalPrograms.getStyleClass().add("sectionHeader");
        builder.add(externalPrograms, 1, 9);

        GridPane butpan = new GridPane();
        int index = 0;
        for (PushToApplication pushToApplication : frame.getPushApplications().getApplications()) {
            addSettingsButton(pushToApplication, butpan, index);
            index++;
        }
        builder.add(butpan, 1, 10);

        // Cite command configuration
        HBox citeCommandBox = new HBox();
        citeCommandBox.setSpacing(10);
        citeCommandBox.setAlignment(Pos.CENTER_LEFT);
        Label citeCommandLabel = new Label(Localization.lang("Cite command") + ':');
        citeCommandBox.getChildren().setAll(citeCommandLabel, citeCommand, editFileTypes);
        builder.add(citeCommandBox, 1, 12);

        builder.add(new Separator(), 1, 16);

        // Open console title
        Label openConsole = new Label(Localization.lang("Open console"));
        openConsole.getStyleClass().add("sectionHeader");
        builder.add(openConsole, 1, 18);

        builder.add(consoleOptionPanel, 1, 21);

        builder.add(new Separator(), 1, 25);

        // Open PDF title
        Label openPdf = new Label(Localization.lang("Open PDF"));
        openPdf.getStyleClass().add("sectionHeader");
        builder.add(openPdf, 1, 27);

        builder.add(pdfOptionPanel, 1, 29);

        builder.add(new Separator(), 1, 33);

        // Open file browser title
        Label openFileBrowser = new Label(Localization.lang("Open File Browser"));
        openFileBrowser.getStyleClass().add("sectionHeader");
        builder.add(openFileBrowser,  1, 35);

        builder.add(fileBrowserOptionPanel, 1, 36);

    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    private void addSettingsButton(final PushToApplication application, GridPane panel, int index) {
        PushToApplicationSettings settings = PushToApplicationsManager.getSettings(application);
        Button button = new Button(Localization.lang("Settings for %0", application.getApplicationName()));
        button.setPrefSize(150, 20);
        button.setOnAction(e -> PushToApplicationSettingsDialog.showSettingsDialog(dialogService, settings, index));
        if ((index % 2) == 0) {
            panel.add(button, 1, (index / 2) + 1);
        } else {
            panel.add(button, 2, (index / 2) + 1);
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

        defaultFileBrowser.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION));
        executeFileBrowser.setSelected(!Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION));
        fileBrowserCommand.setText(Globals.prefs.get(JabRefPreferences.FILE_BROWSER_COMMAND));

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

        prefs.putBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION, defaultFileBrowser.isSelected());
        if (StringUtil.isNotBlank(fileBrowserCommand.getText())) {
            prefs.put(JabRefPreferences.FILE_BROWSER_COMMAND, fileBrowserCommand.getText());
        } else {
            prefs.putBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION, true); //default if no command specified
        }
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
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> consoleCommand.setText(file.toAbsolutePath().toString()));
    }

    private void showAdobeChooser() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> adobeAcrobatReaderPath.setText(file.toAbsolutePath().toString()));
    }

    private void showSumatraChooser() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> sumatraReaderPath.setText(file.toAbsolutePath().toString()));
    }

    private void showFileBrowserCommandChooser() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> fileBrowserCommand.setText(file.toAbsolutePath().toString()));
    }

    private void readerSelected() {
        if (adobeAcrobatReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, adobeAcrobatReaderPath.getText());
        } else if (sumatraReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, sumatraReaderPath.getText());
        }
    }
}
