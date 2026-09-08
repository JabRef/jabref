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
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            // Started by something outside this utility (another test class, a TestFX extension, etc.).
            // The toolkit is up either way, so there is nothing left to wait for.
            return;
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for JavaFX toolkit to initialize");
        }
    }

    //    public static void initializeJavaFxToolkit() throws InterruptedException {
    //        if (STARTED.compareAndSet(false, true)) {
    //            CountDownLatch latch = new CountDownLatch(1);
    //            Platform.startup(latch::countDown);
    //            if (!latch.await(5, TimeUnit.SECONDS)) {
    //                throw new IllegalStateException("Timed out waiting for JavaFX toolkit to initialize");
    //            }
    //        }
    //    }
}
