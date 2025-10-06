package org.jabref.logic.util.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CitationKeyBasedFileFinder implements FileFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationKeyBasedFileFinder.class);

    private final boolean exactKeyOnly;

    CitationKeyBasedFileFinder(boolean exactKeyOnly) {
        this.exactKeyOnly = exactKeyOnly;
    }

    @Override
    public List<Path> findAssociatedFiles(@NonNull BibEntry entry,
                                          @NonNull List<Path> directories,
                                          List<String> extensions) throws IOException {
        Optional<String> citeKeyOptional = entry.getCitationKey();
        if (StringUtil.isBlank(citeKeyOptional)) {
            LOGGER.debug("No citation key found in entry {}", entry);
            return List.of();
        }
        String citeKey = citeKeyOptional.get();

        Function<Path, Boolean> filteringFunction;

        if (exactKeyOnly) {
            // LOGGER.debug("Found exact match for key {} in file {}", citeKey, file);
            filteringFunction = (Path p) -> FileUtil.getBaseName(p.getFileName().toString()).equals(citeKey);
        } else {
            // LOGGER.debug("Found non-exact match for key {} in file {}", citeKey, file);
            filteringFunction = (Path p) -> matches(p.getFileName().toString(), citeKey);
        }

        SortedSet<Path> result = findFilesByExtension(directories, extensions, filteringFunction);

        return result.stream().toList();
    }

    private boolean matches(String filename, String citeKey) {
        boolean startsWithKey = filename.startsWith(citeKey) || filename.startsWith(FileNameCleaner.cleanFileName(citeKey));
        if (startsWithKey) {
            // The file name starts with the key, that's already a good start
            // However, we do not want to match "JabRefa" for "JabRef" since this is probably a file belonging to another entry published in the same time / same name
            char charAfterKey = filename.charAt(citeKey.length());
            return !CitationKeyGenerator.APPENDIX_CHARACTERS.contains(Character.toString(charAfterKey));
        }
        return false;
    }

    /**
     * Returns a list of all files in the given directories which have one of the given extension.
     */
    private SortedSet<Path> findFilesByExtension(List<Path> directories,
                                                 @NonNull Collection<String> extensions,
                                                 Function<Path, Boolean> filteringFunction) throws IOException {
        BiPredicate<Path, BasicFileAttributes> isFileWithCorrectExtension = (path, _) -> !Files.isDirectory(path)
                && extensions.contains(FileUtil.getFileExtension(path).orElse("")) && filteringFunction.apply(path);

        SortedSet<Path> result = new TreeSet<>();
        for (Path directory : directories) {
            if (Files.exists(directory)) {
                try (Stream<Path> pathStream = Files.find(directory, Integer.MAX_VALUE, isFileWithCorrectExtension, FileVisitOption.FOLLOW_LINKS)) {
                    result.addAll(pathStream.collect(Collectors.toSet()));
                } catch (UncheckedIOException e) {
                    throw new IOException("Problem in finding files", e);
                }
            }
        }
        return result;
    }
}
