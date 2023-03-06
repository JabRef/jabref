package org.jabref.logic.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProxyTest {
   /**
    * The test checks if ProxyPreference class is still able to store password and use it from memory,
    * even though it's no longer stored in register.
    */
   @Test
   public void testProxyPreferencesStorePassword() {
       // mock data
       Boolean useProxy = true;
       String hostname = "testName";
       String port = "8080";
       Boolean useAuthentication = true;
       String username = "testUserName";
       String password = "testPassword";
       // Creates proxy preference
       ProxyPreferences proxyPref = new ProxyPreferences(
               useProxy,
               hostname,
               port,
               useAuthentication,
               username,
               password);
       // Check if mock data is stored in object memory and can be extracted
       assertEquals(proxyPref.shouldUseProxy(), true);
       assertEquals(proxyPref.getHostname(), hostname);
       assertEquals(proxyPref.getPort(), port);
       assertEquals(proxyPref.shouldUseAuthentication(), true);
       assertEquals(proxyPref.getUsername(), username);
       assertEquals(proxyPref.getPassword(), password);
   }
}
