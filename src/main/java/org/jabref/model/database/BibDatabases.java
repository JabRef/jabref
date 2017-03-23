package org.jabref.model.database;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.IdGenerator;

public class BibDatabases {

    private BibDatabases() {
    }

    /**
     * Gets a collection of bibentries and sets an ID for every entry. After that
     * all entries will be inserted into a new BibDatabase.
     *
     * @param bibentries a collection that contains {@link BibEntry}
     * @return BibDatabase that contains the entries
     */
    public static BibDatabase createDatabase(Collection<BibEntry> bibentries) {
        BibDatabase database = new BibDatabase();

        for (BibEntry entry : bibentries) {
            entry.setId(IdGenerator.next());
            database.insertEntry(entry);
        }

        return database;
    }

    /**
     * Receives a Collection of BibEntry instances, iterates through them, and
     * removes all entries that have no fields set. This is useful for rooting out
     * an unsucessful import (wrong format) that returns a number of empty entries.
     */
    public static List<BibEntry> purgeEmptyEntries(Collection<BibEntry> entries) {
        return entries.stream().filter(e -> !e.getFieldNames().isEmpty()).collect(Collectors.toList());
    }
}
