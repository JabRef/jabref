package org.jabref.logic.net;

import org.jabref.gui.Globals;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.preferences.JabRefPreferences;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.*;
public class ProxyTest {
    /**
     * The method test wheter storing a mock data of proxy information gets stored as intented, where
     * proxy host, port, username should be stored in the system register and not password. The test also
     * ensures that the previous stored information isn't lost through extracting the
     * information first and then restores it.
     */
    @Test
    public void testProxyPasswordNotStoredInRegister(){
        //Get stored proxy preferences from registers
        final JabRefPreferences preferences = JabRefPreferences.getInstance();
        PreferencesMigrations.runMigrations(preferences);
        ProxyPreferences prox = preferences.getProxyPreferences();
        //Save old registers
        String oldUseProxy = prox.shouldUseProxy().toString();
        String oldHostname = prox.getHostname();
        String oldPort = prox.getPort();
        String oldUseAuthentication = prox.shouldUseAuthentication().toString();
        String oldUsername = prox.getUsername();
        String oldPassword = prox.getPassword();
        //mock data
        String useProxy = "true";
        String hostname = "testName";
        String port = "8080";
        String useAuthentication = "true";
        String username = "testUserName";
        String password = "testPassword";
        //String used for enabling usage of preference.put() for each proxy register
        String PROXY_USE = "useProxy";
        String PROXY_PORT = "proxyPort";
        String PROXY_HOSTNAME = "proxyHostname";
        String PROXY_USERNAME = "proxyUsername";
        String PROXY_PASSWORD = "proxyPassword";
        String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";
        //Writing to register with mock data
        preferences.put(PROXY_USE, useProxy);
        preferences.put(PROXY_HOSTNAME, hostname);
        preferences.put(PROXY_PORT, port);
        preferences.put(PROXY_USE_AUTHENTICATION, useAuthentication);
        preferences.put(PROXY_USERNAME, username);
        preferences.put(PROXY_PASSWORD, password);
        //Actual test being conducted
        if(prox.shouldUseProxy()){
            assertEquals(prox.getHostname(),"testName");
            assertEquals(prox.getPort(),"8080");
            System.out.println();
        }
        if(prox.shouldUseAuthentication()){
            assertEquals(prox.getUsername(),"testUserName");
            assertNotEquals(prox.getPassword(), "testPassword");
        }
        //Restores registers to previous state
        preferences.put(PROXY_USE, oldUseProxy);
        preferences.put(PROXY_HOSTNAME, oldHostname);
        preferences.put(PROXY_PORT, oldPort);
        preferences.put(PROXY_USE_AUTHENTICATION, oldUseAuthentication);
        preferences.put(PROXY_USERNAME, oldUsername);
        preferences.put(PROXY_PASSWORD, oldPassword);
    }

    /**
     * The test checks if ProxyPreference class is still able to store password and use it from memory,
     * even though it's no longer stored in register.
     */
    @Test
    public void testProxyPreferencesStorePassword() {
        //mock data
        Boolean useProxy = true;
        String hostname = "testName";
        String port = "8080";
        Boolean useAuthentication = true;
        String username = "testUserName";
        String password = "testPassword";
        //Creates proxy preference
        ProxyPreferences proxyPref = new ProxyPreferences(
                useProxy,
                hostname,
                port,
                useAuthentication,
                username,
                password);
        //Check if mock data is stored in object memory and can be extracted
        assertEquals(proxyPref.shouldUseProxy(), true);
        assertEquals(proxyPref.getHostname(), "testName");
        assertEquals(proxyPref.getPort(), port);
        assertEquals(proxyPref.shouldUseAuthentication(), true);
        assertEquals(proxyPref.getUsername(), username);
        assertEquals(proxyPref.getPassword(), password);
    }
}
