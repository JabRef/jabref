package org.jabref.gui.logging;

import java.util.Locale;

import org.tinylog.Level;
import org.tinylog.format.AdvancedMessageFormatter;
import org.tinylog.format.MessageFormatter;
import org.tinylog.provider.ContextProvider;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.provider.NopContextProvider;

public class GuiAppender implements LoggingProvider {

    @Override
    public ContextProvider getContextProvider() {
        return new NopContextProvider();
    }

    @Override
    public Level getMinimumLevel() {
        return Level.INFO;
    }

    @Override
    public Level getMinimumLevel(String tag) {
        return Level.INFO;
    }

    @Override
    public boolean isEnabled(int depth, String tag, Level level) {
        return level.ordinal() >= Level.INFO.ordinal();
    }

    @Override
    public void shutdown() {
        // Nothing to do
    }

    private void log(Throwable exception, String message, Object[] arguments) {
        StringBuilder builder = new StringBuilder();
        if (message != null) {
            builder.append(new AdvancedMessageFormatter(Locale.ENGLISH, true).format(message, arguments));
        }
        if (exception != null) {
            if (builder.length() > 0) {
                builder.append(": ");
            }
            builder.append(exception);
        }
        System.out.println(builder);
    }

    @Override
    public void log(int depth, String tag, Level level, Throwable exception, MessageFormatter formatter, Object obj, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void log(String loggerClassName, String tag, Level level, Throwable exception, MessageFormatter formatter, Object obj, Object... arguments) {
        // TODO Auto-generated method stub

    }

    /*
      The log event will be forwarded to the {@link LogMessages} archive.

    public void append(LoggingEvent event) {
        // We need to make a copy as instances of LogEvent are reused by log4j
        DefaultTaskExecutor.runInJavaFXThread(() -> LogMessages.getInstance().add(copy));
    }
    */
}