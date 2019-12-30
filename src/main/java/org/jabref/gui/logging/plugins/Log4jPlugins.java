package org.jabref.gui.logging.plugins;

import org.apache.logging.log4j.plugins.processor.PluginEntry;
import org.apache.logging.log4j.plugins.processor.PluginService;

public class Log4jPlugins extends PluginService {

    private static PluginEntry[] entries = new PluginEntry[] {
        new PluginEntry("ourapplicationinsightsappender", "org.jabref.gui.logging.ApplicationInsightsAppender", "appender", true, false, "Core"),
        new PluginEntry("guiappender", "org.jabref.gui.logging.GuiAppender", "appender", true, false, "Core")
    };
    @Override
    public PluginEntry[] getEntries() { return entries;}
}
