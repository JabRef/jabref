package org.jabref.gui.logging;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.logging.LogMessages;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("unused") // class is indirectly constructed by log4j
public class GuiAppender extends AbstractAppender {

    private GuiAppender(String name, Filter filter, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
    }

    @PluginFactory
    public static <B extends GuiAppender.Builder<B>> B newBuilder() {
        return new GuiAppender.Builder<B>().asBuilder();
    }

    /**
     * The log event will be forwarded to the {@link LogMessages} archive.
     */
    @Override
    public void append(LogEvent event) {
        // We need to make a copy as instances of LogEvent are reused by log4j
        MutableLogEvent copy = new MutableLogEvent();
        copy.initFrom(event);
        DefaultTaskExecutor.runInJavaFXThread(() -> LogMessages.getInstance().add(copy));
    }

    public static class Builder<B extends GuiAppender.Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<GuiAppender> {

        @Override
        public GuiAppender build() {
            return new GuiAppender(this.getName(), this.getFilter(), this.isIgnoreExceptions(), this.getPropertyArray());
        }
    }
}
