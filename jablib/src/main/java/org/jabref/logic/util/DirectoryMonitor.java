package org.jabref.logic.util;

import java.time.Duration;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryMonitor {

    public static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryMonitor.class);

    private final FileAlterationMonitor monitor;

    public DirectoryMonitor() {
        monitor = new FileAlterationMonitor(POLL_INTERVAL.toMillis());
        start();
    }

    public void addObserver(FileAlterationObserver observer, FileAlterationListener listener) {
        if (observer != null) {
            observer.addListener(listener);
            monitor.addObserver(observer);
        }
    }

    public void removeObserver(FileAlterationObserver observer) {
        if (observer != null) {
            monitor.removeObserver(observer);
        }
    }

    public void start() {
        try {
            monitor.start();
        } catch (Exception e) {
            LOGGER.error("Error starting directory monitor", e);
        }
    }

    public void shutdown() {
        try {
            monitor.stop();
        } catch (Exception e) {
            LOGGER.error("Error stopping directory monitor", e);
        }
    }
}
