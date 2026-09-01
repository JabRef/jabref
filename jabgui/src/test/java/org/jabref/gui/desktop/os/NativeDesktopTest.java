package org.jabref.gui.desktop.os;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeDesktopTest {

    @Test
    void splitCommandLineWithSpacesNoQuotes() {
        String command = "gnome-terminal --working-directory=/tmp";
        List<String> expected = List.of("gnome-terminal", "--working-directory=/tmp");
        assertEquals(expected, NativeDesktop.splitCommandLine(command));
    }

    @Test
    void splitCommandLineWithSingleQuotedDir() {
        String command = "/usr/bin/gnome-terminal --working-directory='%DIR'";
        List<String> expected = List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR");
        assertEquals(expected, NativeDesktop.splitCommandLine(command));
    }

    @Test
    void splitCommandLineWithDoubleQuotedDir() {
        String command = "/usr/bin/gnome-terminal --working-directory=\"%DIR\"";
        List<String> expected = List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR");
        assertEquals(expected, NativeDesktop.splitCommandLine(command));
    }

    @Test
    void splitCommandLineWithQuotedSpacesInDir() {
        String command = "konsole --workdir '/home/user/My Documents'";
        List<String> expected = List.of("konsole", "--workdir", "/home/user/My Documents");
        assertEquals(expected, NativeDesktop.splitCommandLine(command));
    }

    @Test
    void splitCommandLineWithEscapedSpaces() {
        String command = "xfce4-terminal --working-directory=/home/user/My\\ Documents";
        List<String> expected = List.of("xfce4-terminal", "--working-directory=/home/user/My Documents");
        assertEquals(expected, NativeDesktop.splitCommandLine(command));
    }

    @Test
    void splitCommandLineNullOrBlank() {
        assertEquals(List.of(), NativeDesktop.splitCommandLine(null));
        assertEquals(List.of(), NativeDesktop.splitCommandLine("  "));
    }
}
