package org.jabref.logic.util;

public class UserAndHost {
    private final String user;
    private final String host;

    public UserAndHost(String user, String host) {
        this.user = user;
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public String getHost() {
        return host;
    }

    public String getString() {
        return "User: " + user + ", Host: " + host;
    }
}
