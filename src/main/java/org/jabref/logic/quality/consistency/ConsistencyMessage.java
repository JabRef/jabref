package org.jabref.logic.quality.consistency;

import org.jabref.model.entry.BibEntry;

public record ConsistencyMessage(String message, BibEntry bibEntry) implements Cloneable {

    @Override
    public String toString() {
        return "[" + message() + "]";
    }

    @Override
    public Object clone() {
        return new ConsistencyMessage(message, bibEntry);
    }
}
