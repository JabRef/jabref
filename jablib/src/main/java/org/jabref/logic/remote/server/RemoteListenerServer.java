package org.jabref.logic.remote.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javafx.util.Pair;

import org.jabref.logic.remote.Protocol;
import org.jabref.logic.remote.RemoteMessage;
import org.jabref.logic.remote.RemotePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteListenerServer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServer.class);

    private static final int BACKLOG = 1;

    private static final int TIMEOUT = 1000;

    private static final byte[] HEALTH_CHECK_PREFIX = "JABREF/1 ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HEALTH_CHECK_REMAINING_PREFIX = Arrays.copyOfRange(HEALTH_CHECK_PREFIX, 1, HEALTH_CHECK_PREFIX.length);
    private static final byte[] HEALTH_CHECK_REQUEST = "PING\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HEALTH_CHECK_RESPONSE = ("JABREF/1 PONG " + Protocol.IDENTIFIER + "\n").getBytes(StandardCharsets.UTF_8);

    private final RemoteMessageHandler messageHandler;
    private final ServerSocket serverSocket;

    public RemoteListenerServer(RemoteMessageHandler messageHandler, int port) throws IOException {
        this.serverSocket = new ServerSocket(port, BACKLOG, RemotePreferences.getIpAddress());
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                try (Socket socket = serverSocket.accept()) {
                    socket.setSoTimeout(TIMEOUT);
                    handleConnection(socket);
                } catch (SocketException ex) {
                    return;
                } catch (IOException e) {
                    LOGGER.warn("RemoteListenerServer crashed", e);
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    // [impl->req~jabref.remote.health-check~1]
    private void handleConnection(Socket socket) throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(socket.getInputStream())) {
            if (isHealthCheck(input)) {
                handleHealthCheck(input, socket.getOutputStream());
                return;
            }

            try (Protocol protocol = new Protocol(socket, input)) {
                Pair<RemoteMessage, Object> message = protocol.receiveMessage();
                handleMessage(protocol, message.getKey(), message.getValue());
            }
        }
    }

    /// Probing must not consume anything on a non-match: [Protocol] deserializes from the same
    /// stream afterwards, so any partially matching prefix is un-read via mark/reset.
    ///
    /// The full prefix is only read after the first byte matched. A serialized [Protocol] request
    /// starts with the object-stream header (`0xAC`...), of which the client initially sends only
    /// four bytes — blocking here for the full prefix length would deadlock both sides until the
    /// socket timeout.
    private boolean isHealthCheck(BufferedInputStream input) throws IOException {
        input.mark(HEALTH_CHECK_PREFIX.length);
        int firstByte = input.read();
        if (firstByte != HEALTH_CHECK_PREFIX[0]) {
            input.reset();
            return false;
        }

        byte[] remainingPrefix = input.readNBytes(HEALTH_CHECK_PREFIX.length - 1);
        if (Arrays.equals(remainingPrefix, HEALTH_CHECK_REMAINING_PREFIX)) {
            return true;
        }
        input.reset();
        return false;
    }

    private void handleHealthCheck(InputStream input, OutputStream output) throws IOException {
        byte[] request = input.readNBytes(HEALTH_CHECK_REQUEST.length);
        if (Arrays.equals(request, HEALTH_CHECK_REQUEST)) {
            output.write(HEALTH_CHECK_RESPONSE);
            output.flush();
        }
    }

    private void handleMessage(Protocol protocol, RemoteMessage type, Object argument) throws IOException {
        switch (type) {
            case PING:
                protocol.sendMessage(RemoteMessage.PONG, Protocol.IDENTIFIER);
                break;
            case SEND_COMMAND_LINE_ARGUMENTS:
                if (argument instanceof String[] strings) {
                    messageHandler.handleCommandLineArguments(strings);
                    protocol.sendMessage(RemoteMessage.OK);
                } else {
                    throw new IOException("Argument for 'SEND_COMMAND_LINE_ARGUMENTS' is not of type String[]. Got " + argument);
                }
                break;
            case FOCUS:
                messageHandler.handleFocus();
                protocol.sendMessage(RemoteMessage.OK);
                break;
            default:
                throw new IOException("Unhandled message to server " + type);
        }
    }

    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.warn("Unable to close server socket", e);
        }
    }
}
