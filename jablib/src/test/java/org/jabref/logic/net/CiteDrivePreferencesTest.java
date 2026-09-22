package org.jabref.logic.net;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CiteDrivePreferencesTest {

    @Test
    void endpointsFollowBaseUrls() {
        CiteDrivePreferences preferences = new CiteDrivePreferences(null, true, "https://api.example.com/", "https://app.example.com/");

        assertEquals(URI.create("https://api.example.com/jabref/login/"), preferences.getAuthorizationEndpoint());
        assertEquals(URI.create("https://api.example.com/o/token/"), preferences.getTokenEndpoint());
        assertEquals(URI.create("https://api.example.com/jabref/push/"), preferences.getPushEndpoint());
        assertEquals(URI.create("https://app.example.com/jabref/push/"), preferences.getImportPage());
    }
}
