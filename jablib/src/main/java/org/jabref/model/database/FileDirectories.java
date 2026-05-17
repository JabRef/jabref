package org.jabref.model.database;

import java.nio.file.Path;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Holds the configured file directories in a fixed order.
@NullMarked
public record FileDirectories(@Nullable Path userDirectory,
                              @Nullable Path libraryDirectory,
                              @Nullable Path fallbackDirectory) {

    public Optional<Path> getUserDirectoryOpt() {
        return Optional.ofNullable(userDirectory);
    }

    public Optional<Path> getLibraryDirectoryOpt() {
        return Optional.ofNullable(libraryDirectory);
    }

    public Optional<Path> getFallbackDirectoryOpt() {
        return Optional.ofNullable(fallbackDirectory);
    }
}
