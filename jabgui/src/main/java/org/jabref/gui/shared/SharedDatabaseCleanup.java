package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.jabref.logic.shared.DatabaseConnectionProperties;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.shared.SharedDatabaseIdentity;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Coordinates shared-library cleanup with reconnecting and application shutdown.
@NullMarked
public final class SharedDatabaseCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseCleanup.class);
    private static final Set<CompletableFuture<Void>> ACTIVE = new HashSet<>();
    private static final Map<SharedDatabaseIdentity, CompletableFuture<Void>> BY_DATABASE = new HashMap<>();

    private SharedDatabaseCleanup() {
    }

    // [impl->req~shared-database.background-close~1]
    public static void close(BibDatabaseContext context, TaskExecutor executor) {
        DatabaseSynchronizer synchronizer = context.getDBMSSynchronizer();
        assert synchronizer != null;
        SharedDatabaseIdentity identity = SharedDatabaseIdentity.from(synchronizer.getConnectionProperties());
        synchronizer.prepareForClose();
        context.convertToLocalDatabase();
        context.clearDBMSSynchronizer();

        CompletableFuture<Void> completion = new CompletableFuture<>();
        @Nullable CompletableFuture<Void> previous;
        synchronized (ACTIVE) {
            previous = BY_DATABASE.put(identity, completion);
            ACTIVE.add(completion);
        }
        BackgroundTask<Void> cleanup = BackgroundTask.wrap(() -> {
            try {
                if (previous != null) {
                    try {
                        previous.join();
                    } catch (CompletionException exception) {
                        LOGGER.error("Earlier shared database cleanup failed", exception);
                    }
                }
                synchronizer.finishClose();
                completion.complete(null);
            } catch (Throwable exception) {
                LOGGER.error("Could not close shared database", exception);
                completion.completeExceptionally(exception);
            } finally {
                synchronized (ACTIVE) {
                    ACTIVE.remove(completion);
                    if (!completion.isCompletedExceptionally()) {
                        BY_DATABASE.remove(identity, completion);
                    }
                }
            }
        });
        try {
            cleanup.executeWith(executor);
        } catch (RejectedExecutionException exception) {
            completion.completeExceptionally(exception);
            synchronized (ACTIVE) {
                ACTIVE.remove(completion);
            }
            LOGGER.error("Could not schedule shared database cleanup", exception);
        }
    }

    /// Called by the connecting worker, before opening the same database again.
    public static void awaitDatabase(DatabaseConnectionProperties properties) throws SQLException {
        @Nullable CompletableFuture<Void> completion;
        synchronized (ACTIVE) {
            completion = BY_DATABASE.get(SharedDatabaseIdentity.from(properties));
        }
        if (completion != null) {
            try {
                completion.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for shared database cleanup", exception);
            } catch (ExecutionException exception) {
                throw new SQLException("Shared database cleanup failed", exception);
            }
        }
    }

    /// Called during application shutdown before stopping the task executor.
    public static void awaitAll() {
        List<CompletableFuture<Void>> pending;
        synchronized (ACTIVE) {
            pending = List.copyOf(ACTIVE);
        }
        boolean interrupted = false;
        for (CompletableFuture<Void> completion : pending) {
            boolean finished = false;
            while (!finished) {
                try {
                    completion.get();
                    finished = true;
                } catch (InterruptedException exception) {
                    interrupted = true;
                    LOGGER.debug("Interrupted while waiting for shared database cleanup", exception);
                } catch (ExecutionException exception) {
                    LOGGER.error("Shared database cleanup failed", exception);
                    finished = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
