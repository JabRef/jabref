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
     * The method test if by storing a mock data of proxy information it get's stored as intented, where
     * proxy host, port, username are stored and password aren't stored in
     * the system register. It also ensures that the previous stored information isn't lost through extracting the
     * information first and then restores it.
     */
    @Test
    public void testProxyPasswordNotStoredInRegister(){
        //Get stored proxy preferences
        final JabRefPreferences preferences = JabRefPreferences.getInstance();
        //Globals.prefs = preferences;
        PreferencesMigrations.runMigrations(preferences);
        ProxyPreferences prox = preferences.getProxyPreferences();

        String oldUseProxy = prox.shouldUseProxy().toString();
        String oldHostname = prox.getHostname();
        String oldPort = prox.getPort();
        String oldUseAuthentication = prox.shouldUseAuthentication().toString();
        String oldUsername = prox.getUsername();
        String oldPassword = prox.getPassword();

        //Part 2 of test
        String useProxy = "true";
        String hostname = "testName";
        String port = "8080";
        String useAuthentication = "true";
        String username = "testUserName";
        String password = "testPassword";

        String PROXY_USE = "useProxy";
        String PROXY_PORT = "proxyPort";
        String PROXY_HOSTNAME = "proxyHostname";
        String PROXY_USERNAME = "proxyUsername";
        String PROXY_PASSWORD = "proxyPassword";
        String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";

        preferences.put(PROXY_USE, useProxy);
        preferences.put(PROXY_HOSTNAME, hostname);
        preferences.put(PROXY_PORT, port);
        preferences.put(PROXY_USE_AUTHENTICATION, useAuthentication);
        preferences.put(PROXY_USERNAME, username);
        preferences.put(PROXY_PASSWORD, password);

        if(prox.shouldUseProxy()){
            assertEquals(prox.getHostname(),"testName");
            assertEquals(prox.getPort(),"8080");
            System.out.println();
        }
        if(prox.shouldUseAuthentication()){
            assertEquals(prox.getUsername(),"testUserName");
            assertNotEquals(prox.getPassword(), "testPassword");
        }

        preferences.put(PROXY_USE, oldUseProxy);
        preferences.put(PROXY_HOSTNAME, oldHostname);
        preferences.put(PROXY_PORT, oldPort);
        preferences.put(PROXY_USE_AUTHENTICATION, oldUseAuthentication);
        preferences.put(PROXY_USERNAME, oldUsername);
        preferences.put(PROXY_PASSWORD, oldPassword);
    }
    /**
     * Add an additional test for testing that password get's stored somehow!
     */
}
