package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>ChangedFieldEvent</code> is fired when a field of <code>BibEntry</code> has been modified, removed or added.
 */
public class ChangedFieldEvent extends ChangedEntryEvent {

    private final String fieldName;
    private final String newValue;

    /**
     * @param bibEntry Affected BibEntry object
     * @param fieldName Name of field which has been changed
     * @param newValue new field value
     */
    public ChangedFieldEvent(BibEntry bibEntry, String fieldName, String newValue) {
        super(bibEntry);
        this.fieldName = fieldName;
        this.newValue = newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getNewValue() {
        return newValue;
    }

}
