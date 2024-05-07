package org.jabref.gui.push;

import java.util.Map;

import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.gui.DialogService;
import org.jabref.logic.util.OS;
import org.jabref.preferences.ExternalApplicationsPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("Needs running emacs daemon. Start emacs with --daemon")
class PushToEmacsTest {
    PushToEmacs pushToEmacs;

    @BeforeEach
    public void setup() {
        DialogService dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);
        PreferencesService preferencesService = mock(PreferencesService.class);

        PushToApplicationPreferences pushToApplicationPreferences = mock(PushToApplicationPreferences.class);

        String emacsClient = OS.WINDOWS ? "\"C:\\tools\\emacs\\bin\\emacsclientw.exe\"" : "emacsclient";
        Map<String, String> emacsConfig = Map.of("Emacs", emacsClient);
        ObservableMap<String, String> emacsConfigObservableMap = FXCollections.observableMap(emacsConfig);
        when(pushToApplicationPreferences.getCommandPaths()).thenReturn(new SimpleMapProperty<>(emacsConfigObservableMap));

        when(pushToApplicationPreferences.getEmacsArguments()).thenReturn("-n -e");

        when(preferencesService.getPushToApplicationPreferences()).thenReturn(pushToApplicationPreferences);

        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getCiteCommand().toString()).thenReturn("\\cite{key1,key2}");
        when(preferencesService.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);

        pushToEmacs = new PushToEmacs(dialogService, preferencesService);
    }

    @Test
    void pushEntries() {
        pushToEmacs.pushEntries(null, null, "key1,key2");
    }
}
