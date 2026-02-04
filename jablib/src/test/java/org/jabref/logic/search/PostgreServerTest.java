package org.jabref.logic.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
}
