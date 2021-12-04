package org.jabref.gui.errorconsole;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.util.OS;

import com.google.common.base.Throwables;
import org.slf4j.event.LoggingEvent;

public class LogEventViewModel {

    private final LoggingEvent logEvent;

    public LogEventViewModel(LoggingEvent logEvent) {
        this.logEvent = Objects.requireNonNull(logEvent);
    }

    public String getDisplayText() {
        return logEvent.getMessage();
    }

    public String getStyleClass() {
        switch (logEvent.getLevel()) {
            case ERROR:
                return "exception";
            case WARN:
                return "output";
            case INFO:
            default:
                return "log";
        }
    }

    public JabRefIcon getIcon() {
        switch (logEvent.getLevel()) {
            case ERROR:
                return (IconTheme.JabRefIcons.INTEGRITY_FAIL);
            case WARN:
                return (IconTheme.JabRefIcons.INTEGRITY_WARN);
            case INFO:
            default:
                return (IconTheme.JabRefIcons.INTEGRITY_INFO);
        }
    }

    public Optional<String> getStackTrace() {
        return Optional.ofNullable(logEvent.getThrowable()).map(Throwables::getStackTraceAsString);
    }

    public String getDetailedText() {
        return getDisplayText() + getStackTrace().map(stacktrace -> OS.NEWLINE + stacktrace).orElse("");
    }
}
