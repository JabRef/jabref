package org.jabref.logic.net;

public class ProxyRegisterer {

    private ProxyRegisterer() {
    }

    public static void register(ProxyPreferences proxyPrefs) {
        if (proxyPrefs.shouldUseProxy()) {
            // NetworkTabView.java ensures that proxyHostname and proxyPort are not null
            System.setProperty("http.proxyHost", proxyPrefs.getHostname());
            System.setProperty("http.proxyPort", proxyPrefs.getPort());

            System.setProperty("https.proxyHost", proxyPrefs.getHostname());
            System.setProperty("https.proxyPort", proxyPrefs.getPort());

            // NetworkTabView.java ensures that proxyUsername and proxyPassword are neither null nor empty
            if (proxyPrefs.shouldUseAuthentication()) {
                System.setProperty("http.proxyUser", proxyPrefs.getUsername());
                System.setProperty("http.proxyPassword", proxyPrefs.getPassword());

                System.setProperty("https.proxyUser", proxyPrefs.getUsername());
                System.setProperty("https.proxyPassword", proxyPrefs.getPassword());
            }
        } else {
            // The following two lines signal that the system proxy settings
            // should be used:
            System.setProperty("java.net.useSystemProxies", "true");
            System.setProperty("proxySet", "true");
        }
    }
}
