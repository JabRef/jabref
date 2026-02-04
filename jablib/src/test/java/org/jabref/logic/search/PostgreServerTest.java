package org.jabref.logic.search;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("embeddedPostgres")
class PostgreServerTest {
    private static final Path SYSTEM_TEMP_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir", System.getProperty("user.home", ".")));

    @Test
    void cleanupRemovesStaleDirectoryWhenOwnerIsDead() throws IOException {
        long deadOwnerPid = 99999L;
        Path staleDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);
        Files.createDirectories(staleDirectory);
        Files.createFile(staleDirectory.resolve(".jabref-embedded-pg"));
        Files.writeString(staleDirectory.resolve("postmaster.pid"), "88888" + System.lineSeparator());

        PostgreServer.cleanupOrphanedInstances();

        assertFalse(Files.exists(staleDirectory));
    }

    @Test
    void cleanupKeepsDirectoryWhenOwnerIsAlive() throws IOException {
        long aliveOwnerPid = ProcessHandle.current().pid();
        Path dataDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + aliveOwnerPid);
        if (Files.exists(dataDirectory)) {
            FileUtils.deleteDirectory(dataDirectory.toFile());
        }
        Files.createDirectories(dataDirectory);
        Files.createFile(dataDirectory.resolve(".jabref-embedded-pg"));
        Files.writeString(dataDirectory.resolve("postmaster.pid"), Long.toString(aliveOwnerPid));

        try {
            PostgreServer.cleanupOrphanedInstances();
            assertTrue(Files.exists(dataDirectory));
        } finally {
            FileUtils.deleteDirectory(dataDirectory.toFile());
        }
    }

    @Test
    void cleanupHandlesDirectoryWithoutPostmasterPid() throws IOException {
        long deadOwnerPid = 99998L;
        Path staleDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);
        Files.createDirectories(staleDirectory);
        Files.createFile(staleDirectory.resolve(".jabref-embedded-pg"));

        PostgreServer.cleanupOrphanedInstances();

        assertFalse(Files.exists(staleDirectory), "Directory without postmaster.pid should still be cleaned up");
    }

    @Test
    void cleanupHandlesMultipleStaleDirectories() throws IOException {
        Path staleDirectory1 = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-99991");
        Path staleDirectory2 = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-99992");
        Path staleDirectory3 = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-99993");

        Files.createDirectories(staleDirectory1);
        Files.createDirectories(staleDirectory2);
        Files.createDirectories(staleDirectory3);
        Files.createFile(staleDirectory1.resolve(".jabref-embedded-pg"));
        Files.createFile(staleDirectory2.resolve(".jabref-embedded-pg"));
        Files.createFile(staleDirectory3.resolve(".jabref-embedded-pg"));
        Files.writeString(staleDirectory1.resolve("postmaster.pid"), "88881\n");
        Files.writeString(staleDirectory2.resolve("postmaster.pid"), "88882\n");

        PostgreServer.cleanupOrphanedInstances();

        assertFalse(Files.exists(staleDirectory1), "Stale directory 1 should be cleaned");
        assertFalse(Files.exists(staleDirectory2), "Stale directory 2 should be cleaned");
        assertFalse(Files.exists(staleDirectory3), "Stale directory 3 should be cleaned");
    }

    @Test
    void cleanupSkipsKillForNonPostgresCommandButDeletesDirectory() throws IOException {
        long deadOwnerPid = 99994L;
        Path staleDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);
        Files.createDirectories(staleDirectory);
        Files.createFile(staleDirectory.resolve(".jabref-embedded-pg"));
        // postmaster pid set to current java process; killOrphanedPostgres should skip kill
        Files.writeString(staleDirectory.resolve("postmaster.pid"), Long.toString(ProcessHandle.current().pid()));

        PostgreServer.cleanupOrphanedInstances();

        assertFalse(Files.exists(staleDirectory), "Directory should still be removed even if postgres PID check is skipped");
        assertTrue(ProcessHandle.current().isAlive(), "Current process must remain alive");
    }

    @Test
    void cleanupIgnoresDirectoryWithoutMarkerFile() throws IOException {
        long deadOwnerPid = 99995L;
        Path directoryWithoutMarker = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);
        Files.createDirectories(directoryWithoutMarker);
        Files.writeString(directoryWithoutMarker.resolve("postmaster.pid"), "88885\n");

        try {
            PostgreServer.cleanupOrphanedInstances();
            assertTrue(Files.exists(directoryWithoutMarker),
                    "Directory without marker file should be ignored by cleanup");
        } finally {
            FileUtils.deleteDirectory(directoryWithoutMarker.toFile());
        }
    }

    @Test
    void cleanupIgnoresNonNumericDirectoryNames() throws IOException {
        Path nonNumericDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-notanumber");
        Files.createDirectories(nonNumericDirectory);
        Files.createFile(nonNumericDirectory.resolve(".jabref-embedded-pg"));

        try {
            PostgreServer.cleanupOrphanedInstances();
            assertTrue(Files.exists(nonNumericDirectory), "Non-numeric directory should be ignored");
        } finally {
            FileUtils.deleteDirectory(nonNumericDirectory.toFile());
        }
    }

    @Test
    void fullLifecycleStartAndClose() throws SQLException {
        long currentPid = ProcessHandle.current().pid();
        Path expectedDataDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + currentPid);

        try (PostgreServer server = PostgreServer.createWithoutWatchdog()) {
            assertTrue(Files.exists(expectedDataDirectory), "Data directory should exist while server is running");
            assertTrue(Files.exists(expectedDataDirectory.resolve("postmaster.pid")),
                    "postmaster.pid should exist while server is running");

            Connection connection = server.getConnection();
            assertNotNull(connection, "Should be able to get a connection from running server");
            connection.close();
        }
    }

    @Test
    void postmasterPidContainsValidPid() throws IOException {
        long currentPid = ProcessHandle.current().pid();
        Path expectedDataDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + currentPid);

        try (PostgreServer server = PostgreServer.createWithoutWatchdog()) {
            Path postmasterPidPath = expectedDataDirectory.resolve("postmaster.pid");
            assertTrue(Files.exists(postmasterPidPath), "postmaster.pid should exist");

            List<String> lines = Files.readAllLines(postmasterPidPath);
            assertFalse(lines.isEmpty(), "postmaster.pid should not be empty");

            long postgresPid = Long.parseLong(lines.getFirst().trim());
            assertTrue(postgresPid > 0, "Postgres PID should be positive");
            assertTrue(ProcessHandle.of(postgresPid).map(ProcessHandle::isAlive).orElse(false),
                    "Postgres process should be alive while server is running");
        }
    }

    @Test
    void closeIsIdempotent() {
        PostgreServer server = PostgreServer.createWithoutWatchdog();
        assertDoesNotThrow(() -> {
            server.close();
            server.close();
            server.close();
        }, "Calling close() multiple times should not throw");
    }

    @Test
    void serverExecutesQuerySuccessfully() throws SQLException {
        try (PostgreServer server = PostgreServer.createWithoutWatchdog();
             Connection connection = server.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1");
            assertTrue(resultSet.next(), "SELECT 1 should return a row");
            assertEquals(1, resultSet.getInt(1), "SELECT 1 should return 1");
        }
    }

    @Test
    void staleDirectoryCleanedOnRestart() throws IOException {
        long currentPid = ProcessHandle.current().pid();
        Path dataDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + currentPid);

        // First server lifecycle
        PostgreServer firstServer = PostgreServer.createWithoutWatchdog();
        assertTrue(Files.exists(dataDirectory));
        firstServer.close();

        // Simulate stale state: recreate directory with a dead PID in postmaster.pid
        long deadPid = 99997L;
        Path staleDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadPid);
        Files.createDirectories(staleDirectory);
        Files.createFile(staleDirectory.resolve(".jabref-embedded-pg"));
        Files.writeString(staleDirectory.resolve("postmaster.pid"), "88877\n");

        // Second server should clean the stale directory
        try (PostgreServer secondServer = PostgreServer.createWithoutWatchdog()) {
            assertFalse(Files.exists(staleDirectory), "Stale directory should be cleaned on new server start");
            assertNotNull(secondServer.getConnection(), "Second server should be functional");
        }
    }

    @Test
    void cleanupKillsRealOrphanedPostgresProcess() throws Exception {
        // Core issue #12844 scenario: JabRef died, leaving an orphaned postgres process.
        // Cleanup should detect the dead owner and kill the real postgres process.
        // Note: directory deletion is tested separately — in this test the EmbeddedPostgres
        // Java object holds an epg-lock FileLock that prevents deletion within the same JVM.
        // In a real crash scenario the owning JVM would be dead and the lock released.
        long deadOwnerPid = 99990L;
        Path staleDataDir = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);

        if (Files.exists(staleDataDir)) {
            FileUtils.deleteDirectory(staleDataDir.toFile());
        }
        Files.createDirectories(staleDataDir);

        // Start a real postgres in this directory (simulating an orphaned instance)
        EmbeddedPostgres orphanedPostgres = EmbeddedPostgres.builder()
                .setDataDirectory(staleDataDir.toFile())
                .setOutputRedirector(ProcessBuilder.Redirect.DISCARD)
                .start();
        // Place the JabRef marker so cleanup recognizes this as ours
        Files.createFile(staleDataDir.resolve(".jabref-embedded-pg"));

        // Read the real postgres PID
        Path postmasterPidPath = staleDataDir.resolve("postmaster.pid");
        assertTrue(Files.exists(postmasterPidPath), "postmaster.pid should exist after starting postgres");
        long postgresPid = Long.parseLong(Files.readAllLines(postmasterPidPath).getFirst().trim());
        assertTrue(ProcessHandle.of(postgresPid).map(ProcessHandle::isAlive).orElse(false),
                "Orphaned postgres should be alive before cleanup");

        try {
            // Cleanup should detect dead owner and kill the real postgres process
            PostgreServer.cleanupOrphanedInstances();

            // Give a brief moment for process exit to be visible to the OS
            Thread.sleep(500);
            assertFalse(ProcessHandle.of(postgresPid).map(ProcessHandle::isAlive).orElse(false),
                    "Orphaned postgres process should be killed by cleanup");
        } finally {
            try {
                orphanedPostgres.close();
            } catch (IOException ignored) {
                // May fail if already killed by cleanup
            }
            if (Files.exists(staleDataDir)) {
                FileUtils.deleteDirectory(staleDataDir.toFile());
            }
        }
    }

    @Test
    void newServerStartsSuccessfullyAfterOrphanCleanup() throws Exception {
        // End-to-end: orphaned postgres exists, new JabRef instance kills it and starts fresh.
        // We close the EmbeddedPostgres object first to release the epg-lock FileLock
        // (simulating what happens when the owning JVM dies), then verify cleanup + new server.
        long deadOwnerPid = 99989L;
        Path staleDataDir = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);

        if (Files.exists(staleDataDir)) {
            FileUtils.deleteDirectory(staleDataDir.toFile());
        }
        Files.createDirectories(staleDataDir);

        EmbeddedPostgres orphanedPostgres = EmbeddedPostgres.builder()
                .setDataDirectory(staleDataDir.toFile())
                .setOutputRedirector(ProcessBuilder.Redirect.DISCARD)
                .start();

        long postgresPid = Long.parseLong(
                Files.readAllLines(staleDataDir.resolve("postmaster.pid")).getFirst().trim());

        // Close the EmbeddedPostgres to release epg-lock (simulates dead JVM releasing file locks).
        // This also stops postgres, but we recreate the stale state below.
        orphanedPostgres.close();

        // Recreate stale directory state as if postgres was an orphan left behind
        if (!Files.exists(staleDataDir)) {
            Files.createDirectories(staleDataDir);
        }
        Path markerPath = staleDataDir.resolve(".jabref-embedded-pg");
        if (!Files.exists(markerPath)) {
            Files.createFile(markerPath);
        }
        Files.writeString(staleDataDir.resolve("postmaster.pid"), Long.toString(postgresPid));

        try {
            // Starting a new PostgreServer triggers cleanup, then starts fresh
            try (PostgreServer newServer = PostgreServer.createWithoutWatchdog()) {
                assertFalse(Files.exists(staleDataDir),
                        "Orphaned data directory should be cleaned up on new server start");
                assertNotNull(newServer.getConnection(), "New server should provide a working connection");
            }
        } finally {
            if (Files.exists(staleDataDir)) {
                FileUtils.deleteDirectory(staleDataDir.toFile());
            }
        }
    }

    @Test
    void serverRecoversAfterPostgresCrash() throws Exception {
        // Simulate postgres crashing mid-session: kill the process, then verify
        // JabRef can close gracefully and start a new server
        Path dataDir = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + ProcessHandle.current().pid());

        PostgreServer server = PostgreServer.createWithoutWatchdog();
        Path postmasterPidPath = dataDir.resolve("postmaster.pid");
        assertTrue(Files.exists(postmasterPidPath), "postmaster.pid should exist");
        long postgresPid = Long.parseLong(Files.readAllLines(postmasterPidPath).getFirst().trim());

        // Kill postgres externally (simulating an unexpected crash)
        ProcessHandle.of(postgresPid).ifPresent(ProcessHandle::destroyForcibly);
        // Wait for process to actually exit
        ProcessHandle.of(postgresPid).ifPresent(ph -> {
            try {
                ph.onExit().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Process will be gone
            }
        });

        // Close should handle the already-dead postgres gracefully
        assertDoesNotThrow(() -> server.close(), "close() should not throw when postgres is already dead");

        // Starting a new server should succeed
        try (PostgreServer newServer = PostgreServer.createWithoutWatchdog()) {
            Connection connection = newServer.getConnection();
            assertNotNull(connection, "New server should provide a working connection after crash recovery");
            ResultSet rs = connection.createStatement().executeQuery("SELECT 1");
            assertTrue(rs.next(), "Should be able to execute queries on recovered server");
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void cleanupDeletesDirectoryWithTransientFileLockUsingRetry() throws Exception {
        long deadOwnerPid = 99996L;
        Path staleDirectory = SYSTEM_TEMP_DIRECTORY.resolve("jabref-embedded-pg-" + deadOwnerPid);

        if (Files.exists(staleDirectory)) {
            FileUtils.deleteDirectory(staleDirectory.toFile());
        }
        Files.createDirectories(staleDirectory);
        Files.createFile(staleDirectory.resolve(".jabref-embedded-pg"));
        Files.writeString(staleDirectory.resolve("postmaster.pid"), "88886\n");

        // Simulate Windows file locking: hold a lock on a file in the directory,
        // then release it after a delay (mimics Windows releasing handles after process death)
        Path lockedFile = staleDirectory.resolve("pg_wal_lock_simulation");
        FileChannel channel = FileChannel.open(lockedFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock = channel.lock();

        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(1500);
                lock.release();
                channel.close();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
        releaser.start();

        try {
            // cleanupOrphanedInstances should retry deletion until the lock is released
            PostgreServer.cleanupOrphanedInstances();

            assertFalse(Files.exists(staleDirectory),
                    "Directory should be deleted after retry succeeds when file lock is released");
        } finally {
            releaser.join(5000);
            if (lock.isValid()) {
                lock.release();
            }
            if (channel.isOpen()) {
                channel.close();
            }
            if (Files.exists(staleDirectory)) {
                FileUtils.deleteDirectory(staleDirectory.toFile());
            }
        }
    }
}
