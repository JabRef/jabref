package org.jabref.logic.util;

public record UserAndHost(String user, String host) {
    public String getCanonicalForm() {
        return "user: " + user + ", host: " + host;
    }
}
