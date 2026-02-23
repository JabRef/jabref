package org.jabref.gui.openoffice;

/**
 * Functional interface that abstracts thread sleeping behavior.
 */
@FunctionalInterface
interface Sleeper {
    void sleep(long millis) throws InterruptedException;

    Sleeper THREAD_SLEEPER = Thread::sleep;
}
