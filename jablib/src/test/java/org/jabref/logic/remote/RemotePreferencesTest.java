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
        preferences = new RemotePreferences(1000, true, 3000, false, false, 2087);
    }

    @Test
    void getPort() {
        assertEquals(1000, preferences.getPort());
    }

    @Test
    void getHttpPort() {
        assertEquals(3000, preferences.getHttpPort());
    }

    @Test
    void setPort() {
        preferences.setPort(2000);
        assertEquals(2000, preferences.getPort());
    }

    @Test
    void setHttpPort() {
        preferences.setHttpPort(4000);
        assertEquals(4000, preferences.getHttpPort());
    }

    @Test
    void useRemoteServer() {
        assertTrue(preferences.useRemoteServer());
    }

    @Test
    void setUseRemoteServer() {
        preferences.setUseRemoteServer(false);
        assertFalse(preferences.useRemoteServer());
    }

    @Test
    void isDifferentPortTrue() {
        assertTrue(preferences.isDifferentPort(2000));
    }

    @Test
    void isDifferentHttpPortTrue() {
        assertTrue(preferences.isDifferentHttpPort(4000));
    }

    @Test
    void isDifferentPortFalse() {
        assertFalse(preferences.isDifferentPort(1000));
    }

    @Test
    void isDifferentHttpPortFalse() {
        assertFalse(preferences.isDifferentHttpPort(3000));
    }
}
