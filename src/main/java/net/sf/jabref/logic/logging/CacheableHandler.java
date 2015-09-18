package net.sf.jabref.logic.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class CacheableHandler extends Handler {

    private final SimpleFormatter fmt = new SimpleFormatter();
    private final Cache cache = new Cache();

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
        cache.add(fmt.format(record));
    }

    public String getLog() {
        return cache.get();
    }

}
