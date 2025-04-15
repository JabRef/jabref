package org.jabref.logic.integrity;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public record IntegrityMessage(
        String message,
        BibEntry entry,
        Field field) implements Cloneable {

    public IntegrityMessage(IntegrityIssue issue, BibEntry entry, Field field) {
        this(issue.getText(), entry, field);
    }

    @Override
    public String toString() {
        return "[" + entry().getCitationKey().orElse(entry().getAuthorTitleYear(50)) + "] in " + field.getDisplayName() + ": " + message();
    }

    @Override
    public Object clone() {
        return new IntegrityMessage(message, entry, field);
    }
}
