package org.jabref.gui.logging;

import org.jabref.Globals;
import org.jabref.logic.logging.LogMessages;

import com.microsoft.applicationinsights.log4j.v2.internal.ApplicationInsightsLogEvent;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "OurApplicationInsightsAppender", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("unused") // class is indirectly constructed by log4j
public class ApplicationInsightsAppender extends AbstractAppender {

    private ApplicationInsightsAppender(String name, Filter filter) {
        super(name, filter, null);
    }

    @PluginFactory
    public static ApplicationInsightsAppender createAppender(@PluginAttribute("name") String name,
                                                             @PluginElement("Filters") Filter filter) {
        return new ApplicationInsightsAppender(name, filter);
    }

    /**
     * The log event will be forwarded to the {@link LogMessages} archive.
     */
    @Override
    public void append(LogEvent rawEvent) {
        ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(rawEvent);

        Telemetry telemetry;
        if (event.isException()) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(event.getException());
            exceptionTelemetry.getProperties().put("Message", rawEvent.getMessage().getFormattedMessage());
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
