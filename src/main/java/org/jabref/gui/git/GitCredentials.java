package org.jabref.gui.git;

public class GitCredentials {
    private String gitUsername;
    private String gitPassword;

    public GitCredentials() {
        this.gitUsername = null;
        this.gitPassword = null;
    }

    public GitCredentials(String gitUsername, String gitPassword) {
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
    }

    public void setGitUsername(String gitUsername) {
        this.gitUsername = gitUsername;
    }

    public void setGitPassword(String gitPassword) {
        this.gitPassword = gitPassword;
    }

    public String getGitPassword() {
        return this.gitPassword;
    }

    public String getGitUsername() {
        return this.gitUsername;
    }
}
