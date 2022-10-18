package org.jabref.logic.jabrefonline;

public class RemoteSettings {
    private final String userId;

    public RemoteSettings(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
