package org.jabref.logic.git.conflicts;

import org.jabref.model.entry.BibEntry;

public record ThreeWayEntryConflict(
    BibEntry base,
    BibEntry local,
    BibEntry remote
) { }
