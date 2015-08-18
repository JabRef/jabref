package net.sf.jabref.logic.remote;

public class RemoteUtil {

    public static boolean isUserPort(int portNumber) {
        return portNumber >= 1024 && portNumber <= 65535;
    }
}
