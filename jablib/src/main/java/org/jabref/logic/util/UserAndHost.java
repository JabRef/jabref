package org.jabref.logic.util;

import java.util.Objects;

public class UserAndHost {
    private final String user;
    private final String host;

    public UserAndHost(String user, String host) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.host = Objects.requireNonNull(host, "host must not be null");
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
