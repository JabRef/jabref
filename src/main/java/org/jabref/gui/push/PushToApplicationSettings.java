package org.jabref.gui.push;

import javax.swing.JPanel;

import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;

public class PushToApplicationSettings {

    protected final TextField path1 = new TextField();
    protected JPanel settings;
    protected GridPane jfxSettings;
    protected FormBuilder builder;
    protected AbstractPushToApplication application;
    protected DialogService dialogService;
    protected DialogPane dialogPane = new DialogPane();

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
        path1.setText(commandPath);

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
        jfxSettings.add(new Label(label.toString()), 0, 0);
        jfxSettings.add(path1, 1, 0);
        Button browse = new Button(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();

        browse.setOnAction(e -> dialogService.showFileOpenDialog(fileDialogConfiguration)
                                             .ifPresent(f -> path1.setText(f.toAbsolutePath().toString())));
        jfxSettings.add(browse, 2, 0);
        dialogPane.setContent(jfxSettings);
    }

    /**
     * This method is called to indicate that the settings panel returned from the getSettingsPanel() method has been
     * shown to the user and that the user has indicated that the settings should be stored. This method must store the
     * state of the widgets in the settings panel to Globals.prefs.
     */
    public void storeSettings() {
        Globals.prefs.put(application.commandPathPreferenceKey, path1.getText());
    }
}
