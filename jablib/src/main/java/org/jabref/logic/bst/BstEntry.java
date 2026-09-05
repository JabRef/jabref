package org.jabref.logic.bst;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BstEntry {

    public final BibEntry entry;

    // ENTRY: First sub list
    public final Map<String, @Nullable String> fields = new HashMap<>();

    // ENTRY: Second sub list
    public final Map<String, Integer> localIntegers = new HashMap<>();

    // ENTRY: Third sub list
    public final Map<String, @Nullable String> localStrings = new HashMap<>();

    public BstEntry(BibEntry bibEntry) {
        this.entry = bibEntry;
    }
}
