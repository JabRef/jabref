package net.sf.jabref.logic.logging;

import java.util.LinkedList;
import java.util.Queue;

import net.sf.jabref.logic.error.LogMessage;
import net.sf.jabref.logic.error.MessageType;

/**
 * Enables caching of messages
 */
public class Cache {

    public static final int DEFAULT_CAPACITY = 500;
    private final int capacity;
    private final Queue<String> queue = new LinkedList<>();

    private String cache = "";

    public Cache() {
        this(DEFAULT_CAPACITY);
    }

    public Cache(int capacity) {
        this.capacity = capacity;
    }

    public synchronized Queue<String> get() {
        return queue;
    }

    public synchronized void add(String message) {
        queue.add(message);
        LogMessage messageWithPriority = new LogMessage(message.replaceAll(System.lineSeparator(), ""), MessageType.LOG);
        LogMessages.getInstance().add(messageWithPriority);

        if (isCapacityExceeded()) {
            // if we reached capacity, we switch to the "real" caching method and remove old lines
            truncateLog();
        } else {
            // if we did not yet reach capacity, we just append the string to the cache
            // cache is still up to date
            cache = cache + message;
        }
    }

    private void truncateLog() {
        while (isCapacityExceeded()) {
            // if log is too large, remove first line
            // we need a while loop as the formatter may output more than one line
            queue.poll();
        }
    }

    private boolean isCapacityExceeded() {
        return queue.size() > capacity;
    }
}
