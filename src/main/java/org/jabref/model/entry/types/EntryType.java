package org.jabref.model.entry.types;

public interface EntryType<T extends Enum<T>> {

    /**
     * Returns the tag name of the entry type.
     *
     * @return tag name of the entry type.
     */
    String getName();

    String getDisplayName();

}
