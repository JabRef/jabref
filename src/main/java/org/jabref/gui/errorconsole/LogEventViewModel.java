package org.jabref.gui.errorconsole;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.IconTheme;
import org.jabref.logic.util.OS;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.core.LogEvent;

public class LogEventViewModel {

    private LogEvent logEvent;

    public LogEventViewModel(LogEvent logEvent) {
        this.logEvent = Objects.requireNonNull(logEvent);
    }

    public String getDisplayText() {
        return logEvent.getMessage().getFormattedMessage();
    }

    public String getStyleClass() {
        switch (logEvent.getLevel().getStandardLevel()) {
            case ERROR:
                return "exception";
            case WARN:
                return "output";
            case INFO:
            default:
                return "log";
        }
    }

    public IconTheme.JabRefIcon getIcon() {
        switch (logEvent.getLevel().getStandardLevel()) {
            case ERROR:
                return (IconTheme.JabRefIcon.INTEGRITY_FAIL);
            case WARN:
                return (IconTheme.JabRefIcon.INTEGRITY_WARN);
            case INFO:
            default:
                return (IconTheme.JabRefIcon.INTEGRITY_INFO);
        }
    }

    public Optional<String> getStackTrace() {
        return Optional.ofNullable(logEvent.getMessage().getThrowable()).map(ExceptionUtils::getStackTrace);
    }

    public String getDetailedText() {
        return getDisplayText() + getStackTrace().map(stacktrace -> OS.NEWLINE + stacktrace).orElse("");
    }
}
