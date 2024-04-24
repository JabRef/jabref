package org.jabref.model.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class DirectoryMonitorManager {

    private final DirectoryMonitor directoryMonitor;
    private final List<FileAlterationObserver> observers = new ArrayList<>();

    public DirectoryMonitorManager(DirectoryMonitor directoryMonitor) {
        this.directoryMonitor = directoryMonitor;
    }

    public void addObserver(FileAlterationObserver observer, FileAlterationListener listener) {
        directoryMonitor.addObserver(observer, listener);
        observers.add(observer);
    }

    public void removeObserver(FileAlterationObserver observer) {
        directoryMonitor.removeObserver(observer);
        observers.remove(observer);
    }

    /**
     * Unregisters all observers from the directory monitor.
    **/
    public void unregister() {
        observers.forEach(directoryMonitor::removeObserver);
        observers.clear();
    }
}
