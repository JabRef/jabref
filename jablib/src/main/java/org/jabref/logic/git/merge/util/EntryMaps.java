package org.jabref.logic.git.merge.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class EntryMaps {
    public static Map<String, BibEntry> getCitationKeyToEntryMap(BibDatabaseContext context) {
        return context.getDatabase().getEntries().stream()
                      .filter(entry -> entry.getCitationKey().isPresent())
                      .collect(Collectors.toMap(
                              entry -> entry.getCitationKey().get(),
                              Function.identity(),
                              (existing, replacement) -> replacement,
                              LinkedHashMap::new
                      ));
    }
}
