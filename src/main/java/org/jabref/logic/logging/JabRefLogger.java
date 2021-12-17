package org.jabref.logic.logging;

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
        // TODO
    }
}
