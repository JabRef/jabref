package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GitPreferences {
    private BooleanProperty gitSupportEnabledProperty = new SimpleBooleanProperty();
    private BooleanProperty frequencyLabelEnabledProperty = new SimpleBooleanProperty();
    private ObjectProperty<AuthenticationViewMode> authenticationMethod = new SimpleObjectProperty<>();

    public GitPreferences(boolean gitSupportEnabledProperty, AuthenticationViewMode authenticationMethod, boolean frequencyLabelEnabledProperty) {
        this.gitSupportEnabledProperty = new SimpleBooleanProperty(gitSupportEnabledProperty);
        this.authenticationMethod = new SimpleObjectProperty<>(authenticationMethod);
        this.frequencyLabelEnabledProperty = new SimpleBooleanProperty(frequencyLabelEnabledProperty);
    }

    public AuthenticationViewMode getAuthenticationMethod() {
        return authenticationMethod.getValue();
    }

    public ObjectProperty<AuthenticationViewMode> getAuthenticationProperty() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(AuthenticationViewMode authenticationMethod) {
        this.authenticationMethod.set(authenticationMethod);
    }

    public boolean isGitEnabled() {
        return gitSupportEnabledProperty.getValue();
    }

    public boolean isFrequencyLabelEnabled() {
        return frequencyLabelEnabledProperty.getValue();
    }

    public BooleanProperty getGitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public BooleanProperty getFrequencyLabelEnabledProperty() {
        return frequencyLabelEnabledProperty;
    }

    public void setGitSupportEnabledProperty(boolean gitSupportEnabledProperty) {
        this.gitSupportEnabledProperty.set(gitSupportEnabledProperty);
    }

    public void setFrequencyLabelEnabledProperty(boolean frequencyLabelEnabledProperty) {
        this.frequencyLabelEnabledProperty.set(frequencyLabelEnabledProperty);
    }
}
