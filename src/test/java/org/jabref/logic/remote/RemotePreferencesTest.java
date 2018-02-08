package org.jabref.logic.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemotePreferencesTest {

    private RemotePreferences preferences;


    @BeforeEach
    public void setUp() {
        preferences = new RemotePreferences(1000, true);
    }

    @Test
    public void testGetPort() {
        assertEquals(1000, preferences.getPort());
    }

    @Test
    public void testSetPort() {
        preferences.setPort(2000);
        assertEquals(2000, preferences.getPort());
    }

    @Test
    public void testUseRemoteServer() {
        assertTrue(preferences.useRemoteServer());
    }

    @Test
    public void testSetUseRemoteServer() {
        preferences.setUseRemoteServer(false);
        assertFalse(preferences.useRemoteServer());
    }

    @Test
    public void testIsDifferentPortTrue() {
        assertTrue(preferences.isDifferentPort(2000));
    }

    @Test
    public void testIsDifferentPortFalse() {
        assertFalse(preferences.isDifferentPort(1000));
    }

}
