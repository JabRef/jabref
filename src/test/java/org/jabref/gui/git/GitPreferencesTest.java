package org.jabref.gui.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GitPreferencesTest {
    @Test
    void GitCredentials() {
        String testUsername = "testUsername";
        String testPassword = "testPassword";
        GitPreferences gitPreferences = new GitPreferences(testUsername, testPassword);

        assertEquals(testUsername, gitPreferences.getUsername());
        assertEquals(testPassword, gitPreferences.getPassword());
        assertEquals(testUsername, gitPreferences.getUsernameProperty().get());
        assertEquals(testPassword, gitPreferences.getPasswordProperty().get());
    }

    @Test
    void GitCredentialsWithGitProperty() {
        String testUsername = "testUsername";
        String testPassword = "testPassword";
        StringProperty gitPasswordProperty = new SimpleStringProperty(testPassword);
        StringProperty gitUsernameProperty = new SimpleStringProperty(testUsername);
        GitPreferences gitPreferences = new GitPreferences(gitUsernameProperty, gitPasswordProperty);

        assertEquals(testUsername, gitPreferences.getUsername());
        assertEquals(testPassword, gitPreferences.getPassword());
        assertEquals(testUsername, gitPreferences.getUsernameProperty().get());
        assertEquals(testPassword, gitPreferences.getPasswordProperty().get());
    }

    @Test
    void GitCredentialsWithGitPropertyGetterAndSetters() {
        String testUsername = "testUsername";
        String testPassword = "testPassword";
        String testString = "updated";
        StringProperty gitPasswordProperty = new SimpleStringProperty(testPassword);
        StringProperty gitUsernameProperty = new SimpleStringProperty(testUsername);
        GitPreferences gitPreferences = new GitPreferences(gitUsernameProperty, gitPasswordProperty);
        gitPreferences.setUsername(testUsername + testString);
        gitPreferences.setPassword(testPassword + testString);

        assertEquals(testUsername + testString, gitPreferences.getUsername());
        assertEquals(testPassword + testString, gitPreferences.getPassword());
        assertEquals(testUsername + testString, gitPreferences.getUsernameProperty().get());
        assertEquals(testPassword + testString, gitPreferences.getPasswordProperty().get());
    }

    @Test
    void GitCredentialsWithGitGetterAndSetters() {
        String testUsername = "testUsername";
        String testPassword = "testPassword";
        String testString = "updated";
        GitPreferences gitPreferences = new GitPreferences(testUsername, testPassword);
        gitPreferences.setUsername(testUsername + testString);
        gitPreferences.setPassword(testPassword + testString);

        assertEquals(testUsername + testString, gitPreferences.getUsername());
        assertEquals(testPassword + testString, gitPreferences.getPassword());
        assertEquals(testUsername + testString, gitPreferences.getUsernameProperty().get());
        assertEquals(testPassword + testString, gitPreferences.getPasswordProperty().get());
    }
}
