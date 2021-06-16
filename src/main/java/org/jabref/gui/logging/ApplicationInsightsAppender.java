package org.jabref.gui.logging;

import org.jabref.gui.Globals;
import org.jabref.logic.logging.LogMessages;

import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Plugin(name = "OurApplicationInsightsAppender", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("unused") // class is indirectly constructed by log4j
public class ApplicationInsightsAppender extends AbstractAppender {

    private ApplicationInsightsAppender(String name, Filter filter, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
    }

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
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

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<ApplicationInsightsAppender> {

        @Override
        public ApplicationInsightsAppender build() {
            return new ApplicationInsightsAppender(this.getName(), this.getFilter(), this.isIgnoreExceptions(), this.getPropertyArray());
        }
    }
}
