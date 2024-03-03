package org.jabref.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.gui.JabRefExecutorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFileUpdateMonitorTest {
    @Test
    void demonstrateListenerForDirectory(@TempDir Path rootPath) throws Exception {
        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");
        while (fileUpdateMonitor.watcher == null) {
            Thread.sleep(100);
            Thread.yield();
        }
        rootPath.register(fileUpdateMonitor.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        final AtomicBoolean called = new AtomicBoolean(false);
        fileUpdateMonitor.listeners.put(rootPath, () -> {
            called.set(true);
        });
        Files.createFile(rootPath.resolve("test.txt"));
        int callCount = 0;
        while ((callCount < 10) && (!called.get())) {
            Thread.sleep(100);
            Thread.yield();
            callCount++;
        }
        JabRefExecutorService.INSTANCE.shutdownEverything();
        assertTrue(called.get());
    }
}
