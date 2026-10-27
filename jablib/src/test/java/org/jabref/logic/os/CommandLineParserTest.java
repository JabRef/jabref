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

    private static Stream<Arguments> legacyCommands() {
        return Stream.of(
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR"),
                        "/usr/bin/gnome-terminal --working-directory='%DIR'"),
                Arguments.of(
                        List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/single", "/dir", "%DIR%"),
                        "\"C:\\Program Files\\ConEmu\\ConEmu64.exe\" /single /dir \"%DIR%\""),
                Arguments.of(
                        List.of("C:\\Users\\O'Brien\\terminal.exe", "--flag"),
                        "C:\\Users\\O'Brien\\terminal.exe --flag"),
                Arguments.of(
                        List.of("cmd", "", "arg"),
                        "cmd \"\" arg"),
                Arguments.of(
                        List.of(),
                        "cmd 'unfinished"));
    }

    @ParameterizedTest
    @MethodSource("legacyCommands")
    void splitLegacyCommandLine(List<String> expected, String commandLine) {
        assertEquals(expected, CommandLineParser.splitLegacyCommandLine(commandLine));
    }

    private static Stream<Arguments> directoryArguments() {
        return Stream.of(
                Arguments.of(
                        List.of("gnome-terminal", "--working-directory=/home/user/My Library"),
                        List.of("gnome-terminal", "--working-directory=%DIR"),
                        "/home/user/My Library"),
                Arguments.of(
                        List.of("explorer.exe", "C:\\My Library"),
                        List.of("explorer.exe", "%DIR%"),
                        "C:\\My Library"),
                Arguments.of(
                        List.of("terminal", "/home/user/%DIR_work"),
                        List.of("terminal", "%DIR%"),
                        "/home/user/%DIR_work"),
                Arguments.of(
                        List.of("terminal", "/tmp/$backup\\folder"),
                        List.of("terminal", "%DIR%"),
                        "/tmp/$backup\\folder"));
    }

    @ParameterizedTest
    @MethodSource("directoryArguments")
    void withDirectory(List<String> expected, List<String> command, String directory) {
        assertEquals(expected, CommandLineParser.withDirectory(command, directory));
    }
}
