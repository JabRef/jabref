package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.EntryTypeFactory;

/*
 * Camel casing of entry type string, unknown entry types gets a leading capital
 *
 * Example (known): inbook -> InBook
 * Example (unknown): banana -> Banana
 */
public class EntryTypeFormatter implements LayoutFormatter {

    /**
     * Input: entry type as a string
     */
    @Override
    public String format(String entryType) {
        return EntryTypeFactory.parse(entryType).getDisplayName();
    }
}
