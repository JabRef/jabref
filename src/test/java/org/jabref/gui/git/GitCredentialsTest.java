package org.jabref.gui.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GitCredentialsTest {
    @Test
    void GitCredentials() {
        GitCredentials gitCredentials = new GitCredentials();
        String gitPassword = gitCredentials.getGitPassword();
        String gitUsername = gitCredentials.getGitUsername();

        assertNull(gitUsername);
        assertNull(gitPassword);
    }

    @Test
    void GitCredentialsWithUsernameAndPassword() {
        GitCredentials gitCredentials = new GitCredentials("testUsername", "testPassword");
        String gitPassword = gitCredentials.getGitPassword();
        String gitUsername = gitCredentials.getGitUsername();

        assertEquals("testUsername", gitUsername);
        assertEquals("testPassword", gitPassword);
    }

    @Test
    void GitCredentialsGettersAndSetters() {
        GitCredentials gitCredentials = new GitCredentials();

        gitCredentials.setGitPassword("testPassword");
        gitCredentials.setGitUsername("testUsername");

        String gitPassword = gitCredentials.getGitPassword();
        String gitUsername = gitCredentials.getGitUsername();

        assertEquals("testUsername", gitUsername);
        assertEquals("testPassword", gitPassword);
    }
}
