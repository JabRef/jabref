package net.sf.jabref.logic.integrity;

import net.sf.jabref.model.entry.BibtexEntry;

public class IntegrityMessage implements Cloneable {

    private final BibtexEntry entry;
    private final String fieldName;
    private final String message;

    public IntegrityMessage(String message, BibtexEntry entry, String fieldName) {
        this.message = message;
        this.entry = entry;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return "[" + entry.getCiteKey() + "] in " + fieldName + ": " + message;
    }

    public String getMessage() {
        return message;
    }

    public BibtexEntry getEntry() {
        return entry;
    }

    public String getFieldName() {
        return fieldName;
    }

}
