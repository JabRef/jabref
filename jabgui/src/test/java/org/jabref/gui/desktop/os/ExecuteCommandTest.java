package org.jabref.gui.desktop.os;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExecuteCommandTest {
    @Test
    void noSpaceTestCaseInDirName() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"};
        assertEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory=jabrefStuff"));
    }

    @Test
    void oneSpaceTestCaseInDirName() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabref Stuff"};
        assertEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory='jabref Stuff'"));
    }

    @Test
    void noSpaceTestCaseInDirNameWithQuotes() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"};
        assertEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory='jabrefStuff'"));
    }

    @Test
    void manySpaceTestCaseInDirName() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabref               Stuff"};
        assertEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory='jabref               Stuff'"));
    }

    @Test
    void spaceBetweenDirNameAndEquals() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabrefStuff"};
        assertNotEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory= jabrefStuff")); // will end up opening a normal terminal as there is nothing
    }

    @Test
    void quoteTypeMismatch() {
        assertThrows(InvocationTargetException.class, () -> getSubcommands("/usr/bin/gnome-terminal --working-directory= 'jabrefStuff\""), "Illegal arg happened");
    }

    @Test
    void doubleQuotes() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String[] exp = {"/usr/bin/gnome-terminal", "--working-directory=jabref Stuff"};
        assertEquals(List.of(exp), getSubcommands("/usr/bin/gnome-terminal --working-directory=\"jabref Stuff\""));
    }

    private ArrayList<String> getSubcommands(String s) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = NativeDesktop.class.getDeclaredMethod("getSubcommands", String.class);
        method.setAccessible(true);
        return (ArrayList<String>) method.invoke(null, s);
    }
}

