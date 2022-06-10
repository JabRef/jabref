package org.jabref.logic.bst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabase;

public record BstVMContext(List<BstEntry> entries,
                           Map<String, String> strings,
                           Map<String, Integer> integers,
                           Map<String, BstFunctions.BstFunction> functions,
                           BibDatabase bibDatabase) {

    public BstVMContext(List<BstEntry> entries, BibDatabase bibDatabase) {
        this(entries, new HashMap<>(), new HashMap<>(), new HashMap<>(), bibDatabase);

        this.integers.put("entry.max$", Integer.MAX_VALUE);
        this.integers.put("global.max$", Integer.MAX_VALUE);
    }
}
