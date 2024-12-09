package org.jabref.gui.util;

import org.jabref.model.util.DirectoryMonitor;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDirectoryMonitor implements DirectoryMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDirectoryMonitor.class);
    private static final int POLL_INTERVAL = 1000;

    private final FileAlterationMonitor monitor;

    public DefaultDirectoryMonitor() {
        monitor = new FileAlterationMonitor(POLL_INTERVAL);
        start();
    }

    @Override
    public void addObserver(FileAlterationObserver observer, FileAlterationListener listener) {
        if (observer != null) {
            observer.addListener(listener);
            monitor.addObserver(observer);
        }
    }

    @Override
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

    @Override
    public void shutdown() {
        try {
            monitor.stop();
        } catch (Exception e) {
            LOGGER.error("Error stopping directory monitor", e);
        }
    }
}
