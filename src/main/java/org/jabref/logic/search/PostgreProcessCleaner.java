package org.jabref.logic.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class PostgreProcessCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreProcessCleaner.class);
    private static final PostgreProcessCleaner INSTANCE = new PostgreProcessCleaner();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final String FILE_PREFIX = "jabref-postgres-info-";
    private static final String FILE_SUFFIX = ".json";

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
        if (isPortOpen("localhost", port)) {
            long pid = getPidUsingPort(port);
            if (pid != -1) {
                LOGGER.info("Old Postgres instance found on port {} (PID {}). Killing it.", port, pid);
                destroyProcessByPID(pid, 1500);
            } else {
                LOGGER.warn("Could not determine PID using port {}. Skipping kill step.", port);
            }
        }
    }

    private void destroyProcessByPID(long pid, int millis) throws InterruptedException {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isPresent() && handle.get().isAlive()) {
            handle.get().destroy();
            Thread.sleep(millis);
        }
    }

    private boolean isJavaProcessAlive(long javaPid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(javaPid);
        return handle.map(ProcessHandle::isAlive).orElse(false);
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket _ = new Socket(host, port)) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private long getPidUsingPort(int port) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            Process process = createPortLookupProcess(os, port);
            if (process == null) {
                return -1;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return extractPidFromOutput(os, reader);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get PID for port {}: {}", port, e.getMessage(), e);
        }
        return -1;
    }

    private Process createPortLookupProcess(String os, int port) throws IOException {
        if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            return new ProcessBuilder("lsof", "-i", "tcp:" + port, "-sTCP:LISTEN", "-Pn")
                    .redirectErrorStream(true).start();
        } else if (os.contains("win")) {
            return executeWindowsCommand(port);
        }
        return null;
    }

    private Process executeWindowsCommand(int port) throws IOException {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isBlank()) {
            String netStatPath = systemRoot + "\\System32\\netstat.exe";
            String findStrPath = systemRoot + "\\System32\\findstr.exe";
            String command = netStatPath + " -ano | " + findStrPath + " :" + port;
            return new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true).start();
        }
        return null;
    }

    private long extractPidFromOutput(String os, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                Long pid = parseUnixPidFromLine(line);
                if (pid != null) {
                    return pid;
                }
            } else if (os.contains("win")) {
                Long pid = parseWindowsPidFromLine(line);
                if (pid != null) {
                    return pid;
                }
            }
        }
        return -1;
    }

    private Long parseUnixPidFromLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length > 1 && parts[1].matches("\\d+")) {
            return Long.parseLong(parts[1]);
        }
        return null;
    }

    private Long parseWindowsPidFromLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 5 && parts[parts.length - 1].matches("\\d+")) {
            return Long.parseLong(parts[parts.length - 1]);
        }
        return null;
    }
}
