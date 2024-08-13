package org.jabref.model.search.envent;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public record IndexAddedOrUpdatedEvent(List<BibEntry> addedEntries) {
}
