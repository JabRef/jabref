package org.jabref.logic.push;

import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.util.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushToSublimeTextTest {

    private PushToSublimeText pushToSublimeText;
    private PushToApplicationPreferences preferences;

    @BeforeEach
    void setup() {
        preferences = mock(PushToApplicationPreferences.class);
        NotificationService notificationService = mock(NotificationService.class);
        pushToSublimeText = new PushToSublimeText(notificationService, preferences);

        Map<String, String> commandPaths = Map.of(pushToSublimeText.getDisplayName(), "/usr/bin/subl");
        SimpleMapProperty<String, String> commandPathsProperty = new SimpleMapProperty<>(FXCollections.observableMap(commandPaths));
        when(preferences.getCommandPaths()).thenReturn(commandPathsProperty);
    }

    @Test
    void getCommandLineEscapingUnix() {
        CitationCommandString maliciousCommand = new CitationCommandString("\\cite{'; touch /tmp/pwned; #", ",", "}");
        when(preferences.getCiteCommand()).thenReturn(maliciousCommand);

        // this adds the /usr/bin/subl
        pushToSublimeText.pushEntries(List.of());

        String[] commandLine = pushToSublimeText.getCommandLine("key");

        String[] expected = {"/usr/bin/subl", "--command", "insert {\"characters\": \"\\\\cite{'; touch /tmp/pwned; #key}\"}"};
        assertArrayEquals(expected, commandLine);
    }
}
