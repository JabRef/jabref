package org.jabref.gui.logging;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.jabref.gui.Globals;

import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.AbstractFormatPatternWriter;

public class ApplicationInsightsWriter extends AbstractFormatPatternWriter {

    public ApplicationInsightsWriter(final Map<String, String> properties) {
        super(properties);
    }

    public ApplicationInsightsWriter() {
        this(Collections.emptyMap());
    }

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.allOf(LogEntryValue.class);
    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(logEntry);

        Telemetry telemetry;
        if (event.isException()) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(event.getException());
            exceptionTelemetry.getProperties().put("Message", logEntry.getMessage());
            exceptionTelemetry.setSeverityLevel(event.getNormalizedSeverityLevel());
            telemetry = exceptionTelemetry;
        } else {
            TraceTelemetry traceTelemetry = new TraceTelemetry(event.getMessage());
            traceTelemetry.setSeverityLevel(event.getNormalizedSeverityLevel());
            telemetry = traceTelemetry;
        }
        telemetry.getContext().getProperties().putAll(event.getCustomParameters());

        Globals.getTelemetryClient().ifPresent(client -> client.track(telemetry));
    }

    @Override
    public void flush() throws Exception {
    }

    @Override
    public void close() throws Exception {

    }
}
