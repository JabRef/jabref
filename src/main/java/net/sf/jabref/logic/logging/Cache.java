/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.logging;

import java.util.LinkedList;
import java.util.Queue;

import net.sf.jabref.logic.error.MessagePriority;
import net.sf.jabref.logic.error.ObservableMessageWithPriority;

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

    public Queue<String> get() {
        return queue;
    }

    private void ensureCacheIsFresh() {
        if (cacheRefreshNeeded) {
            cache = String.join("", queue);
        }
    }

    public synchronized void add(String message) {
        queue.add(message);
        ObservableMessageWithPriority messageWithPriority = new ObservableMessageWithPriority(message.replaceAll(System.lineSeparator(), ""), MessagePriority.LOW);
        ObservableMessages.INSTANCE.add(messageWithPriority);

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
