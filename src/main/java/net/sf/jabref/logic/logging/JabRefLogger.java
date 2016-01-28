package net.sf.jabref.logic.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class JabRefLogger {

    private static final Log LOGGER = LogFactory.getLog(JabRefLogger.class);

    public static void setDebug() {
        setLogLevelToDebugForJabRefClasses();
        LOGGER.debug("Showing debug messages");
    }

    private static void setLogLevelToDebugForJabRefClasses() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("net.sf.jabref");
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
    }

}
