package org.jabref.gui.telemetry;

import java.util.Map;

public class TelemetryClient {
    public void trackEvent(String actionName) {
    }

    public <K, V> void trackEvent(String actionName, Map<K, V> source, Map<K, V> of) {
    }
}
