package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

public class GitIgnoreFileFilter implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreFileFilter.class);

    private final Set<PathMatcher> gitIgnorePatterns;
    private final Path baseDir;

    public GitIgnoreFileFilter(Path path) {
        Path currentPath = path;
        while ((currentPath != null) && !Files.exists(currentPath.resolve(".gitignore"))) {
            currentPath = currentPath.getParent();
        }
        if (currentPath == null) {
            // we did not find any gitignore, set baseDir to provided path and use default ignores
            this.baseDir = path;
            gitIgnorePatterns = Set.of(".git", ".DS_Store", "desktop.ini", "Thumbs.db").stream()
                                   // duplicate code as below
                                   .map(line -> "glob:" + line)
                                   .map(matcherString -> FileSystems.getDefault().getPathMatcher(matcherString))
                                   .collect(Collectors.toSet());
        } else {
            this.baseDir = currentPath;
            Path gitIgnore = currentPath.resolve(".gitignore");
            Set<PathMatcher> patterns;
            try {
                patterns = Files.readAllLines(gitIgnore).stream()
                                .map(String::trim)
                                .filter(not(String::isEmpty))
                                .filter(line -> !line.startsWith("#"))
                                // expand patterns so that leading "**/" also matches files in the base directory
                                .flatMap(line -> {
                                    if (line.startsWith("**/")) {
                                        return Stream.of(line, line.substring(3));
                                    }
                                    return Stream.of(line);
                                })
                                // convert to Java syntax for Glob patterns
                                .map(line -> "glob:" + line)
                                .map(matcherString -> FileSystems.getDefault().getPathMatcher(matcherString))
                                .collect(Collectors.toSet());
                // we want to ignore ".gitignore" itself
                patterns.add(FileSystems.getDefault().getPathMatcher("glob:.gitignore"));
            } catch (IOException e) {
                LOGGER.info("Could not read .gitignore from {}", gitIgnore, e);
                patterns = Set.of();
            }
            gitIgnorePatterns = patterns;
        }
    }

    @Override
    public boolean accept(Path path) throws IOException {
        // Match patterns relative to baseDir because .gitignore patterns are applied relative to their location
        Path relative = safeRelativize(baseDir, path);
        // We assume that git does not stop at a pattern, but tries all. We implement that behavior
        return gitIgnorePatterns.stream().noneMatch(filter ->
                // for patterns like "*.png" or ".gitignore"
                filter.matches(relative.getFileName()) ||
                        // for patterns like "ignore/*" or "**/*.png"
                        filter.matches(relative));
    }

    private static Path safeRelativize(Path base, Path child) {
        try {
            return base.relativize(child);
        } catch (IllegalArgumentException e) {
            // If paths are on different roots, fall back to just the file name for relative matching
            return child.getFileName();
        }
    }
}
