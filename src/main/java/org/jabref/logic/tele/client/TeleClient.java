package org.jabref.logic.tele.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javafx.util.Pair;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.tele.TeleMessage;
import org.jabref.logic.tele.TelePreferences;
import org.jabref.logic.tele.TeleProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeleClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleClient.class);

    private static final int TIMEOUT = 1000;
    private final int port;

    public TeleClient(int port) {
        this.port = port;
    }

    public boolean ping() {
        try (TeleProtocol protocol = openNewConnection()) {
            protocol.sendMessage(TeleMessage.PING);
            Pair<TeleMessage, Object> response = protocol.receiveMessage();

            if (response.getKey() == TeleMessage.PONG && TeleProtocol.IDENTIFIER.equals(response.getValue())) {
                return true;
            } else {
                String port = String.valueOf(this.port);
                String errorMessage = Localization.lang("Cannot use port %0 for remote operation; another application may be using it. Try specifying another port.", port);
                LOGGER.error(errorMessage);
                return false;
            }
        } catch (IOException e) {
            LOGGER.debug("Could not ping server at port " + port, e);
            return false;
        }
    }

    /**
     * Attempt to send command line arguments to already running JabRef instance.
     *
     * @param args command line arguments.
     * @return true if successful, false otherwise.
     */
    public boolean sendCommandLineArguments(String[] args) {
        try (TeleProtocol protocol = openNewConnection()) {
            protocol.sendMessage(TeleMessage.SEND_COMMAND_LINE_ARGUMENTS, args);
            Pair<TeleMessage, Object> response = protocol.receiveMessage();
            return response.getKey() == TeleMessage.OK;
        } catch (IOException e) {
            LOGGER.debug("Could not send args " + String.join(", ", args) + " to the server at port " + port, e);
            return false;
        }
    }

    private TeleProtocol openNewConnection() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(TIMEOUT);
        socket.connect(new InetSocketAddress(TelePreferences.getIpAddress(), port), TIMEOUT);
        return new TeleProtocol(socket);
    }
}
