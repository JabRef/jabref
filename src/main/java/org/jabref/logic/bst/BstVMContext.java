package org.jabref.logic.bst;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.jabref.model.database.BibDatabase;

public record BstVMContext(List<BstEntry> entries,
                           Map<String, String> strings,
                           Map<String, Integer> integers,
                           Map<String, BstFunctions.BstFunction> functions,
                           Stack<Object> stack,
                           BibDatabase bibDatabase,
                           Optional<Path> path) {
    public BstVMContext(List<BstEntry> entries, BibDatabase bibDatabase, Path path) {
        this(entries, new HashMap<>(), new HashMap<>(), new HashMap<>(), new Stack<>(), bibDatabase, Optional.ofNullable(path));
    }
}
