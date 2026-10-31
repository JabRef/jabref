package org.jabref.gui.preferences;

import java.util.List;
import java.util.Set;

import org.jabref.gui.entryeditor.EntryEditorTabModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JabRefGuiPreferencesTest {

    private static final String CONSOLE_COMMAND = "consoleCommand";
    private static final String CONSOLE_COMMAND_ARGUMENTS = "consoleCommandArguments";

    private static final EntryEditorTabModel PREVIEW =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.PREVIEW, true);
    private static final EntryEditorTabModel MAIN =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
    private static final EntryEditorTabModel SOURCE =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.SOURCE, true);

    @Test
    void storedTabOrderIsApplied() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE", "ALL_FIELDS"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void tabsUnknownToStoredOrderAreAppendedInDefaultOrder() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void duplicateCustomTabNamesAreAllKept() {
        EntryEditorTabModel first = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("keywords"));
        EntryEditorTabModel second = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("doi"));

        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, first, second),
                List.of("custom:General", "ALL_FIELDS"));

        // The stored-order entry consumes the first duplicate; the second survives at the end.
        assertEquals(List.of(PREVIEW, first, MAIN, second), ordered);
    }

    // [utest->req~entry-editor.custom-tabs.extract-field~1]
    @Test
    void parseCustomTabsReadsPlainStringsAndExtractObjects() {
        assertEquals(
                List.of(new EntryEditorTabModel.CustomizedFieldsTab(
                        "My tab", List.of("author", "doi"), Set.of("doi"))),
                JabRefGuiPreferences.parseCustomTabs(
                        """
                                {"My tab":["author",{"pattern":"doi","extract":true}]}
                                """));
    }

    @Test
    void migrateLegacyWindowsConsoleCommandKeepsExecutableWithSpaces() {
        List<String> expected = List.of("C:\\Program Files\\ConEmu\\ConEmu64.exe", "/single", "/dir", "%DIR");

        assertEquals(expected, JabRefGuiPreferences.migrateLegacyCommand(
                "C:\\Program Files\\ConEmu\\ConEmu64.exe /single /dir \"%DIR\"", expected));
    }

    @Test
    void migrateLegacyIssueCommandKeepsDirectoryAsOneArgument() {
        assertEquals(
                List.of("/usr/bin/gnome-terminal", "--working-directory=%DIR"),
                JabRefGuiPreferences.migrateLegacyCommand(
                        "/usr/bin/gnome-terminal --working-directory='%DIR'", List.of()));
    }

    @Test
    void storedCommandArgumentsUseStringListPreference() {
        JabRefGuiPreferences preferences = mock(JabRefGuiPreferences.class);
        List<String> command = List.of("browser", "--title=a;b", "C:\\Tools\\app.exe");
        when(preferences.hasKey(CONSOLE_COMMAND_ARGUMENTS)).thenReturn(true);
        when(preferences.getStringList(CONSOLE_COMMAND_ARGUMENTS)).thenReturn(command);

        assertEquals(command, JabRefGuiPreferences.getCommandArguments(
                preferences, CONSOLE_COMMAND_ARGUMENTS, CONSOLE_COMMAND, List.of()));
        verify(preferences).getStringList(CONSOLE_COMMAND_ARGUMENTS);
    }

    @Test
    void legacyCommandArgumentsAreStoredAsStringList() {
        JabRefGuiPreferences preferences = mock(JabRefGuiPreferences.class);
        List<String> command = List.of("terminal", "--title=a;b");
        when(preferences.hasKey(CONSOLE_COMMAND)).thenReturn(true);
        when(preferences.get(CONSOLE_COMMAND, "")).thenReturn("terminal '--title=a;b'");

        assertEquals(command, JabRefGuiPreferences.getCommandArguments(
                preferences, CONSOLE_COMMAND_ARGUMENTS, CONSOLE_COMMAND, List.of()));
        verify(preferences).putStringList(CONSOLE_COMMAND_ARGUMENTS, command);
    }
}
