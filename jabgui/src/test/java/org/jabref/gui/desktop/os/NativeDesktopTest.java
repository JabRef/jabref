package org.jabref.gui.desktop.os;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeDesktopTest {

    @Test
    void parseCommandPreservesSingleQuotedArgument() {
        assertEquals(
                List.of("/usr/bin/gnome-terminal", "--working-directory=/path/with spaces"),
                NativeDesktop.parseCommand("/usr/bin/gnome-terminal --working-directory='/path/with spaces'"));
    }

    @Test
    void parseCommandPreservesDoubleQuotedArgument() {
        assertEquals(
                List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/single", "/dir", "C:\\path with spaces"),
                NativeDesktop.parseCommand("\"C:\\Program Files\\ConEmu\\ConEmu64.exe\" /single /dir \"C:\\path with spaces\""));
    }

    @Test
    void parseCommandIgnoresWhitespaceBetweenArguments() {
        assertEquals(
                List.of("terminal", "--option", "directory"),
                NativeDesktop.parseCommand("  terminal  \t --option   directory  "));
    }
}
