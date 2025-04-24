package org.jabref.logic.bst;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

public class BstEntry {

    public final BibEntry entry;

    // ENTRY: First sub list
    public final Map<String, String> fields = new HashMap<>();

    // ENTRY: Second sub list
    public final Map<String, Integer> localIntegers = new HashMap<>();

    // ENTRY: Third sub list
    public final Map<String, String> localStrings = new HashMap<>();

    public BstEntry(BibEntry e) {
        this.entry = e;
    }
}
