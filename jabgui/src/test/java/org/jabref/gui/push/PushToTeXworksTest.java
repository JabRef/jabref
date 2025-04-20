package org.jabref.gui.push;

import java.util.Map;

import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.push.CitationCommandString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushToTeXworksTest {

    private static final String TEXWORKS_CLIENT_PATH = "/usr/bin/texworks";
    private static final String DISPLAY_NAME = "TeXworks";

    private PushToTeXworks pushToTeXworks;

    @BeforeEach
    void setup() {
        DialogService dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);
        GuiPreferences preferences = mock(GuiPreferences.class);
        PushToApplicationPreferences pushToApplicationPreferences = mock(PushToApplicationPreferences.class);

        // Mock the command path
        Map<String, String> commandPaths = Map.of(DISPLAY_NAME, TEXWORKS_CLIENT_PATH);
        ObservableMap<String, String> observableCommandPaths = FXCollections.observableMap(commandPaths);
        when(pushToApplicationPreferences.getCommandPaths()).thenReturn(new SimpleMapProperty<>(observableCommandPaths));
        when(preferences.getPushToApplicationPreferences()).thenReturn(pushToApplicationPreferences);

        // Mock the return value for getCiteCommand()
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        CitationCommandString mockCiteCommand = mock(CitationCommandString.class);
        when(mockCiteCommand.prefix()).thenReturn("");
        when(mockCiteCommand.suffix()).thenReturn("");
        when(externalApplicationsPreferences.getCiteCommand()).thenReturn(mockCiteCommand);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);

        // Create a new instance of PushToTeXworks
        pushToTeXworks = new PushToTeXworks(dialogService, preferences);
    }

    /**
     * To verify that the PushToTeXworks class correctly returns its designated display name.
     * The display name is used to identify the application in the GUI.
     */
    @Test
    void displayName() {
        assertEquals(DISPLAY_NAME, pushToTeXworks.getDisplayName());
    }

    /**
     * To verify that the PushToTeXworks class correctly returns the command line for TeXworks.
     * The command line is used to execute the application from the command line.
     */
    @Test
    void getCommandLine() {
        String keyString = "TestKey";
        String[] expectedCommand = new String[] {null, "--insert-text", keyString}; // commandPath is only set in pushEntries

        String[] actualCommand = pushToTeXworks.getCommandLine(keyString);

        assertArrayEquals(expectedCommand, actualCommand);
    }

    /**
     * Check for the actual command and path with path is run.
     */
    @Test
    void pushEntries() {
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);

        String testKey = "TestKey";
        String[] expectedCommand = new String[] {TEXWORKS_CLIENT_PATH, "--insert-text", testKey};

        pushToTeXworks.pushEntries(null, null, testKey, processBuilder);

        verify(processBuilder).command(expectedCommand);
    }

    /**
     * To verify that the PushToTeXworks class correctly returns the tooltip for TeXworks.
     * The tooltip is used to display a short description of the application in the GUI.
     */
    @Test
    void getTooltip() {
        assertEquals("Push entries to external application (TeXworks)", pushToTeXworks.getTooltip());
    }
}
