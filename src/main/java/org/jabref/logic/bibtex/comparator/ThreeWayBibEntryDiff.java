package org.jabref.logic.bibtex.comparator;

import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ThreeWayBibEntryDiff {
    private final BibEntry baseEntry;
    private final BibEntry localEntry;
    private final BibEntry remoteEntry;
    private final Map<Field, FieldChange> fieldChanges;

    public ThreeWayBibEntryDiff(BibEntry baseEntry, BibEntry localEntry, BibEntry remoteEntry) {
        this.baseEntry = baseEntry;
        this.localEntry = localEntry;
        this.remoteEntry = remoteEntry;
        this.fieldChanges = findFieldChanges();
    }
    private Map<Field>
}
