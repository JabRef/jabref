package org.jabref.preferences;

public class TelemetryPreferences {
    private boolean collectTelemetry;
    private boolean askToCollectTelemetry;

    public TelemetryPreferences(boolean shouldCollectTelemetry,
                                boolean shouldAskToCollectTelemetry) {
        this.collectTelemetry = shouldCollectTelemetry;
        this.askToCollectTelemetry = shouldAskToCollectTelemetry;
    }

    public boolean shouldCollectTelemetry() {
        return collectTelemetry;
    }

    public TelemetryPreferences withCollectTelemetry(boolean shouldCollectTelemetry) {
        this.collectTelemetry = shouldCollectTelemetry;
        return this;
    }

    public boolean shouldAskToCollectTelemetry() {
        return askToCollectTelemetry;
    }

    public TelemetryPreferences withAskToCollectTelemetry(boolean shouldAskToCollectTelemetry) {
        this.askToCollectTelemetry = shouldAskToCollectTelemetry;
        return this;
    }
}
