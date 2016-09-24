package net.sf.jabref.model.groups;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

public class EntriesGroupChange {

    private Set<BibEntry> oldEntries;
    private Set<BibEntry> newEntries;
    private List<FieldChange> entryChanges;

    public EntriesGroupChange(Set<BibEntry> oldEntries, Set<BibEntry> newEntries) {
        this(oldEntries, newEntries, Collections.emptyList());
    }

    public EntriesGroupChange(List<FieldChange> entryChanges) {
        this(Collections.emptySet(), Collections.emptySet(), entryChanges);
    }

    public EntriesGroupChange(Set<BibEntry> oldEntries, Set<BibEntry> newEntries,
            List<FieldChange> entryChanges) {
        this.oldEntries = oldEntries;
        this.newEntries = newEntries;
        this.entryChanges = entryChanges;
    }

    public Set<BibEntry> getOldEntries() {
        return oldEntries;
    }

    public Set<BibEntry> getNewEntries() {
        return newEntries;
    }

    public Iterable<FieldChange> getEntryChanges() {
        return entryChanges;
    }

}
