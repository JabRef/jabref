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
    void pastedWindowsLineEndingsDoNotBecomePartOfArguments() {
        assertEquals(
                List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/dir", "%DIR%"),
                ExternalTabViewModel.toCommandArguments("C:\\Program Files\\ConEmu\\ConEmu64.exe\r\n/dir\r\n%DIR%"));
    }
}
