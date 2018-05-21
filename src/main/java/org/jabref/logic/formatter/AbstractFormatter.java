package org.jabref.logic.formatter;

import org.jabref.model.cleanup.Formatter;

/**
 * All formatters should implement hashcode and equals in the way the interface {@link Formatter} provides. This superclass ensures this.
 */
public abstract class AbstractFormatter implements Formatter {
    @Override
    public int hashCode() {
        return defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return defaultEquals(obj);
    }
}
