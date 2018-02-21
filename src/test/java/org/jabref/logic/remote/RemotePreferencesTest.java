package org.jabref.logic.remote;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RemotePreferencesTest {

    private RemotePreferences preferences;


    @Before
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
