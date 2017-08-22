package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CiteKeyBasedFileFinder implements FileFinder {

    private static final Log LOGGER = LogFactory.getLog(CiteKeyBasedFileFinder.class);
    private final boolean exactKeyOnly;

    CiteKeyBasedFileFinder(boolean exactKeyOnly) {
        this.exactKeyOnly = exactKeyOnly;
    }

    @Override
    public Map<BibEntry, List<Path>> findAssociatedFiles(List<BibEntry> entries, List<Path> directories, List<String> extensions) {
        Objects.requireNonNull(directories);
        Objects.requireNonNull(entries);

        Map<BibEntry, List<Path>> result = new HashMap<>();

        // First scan directories
        Set<Path> filesWithExtension = findFilesByExtension(directories, extensions);

        // Initialize Result-Set
        for (BibEntry entry : entries) {
            result.put(entry, new ArrayList<>());
        }

        // Now look for keys
        nextFile:
        for (Path file : filesWithExtension) {
            String name = file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            // First, look for exact matches:
            for (BibEntry entry : entries) {
                Optional<String> citeKey = entry.getCiteKeyOptional();
                if ((citeKey.isPresent()) && !citeKey.get().isEmpty() && (dot > 0)
                        && name.substring(0, dot).equals(citeKey.get())) {
                    result.get(entry).add(file);
                    continue nextFile;
                }
            }
            // If we get here, we did not find any exact matches. If non-exact
            // matches are allowed, try to find one:
            if (!exactKeyOnly) {
                for (BibEntry entry : entries) {
                    Optional<String> citeKey = entry.getCiteKeyOptional();
                    if ((citeKey.isPresent()) && !citeKey.get().isEmpty() && name.startsWith(citeKey.get())) {
                        result.get(entry).add(file);
                        continue nextFile;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns a list of all files in the given directories which have one of the given extension.
     */
    public Set<Path> findFilesByExtension(List<Path> directories, List<String> extensions) {
        Objects.requireNonNull(extensions, "Extensions must not be null!");

        BiPredicate<Path, BasicFileAttributes> isFileWithCorrectExtension = (path, attributes) ->
                !Files.isDirectory(path)
                        && extensions.contains(FileHelper.getFileExtension(path).orElse(""));

        Set<Path> result = new HashSet<>();
        for (Path directory : directories) {
            try (Stream<Path> files = Files.find(directory, Integer.MAX_VALUE, isFileWithCorrectExtension)) {
                result.addAll(files.collect(Collectors.toSet()));
            } catch (IOException e) {
                LOGGER.error("Problem in finding files", e);
            }
        }
        return result;
    }
}
