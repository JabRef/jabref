package org.jabref.logic.bst;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

public record BstVMContext(List<BstEntry> entries,
                           Map<String, String> strings,
                           Map<String, Integer> integers,
                           Map<String, BstFunctions.BstFunction> functions,
                           Deque<Object> stack,
                           BibDatabase bibDatabase,
                           Optional<Path> path) {
    public BstVMContext(List<BstEntry> entries, BibDatabase bibDatabase, Path path) {
        this(entries, new HashMap<>(), new HashMap<>(), new HashMap<>(), new ArrayDeque<>(), bibDatabase, Optional.ofNullable(path));
    }
}
