package org.jabref.logic.search;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class PostgreProcessCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreProcessCleaner.class);
    private static final PostgreProcessCleaner INSTANCE = new PostgreProcessCleaner();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final OperatingSystem OS = SYSTEM_INFO.getOperatingSystem();
    private static final String FILE_PREFIX = "jabref-postgres-info-";
    private static final String FILE_SUFFIX = ".json";
    private static final int POSTGRES_SHUTDOWN_WAIT_MILLIS = 1500;

    private PostgreProcessCleaner() {
    }

    public static PostgreProcessCleaner getInstance() {
        return INSTANCE;
    }

    public void checkAndCleanupOldInstances() {
        try (Stream<Path> files = Files.list(TEMP_DIR)) {
            files.filter(path -> path.getFileName().toString().startsWith(FILE_PREFIX))
                    .filter(path -> path.getFileName().toString().endsWith(FILE_SUFFIX))
                    .forEach(this::cleanupIfDeadProcess);
        } catch (IOException e) {
            LOGGER.warn("Failed to list temp directory for Postgres metadata cleanup: {}", e.getMessage(), e);
        }
    }

    private void cleanupIfDeadProcess(Path metadataPath) {
        try {
            Map<String, Object> metadata = new HashMap<>(OBJECT_MAPPER.readValue(Files.readAllBytes(metadataPath), HashMap.class));
            long javaPid = ((Number) metadata.get("javaPid")).longValue();
            if (isJavaProcessAlive(javaPid)) {
                return;
            }
            int postgresPort = ((Number) metadata.get("postgresPort")).intValue();
            destroyPostgresProcess(postgresPort);
            Files.deleteIfExists(metadataPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to read or parse metadata file '{}': {}", metadataPath, e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.warn("Cleanup was interrupted for '{}': {}", metadataPath, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.warn("Unexpected error during cleanup of '{}': {}", metadataPath, e.getMessage(), e);
        }
    }

    private void destroyPostgresProcess(int port) throws InterruptedException {
        if (isPortOpen(port)) {
            long pid = getPidUsingPort(port);
            if (pid != -1) {
                LOGGER.info("Old Postgres instance found on port {} (PID {}). Killing it.", port, pid);
                destroyProcessByPID(pid);
            } else {
                LOGGER.warn("Could not determine PID using port {}. Skipping kill step.", port);
            }
        }
    }

    private void destroyProcessByPID(long pid) throws InterruptedException {
        Optional<ProcessHandle> aliveProcess = ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
        if (aliveProcess.isPresent()) {
            aliveProcess.get().destroy();
            Thread.sleep(PostgreProcessCleaner.POSTGRES_SHUTDOWN_WAIT_MILLIS);
        }
    }

    private boolean isJavaProcessAlive(long javaPid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(javaPid);
        return handle.map(ProcessHandle::isAlive).orElse(false);
    }

    private boolean isPortOpen(int port) {
        try (Socket _ = new Socket("localhost", port)) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private long getPidUsingPort(int port) {
        for (OSProcess process : OS.getProcesses()) {
            String command = process.getCommandLine();
            if (command != null && command.toLowerCase().contains("postgres")
                    && command.contains(String.valueOf(port))) {
                return process.getProcessID();
            }
        }
        return -1L;
    }
}
