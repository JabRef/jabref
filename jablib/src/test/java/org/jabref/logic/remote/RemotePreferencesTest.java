package org.jabref.logic.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemotePreferencesTest {

    private RemotePreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = new RemotePreferences(true, 1000, false, 3000, false, 2087, true);
    }

    @Test
    void getPort() {
        assertEquals(1000, preferences.getRemoteServerPort());
    }

    @Test
    void getHttpPort() {
        assertEquals(3000, preferences.getHttpServerPort());
    }

    @Test
    void setPort() {
        preferences.setRemoteServerPort(2000);
        assertEquals(2000, preferences.getRemoteServerPort());
    }

    @Test
    void setHttpPort() {
        preferences.setHttpServerPort(4000);
        assertEquals(4000, preferences.getHttpServerPort());
    }

    @Test
    void shouldEnableRemoteServer() {
        assertTrue(preferences.shouldEnableRemoteServer());
    }

    @Test
    void setShouldEnableRemoteServer() {
        preferences.setEnableRemoteServer(false);
        assertFalse(preferences.shouldEnableRemoteServer());
    }

    @Test
    void isDifferentRemoteServerPortTrue() {
        assertTrue(preferences.isDifferentRemoteServerPort(2000));
    }

    @Test
    void isDifferentHttpServerPortTrue() {
        assertTrue(preferences.isDifferentHttpServerPort(4000));
    }

    @Test
    void isDifferentRemoteServerPortFalse() {
        assertFalse(preferences.isDifferentRemoteServerPort(1000));
    }

    @Test
    void isDifferentHttpServerPortFalse() {
        assertFalse(preferences.isDifferentHttpServerPort(3000));
    }
}
