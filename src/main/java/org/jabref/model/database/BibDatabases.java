package org.jabref.model.database;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;

public class BibDatabases {

    private BibDatabases() {
    }

    /**
     * Receives a Collection of BibEntry instances, iterates through them, and
     * removes all entries that have no fields set. This is useful for rooting out
     * an unsucessful import (wrong format) that returns a number of empty entries.
     */
    public static List<BibEntry> purgeEmptyEntries(Collection<BibEntry> entries) {
        return entries.stream()
                      .filter(entry -> !entry.getFields().isEmpty())
                      .collect(Collectors.toList());
    }
}
