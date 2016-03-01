package net.sf.jabref.logic.net;

public class ProxyRegisterer {

    public static void register(ProxyPreferences proxyPrefs) {
        if (proxyPrefs.isUseProxy()) {
            // NetworkTab.java ensures that proxyHostname and proxyPort are not null
            System.setProperty("http.proxyHost", proxyPrefs.getHostname());
            System.setProperty("http.proxyPort", proxyPrefs.getPort());

            // NetworkTab.java ensures that proxyUsername and proxyPassword are neither null nor empty
            if (proxyPrefs.isUseAuthentication()) {
                System.setProperty("http.proxyUser", proxyPrefs.getUsername());
                System.setProperty("http.proxyPassword", proxyPrefs.getPassword());
            }
        } else {
            // The following two lines signal that the system proxy settings
            // should be used:
            System.setProperty("java.net.useSystemProxies", "true");
            System.setProperty("proxySet", "true");
        }
    }
}
