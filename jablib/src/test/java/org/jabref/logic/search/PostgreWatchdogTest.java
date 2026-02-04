package org.jabref.logic.search;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgreWatchdogTest {

    @Test
    void unixScriptContainsExpectedPlaceholders() {
        Path dataDirectory = Path.of("/tmp/jabref");
        Path scriptPath = Path.of("/tmp/watchdog.sh");

        String script = PostgreWatchdog.buildUnixWatchdogScript(123, 456, dataDirectory, scriptPath);

        assertTrue(script.contains("JABREF_PID=123"), "Unix script should embed JabRef PID");
        assertTrue(script.contains("POSTGRES_PID=456"), "Unix script should embed Postgres PID");
        assertTrue(script.contains("DATA_DIR=\"/tmp/jabref\""), "Unix script should include data directory path");
        assertTrue(script.contains("SCRIPT_PATH=\"/tmp/watchdog.sh\""), "Unix script should include script path");
        assertTrue(script.contains("rm -f \"$SCRIPT_PATH\""), "Unix script should remove itself at the end");
    }

    @Test
    void windowsScriptContainsExpectedPlaceholders() {
        Path dataDirectory = Path.of("C:/temp/jabref");
        Path scriptPath = Path.of("C:/temp/watchdog.ps1");

        String script = PostgreWatchdog.buildWindowsWatchdogScript(321, 654, dataDirectory, scriptPath);

        assertTrue(script.contains("$jabrefPid = 321"), "Windows script should embed JabRef PID");
        assertTrue(script.contains("$postgresPid = 654"), "Windows script should embed Postgres PID");
        assertTrue(script.contains("$dataDir = \"C:/temp/jabref\""), "Windows script should reference the data directory");
        assertTrue(script.contains("Remove-Item -Path \"C:/temp/watchdog.ps1\""), "Windows script should delete itself");
    }

    @Test
    void stopGracefullyDestroysActiveProcess() throws Exception {
        PostgreWatchdog watchdog = new PostgreWatchdog();
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        when(process.waitFor(3, TimeUnit.SECONDS)).thenReturn(true);

        Field field = PostgreWatchdog.class.getDeclaredField("watchdogProcess");
        field.setAccessible(true);
        field.set(watchdog, process);

        watchdog.stop();

        verify(process).destroy();
        verify(process, never()).destroyForcibly();
    }

    @Test
    void stopForceKillsWhenGracefulTimesOut() throws Exception {
        PostgreWatchdog watchdog = new PostgreWatchdog();
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        when(process.waitFor(3, TimeUnit.SECONDS)).thenReturn(false);
        when(process.destroyForcibly()).thenReturn(process);

        Field field = PostgreWatchdog.class.getDeclaredField("watchdogProcess");
        field.setAccessible(true);
        field.set(watchdog, process);

        watchdog.stop();

        verify(process).destroy();
        verify(process).destroyForcibly();
    }
}
