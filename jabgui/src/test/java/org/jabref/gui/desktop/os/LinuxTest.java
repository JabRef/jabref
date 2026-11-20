package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Uses symbolic links and POSIX file permissions, which Windows does not offer without extra privileges
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

    @Test
    void defaultTerminalPrefersTheAlternativesLink() {
        assertEquals(Optional.of(List.of("gnome-terminal", "--working-directory", "/home/user")),
                Linux.defaultTerminalCommand(Optional.of("gnome-terminal.wrapper"), _ -> true, "xterm", "kde", "/home/user"));
    }

    @Test
    void defaultTerminalUsesXdgTerminalExecWithoutAlternativesLink() {
        assertEquals(Optional.of(List.of("xdg-terminal-exec")),
                Linux.defaultTerminalCommand(Optional.empty(), "xdg-terminal-exec"::equals, "xterm", "kde", "/home/user"));
    }

    @Test
    void defaultTerminalUsesTerminalVariableWithoutXdgTerminalExec() {
        assertEquals(Optional.of(List.of("alacritty")),
                Linux.defaultTerminalCommand(Optional.empty(), _ -> false, "alacritty", "kde", "/home/user"));
    }

    // Fedora and Nobara with KDE have no alternatives link, see https://github.com/JabRef/jabref/pull/17752#issuecomment-6945624391
    @Test
    void defaultTerminalUsesTheTerminalOfTheDesktopEnvironment() {
        assertEquals(Optional.of(List.of("konsole", "--workdir", "/home/user")),
                Linux.defaultTerminalCommand(Optional.empty(), "konsole"::equals, null, "kde:plasma", "/home/user"));
    }

    @Test
    void defaultTerminalOfGnomeIsTheNewestInstalledOne() {
        assertEquals(Optional.of(List.of("kgx", "--working-directory=/home/user")),
                Linux.defaultTerminalCommand(Optional.empty(), program -> List.of("kgx", "gnome-terminal").contains(program), null, "gnome", "/home/user"));
    }

    @Test
    void defaultTerminalIsEmptyWithoutAnyCandidate() {
        assertEquals(Optional.empty(),
                Linux.defaultTerminalCommand(Optional.empty(), _ -> false, " ", "gnome", "/home/user"));
    }

    @Test
    void applicationCommandKeepsExecutablePathWithSpaces() throws IOException {
        Path viewer = Files.createFile(Files.createDirectory(tempDir.resolve("My Viewer")).resolve("viewer"));
        Files.setPosixFilePermissions(viewer, EnumSet.allOf(PosixFilePermission.class));

        assertEquals(List.of(viewer.toString()), Linux.applicationCommand(viewer.toString()));
    }

    @Test
    void applicationCommandSplitsArguments() {
        assertEquals(List.of("evince", "--fullscreen"), Linux.applicationCommand("evince --fullscreen"));
    }

    @Test
    void applicationCommandKeepsQuotedPathTogether() {
        assertEquals(List.of("/opt/My Viewer/viewer", "--flag"), Linux.applicationCommand("\"/opt/My Viewer/viewer\" --flag"));
    }

    @Test
    void applicationCommandWithUnbalancedQuotesIsKeptAsIs() {
        assertEquals(List.of("\"evince --fullscreen"), Linux.applicationCommand("\"evince --fullscreen"));
    }
}
