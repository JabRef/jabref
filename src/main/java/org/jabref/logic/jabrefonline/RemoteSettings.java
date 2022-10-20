package org.jabref.logic.jabrefonline;

import java.time.ZonedDateTime;
import java.util.Optional;

public class RemoteSettings {
    private final String userId;
    private final Optional<SyncCheckpoint> lastSync;

    public RemoteSettings(String userId) {
        this.userId = userId;
        // TODO: Init from params
        this.lastSync = Optional.of(new SyncCheckpoint(ZonedDateTime.now(), ""));
    }

    public String getUserId() {
        return userId;
    }

    public Optional<SyncCheckpoint> getLastSync() {
        return lastSync;
    }
}
