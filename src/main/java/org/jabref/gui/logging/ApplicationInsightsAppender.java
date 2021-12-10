package org.jabref.gui.logging;

import org.jabref.gui.Globals;
import org.jabref.logic.logging.LogMessages;

import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.slf4j.event.LoggingEvent;

public class ApplicationInsightsAppender {

    /**
     * The log event will be forwarded to the {@link LogMessages} archive.
     */
    public void append(LoggingEvent rawEvent) {
        ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(rawEvent);

        Telemetry telemetry;
        if (event.isException()) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(event.getException());
            exceptionTelemetry.getProperties().put("Message", rawEvent.getMessage());
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
}
