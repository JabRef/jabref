package org.jabref.model.entry.types;

public interface EntryType {

    /**
     * Returns the tag name of the entry type.
     */
    String getName();

    /**
     * Returns the name presented in the UI
     */
    String getDisplayName();
}
