package org.jabref.logic.util;

@FunctionalInterface
public interface NotificationService {
    /**
     * Notify the user in a non-blocking way.
     *
     * @param message the message to show.
     */
    void notify(String message);
}
