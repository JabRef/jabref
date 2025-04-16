package org.jabref.logic.msc;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

/**
 * Utility to ensure JavaFX is initialized only once in tests.
 */
public class JavaFXInitializer {

    private static boolean initialized = false;

    public static void initialize() {
        if (!initialized) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                // No-op, just trigger JavaFX
                latch.countDown();
            });

            try {
                latch.await();
                initialized = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("JavaFX initialization interrupted", e);
            }
        }
    }
}
