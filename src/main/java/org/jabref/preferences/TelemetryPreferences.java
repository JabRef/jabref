package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class TelemetryPreferences {
    private BooleanProperty collectTelemetry;
    private BooleanProperty askToCollectTelemetry;

    public TelemetryPreferences(boolean shouldCollectTelemetry,
                                boolean shouldAskToCollectTelemetry) {
        this.collectTelemetry = new SimpleBooleanProperty(shouldCollectTelemetry);
        this.askToCollectTelemetry = new SimpleBooleanProperty(shouldAskToCollectTelemetry);
    }

    public boolean shouldCollectTelemetry() {
        return collectTelemetry.get();
    }

    public BooleanProperty collectTelemetryProperty() {
        return collectTelemetry;
    }

    public void setCollectTelemetry(boolean collectTelemetry) {
        this.collectTelemetry.set(collectTelemetry);
    }

    public boolean shouldAskToCollectTelemetry() {
        return askToCollectTelemetry.get();
    }

    public BooleanProperty askToCollectTelemetryProperty() {
        return askToCollectTelemetry;
    }

    public void setAskToCollectTelemetry(boolean askToCollectTelemetry) {
        this.askToCollectTelemetry.set(askToCollectTelemetry);
    }
}
