package net.sf.jabref.groups;

import java.util.*;
import java.util.Map;
import net.sf.jabref.*;

/**
 * This group contains all entries.
 */
public class AllEntriesGroup extends AbstractGroup implements SearchRule {
    public static final String ID = "AllEntriesGroup:";

    private static final String m_name = "All Entries";

    public static AbstractGroup fromString(String s) throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: AllEntriesGroup cannot be created from \""
                            + s + "\"");
        return new AllEntriesGroup();
    }

    public SearchRule getSearchRule() {
        return this;
    }

    public String getName() {
        return m_name;
    }

    public boolean supportsAdd() {
        return false;
    }

    public boolean supportsRemove() {
        return false;
    }

    public void addSelection(BasePanel basePanel) {
        // not supported -> ignore
    }

    public void removeSelection(BasePanel basePanel) {
        // not supported -> ignore
    }

    public int contains(Map searchOptions, BibtexEntry entry) {
        return 1; // contains everything
    }

    public AbstractGroup deepCopy() {
        return new AllEntriesGroup();
    }

    public int applyRule(Map searchStrings, BibtexEntry bibtexEntry) {
        return 1; // contains everything
    }

    public boolean equals(Object o) {
        return o instanceof AllEntriesGroup;
    }

    public String toString() {
        return ID;
    }
}
