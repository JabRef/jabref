package org.jabref.logic.search;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.jabref.model.search.PostgreConstants;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.BIB_FIELDS_SCHEME;

@NullMarked
public class PostgreServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreServer.class);
    private static final String DATA_DIR_PREFIX = "jabref-embedded-pg-";
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir", System.getProperty("user.home", ".")));
    private static final String POSTMASTER_PID_FILENAME = "postmaster.pid";
    private static final String JABREF_MARKER_FILENAME = ".jabref-embedded-pg";
    private static final int ORPHAN_TERMINATION_TIMEOUT_SECONDS = 3;

    private final Path dataDirectory;
    private @Nullable EmbeddedPostgres embeddedPostgres;
    private @Nullable DataSource dataSource;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private @Nullable PostgreWatchdog watchdog;

    /// Creates a PostgreServer with the watchdog enabled.
    /// Use {@link #createWithoutWatchdog()} for CLI and test contexts where lifecycle is managed externally.
    public PostgreServer() {
        this(true);
    }

    private PostgreServer(boolean enableWatchdog) {
        cleanupOrphanedInstances();

        this.dataDirectory = TEMP_DIR.resolve(DATA_DIR_PREFIX + ProcessHandle.current().pid());

        EmbeddedPostgres embeddedPostgresInstance = null;
        try {
            if (Files.exists(dataDirectory)) {
                FileUtils.deleteDirectory(dataDirectory.toFile());
            }
            Files.createDirectories(dataDirectory);
            embeddedPostgresInstance = EmbeddedPostgres.builder()
                                                       .setDataDirectory(dataDirectory.toFile())
                                                       .setOutputRedirector(ProcessBuilder.Redirect.DISCARD)
                                                       .start();
            Files.createFile(dataDirectory.resolve(JABREF_MARKER_FILENAME));
            LOGGER.info("Postgres server started on port {}, data dir: {}",
                    embeddedPostgresInstance.getPort(), dataDirectory);
        } catch (IOException e) {
            LOGGER.error("Could not start Postgres server", e);
            if (embeddedPostgresInstance != null) {
                try {
                    embeddedPostgresInstance.close();
                } catch (IOException closeException) {
                    LOGGER.debug("Failed to close partially initialized Postgres", closeException);
                }
            }
            try {
                FileUtils.deleteDirectory(dataDirectory.toFile());
            } catch (IOException cleanupException) {
                LOGGER.debug("Failed to clean partially initialized data directory {}", dataDirectory, cleanupException);
            }
            this.embeddedPostgres = null;
            this.dataSource = null;
            return;
        }

        this.embeddedPostgres = embeddedPostgresInstance;
        this.dataSource = embeddedPostgresInstance.getPostgresDatabase();

        if (enableWatchdog) {
            long postgresPid = readPostgresPid();
            if (postgresPid > 0) {
                this.watchdog = new PostgreWatchdog();
                this.watchdog.start(ProcessHandle.current().pid(), postgresPid, dataDirectory);
            }
        }

        addTrigramExtension();
        createScheme();
        addFunctions();
    }

    /// Creates a PostgreServer without the watchdog.
    /// Intended for CLI mode and tests, where the lifecycle is managed via try-with-resources or {@code @AfterEach}.
    public static PostgreServer createWithoutWatchdog() {
        return new PostgreServer(false);
    }

    private void createScheme() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                LOGGER.debug("Creating scheme for bib fields");
                connection.createStatement().execute("DROP SCHEMA IF EXISTS " + BIB_FIELDS_SCHEME);
                connection.createStatement().execute("CREATE SCHEMA " + BIB_FIELDS_SCHEME);
            }
        } catch (SQLException e) {
            LOGGER.error("Could not create scheme for bib fields", e);
        }
    }

    private void addTrigramExtension() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                LOGGER.debug("Adding trigram extension to Postgres server");
                connection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            }
        } catch (SQLException e) {
            LOGGER.error("Could not add trigram extension to Postgres server", e);
        }
    }

    private void addFunctions() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                LOGGER.debug("Adding functions to Postgres server");
                for (String function : PostgreConstants.POSTGRES_FUNCTIONS) {
                    connection.createStatement().execute(function);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Could not add functions to Postgres server", e);
        }
    }

    public @Nullable Connection getConnection() {
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                LOGGER.error("Could not get connection to Postgres server", e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        if (watchdog != null) {
            watchdog.stop();
        }
        if (embeddedPostgres != null) {
            try {
                embeddedPostgres.close();
            } catch (IOException e) {
                LOGGER.error("Could not shutdown Postgres server", e);
            }
        }
    }

    private long readPostgresPid() {
        Path postmasterPidPath = dataDirectory.resolve(POSTMASTER_PID_FILENAME);
        if (!Files.exists(postmasterPidPath)) {
            return -1;
        }
        try {
            List<String> lines = Files.readAllLines(postmasterPidPath);
            if (!lines.isEmpty()) {
                return Long.parseLong(lines.getFirst().trim());
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Could not read postmaster.pid", e);
        }
        return -1;
    }

    static void cleanupOrphanedInstances() {
        Path tempDirectory = TEMP_DIR;
        if (!Files.isDirectory(tempDirectory)) {
            return;
        }
        long currentPid = ProcessHandle.current().pid();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDirectory, DATA_DIR_PREFIX + "*")) {
            for (Path staleDirectory : stream) {
                if (!Files.isDirectory(staleDirectory)) {
                    continue;
                }
                if (!Files.exists(staleDirectory.resolve(JABREF_MARKER_FILENAME))) {
                    continue;
                }
                String directoryName = staleDirectory.getFileName().toString();
                if (!directoryName.startsWith(DATA_DIR_PREFIX)) {
                    continue;
                }
                String pidString = directoryName.substring(DATA_DIR_PREFIX.length());
                long ownerPid;
                try {
                    ownerPid = Long.parseLong(pidString);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (ownerPid == currentPid) {
                    continue;
                }

                ProcessHandle ownerHandle = ProcessHandle.of(ownerPid)
                                                         .filter(ProcessHandle::isAlive)
                                                         .orElse(null);
                if (ownerHandle != null) {
                    continue;
                }

                Path postmasterPidPath = staleDirectory.resolve(POSTMASTER_PID_FILENAME);
                if (Files.exists(postmasterPidPath)) {
                    killOrphanedPostgres(postmasterPidPath);
                }

                deleteDirectoryWithRetry(staleDirectory);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not scan for orphaned PostgreSQL instances", e);
        }
    }

    /**
     * Attempts to delete a directory with retries to handle Windows file locking.
     * On Windows, file locks may not be released immediately after a process is killed,
     * so we retry with increasing delays to give the OS time to release handles.
     */
    private static void deleteDirectoryWithRetry(Path directory) {
        int maxAttempts = 5;
        long baseDelayMillis = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                FileUtils.deleteDirectory(directory.toFile());
                LOGGER.info("Cleaned up stale postgres data directory: {}", directory);
                return;
            } catch (IOException e) {
                if (attempt < maxAttempts) {
                    LOGGER.debug("Directory deletion attempt {}/{} failed for {}, retrying after delay",
                            attempt, maxAttempts, directory);
                    try {
                        Thread.sleep(baseDelayMillis * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.warn("Could not delete stale postgres data directory {}", directory, e);
                        return;
                    }
                } else {
                    LOGGER.warn("Could not delete stale postgres data directory {} after {} attempts", directory, maxAttempts, e);
                }
            }
        }
    }

    private static void killOrphanedPostgres(Path postmasterPidFile) {
        try {
            List<String> lines = Files.readAllLines(postmasterPidFile);
            if (lines.isEmpty()) {
                return;
            }
            long postgresPid = Long.parseLong(lines.getFirst().trim());
            ProcessHandle.of(postgresPid)
                         .ifPresent(processHandle -> {
                             ProcessHandle.Info info = processHandle.info();
                             // Check command name first; fall back to commandLine if unavailable
                             // (Windows often returns empty for command() due to access restrictions)
                             boolean isPostgres = info.command()
                                                      .map(cmd -> {
                                                          String name = Path.of(cmd).getFileName().toString();
                                                          return "postgres".equals(name) || "postgres.exe".equals(name);
                                                      })
                                                      .orElseGet(() -> info.commandLine()
                                                                           .map(cmdLine -> cmdLine.contains("postgres"))
                                                                           .orElse(false));
                             if (!isPostgres) {
                                 LOGGER.warn("Skipping cleanup for PID {} because it does not look like PostgreSQL", postgresPid);
                                 return;
                             }
                             LOGGER.info("Killing orphaned PostgreSQL process (PID: {})", postgresPid);
                             // Collect descendants BEFORE killing the main process, because on Windows
                             // child processes become orphans and may no longer appear as descendants
                             List<ProcessHandle> descendants = processHandle.descendants().toList();
                             processHandle.destroy();
                             try {
                                 processHandle.onExit().get(ORPHAN_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                             } catch (InterruptedException e) {
                                 Thread.currentThread().interrupt();
                                 processHandle.destroyForcibly();
                             } catch (ExecutionException | TimeoutException e) {
                                 processHandle.destroyForcibly();
                             }
                             // Kill all descendants and wait for them to exit — on Windows,
                             // file locks persist until processes fully terminate
                             descendants.forEach(ProcessHandle::destroyForcibly);
                             CompletableFuture<?>[] descendantExits = descendants.stream()
                                     .map(ProcessHandle::onExit)
                                     .toArray(CompletableFuture[]::new);
                             try {
                                 CompletableFuture.allOf(descendantExits)
                                                  .get(ORPHAN_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                             } catch (InterruptedException e) {
                                 Thread.currentThread().interrupt();
                             } catch (ExecutionException | TimeoutException e) {
                                 LOGGER.debug("Some postgres descendants did not exit within timeout");
                             }
                         });
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Could not read postmaster.pid for cleanup", e);
        }
    }
}
