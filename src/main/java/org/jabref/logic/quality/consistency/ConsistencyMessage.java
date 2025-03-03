package org.jabref.logic.quality.consistency;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public record ConsistencyMessage(List<String> message, BibEntry bibEntry) implements Cloneable {

    @Override
    public Object clone() {
        return new ConsistencyMessage(message, bibEntry);
    }
}
