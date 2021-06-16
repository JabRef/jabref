package org.jabref.gui.push;

import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

public class PushToApplicationSettings {

    protected final Label commandLabel;
    protected final TextField path;
    protected final GridPane settingsPane;
    protected final ObjectProperty<PushToApplicationPreferences> preferences;
    protected final AbstractPushToApplication application;

    public PushToApplicationSettings(PushToApplication application,
                                     DialogService dialogService,
                                     PreferencesService preferencesService,
                                     ObjectProperty<PushToApplicationPreferences> preferences) {
        this.application = (AbstractPushToApplication) application;
        this.preferences = preferences;

        settingsPane = new GridPane();
        commandLabel = new Label();
        path = new TextField();
        Button browse = new Button();

        settingsPane.setHgap(4.0);
        settingsPane.setVgap(4.0);

        browse.setTooltip(new Tooltip(Localization.lang("Browse")));
        browse.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        browse.getStyleClass().addAll("icon-button", "narrow");
        browse.setPrefHeight(20.0);
        browse.setPrefWidth(20.0);

        // In case the application name and the actual command is not the same, add the command in brackets
        StringBuilder commandLine = new StringBuilder(Localization.lang("Path to %0", application.getDisplayName()));
        if (this.application.getCommandName() == null) {
            commandLine.append(':');
        } else {
            commandLine.append(" (").append(this.application.getCommandName()).append("):");
        }
        commandLabel.setText(commandLine.toString());
        settingsPane.add(commandLabel, 0, 0);

        path.setText(preferences.get().getPushToApplicationCommandPaths().get(this.application.getDisplayName()));
        settingsPane.add(path, 1, 0);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferencesService.getWorkingDir()).build();
        browse.setOnAction(e -> dialogService.showFileOpenDialog(fileDialogConfiguration)
                                             .ifPresent(f -> path.setText(f.toAbsolutePath().toString())));
        settingsPane.add(browse, 2, 0);

        ColumnConstraints textConstraints = new ColumnConstraints();
        ColumnConstraints pathConstraints = new ColumnConstraints();
        pathConstraints.setHgrow(Priority.ALWAYS);
        ColumnConstraints browseConstraints = new ColumnConstraints(20.0);
        browseConstraints.setHgrow(Priority.NEVER);
        settingsPane.getColumnConstraints().addAll(textConstraints, pathConstraints, browseConstraints);
    }

    /**
     * This method is called to indicate that the settings panel returned from the getSettingsPanel() method has been
     * shown to the user and that the user has indicated that the settings should be stored. This method must store the
     * state of the widgets in the settings panel to Globals.prefs.
     */
    public void storeSettings() {
        Map<String, String> commandPaths = preferences.get().getPushToApplicationCommandPaths();
        commandPaths.put(application.getDisplayName(), path.getText());
        preferences.setValue(preferences.get().withPushToApplicationCommandPaths(commandPaths));
    }

    public GridPane getSettingsPane() {
        return this.settingsPane;
    }
}
