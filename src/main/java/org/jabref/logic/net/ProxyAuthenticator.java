package org.jabref.logic.net;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Locale;

public class ProxyAuthenticator extends Authenticator {

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestorType() == RequestorType.PROXY) {
            String prot = getRequestingProtocol().toLowerCase(Locale.ROOT);
            String host = System.getProperty(prot + ".proxyHost", "");
            String port = System.getProperty(prot + ".proxyPort", "80");
            String user = System.getProperty(prot + ".proxyUser", "");
            String password = System.getProperty(prot + ".proxyPassword", "");
            if (getRequestingHost().equalsIgnoreCase(host) && (Integer.parseInt(port) == getRequestingPort())) {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        }
        return null;
    }
}
