package org.jabref.logic.tele;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.tele.client.TeleClient;
import org.jabref.logic.tele.server.TeleMessageHandler;
import org.jabref.logic.tele.server.TeleServerManager;
import org.jabref.logic.util.OS;
import org.jabref.support.DisabledOnCIServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisabledOnCIServer("Tests fails sporadically on CI server")
class TeleSetupTest {

    private TeleMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = mock(TeleMessageHandler.class);
    }

    @Test
    void testGoodCase() {
        final int port = 34567;
        final String[] message = new String[]{"MYMESSAGE"};

        try (TeleServerManager server = new TeleServerManager()) {
            assertFalse(server.isOpen());
            server.openAndStart(messageHandler, port);
            assertTrue(server.isOpen());
            assertTrue(new TeleClient(port).sendCommandLineArguments(message));
            verify(messageHandler).handleCommandLineArguments(message);
            server.stop();
            assertFalse(server.isOpen());
        }
    }

    @Test
    void testGoodCaseWithAllLifecycleMethods() {
        final int port = 34567;
        final String[] message = new String[]{"MYMESSAGE"};

        try (TeleServerManager server = new TeleServerManager()) {
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.open(messageHandler, port);
            assertTrue(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.start();
            assertTrue(server.isOpen());
            assertFalse(server.isNotStartedBefore());

            assertTrue(new TeleClient(port).sendCommandLineArguments(message));
            verify(messageHandler).handleCommandLineArguments(message);
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
        }
    }

    @Test
    void testPortAlreadyInUse() throws IOException {
        assumeFalse(OS.OS_X);

        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            assertTrue(socket.isBound());

            try (TeleServerManager server = new TeleServerManager()) {
                assertFalse(server.isOpen());
                server.openAndStart(messageHandler, port);
                assertFalse(server.isOpen());
                verify(messageHandler, never()).handleCommandLineArguments(any());
            }
        }
    }

    @Test
    void testClientTimeout() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        assertFalse(new TeleClient(port).sendCommandLineArguments(new String[]{message}));
    }

    @Test
    void pingReturnsFalseForWrongServerListening() throws IOException, InterruptedException {
        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            // Setup dummy server always answering "whatever"
            new Thread(() -> {
                try (Socket message = socket.accept(); OutputStream os = message.getOutputStream()) {
                    os.write("whatever".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // Ignored
                }
            }).start();
            Thread.sleep(100);

            assertFalse(new TeleClient(port).ping());
        }
    }

    @Test
    void pingReturnsFalseForNoServerListening() throws IOException, InterruptedException {
        final int port = 34567;

        assertFalse(new TeleClient(port).ping());
    }

    @Test
    void pingReturnsTrueWhenServerIsRunning() {
        final int port = 34567;

        try (TeleServerManager server = new TeleServerManager()) {
            server.openAndStart(messageHandler, port);

            assertTrue(new TeleClient(port).ping());
        }
    }
}
