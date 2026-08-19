package org.jabref.gui.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

// Starts the JavaFX toolkit exactly once per JVM, for tests that need a live JavaFX toolkit
public final class JavaFxThreadingUtil {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFxThreadingUtil() {
    }

    public static void initializeJavaFxToolkit() throws InterruptedException {
        if (STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for JavaFX toolkit to initialize");
            }
        }
    }
}
