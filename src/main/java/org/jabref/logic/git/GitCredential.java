package org.jabref.logic.git;

public class GitCredential {
    private final String username;
    private final String password;

    public GitCredential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
