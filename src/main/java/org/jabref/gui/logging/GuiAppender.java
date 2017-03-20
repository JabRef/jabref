package org.jabref.gui.logging;

import java.io.Serializable;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.logging.LogMessages;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("unused") // class is indirectly constructed by log4j
public class GuiAppender extends AbstractAppender {

    private GuiAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    @PluginFactory
    public static GuiAppender createAppender(@PluginAttribute("name") String name,
                                             @PluginElement("Layout") Layout<?> layout,
                                             @PluginElement("Filters") Filter filter) {

        if (name == null) {
            LOGGER.error("No name provided for GuiAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new GuiAppender(name, filter, layout);
    }

    /**
     * The log event will be forwarded to the {@link LogMessages} archive.
     */
    @Override
    public void append(LogEvent event) {
        DefaultTaskExecutor.runInJavaFXThread(() -> LogMessages.getInstance().add(event));
    }
}
