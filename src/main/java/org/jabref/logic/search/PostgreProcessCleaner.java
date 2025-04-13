package org.jabref.logic.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.jabref.logic.os.OS.getHostName;
import static org.jabref.logic.search.PostgreServer.POSTGRES_METADATA_FILE;

public class PostgreProcessCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreProcessCleaner.class);
    private static final PostgreProcessCleaner INSTANCE = new PostgreProcessCleaner();

    private PostgreProcessCleaner() {}

    public static PostgreProcessCleaner getInstance() {
        return INSTANCE;
    }

    public void checkAndCleanupOldInstance() {
        if (!Files.exists(POSTGRES_METADATA_FILE))
            return;

        try {
            Map<String, Object> metadata = new HashMap<>(new ObjectMapper()
                    .readValue(Files.readAllBytes(POSTGRES_METADATA_FILE), HashMap.class));
            if(!metadata.isEmpty()) {
                int port = ((Number) metadata.get("postgresPort")).intValue();
                destroyPreviousJavaProcess(metadata);
                destroyPostgresProcess(port);
            }
            Files.deleteIfExists(POSTGRES_METADATA_FILE);
        } catch (IOException e) {
            LOGGER.warn("Failed to read Postgres metadata file: {}", e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.warn("Thread sleep was interrupted while Postgres cleanup: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.warn("Failed to clean up old embedded Postgres: {}", e.getMessage());
        }
    }

    private void destroyPreviousJavaProcess(Map<String, Object> meta) throws InterruptedException {
        long javaPid = ((Number) meta.get("javaPid")).longValue();
        destroyProcessByPID(javaPid, 1000);
    }

    private void destroyPostgresProcess(int port) throws InterruptedException {
        if (isPortOpen(getHostName(), port)) {
            long pid = getPidUsingPort(port);
            if (pid != -1) {
                LOGGER.info("Old Postgres instance found on port {} (PID {}). Killing it...", port, pid);
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
                LOGGER.warn("Unsupported OS for port-based PID lookup.");
                return -1;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return extractPidFromOutput(os, reader);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get PID for port {}: {}", port, e.getMessage());
        }

        return -1;
    }

    private Process createPortLookupProcess(String os, int port) throws IOException {
        if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            return new ProcessBuilder("lsof", "-i", "tcp:" + port, "-sTCP:LISTEN", "-Pn")
                    .redirectErrorStream(true).start();
        } else if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + port)
                    .redirectErrorStream(true).start();
        }
        return null;
    }

    private long extractPidFromOutput(String os, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                Long pid = parseUnixPidFromLine(line);
                if (pid != null) return pid;
            } else if (os.contains("win")) {
                Long pid = parseWindowsPidFromLine(line);
                if (pid != null) return pid;
            }
        }
        return -1;
    }

    private Long parseUnixPidFromLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length > 1 && parts[1].matches("\\d+"))
            return Long.parseLong(parts[1]);
        return null;
    }

    private Long parseWindowsPidFromLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 5 && parts[parts.length - 1].matches("\\d+"))
            return Long.parseLong(parts[parts.length - 1]);
        return null;
    }

}
