package org.jabref.gui;

import java.util.Optional;

import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.TelemetryPreferences;

public class Telemetry {
    private Telemetry() {
    }

    public static Optional<TelemetryClient> getTelemetryClient() {
        return Optional.empty();
    }

    private static void start(TelemetryPreferences telemetryPreferences, BuildInfo buildInfo) {
    }

    public static void shutdown() {
        getTelemetryClient().ifPresent(client -> {
        });
    }
}
