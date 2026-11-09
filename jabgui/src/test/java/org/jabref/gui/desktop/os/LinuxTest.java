package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Creating symbolic links on Windows requires extra privileges
@NullMarked
@DisabledOnOs(OS.WINDOWS)
class LinuxTest {

    @TempDir Path tempDir;

    @Test
    void terminalEmulatorNameIsTheFileNameOfTheLinkTarget() throws IOException {
        Path terminal = Files.createFile(tempDir.resolve("gnome-terminal.wrapper"));
        Path link = Files.createSymbolicLink(tempDir.resolve("x-terminal-emulator"), terminal);

        assertEquals(Optional.of("gnome-terminal.wrapper"), Linux.terminalEmulatorName(link));
    }

    @Test
    void terminalEmulatorNameIsEmptyForMissingLink() {
        assertEquals(Optional.empty(), Linux.terminalEmulatorName(tempDir.resolve("x-terminal-emulator")));
    }

    @Test
    void terminalEmulatorNameIsEmptyForDanglingLink() throws IOException {
        Path link = Files.createSymbolicLink(tempDir.resolve("x-terminal-emulator"), tempDir.resolve("removed-terminal"));

        assertEquals(Optional.empty(), Linux.terminalEmulatorName(link));
    }

    @Test
    void terminalEmulatorNameIsEmptyForRegularFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("x-terminal-emulator"));

        assertEquals(Optional.empty(), Linux.terminalEmulatorName(file));
    }
}
