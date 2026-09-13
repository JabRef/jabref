package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The file `just run-loop` looks for after JabRef quits: present, the recipe pulls, rebuilds and starts JabRef
/// again. It lives in the checkout's git directory, next to the announced entries.
public final class RestartMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartMarker.class);
    private static final String FILE_NAME = "restart-requested";

    private final Path file;

    public RestartMarker(Path file) {
        this.file = file;
    }

    /// The marker of the checkout whose git directory is `gitDir`.
    public static RestartMarker inGitDir(Path gitDir) {
        return new RestartMarker(gitDir.resolve(FILE_NAME));
    }

    /// Leaves the marker; `false` when it cannot be written, which is logged.
    public boolean place() {
        try {
            Files.writeString(file, "");
            return true;
        } catch (IOException e) {
            LOGGER.warn("Cannot write {}", file, e);
            return false;
        }
    }

    /// Takes the marker back, so the next quit is an ordinary one.
    public void withdraw() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Cannot remove {}", file, e);
        }
    }
}
