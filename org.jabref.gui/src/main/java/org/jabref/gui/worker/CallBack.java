package org.jabref.gui.worker;

/**
 * Represents a task that is to be executed on the GUI thread
 */
@FunctionalInterface
public interface CallBack {

    void update();

}
