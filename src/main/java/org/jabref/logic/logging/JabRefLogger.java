package org.jabref.logic.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefLogger.class);

    private JabRefLogger() {
    }

    public static void setDebug() {
        setLogLevelToDebugForJabRefClasses();
        LOGGER.debug("Showing debug messages");
    }

    private static void setLogLevelToDebugForJabRefClasses() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("org.jabref");
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
    }
}
