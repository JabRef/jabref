package org.jabref.websocket;

public class JabRefWebsocketServerInstance {

    private static final int PORT = 8855;
    private static JabRefWebsocketServer jabRefWebsocketServer = null;

    private JabRefWebsocketServerInstance() {

    }

    public static JabRefWebsocketServer getInstance() {
        if (jabRefWebsocketServer == null) {
            jabRefWebsocketServer = new JabRefWebsocketServer(PORT);
            addShutdownHook();
        }

        return jabRefWebsocketServer;
    }

    private static void addShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            /**
             * run() is invoked, when JabRef gets terminated.
             */
            public void run()
            {
                if (jabRefWebsocketServer != null)
                {
                    jabRefWebsocketServer.stopServer();
                }
            }
        });
    }

    public static void main(String[] args) {
        JabRefWebsocketServerInstance.getInstance().startServer();
    }
}
