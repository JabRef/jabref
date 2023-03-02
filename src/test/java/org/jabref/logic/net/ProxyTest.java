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

     A Javadoc for the testProxyNotStoredInRegisterHttp() method which tests the ProxyRegisterer class
     for HTTP protocol. This method checks if the proxy host, port, username are stored and password aren't stored in
     the system register after registering the new proxy preference for HTTP protocol.

     @throws IOException if an I/O error occurs.
     */
    @Test
    public void testProxyCredentialsMotStoredInRegisterHttp() throws IOException, BackingStoreException {
        /*
        Boolean useProxy = true;
        String hostname = "testName";
        String port = "8080";
        Boolean useAuthentication = true;
        String username = "testUserName";
        String password = "testPassword";


        ProxyPreferences proxyPref = new ProxyPreferences(useProxy, hostname,port,useAuthentication,username,password);
        */

        //Get stored proxy preferences
        final JabRefPreferences preferences = JabRefPreferences.getInstance();
        //Globals.prefs = preferences;
        PreferencesMigrations.runMigrations(preferences);
        ProxyPreferences prox = preferences.getProxyPreferences();
        if(prox.shouldUseProxy()){
            assertNotEquals(prox.getHostname(),"");
            assertNotEquals(prox.getPort(),"");
        }
        if(prox.shouldUseAuthentication()){
            assertNotEquals(prox.getUsername(),"");
            assertEquals(prox.getPassword(), "");
        }
        //Run the test 2 times if test don't work to hopefully flush everything from older version and then connect again
        //preferences.clear();
    }
    /**

     A Javadoc for the testProxyNotStoredInRegisterHttps() method which tests the ProxyRegisterer class
     for HTTPS protocol. This method checks if the proxy host, port, username are stored and password aren't stored in
     the system register after registering the new proxy preference for HTTPS protocol.

     @throws IOException if an I/O error occurs.

    @Test
    public void testProxyCredentialsMotStoredInRegisterHttps() throws IOException {
        String oldhost = System.getProperty("http" + ".proxyHost", "");
        String oldport = System.getProperty("http" + ".proxyPort", "");
        String olduser = System.getProperty("http" + ".proxyUser", "");
        String oldpassword = System.getProperty("http" + ".proxyPassword", "");

        Boolean useProxy = true;
        String hostname = "testName";
        String port = "8080";
        Boolean useAuthentication = true;
        String username = "testUserName";
        String password = "testPassword";

        ProxyPreferences proxyPref = new ProxyPreferences(useProxy, hostname,port,useAuthentication,username,password);
        ProxyPreferences oldProxyPref = new ProxyPreferences(useProxy, oldhost,oldport,useAuthentication,olduser,oldpassword);

        assertNotEquals(hostname , oldhost);
        assertNotEquals(port , oldport);
        assertNotEquals(username, olduser);
        assertNotEquals(password, oldpassword);

        ProxyRegisterer.register(proxyPref);

        assertEquals(hostname , System.getProperty("https" + ".proxyHost", "")  );
        assertEquals(port , System.getProperty("https" + ".proxyPort", "")  );
        assertEquals(username, System.getProperty("https" + ".proxyUser", "")  );
        //assertNotEquals(password, System.getProperty("http" + ".proxyPassword", "")  );
        // Last line shoud be incommented when password not stored in register anymore.

        //reset to what it was before
        ProxyRegisterer.register(oldProxyPref);
    }
     */
}
