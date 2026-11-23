package org.jabref.logic.bst;

import java.nio.file.Path;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record BstVMContext(List<BstEntry> entries,
                           Map<String, String> strings,
                           Map<String, Integer> integers,
                           Map<String, BstFunctions.BstFunction> functions,
                           Deque<Object> stack,
                           BibDatabase bibDatabase,
                           Optional<Path> path,
                           String preamble) {
    public BstVMContext(List<BstEntry> entries, BibDatabase bibDatabase, @Nullable Path path) {
        this(entries, bibDatabase, path, bibDatabase.getPreamble().orElse(""));
    }

    public BstVMContext(List<BstEntry> entries, BibDatabase bibDatabase, @Nullable Path path, String preamble) {
        // LinkedList instead of ArrayDeque, because we (currently) need null support
        this(entries, new HashMap<>(), new HashMap<>(), new HashMap<>(), new LinkedList<>(), bibDatabase, Optional.ofNullable(path), preamble);
    }
}
