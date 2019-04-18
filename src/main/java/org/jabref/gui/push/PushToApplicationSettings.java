package org.jabref.gui.push;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToApplicationSettings {

    protected final TextField path = new TextField();
    protected Label commandLabel = new Label();
    protected GridPane jfxSettings;
    protected AbstractPushToApplication application;
    private DialogService dialogService;

    public GridPane getJFXSettingPane(int n) {
        switch (n) {
            case 0:
                application = new PushToEmacs(dialogService);
                break;
            case 1:
                application = new PushToLyx(dialogService);
                break;
            case 2:
                application = new PushToTexmaker(dialogService);
                break;
            case 3:
                application = new PushToTeXstudio(dialogService);
                break;
            case 4:
                application = new PushToVim(dialogService);
                break;
            case 5:
                application = new PushToWinEdt(dialogService);
                break;
            default:
                application = null;
                break;
        }
        application.initParameters();
        String commandPath = Globals.prefs.get(application.commandPathPreferenceKey);
        if (jfxSettings == null) {
            initJFXSettingsPanel();
        }
        path.setText(commandPath);

        return jfxSettings;
    }

    protected void initJFXSettingsPanel() {
        jfxSettings = new GridPane();
        StringBuilder label = new StringBuilder(Localization.lang("Path to %0", application.getApplicationName()));
        // In case the application name and the actual command is not the same, add the command in brackets
        if (application.getCommandName() == null) {
            label.append(':');
        } else {
            label.append(" (").append(application.getCommandName()).append("):");
        }
        commandLabel = new Label(label.toString());
        jfxSettings.add(commandLabel, 0, 0);
        jfxSettings.add(path, 1, 0);
        Button browse = new Button(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();

        browse.setOnAction(e -> dialogService.showFileOpenDialog(fileDialogConfiguration)
                                             .ifPresent(f -> path.setText(f.toAbsolutePath().toString())));
        jfxSettings.add(browse, 2, 0);
    }

    /**
     * This method is called to indicate that the settings panel returned from the getSettingsPanel() method has been
     * shown to the user and that the user has indicated that the settings should be stored. This method must store the
     * state of the widgets in the settings panel to Globals.prefs.
     */
    public void storeSettings() {
        Globals.prefs.put(application.commandPathPreferenceKey, path.getText());
    }
}
