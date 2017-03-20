package org.jabref.logic.remote;

public class RemoteUtil {

    private RemoteUtil() {
    }

    public static boolean isUserPort(int portNumber) {
        return (portNumber >= 1024) && (portNumber <= 65535);
    }
}
