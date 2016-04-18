package net.sf.jabref.logic.logging;

import java.io.Serializable;

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
public class GuiAppender extends AbstractAppender {

    public static final Cache CACHE = new Cache();

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


    @Override
    public void append(LogEvent event) {
        CACHE.add(new String(this.getLayout().toByteArray(event)));
    }

}
