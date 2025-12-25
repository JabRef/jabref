package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BrokenLinkedFileNameBasedFileFinder implements FileFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokenLinkedFileNameBasedFileFinder.class);

    @Override
    public List<Path> findAssociatedFiles(@NonNull BibEntry entry,
                                          @NonNull List<Path> directories,
                                          List<String> extensions) throws IOException {
        Set<String> brokenLinkedFileNames = getBrokenLinkedFileNames(entry, directories);

        if (brokenLinkedFileNames.isEmpty()) {
            return List.of();
        }

        List<Path> matches = new ArrayList<>();
        for (Path directory : directories) {
            matches.addAll(findAssociatedFilesInDirectory(directory, brokenLinkedFileNames, extensions));
        }
        return matches;
    }

    private static List<Path> findAssociatedFilesInDirectory(
            Path directory,
            Set<String> brokenLinkedFileNames,
            List<String> extensions) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            // find all files (not directory) that match:
            // 1. it has the same name (case-insensitive) as one of the broken linked file names
            // 2. its file extension matches one specified in extensions
            return walk.filter(path -> !Files.isDirectory(path))
                       .filter(path -> brokenLinkedFileNames.contains(FileUtil.getBaseName(path).toLowerCase()))
                       .filter(path -> {
                           String pathExtension = FileUtil.getFileExtension(path).orElse("");
                           return extensions.stream().anyMatch(ext -> ext.equalsIgnoreCase(pathExtension));
                       })
                       .toList();
        }
    }

    private static @NonNull Set<String> getBrokenLinkedFileNames(@NonNull BibEntry entry, @NonNull List<Path> directories) {
        return entry.getFiles().stream()
                    .filter(linkedFile -> linkedFile.findIn(directories).isEmpty())
                    .map(linkedFile -> FileUtil.getBaseName(linkedFile.getLink()).toLowerCase())
                    .collect(Collectors.toSet());
    }
}
