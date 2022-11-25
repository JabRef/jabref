package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TelemetryPreferences {
    private final BooleanProperty collectTelemetry;
    private final BooleanProperty askToCollectTelemetry;
    private final StringProperty userId;

    public TelemetryPreferences(boolean shouldCollectTelemetry,
                                boolean shouldAskToCollectTelemetry,
                                String userId) {
        this.collectTelemetry = new SimpleBooleanProperty(shouldCollectTelemetry);
        this.askToCollectTelemetry = new SimpleBooleanProperty(shouldAskToCollectTelemetry);
        this.userId = new SimpleStringProperty(userId);
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

    public String getUserId() {
        return userId.get();
    }
}
