package net.sf.jabref.logic.logging;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;

@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiAppender extends AbstractAppender {

    protected GuiAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
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
     * <p/>
     * All logged messages will pass this method. For each line in the messages of the log event a new log event will be created.
     * @param event log event
     */
    @Override
    public void append(LogEvent event) {
        String message = new String(this.getLayout().toByteArray(event));
        message = message.replace("\n\r", "\n").replace("\r", "\n");
        // stack traces logged by 'Log.error("message"), e' will be split by new lines so we can create a new log event for each line as 'e.printStackTrace()' would do.
        if (event.getLevel() == Level.ERROR) {
            Arrays.asList(message.split("\n")).stream().filter(s -> !s.isEmpty()).forEach(log -> {
                LogEvent messageWithPriority = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(log.replace("\n", ""))).setLevel(event.getLevel()).build();
                LogMessages.getInstance().add(messageWithPriority);
            });
        } else {
            LogEvent messageWithPriority = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(message.replace("\n", ""))).setLevel(event.getLevel()).build();
            LogMessages.getInstance().add(messageWithPriority);
        }
    }
}
