package org.jabref.logic.ocr;

import org.junit.jupiter.api.Test;

public class JnaDebugTest {

    @Test
    public void debugJnaLoading() {
        System.out.println("=== JNA Debug Test ===");
        System.out.println("os.arch: " + System.getProperty("os.arch"));
        System.out.println("os.name: " + System.getProperty("os.name"));
        System.out.println("java.version: " + System.getProperty("java.version"));

        try {
            // This will trigger JNA loading
            System.out.println("Loading JNA Native class...");
            Class<?> nativeClass = Class.forName("com.sun.jna.Native");
            System.out.println("JNA Native loaded from: " + nativeClass.getProtectionDomain().getCodeSource().getLocation());

            System.out.println("Loading JNA Platform class...");
            Class<?> platformClass = Class.forName("com.sun.jna.Platform");
            System.out.println("JNA Platform loaded from: " + platformClass.getProtectionDomain().getCodeSource().getLocation());

            // Try to load Platform constants
            System.out.println("Platform.ARCH: " + com.sun.jna.Platform.ARCH);
            System.out.println("Platform.RESOURCE_PREFIX: " + com.sun.jna.Platform.RESOURCE_PREFIX);

            // Now try Tesseract
            System.out.println("\nLoading Tesseract class...");
            Class<?> tessClass = Class.forName("net.sourceforge.tess4j.Tesseract");
            System.out.println("Tesseract loaded successfully");

        } catch (Throwable t) {
            System.err.println("Error during loading: " + t);
            t.printStackTrace();
        }
    }
}
