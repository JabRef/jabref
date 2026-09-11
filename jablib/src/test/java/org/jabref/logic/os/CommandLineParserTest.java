package org.jabref.logic.os;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class CommandLineParserTest {

    private static Stream<Arguments> splitCommandLine() {
        return Stream.of(
                Arguments.of(
                        List.of("gnome-terminal", "--working-directory=/tmp"),
                        "gnome-terminal --working-directory=/tmp",
                        "/home/user/dir"),

                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=/home/user/dir"),
                        "/usr/bin/gnome-terminal --working-directory='%DIR%'",
                        "/home/user/dir"),

                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=/home/user/dir"),
                        "/usr/bin/gnome-terminal --working-directory=\"%DIR\"",
                        "/home/user/dir"),

                Arguments.of(
                        List.of("konsole", "--workdir", "/home/user/My Documents"),
                        "konsole --workdir '/home/user/My Documents'",
                        "/home/user/dir"),

                Arguments.of(
                        List.of("xfce4-terminal", "--working-directory=/home/user/My Documents"),
                        "xfce4-terminal --working-directory=/home/user/My\\ Documents",
                        "/home/user/dir"),

                Arguments.of(
                        List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/single", "/dir", "C:\\My Dir"),
                        "\"C:\\Program Files\\ConEmu\\ConEmu64.exe\" /single /dir \"%DIR%\"",
                        "C:\\My Dir"),

                Arguments.of(
                        List.of("gnome-terminal", "--working-directory=/home/user/%DIR_work"),
                        "gnome-terminal --working-directory='%DIR%'",
                        "/home/user/%DIR_work"),

                Arguments.of(
                        List.of("cmd", "", "", "arg"),
                        "cmd \"\" '' arg",
                        "/home/user/dir"),

                Arguments.of(
                        List.of(),
                        "   ",
                        "/home/user/dir"));
    }

    @ParameterizedTest
    @MethodSource
    void splitCommandLine(List<String> expected, String commandLine, String directory) {
        assertEquals(expected, CommandLineParser.toArguments(commandLine, directory));
    }
}
