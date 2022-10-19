package org.jabref.logic.tele.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javafx.util.Pair;

import org.jabref.logic.tele.TeleMessage;
import org.jabref.logic.tele.TelePreferences;
import org.jabref.logic.tele.TeleProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeleServer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleServer.class);

    private static final int BACKLOG = 1;

    private static final int TIMEOUT = 1000;

    private final TeleMessageHandler messageHandler;
    private final ServerSocket serverSocket;

    public TeleServer(TeleMessageHandler messageHandler, int port) throws IOException {
        this.serverSocket = new ServerSocket(port, BACKLOG, TelePreferences.getIpAddress());
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                try (Socket socket = serverSocket.accept()) {
                    socket.setSoTimeout(TIMEOUT);

                    try (TeleProtocol protocol = new TeleProtocol(socket)) {
                        Pair<TeleMessage, Object> input = protocol.receiveMessage();
                        handleMessage(protocol, input.getKey(), input.getValue());
                    }
                } catch (SocketException ex) {
                    return;
                } catch (IOException e) {
                    LOGGER.warn("TeleServer crashed", e);
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    private void handleMessage(TeleProtocol protocol, TeleMessage type, Object argument) throws IOException {
        switch (type) {
            case PING:
                protocol.sendMessage(TeleMessage.PONG, TeleProtocol.IDENTIFIER);
                break;
            case SEND_COMMAND_LINE_ARGUMENTS:
                if (argument instanceof String[]) {
                    messageHandler.handleCommandLineArguments((String[]) argument);
                    protocol.sendMessage(TeleMessage.OK);
                } else {
                    throw new IOException("Argument for 'SEND_COMMAND_LINE_ARGUMENTS' is not of type String[]. Got " + argument);
                }
                break;
            default:
                throw new IOException("Unhandled message to server " + type);
        }
    }

    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException exception) {
            LOGGER.error("Could not close server socket", exception);
        }
    }
}
