package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;

/// Thrown when the target file of an [AtomicFileOutputStream] was modified by another process between opening the
/// stream and committing it. The commit is aborted so that the other process's changes are not overwritten silently.
@NullMarked
public class FileChangedException extends IOException {

    public FileChangedException(Path targetFile) {
        super("File " + targetFile + " was modified by another process while it was being written. The write was aborted to not overwrite the concurrent changes.");
    }
}
