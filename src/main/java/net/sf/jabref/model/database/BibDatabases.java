package net.sf.jabref.model.database;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

public class BibDatabases {

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
