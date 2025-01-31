package org.jabref.logic.quality.consistency;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public record ConsistencyMessage(
        String message,
        BibEntry entry,
        List<StandardField> fields) implements Cloneable {

    @Override
    public String toString() {
        return "[" + entry().getCitationKey().orElse(entry().getAuthorTitleYear(50)) + "]";
    }

    @Override
    public Object clone() {
        return new ConsistencyMessage(message, entry, List.copyOf(fields));
    }
}
