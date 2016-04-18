package net.sf.jabref.gui;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Can catch any exceptions occuring on the EDT thread for assertion.
 */
public class AWTExceptionHandler {

    private final List<Throwable> list = new CopyOnWriteArrayList<>();

    public void installExceptionDetectionInEDT() {
        SwingUtilities.invokeLater(() -> Thread.currentThread().setUncaughtExceptionHandler((t, e) -> list.add(e)));
    }

    public void assertNoExceptions() {
        if (!list.isEmpty()) {
            throw new AssertionError("Uncaught exception in EDT", list.get(0));
        }
    }

}
