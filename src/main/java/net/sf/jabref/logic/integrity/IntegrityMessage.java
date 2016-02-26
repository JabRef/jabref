package net.sf.jabref.logic.integrity;

import net.sf.jabref.model.entry.BibEntry;

public class IntegrityMessage implements Cloneable {

    private final BibEntry entry;
    private final String fieldName;
    private final String message;

    public IntegrityMessage(String message, BibEntry entry, String fieldName) {
        this.message = message;
        this.entry = entry;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return "[" + getEntry().getCiteKey() + "] in " + getFieldName() + ": " + getMessage();
    }

    public String getMessage() {
        return message;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public String getFieldName() {
        return fieldName;
    }

}
