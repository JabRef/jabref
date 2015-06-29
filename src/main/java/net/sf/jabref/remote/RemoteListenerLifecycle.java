package net.sf.jabref.remote;

import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefExecutorService;

public class RemoteListenerLifecycle {

    private static RemoteListener remoteListener = null;

    public static void disableRemoteListener() {
        if(isRemoteListenerOpen()) {
            remoteListener.interrupt();
            remoteListener = null;
        }
    }

    /**
     * startRemoteListener must be called afterwards to start this
     */
    public static void openRemoteListener(JabRef jabRef) {
        if(isRemoteListenerOpen()) {
            return;
        }

        remoteListener = RemoteListener.openRemoteListener(jabRef);
    }

    public static boolean isRemoteListenerOpen() {
        return remoteListener != null;
    }

    public static void startRemoteListener() {
        if (isRemoteListenerOpen()) {
            JabRefExecutorService.INSTANCE.executeInOwnThread(remoteListener);
        }
    }

    public static void openAndStartRemoteListener(JabRef jabRef) {
        openRemoteListener(jabRef);
        startRemoteListener();
    }
}
