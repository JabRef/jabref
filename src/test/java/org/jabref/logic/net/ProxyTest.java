package org.jabref.logic.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxyTest {
   /**
    * The test checks if ProxyPreference class is still able to store password and use it from memory,
    * even though it's no longer stored in register.
    */
   @Test
   public void testProxyPreferencesStorePassword() {
       // mock data
       boolean useProxy = true;
       String hostname = "testName";
       String port = "8080";
       boolean useAuthentication = true;
       String username = "testUserName";
       String password = "testPassword";
       boolean persist = false;

       // Creates proxy preference
       ProxyPreferences proxyPref = new ProxyPreferences(
               useProxy,
               hostname,
               port,
               useAuthentication,
               username,
               password,
               persist);

       // Check if mock data is stored in object memory and can be extracted
       assertTrue(proxyPref.shouldUseProxy());
       assertEquals(proxyPref.getHostname(), hostname);
       assertEquals(proxyPref.getPort(), port);
       assertTrue(proxyPref.shouldUseAuthentication());
       assertEquals(proxyPref.getUsername(), username);
       assertEquals(proxyPref.getPassword(), password);
       assertEquals(proxyPref.shouldPersistPassword(), persist);
   }
}
