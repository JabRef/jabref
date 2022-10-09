package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

public class GitIgnoreFileFilter implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreFileFilter.class);

    private Set<PathMatcher> gitIgnorePatterns;

    public GitIgnoreFileFilter(Path path) {
        Path currentPath = path;
        while ((currentPath != null) && !Files.exists(currentPath.resolve(".gitignore"))) {
            currentPath = currentPath.getParent();
        }
        if (currentPath == null) {
            // we did not find any gitignore, lets use the default
            gitIgnorePatterns = Set.of(".git", ".DS_Store", "desktop.ini", "Thumbs.db").stream()
                                   // duplicate code as below
                                   .map(line -> "glob:" + line)
                                   .map(matcherString -> FileSystems.getDefault().getPathMatcher(matcherString))
                                   .collect(Collectors.toSet());
        } else {
            Path gitIgnore = currentPath.resolve(".gitignore");
            try {
                Set<PathMatcher> plainGitIgnorePatternsFromGitIgnoreFile = Files.readAllLines(gitIgnore).stream()
                                                                                .map(line -> line.trim())
                                                                                .filter(not(String::isEmpty))
                                                                                .filter(line -> !line.startsWith("#"))
                                                                                // convert to Java syntax for Glob patterns
                                                                                .map(line -> "glob:" + line)
                                                                                .map(matcherString -> FileSystems.getDefault().getPathMatcher(matcherString))
                                                                                .collect(Collectors.toSet());
                gitIgnorePatterns = new HashSet<>(plainGitIgnorePatternsFromGitIgnoreFile);
                // we want to ignore ".gitignore" itself
                gitIgnorePatterns.add(FileSystems.getDefault().getPathMatcher("glob:.gitignore"));
            } catch (IOException e) {
                LOGGER.info("Could not read .gitignore from {}", gitIgnore, e);
                gitIgnorePatterns = Set.of();
            }
        }
    }

    @Override
    public boolean accept(Path path) throws IOException {
        // We assume that git does not stop at a patern, but tries all. We implement that behavior
        return gitIgnorePatterns.stream().noneMatch(filter ->
                // we need this one for "*.png"
                filter.matches(path.getFileName()) ||
                // we need this one for "**/*.png"
                filter.matches(path));
    }
}
