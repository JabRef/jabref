package org.jabref.logic.remote.server;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebSocketListenerServerTest {

    private WebSocketListenerServerThread serverThread;
    private final int testPort = 34567;

    @BeforeEach
    void setUp() throws Exception {
        RemoteMessageHandler mockHandler = mock(RemoteMessageHandler.class);
        serverThread = new WebSocketListenerServerThread(mockHandler, testPort);
        serverThread.start();
        Thread.sleep(100); // Give server time to start
    }

    @AfterEach
    void tearDown() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Test
    void testWebSocketHandshake() throws Exception {
        try (Socket client = new Socket("localhost", testPort)) {
            OutputStream output = client.getOutputStream();

            // Send WebSocket handshake
            String handshake = "GET / HTTP/1.1\r\n" +
                    "Host: localhost:" + testPort + "\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                    "Sec-WebSocket-Version: 13\r\n" +
                    "\r\n";

            output.write(handshake.getBytes(StandardCharsets.UTF_8));
            output.flush();

            // Read response
            byte[] response = new byte[1024];
            int bytesRead = client.getInputStream().read(response);
            String responseStr = new String(response, 0, bytesRead, StandardCharsets.UTF_8);

            assertTrue(responseStr.contains("101 Switching Protocols"));
            assertTrue(responseStr.contains("Sec-WebSocket-Accept:"));
        }
    }

    @Test
    void testPingCommand() throws Exception {
        try (Socket client = new Socket("localhost", testPort)) {
            performHandshake(client);

            // Send ping command
            String message = "{\"command\":\"ping\"}";
            sendTextFrame(client.getOutputStream(), message);

            // Read response
            byte[] response = new byte[1024];
            int bytesRead = client.getInputStream().read(response);

            assertTrue(bytesRead > 0);
            String responseMessage = extractTextFromFrame(response, bytesRead);
            assertTrue(responseMessage.contains("pong"));
        }
    }

    @Test
    void testAddCommand() throws Exception {
        try (Socket client = new Socket("localhost", testPort)) {
            performHandshake(client);

            // Send add command with BibTeX entry
            String bibEntry = "@article{test2024,author={Test},title={Test Article},year={2024}}";
            String message = "{\"command\":\"add\",\"argument\":\"" + bibEntry + "\"}";
            sendTextFrame(client.getOutputStream(), message);

            // Read response
            byte[] response = new byte[1024];
            int bytesRead = client.getInputStream().read(response);

            assertTrue(bytesRead > 0);
            String responseMessage = extractTextFromFrame(response, bytesRead);
            assertTrue(responseMessage.contains("success") || responseMessage.contains("added"));
        }
    }

    private void performHandshake(Socket client) throws Exception {
        OutputStream output = client.getOutputStream();

        String handshake = "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + testPort + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "\r\n";

        output.write(handshake.getBytes(StandardCharsets.UTF_8));
        output.flush();

        // Read and discard handshake response
        byte[] response = new byte[1024];
        client.getInputStream().read(response);
    }

    private void sendTextFrame(OutputStream output, String message) throws Exception {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int payloadLength = payload.length;

        if (payloadLength <= 125) {
            byte[] frame = new byte[6 + payloadLength];
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) (0x80 | payloadLength); // Masked + length

            // Masking key (all zeros for simplicity in tests)
            frame[2] = 0;
            frame[3] = 0;
            frame[4] = 0;
            frame[5] = 0;

            // Payload (no actual masking since key is all zeros)
            System.arraycopy(payload, 0, frame, 6, payloadLength);
            output.write(frame);
        } else {
            byte[] frame = new byte[8 + payloadLength];
            frame[0] = (byte) 0x81;
            frame[1] = (byte) (0x80 | 126);
            frame[2] = (byte) ((payloadLength >> 8) & 0xFF);
            frame[3] = (byte) (payloadLength & 0xFF);
            frame[4] = 0;
            frame[5] = 0;
            frame[6] = 0;
            frame[7] = 0;
            System.arraycopy(payload, 0, frame, 8, payloadLength);
            output.write(frame);
        }
        output.flush();
    }

    private String extractTextFromFrame(byte[] frame, int length) {
        if (length < 2) {
            return "";
        }

        int payloadLength = frame[1] & 0x7F;
        int payloadStart = 2;

        if (payloadLength == 126) {
            payloadLength = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            payloadStart = 4;
        }

        byte[] payload = new byte[payloadLength];
        System.arraycopy(frame, payloadStart, payload, 0, payloadLength);
        return new String(payload, StandardCharsets.UTF_8);
    }
}
