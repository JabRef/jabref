package net.sf.jabref.groups;

import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;

/**
 * This group contains all entries.
 */
public class AllEntriesGroup extends AbstractGroup implements SearchRule {
    public static final String ID = "AllEntriesGroup:";

    public AllEntriesGroup() {
        super("All Entries");
    }
    
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

    public boolean supportsAdd() {
        return false;
    }

    public boolean supportsRemove() {
        return false;
    }

    public AbstractUndoableEdit addSelection(BasePanel basePanel) {
        // not supported -> ignore
        return null;
    }

    public AbstractUndoableEdit removeSelection(BasePanel basePanel) {
        // not supported -> ignore
        return null;
    }

    public boolean contains(Map searchOptions, BibtexEntry entry) {
        return true; // contains everything
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
