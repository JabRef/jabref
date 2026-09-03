package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Existence, size, and modification time of a file at one point in time — a version stamp for optimistic concurrency
/// control on files: capture a snapshot before a multi-step operation, compare against a fresh one before committing,
/// and treat a mismatch as a concurrent modification by another process.
///
/// The comparison is best-effort: a concurrent write within the file system's timestamp resolution that also keeps
/// the size identical goes undetected.
///
/// @param exists           whether the file existed; when `false`, size and modification time carry no meaning
/// @param size             the file size in bytes, `-1` when the file did not exist
/// @param lastModifiedTime the modification time, `null` when the file did not exist
@NullMarked
public record FileSnapshot(boolean exists, long size, @Nullable FileTime lastModifiedTime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSnapshot.class);

    private static final FileSnapshot ABSENT = new FileSnapshot(false, -1, null);

    /// Captures the current state of the given file. A missing file yields a snapshot with `exists() == false`.
    ///
    /// @return the snapshot, or `null` when the attributes could not be read (detection should then be disabled
    /// rather than misreporting a conflict)
    @Nullable
    public static FileSnapshot read(Path file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            return new FileSnapshot(true, attributes.size(), attributes.lastModifiedTime());
        } catch (NoSuchFileException exception) {
            return ABSENT;
        } catch (IOException exception) {
            LOGGER.warn("Could not read attributes of {}", file, exception);
            return null;
        }
    }

    /// Best-effort check whether the given file still is in the state captured by this snapshot. An unreadable current
    /// state counts as a mismatch.
    public boolean matches(Path file) {
        return this.equals(read(file));
    }
}
