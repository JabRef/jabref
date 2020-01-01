package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.IdGenerator;

public class BibDatabases {

    private BibDatabases() {
    }

    /**
     * Gets a collection of entries and sets an ID for every entry. After that
     * all entries are inserted into a new BibDatabase.
     *
     * @param entries a collection that contains {@link BibEntry}
     * @return BibDatabase that contains the entries
     */
    public static BibDatabase createDatabase(Collection<BibEntry> entries) {
        BibDatabase database = new BibDatabase();

        for (BibEntry entry : entries) {
            entry.setId(IdGenerator.next());
        }
        database.insertEntries(new ArrayList<>(entries));

        return database;
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
