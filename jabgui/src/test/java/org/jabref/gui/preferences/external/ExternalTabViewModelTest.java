package org.jabref.gui.preferences.external;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class ExternalTabViewModelTest {

    @Test
    void commandLinesKeepSpacesAndApostrophesWithinArguments() {
        assertEquals(
                List.of("/usr/bin/gnome-terminal", "--title=O'Brien's Library", "--working-directory=%DIR%"),
                ExternalTabViewModel.toCommandArguments("""
                        /usr/bin/gnome-terminal
                        --title=O'Brien's Library
                        --working-directory=%DIR%"""));
    }

    @Test
    void browsingForExecutableKeepsArguments() {
        assertEquals(
                "/new location/terminal\n--working-directory=%DIR%",
                ExternalTabViewModel.replaceExecutable(
                        "/old location/terminal\n--working-directory=%DIR%", "/new location/terminal"));
    }

    @Test
    void executableFieldExcludesArgumentsAndKeepsSpaces() {
        assertEquals("C:\\Program Files\\terminal.exe",
                ExternalTabViewModel.getExecutable("C:\\Program Files\\terminal.exe\n/dir\n%DIR%"));
    }

    @Test
    void argumentEditorExcludesExecutable() {
        assertEquals("--title=O'Brien's Library\n--working-directory=%DIR%",
                ExternalTabViewModel.getArguments("/usr/bin/terminal\n--title=O'Brien's Library\n--working-directory=%DIR%"));
    }

    @Test
    void executableWithoutArgumentsOpensEmptyEditor() {
        assertEquals("", ExternalTabViewModel.getArguments("/usr/bin/terminal"));
    }

    @Test
    void editingArgumentsPreservesExecutableAndArgumentBoundaries() {
        assertEquals(List.of("C:\\Program Files\\terminal.exe", "--title=O'Brien's Library", "%DIR%"),
                ExternalTabViewModel.toCommandArguments(ExternalTabViewModel.replaceArguments(
                        "C:\\Program Files\\terminal.exe\n/old", "--title=O'Brien's Library\r\n%DIR%")));
    }

    @Test
    void clearingArgumentsKeepsExecutable() {
        assertEquals("/usr/bin/terminal", ExternalTabViewModel.replaceArguments("/usr/bin/terminal\n/old", ""));
    }

    @Test
    void pastedWindowsLineEndingsDoNotBecomePartOfArguments() {
        assertEquals(
                List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/dir", "%DIR%"),
                ExternalTabViewModel.toCommandArguments("C:\\Program Files\\ConEmu\\ConEmu64.exe\r\n/dir\r\n%DIR%"));
    }
}
