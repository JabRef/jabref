package org.jabref.logic.git.util;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.git.GitHandler;

/**
 * A registry that manages {@link GitHandler} instances per Git repository.
 * <p>
 * Ensures that each Git repository (identified by its root path) has a single {@code GitHandler} instance shared across the application.
 * <p>
 * Usage:
 * - {@link #get(Path)} — for known repository root paths (must contain a .git folder).
 * - {@link #fromAnyPath(Path)} — for arbitrary paths inside a Git repo; will locate the repo root first.
 */
public class GitHandlerRegistry {

    private final Map<Path, GitHandler> handlerCache = new ConcurrentHashMap<>();

    public GitHandler get(Path repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("Path must not be null");
        }

        Path normalized = repoPath.toAbsolutePath().normalize();
        return handlerCache.computeIfAbsent(normalized, GitHandler::new);
    }

    public Optional<GitHandler> fromAnyPath(Path anyPathInsideRepo) {
        if (anyPathInsideRepo == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        
        return GitHandler.findRepositoryRoot(anyPathInsideRepo)
                         .map(this::get);
    }
}
