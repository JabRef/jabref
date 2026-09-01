package org.jabref.gui.desktop.os;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeDesktopTest {

    private static Stream<Arguments> splitCommandLine() {
        return Stream.of(
                Arguments.of(List.of("gnome-terminal", "--working-directory=/tmp"),
                        "gnome-terminal --working-directory=/tmp"),

                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR%"),
                        "/usr/bin/gnome-terminal --working-directory='%DIR%'"),

                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR"),
                        "/usr/bin/gnome-terminal --working-directory=\"%DIR\""),

                Arguments.of(
                        List.of("konsole", "--workdir", "/home/user/My Documents"),
                        "konsole --workdir '/home/user/My Documents'"),

                Arguments.of(List.of("xfce4-terminal", "--working-directory=/home/user/My Documents"),
                        "xfce4-terminal --working-directory=/home/user/My\\ Documents"),

                Arguments.of(List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/single", "/dir", "%DIR%"),
                        "\"C:\\Program Files\\ConEmu\\ConEmu64.exe\" /single /dir \"%DIR%\""),

                Arguments.of(List.of(), "   "));
    }

    @ParameterizedTest
    @MethodSource
    void splitCommandLine(List<String> expected, String commandLine) {
        assertEquals(expected, NativeDesktop.splitCommandLine(commandLine));
    }
}
