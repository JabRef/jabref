package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

/// The files of a directory library whose write is still pending. Writes are debounced per
/// file with a trailing-edge timer: every change re-arms the file's timer, so the write fires
/// once the changes pause and always persists the latest state. [#flush] writes everything
/// now. A file that could not be written stays pending — the next attempt retries it and the
/// caller can report it. All work happens under the shared lock.
@NullMarked
class PendingWrites {

    /// @return whether the file was written; `false` asks for another attempt one debounce
    /// later (the model has yet to take in an external edit, see [SidecarWriteBack])
    interface FileWriter {
        boolean write(Path file, boolean immediate) throws IOException;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingWrites.class);

    private static final Duration DEBOUNCE = Duration.ofMillis(500);

    private final Object lock;
    private final ScheduledExecutorService executor;
    private final FileWriter writer;
    private final SequencedSet<Path> dirtyFiles = new LinkedHashSet<>();
    private final Map<Path, ScheduledFuture<?>> timers = new HashMap<>();

    PendingWrites(Object lock, ScheduledExecutorService executor, FileWriter writer) {
        this.lock = lock;
        this.executor = executor;
        this.writer = writer;
    }

    void schedule(Path file) {
        synchronized (lock) {
            dirtyFiles.add(file);
            Optional.ofNullable(timers.remove(file)).ifPresent(pending -> pending.cancel(false));
            if (executor.isShutdown()) {
                // Written by the final flush
                return;
            }
            timers.put(file, executor.schedule(() -> writeScheduled(file), DEBOUNCE.toMillis(), TimeUnit.MILLISECONDS));
        }
    }

    /// Writes every pending file now.
    ///
    /// @return the files whose changes could not be written; they stay pending
    List<Path> flush() {
        synchronized (lock) {
            timers.values().forEach(pending -> pending.cancel(false));
            timers.clear();
            return write(List.copyOf(dirtyFiles), true);
        }
    }

    private void writeScheduled(Path file) {
        synchronized (lock) {
            timers.remove(file);
            write(List.of(file), false);
        }
    }

    private List<Path> write(List<Path> files, boolean immediate) {
        List<Path> failed = new ArrayList<>();
        for (Path file : files) {
            if (!dirtyFiles.contains(file)) {
                continue;
            }
            try {
                if (writer.write(file, immediate)) {
                    dirtyFiles.remove(file);
                } else {
                    schedule(file);
                }
            } catch (IOException | JacksonException e) {
                LOGGER.error("Could not write {}", file, e);
                failed.add(file);
            }
        }
        return failed;
    }
}
