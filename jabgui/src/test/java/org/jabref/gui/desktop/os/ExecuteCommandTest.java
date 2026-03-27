package org.jabref.gui.desktop.os;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ExecuteCommandTest {
    @ParameterizedTest
    @MethodSource("getSubcommandsTestSource")
    void getSubcommandsTest(List<String> expected, String input) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assertEquals(expected, getSubcommands(input));
    }

    @Test
    void spaceBetweenDirNameAndEquals() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"};
        assertNotEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory= jabrefStuff")); // will end up opening a normal terminal as there is nothing
    }

    private static Stream<Arguments> getSubcommandsTestSource() {
        return Stream.of(
                // No spaces
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"),
                        "/usr/bin/gnome-terminal --working-directory=jabrefStuff"
                ),
                // One space with single quotes
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=jabref Stuff"),
                        "/usr/bin/gnome-terminal --working-directory='jabref Stuff'"
                ),
                // No spaces with single quotes
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"),
                        "/usr/bin/gnome-terminal --working-directory='jabrefStuff'"
                ),
                // Multiple spaces with single quotes
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=jabref               Stuff"),
                        "/usr/bin/gnome-terminal --working-directory='jabref               Stuff'"
                ),
                // Space with double quotes
                Arguments.of(
                        List.of("/usr/bin/gnome-terminal", "--working-directory=jabref Stuff"),
                        "/usr/bin/gnome-terminal --working-directory=\"jabref Stuff\""
                )

        );
    }

    private ArrayList<String> getSubcommands(String s) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = NativeDesktop.class.getDeclaredMethod("getSubcommands", String.class);
        method.setAccessible(true);
        return (ArrayList<String>) method.invoke(null, s);
    }
}

