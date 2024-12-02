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
    private final SimpleStringProperty pushFrequencyProperty = new SimpleStringProperty();
    private final BooleanProperty hostKeyCheckProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty sshPath = new SimpleStringProperty("");
    private final BooleanProperty sshEncryptedProperty = new SimpleBooleanProperty();
    private final DialogService dialogService;
    private final GitPreferences gitPreferences;

    public GitViewModel(GitPreferences gitPreferences, DialogService dialogservice) {
        this.dialogService = dialogservice;
        this.gitPreferences = gitPreferences;
    }

    @Override
    public void setValues() {
        gitSupportEnabledProperty.setValue(gitPreferences.isGitEnabled());
        frequencyLabelEnabledProperty.setValue(gitPreferences.isPushFrequencyEnabled());
        hostKeyCheckProperty.setValue(gitPreferences.isHostKeyCheckDisabled());
        sshEncryptedProperty.setValue(gitPreferences.isSshKeyEncrypted());
        pushFrequencyProperty.setValue(gitPreferences.getPushFrequency().orElse(""));
    }

    /**
     * Saves the current user preferences to the GroupsPreferences object.
     */
    @Override
    public void storeSettings() {
        gitPreferences.setGitEnabled(gitSupportEnabledProperty.getValue());
        gitPreferences.setPushFrequencyEnabled(frequencyLabelEnabledProperty.getValue());
        gitPreferences.setHostKeyCheckDisabled(hostKeyCheckProperty.getValue());
        gitPreferences.setSshKeyEncrypted(sshEncryptedProperty.getValue());
        gitPreferences.setPushFrequency(pushFrequencyProperty.getValue());
    }

    public BooleanProperty gitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public BooleanProperty getHostKeyCheckProperty() {
        return hostKeyCheckProperty;
    }

    public BooleanProperty getSshEncryptedProperty() {
        return sshEncryptedProperty;
    }

    public BooleanProperty getFrequencyLabelEnabledProperty() {
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
    }

    public void sshBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(sshPath.getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(f -> sshPath.setValue(f.toString()));
        gitPreferences.setSshDirPath(sshPath.get());
    }

    public SimpleStringProperty sshPathProperty() {
        return gitPreferences.getSshPathProperty();
    }

    public SimpleStringProperty getPushFrequency() {
        return pushFrequencyProperty;
    }
}
