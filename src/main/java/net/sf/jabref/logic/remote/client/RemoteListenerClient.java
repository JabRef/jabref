package net.sf.jabref.logic.remote.client;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.remote.shared.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.Socket;

public class RemoteListenerClient {

    private static final Log LOGGER = LogFactory.getLog(RemoteListenerClient.class);

    private static final int TIMEOUT = 2000;


    /**
     * Attempt to send command line arguments to already running JabRef instance.
     *
     * @param args Command line arguments.
     * @return true if successful, false otherwise.
     */
    public static boolean sendToActiveJabRefInstance(String[] args, int remoteServerPort) {
        try (Socket socket = new Socket(InetAddress.getByName("localhost"), remoteServerPort)) {
            socket.setSoTimeout(TIMEOUT);

            Protocol protocol = new Protocol(socket);
            try {
                String identifier = protocol.receiveMessage();

                if (!Protocol.IDENTIFIER.equals(identifier)) {
                    String port = String.valueOf(remoteServerPort);
                    String error = Localization.lang("Cannot use port %0 for remote operation; another application may be using it. Try specifying another port.", port);
                    System.out.println(error);
                    return false;
                }
                protocol.sendMessage(String.join("\n", args));
                return true;
            } finally {
                protocol.close();
            }
        } catch (Exception e) {
            LOGGER.debug(
                    "Could not send args " + String.join(", ", args) + " to the server at port " + remoteServerPort, e);
            return false;
        }
    }
}
