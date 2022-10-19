package org.jabref.logic.tele;

public class TeleUtil {

    private TeleUtil() {
    }

    public static boolean isUserPort(int portNumber) {
        return (portNumber >= 1024) && (portNumber <= 65535);
    }
}
