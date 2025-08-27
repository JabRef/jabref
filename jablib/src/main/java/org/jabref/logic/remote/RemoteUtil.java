package org.jabref.logic.remote;

public class RemoteUtil {

    private RemoteUtil() {
    }

    public static boolean isUserPort(int portNumber) {
        return (portNumber >= 1024) && (portNumber <= 65535);
    }

    public static boolean isStringUserPort(String portString) {
        try {
            int portNumber = Integer.parseInt(portString);
            return isUserPort(portNumber);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
