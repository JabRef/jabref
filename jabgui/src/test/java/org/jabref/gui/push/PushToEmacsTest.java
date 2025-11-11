package org.jabref.gui.push;

import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.DialogService;
import org.jabref.logic.os.OS;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("Needs running emacs daemon. Start emacs with --daemon")
class PushToEmacsTest {
    GuiPushToEmacs pushToEmacs;

    @BeforeEach
    void setup() {
        DialogService dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);

        PushToApplicationPreferences pushToApplicationPreferences = mock(PushToApplicationPreferences.class);

        String emacsClient = OS.WINDOWS ? "\"C:\\tools\\emacs\\bin\\emacsclientw.exe\"" : "emacsclient";
        Map<String, String> emacsConfig = Map.of("Emacs", emacsClient);
        ObservableMap<String, String> emacsConfigObservableMap = FXCollections.observableMap(emacsConfig);
        when(pushToApplicationPreferences.getCommandPaths()).thenReturn(new SimpleMapProperty<>(emacsConfigObservableMap));

        when(pushToApplicationPreferences.getEmacsArguments()).thenReturn("-n -e");

        when(pushToApplicationPreferences.getCiteCommand().toString()).thenReturn("\\cite{key1,key2}");

        pushToEmacs = new GuiPushToEmacs(dialogService, pushToApplicationPreferences);
    }

    @Test
    void pushEntries() {
        pushToEmacs.pushEntries(
                List.of(
                        new BibEntry().withCitationKey("key1"),
                        new BibEntry().withCitationKey("key2"))
        );
    }
}
