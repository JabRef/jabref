package org.jabref.gui.preferences.git;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.git.AuthenticationViewMode;
import org.jabref.logic.git.GitPreferences;

public class GitViewModel implements PreferenceTabViewModel {

    private final BooleanProperty gitSupportEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty frequencyLabelEnabledProperty = new SimpleBooleanProperty();
    private final ObjectProperty<AuthenticationViewMode> authenticationMethod = new SimpleObjectProperty<>();
    private final GitPreferences gitPreferences;

    public GitViewModel(GitPreferences gitPreferences) {
        this.gitPreferences = gitPreferences;
    }

    @Override
    public void setValues() {
        switch (gitPreferences.getAuthenticationMethod()) {
            case SSH ->
                    authenticationMethod.setValue(AuthenticationViewMode.SSH);
            case CREDENTIALS ->
                    authenticationMethod.setValue(AuthenticationViewMode.CREDENTIALS);
        }
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

        if (AuthenticationViewMode.SSH == authenticationMethod.getValue()) {
            gitPreferences.setAuthenticationMethod(AuthenticationViewMode.SSH);
        } else if (AuthenticationViewMode.CREDENTIALS == authenticationMethod.getValue()) {
            gitPreferences.setAuthenticationMethod(AuthenticationViewMode.CREDENTIALS);
        }
    }

    public BooleanProperty gitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public BooleanProperty frequencyLabelEnabledProperty() {
        return frequencyLabelEnabledProperty;
    }

    public AuthenticationViewMode authenticationViewMode() {
        return authenticationMethod.getValue();
    }

    public ObjectProperty<AuthenticationViewMode> authenticationViewModeObjectProperty() {
        return authenticationMethod;
    }

    @Override
    public boolean validateSettings() {
        return PreferenceTabViewModel.super.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return PreferenceTabViewModel.super.getRestartWarnings();
    }
}
