package net.sf.jabref.logic.logging;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Enables caching of messages
 */
public class Cache {

    public static final int DEFAULT_CAPACITY = 500;
    private final int capacity;
    private final Queue<String> queue = new LinkedList<>();

    private String cache = "";
    private boolean cacheRefreshNeeded = true;

    public Cache() {
        this(DEFAULT_CAPACITY);
    }

    public Cache(int capacity) {
        this.capacity = capacity;
    }

    public synchronized String get() {
        ensureCacheIsFresh();
        return cache;
    }

    private void ensureCacheIsFresh() {
        if (cacheRefreshNeeded) {
            cache = String.join("", queue);
        }
    }

    public synchronized void add(String message) {
        queue.add(message);

        if (isCapacityExceeded()) {
            // if we reached capacity, we switch to the "real" caching method and remove old lines
            cacheRefreshNeeded = true;
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
