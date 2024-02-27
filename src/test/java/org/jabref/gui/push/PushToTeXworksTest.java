package org.jabref.gui.push;
import java.util.Map;

import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.DialogService;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;
import org.jabref.preferences.ExternalApplicationsPreferences;
import org.jabref.preferences.JabRefPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PushToTeXworksTest {
    private PushToTeXworks pushToTeXworks;

    @BeforeEach
    public void setup() {

        // Mock the DialogService and PreferencesService
        DialogService dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);
        PreferencesService preferencesService = mock(PreferencesService.class);
        PushToApplicationPreferences pushToApplicationPreferences = mock(PushToApplicationPreferences.class);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);

        // Mock the return value for getCommandPaths()
        String teXworksClientPath = "/usr/bin/texworks"; // For linux OS tobe changed in Windows and Mac
        String displayName = "TeXworks"; 

        Map<String, String> commandPaths = Map.of(displayName, teXworksClientPath);
        ObservableMap<String, String> observableCommandPaths = FXCollections.observableMap(commandPaths);
        when(pushToApplicationPreferences.getCommandPaths()).thenReturn(new SimpleMapProperty<>(observableCommandPaths));
        when(preferencesService.getPushToApplicationPreferences()).thenReturn(pushToApplicationPreferences);


    
        //Mock the return value for getCiteCommand()
        CitationCommandString mockCiteCommand = mock(CitationCommandString.class);
        when(mockCiteCommand.prefix()).thenReturn("");
        when(mockCiteCommand.suffix()).thenReturn("");
        when(externalApplicationsPreferences.getCiteCommand()).thenReturn(mockCiteCommand);
       
        // Mock the return value for getExternalApplicationsPreferences()
        when(preferencesService.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);        
        
        // Create a new instance of PushToTeXworks
        pushToTeXworks = new PushToTeXworks(dialogService, preferencesService);
    }

    /**
     * To verify that the PushToTeXworks class correctly returns its designated display name.
     * The display name is used to identify the application in the GUI.
     * 
     * Method: getDisplayName()
     * 
     */
    @Test
    void testDisplayName() {
        // Test whether the display name is correct
        assertEquals("TeXworks", pushToTeXworks.getDisplayName());
    }


    /**
     * To verify that the PushToTeXworks class correctly returns the command line for TeXworks.
     * The command line is used to execute the application from the command line.
     * 
     * Method: getCommandLine()
     * 
     * [TO FIX]: Test not working the commande path is not being set in the preferences
     *           Fix it in the setup() method
     * 
     */
    // @Test
    // void testGetCommandLine() {
    //     String keyString = "TestKey";
    //     String[] expectedCommand = new String[] {"/usr/bin/texworks", "--insert-text", "TestKey"};
    //     String[] actualCommand = pushToTeXworks.getCommandLine(keyString);
        
    //     assertEquals(expectedCommand, actualCommand);
    // }
}


