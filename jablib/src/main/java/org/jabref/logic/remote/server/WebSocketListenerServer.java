package org.jabref.logic.remote.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket server implementation for browser extension communication.
 * Implements RFC 6455 WebSocket protocol for text-based messaging.
 */
public class WebSocketListenerServer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketListenerServer.class);
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final ServerSocket serverSocket;
    private final RemoteMessageHandler messageHandler;

    public WebSocketListenerServer(RemoteMessageHandler messageHandler, int port) throws IOException {
        this.messageHandler = messageHandler;
        this.serverSocket = new ServerSocket(port, 1, java.net.InetAddress.getLoopbackAddress());
        LOGGER.info("WebSocket server started on port {}", port);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                try (Socket clientSocket = serverSocket.accept()) {
                    LOGGER.debug("WebSocket client connected from {}", clientSocket.getRemoteSocketAddress());
                    handleConnection(clientSocket);
                } catch (SocketException e) {
                    if (serverSocket.isClosed()) {
                        LOGGER.debug("WebSocket server socket closed");
                        break;
                    }
                    LOGGER.error("Socket error in WebSocket server", e);
                } catch (IOException e) {
                    LOGGER.error("Error handling WebSocket connection", e);
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    private void handleConnection(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream output = clientSocket.getOutputStream();

        // Perform WebSocket handshake
        if (!performHandshake(reader, output)) {
            LOGGER.warn("WebSocket handshake failed");
            return;
        }

        LOGGER.debug("WebSocket handshake successful");

        // Handle WebSocket messages
        handleWebSocketMessages(clientSocket, output);
    }

    private boolean performHandshake(BufferedReader reader, OutputStream output) throws IOException {
        String line;
        String webSocketKey = null;

        // Read HTTP headers
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Sec-WebSocket-Key:")) {
                webSocketKey = line.substring("Sec-WebSocket-Key:".length()).trim();
            }
        }

        if (webSocketKey == null) {
            return false;
        }

        // Generate accept key
        String acceptKey = generateAcceptKey(webSocketKey);

        // Send handshake response
        String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "\r\n";

        output.write(response.getBytes(StandardCharsets.UTF_8));
        output.flush();

        return true;
    }

    // Generates the Sec-WebSocket-Accept key from the Sec-WebSocket-Key
    // according to RFC 6455 (https://www.rfc-editor.org/rfc/rfc6455)
    private String generateAcceptKey(String webSocketKey) {
        try {
            String concatenated = webSocketKey + WEBSOCKET_GUID;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-1 algorithm not available", e);
            return "";
        }
    }

    private void handleWebSocketMessages(Socket clientSocket, OutputStream output) throws IOException {
        while (!Thread.interrupted() && !clientSocket.isClosed()) {
            // Read the first two bytes to get FIN, opcode, and payload length
            byte[] header = clientSocket.getInputStream().readNBytes(2);
            if (header.length < 2) {
                break; // Connection closed
            }

            byte firstByte = header[0];
            byte secondByte = header[1];

            boolean fin = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;
            boolean masked = (secondByte & 0x80) != 0;
            long payloadLength = secondByte & 0x7F;

            // Handle extended payload length
            if (payloadLength == 126) {
                byte[] extendedPayloadLengthBytes = clientSocket.getInputStream().readNBytes(2);
                if (extendedPayloadLengthBytes.length < 2) {
                    break;
                }
                payloadLength = ((extendedPayloadLengthBytes[0] & 0xFF) << 8) | (extendedPayloadLengthBytes[1] & 0xFF);
            } else if (payloadLength == 127) {
                byte[] extendedPayloadLengthBytes = clientSocket.getInputStream().readNBytes(8);
                if (extendedPayloadLengthBytes.length < 8) {
                    break;
                }
                payloadLength = java.nio.ByteBuffer.wrap(extendedPayloadLengthBytes).getLong();
            }

            // Close frame
            if (opcode == 0x08) {
                LOGGER.debug("WebSocket close frame received");
                break;
            }

            // Ping frame
            if (opcode == 0x09) {
                sendPongFrame(output);
                continue;
            }

            // Text frame
            if (opcode == 0x01 && fin && masked) {
                byte[] maskingKey = new byte[4];
                System.arraycopy(buffer, maskOffset, maskingKey, 0, 4);

                byte[] payload = new byte[payloadLength];
                System.arraycopy(buffer, maskOffset + 4, payload, 0, payloadLength);

                // Unmask payload
                for (int i = 0; i < payloadLength; i++) {
                    payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                }

                String message = new String(payload, StandardCharsets.UTF_8);
                LOGGER.debug("Received WebSocket message: {}", message);

                // Handle the message
                String response = handleTextMessage(message);

                // Send response
                sendTextFrame(output, response);
            }
        }
    }

    private String handleTextMessage(String message) {
        try {
            // Parse JSON-like message (simple parsing without external dependencies)
            String command = extractJsonValue(message, "command");
            String argument = extractJsonValue(message, "argument");

            if (command == null) {
                LOGGER.debug("No command found in WebSocket message");
                return "{\"status\":\"error\",\"message\":\"Command not specified\"}";
            }

            LOGGER.debug("Processing command: {} with argument: {}", command, argument);

            return switch (command) {
                case "ping" ->
                        "{\"status\":\"success\",\"response\":\"pong\"}";
                case "focus" -> {
                    messageHandler.handleCommandLineArguments(new String[] {"--focus"});
                    yield "{\"status\":\"success\",\"response\":\"focused\"}";
                }
                case "open" -> {
                    if (argument != null && !argument.isEmpty()) {
                        messageHandler.handleCommandLineArguments(new String[] {"--import", argument});
                        yield "{\"status\":\"success\",\"response\":\"opened\"}";
                    }
                    yield "{\"status\":\"error\",\"message\":\"No file specified\"}";
                }
                case "add" -> {
                    if (argument != null && !argument.isEmpty()) {
                        messageHandler.handleCommandLineArguments(new String[] {"--importBibtex", argument});
                        yield "{\"status\":\"success\",\"response\":\"added\"}";
                    }
                    yield "{\"status\":\"error\",\"message\":\"No BibTeX entry specified\"}";
                }
                default ->
                        "{\"status\":\"error\",\"message\":\"Unknown command: " + command + "\"}";
            };
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message", e);
            return "{\"status\":\"error\",\"message\":\"Internal error\"}";
        }
    }

    private @Nullable String extractJsonValue(String json, String key) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject obj = parsed.getAsJsonObject();
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            return obj.get(key).getAsString();
        } catch (Exception e) {
            LOGGER.debug("Failed to parse JSON in WebSocket message: {}", e.toString());
            return null;
        }
    }

    private void sendTextFrame(OutputStream output, String message) throws IOException {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int payloadLength = payload.length;

        // Build frame
        byte[] frame;
        if (payloadLength <= 125) {
            frame = new byte[2 + payloadLength];
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) payloadLength;
            System.arraycopy(payload, 0, frame, 2, payloadLength);
        } else {
            frame = new byte[4 + payloadLength];
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = 126; // Extended payload length
            frame[2] = (byte) ((payloadLength >> 8) & 0xFF);
            frame[3] = (byte) (payloadLength & 0xFF);
            System.arraycopy(payload, 0, frame, 4, payloadLength);
        }

        output.write(frame);
        output.flush();
    }

    private void sendPongFrame(OutputStream output) throws IOException {
        byte[] pongFrame = new byte[] {(byte) 0x8A, 0x00}; // FIN + pong frame, no payload
        output.write(pongFrame);
        output.flush();
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                LOGGER.info("WebSocket server stopped");
            }
        } catch (IOException e) {
            LOGGER.error("Error closing WebSocket server socket", e);
        }
    }

    public void close() {
        closeServerSocket();
    }
}
