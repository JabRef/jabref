package org.jabref.gui.preferences.git;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.git.GitPreferences;

public class GitViewModel implements PreferenceTabViewModel {

    private final BooleanProperty gitSupportEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty frequencyLabelEnabledProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty sshPath = new SimpleStringProperty("");
    private final DialogService dialogService;
    private final GitPreferences gitPreferences;

    public GitViewModel(GitPreferences gitPreferences, DialogService dialogservice) {
        this.dialogService = dialogservice;
        this.gitPreferences = gitPreferences;
    }

    @Override
    public void setValues() {
        gitSupportEnabledProperty.setValue(gitPreferences.isGitEnabled());
        frequencyLabelEnabledProperty.setValue(gitPreferences.isFrequencyLabelEnabled());
    }

    /**
     * Saves the current user preferences to the GroupsPreferences object.
     */
    @Override
    public void storeSettings() {
        gitPreferences.setGitSupportEnabledProperty(gitSupportEnabledProperty.getValue());
        gitPreferences.setFrequencyLabelEnabledProperty(frequencyLabelEnabledProperty.getValue());
    }

    public BooleanProperty gitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public BooleanProperty frequencyLabelEnabledProperty() {
        return frequencyLabelEnabledProperty;
    }

    @Override
    public boolean validateSettings() {
        return PreferenceTabViewModel.super.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return PreferenceTabViewModel.super.getRestartWarnings();
    }

    public void authentify() {
        AuthentifyDialogView dialog = new AuthentifyDialogView(this.gitPreferences, this.dialogService);
        dialog.showAndWait();
        // System.out.println("we made it to GitViewModel.authentify()");
    }

    public void sshBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(sshPath.getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(f -> sshPath.setValue(f.toString()));
        gitPreferences.setSshPath(sshPath.get());
    }

    public SimpleStringProperty sshPathProperty() {
        // System.out.println("sshPathProperty() in GitViewModel: " + gitPreferences.getSshPathProperty());
        return gitPreferences.getSshPathProperty();
    }
}
