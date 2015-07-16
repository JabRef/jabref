package net.sf.jabref.remote;

public class RemoteUtil {
    public static boolean isValidPartNumber(int portNumber) {
        return portNumber > 1024 && portNumber <= 65535;
    }
}
