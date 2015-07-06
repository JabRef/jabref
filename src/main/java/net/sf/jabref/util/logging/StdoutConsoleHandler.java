package net.sf.jabref.util.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * With Java 7, one could directly set a format for the SimpleFormatter
 * (http://stackoverflow.com/a/10722260/873282) and use that in a StreamHandler.
 * As JabRef is compatible with Java6, we have to write our own Handler
 */
public class StdoutConsoleHandler extends Handler {

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void flush() {
        System.out.flush();
    }

    @Override
    public void publish(LogRecord record) {
        System.out.println(record.getMessage());
        System.out.flush();
    }
}
