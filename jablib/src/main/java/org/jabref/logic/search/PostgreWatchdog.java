package org.jabref.logic.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.os.OS;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class PostgreWatchdog {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreWatchdog.class);
    private static final int GRACEFUL_STOP_TIMEOUT_SECONDS = 3;

    private @Nullable Process watchdogProcess;
    private @Nullable Path scriptPath;

    public void start(long jabrefPid, long postgresPid, Path dataDirectory) {
        if (postgresPid <= 0) {
            return;
        }
        try {
            if (OS.WINDOWS) {
                startWindowsWatchdog(jabrefPid, postgresPid, dataDirectory);
            } else {
                startUnixWatchdog(jabrefPid, postgresPid, dataDirectory);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not start PostgreSQL watchdog", e);
        }
    }

    public void stop() {
        if (watchdogProcess != null && watchdogProcess.isAlive()) {
            watchdogProcess.destroy();
            try {
                if (!watchdogProcess.waitFor(GRACEFUL_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    watchdogProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                watchdogProcess.destroyForcibly();
            }
        }
        if (scriptPath != null) {
            try {
                Files.deleteIfExists(scriptPath);
            } catch (IOException e) {
                LOGGER.debug("Could not delete watchdog script {}", scriptPath, e);
            }
        }
    }

    private void startUnixWatchdog(long jabrefPid, long postgresPid, Path dataDirectory) throws IOException {
        this.scriptPath = Files.createTempFile("jabref-pg-watchdog-", ".sh");
        String script = buildUnixWatchdogScript(jabrefPid, postgresPid, dataDirectory, this.scriptPath);
        Files.writeString(this.scriptPath, script);
        this.scriptPath.toFile().setExecutable(true);

        watchdogProcess = new ProcessBuilder("/bin/sh", this.scriptPath.toString())
                .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

        LOGGER.info("Started PostgreSQL watchdog (script PID: {}) monitoring JabRef PID {} -> Postgres PID {}",
                watchdogProcess.pid(), jabrefPid, postgresPid);
    }

    private void startWindowsWatchdog(long jabrefPid, long postgresPid, Path dataDirectory) throws IOException {
        this.scriptPath = Files.createTempFile("jabref-pg-watchdog-", ".ps1");
        String script = buildWindowsWatchdogScript(jabrefPid, postgresPid, dataDirectory, this.scriptPath);
        Files.writeString(this.scriptPath, script);

        watchdogProcess = new ProcessBuilder(
                "powershell.exe", "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden", "-File", this.scriptPath.toString())
                .redirectInput(ProcessBuilder.Redirect.from(new File("NUL")))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

        LOGGER.info("Started PostgreSQL watchdog (script PID: {}) monitoring JabRef PID {} -> Postgres PID {}",
                watchdogProcess.pid(), jabrefPid, postgresPid);
    }

    static String buildUnixWatchdogScript(long jabrefPid, long postgresPid, Path dataDirectory, Path scriptPath) {
        String escapedDataDirectory = escapeForSh(dataDirectory);
        String escapedScriptPath = escapeForSh(scriptPath);
        return """
                #!/bin/sh
                JABREF_PID=%d
                POSTGRES_PID=%d
                DATA_DIR="%s"
                SCRIPT_PATH="%s"
                POSTGRES_CMD="$(ps -p "$POSTGRES_PID" -o command= 2>/dev/null || true)"
                case "$POSTGRES_CMD" in
                  *postgres*) ;;
                  *) exit 0 ;;
                esac
                # Wait for the main JabRef process to exit
                while kill -0 "$JABREF_PID" 2>/dev/null; do
                    sleep 2
                done
                # Gracefully terminate the entire PostgreSQL process group
                kill -TERM -"$POSTGRES_PID" 2>/dev/null
                # Wait up to 5 seconds for it to shut down
                for _ in 1 2 3 4 5; do
                    if ! kill -0 "$POSTGRES_PID" 2>/dev/null; then
                        break
                    fi
                    sleep 1
                done
                # If it's still running, forcibly kill the process group
                if kill -0 "$POSTGRES_PID" 2>/dev/null; then
                    kill -KILL -"$POSTGRES_PID" 2>/dev/null
                fi
                # Clean up data and script
                rm -rf "$DATA_DIR"
                rm -f "$SCRIPT_PATH"
                """.formatted(jabrefPid, postgresPid, escapedDataDirectory, escapedScriptPath);
    }

    static String buildWindowsWatchdogScript(long jabrefPid, long postgresPid, Path dataDirectory, Path scriptPath) {
        String escapedDataDirectory = escapeForPowerShell(dataDirectory);
        String escapedScriptPath = escapeForPowerShell(scriptPath);
        return """
                $jabrefPid = %d
                $postgresPid = %d
                $dataDir = "%s"
                
                # Wait until the main JabRef process exits
                Wait-Process -Id $jabrefPid -ErrorAction SilentlyContinue
                
                # Forcibly terminate the PostgreSQL process and its children
                & taskkill /T /F /PID $postgresPid 2>$null
                
                # Give a moment for file locks to be released
                Start-Sleep -Seconds 1
                
                # Clean up the data directory
                if (Test-Path $dataDir) {
                    Remove-Item -Path $dataDir -Recurse -Force -ErrorAction SilentlyContinue
                }
                
                # Self-delete the watchdog script
                Remove-Item -Path "%s" -Force -ErrorAction SilentlyContinue
                """.formatted(jabrefPid, postgresPid, escapedDataDirectory, escapedScriptPath);
    }

    private static String escapeForSh(Path path) {
        return path.toString()
                   .replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("`", "\\`")
                   .replace("$", "\\$");
    }

    private static String escapeForPowerShell(Path path) {
        return path.toString()
                   .replace("`", "``")
                   .replace("\"", "`\"");
    }
}
