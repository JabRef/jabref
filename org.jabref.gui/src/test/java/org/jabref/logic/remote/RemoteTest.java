package org.jabref.logic.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.remote.client.RemoteListenerClient;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class RemoteTest {

    @Test
    public void testGoodCase() {
        final int port = 34567;
        final String message = "MYMESSAGE";


        try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
            assertFalse(server.isOpen());
            server.openAndStart(msg -> assertEquals(message, msg), port);
            assertTrue(server.isOpen());
            assertTrue(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
            server.stop();
            assertFalse(server.isOpen());
        }
    }

    @Test
    public void testGoodCaseWithAllLifecycleMethods() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.open(msg -> assertEquals(message, msg), port);
            assertTrue(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.start();
            assertTrue(server.isOpen());
            assertFalse(server.isNotStartedBefore());

            assertTrue(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
        }
    }

    @Test
    public void testPortAlreadyInUse() throws IOException {
        assumeFalse(OS.OS_X);

        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            assertTrue(socket.isBound());

            try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
                assertFalse(server.isOpen());
                server.openAndStart(msg -> fail("should not happen"), port);
                assertFalse(server.isOpen());
            } catch (Exception e) {
                fail("Exception: " + e.getMessage());
            }
        }
    }

    @Test
    public void testClientTimeout() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        assertFalse(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
    }

    @Test
    public void testClientConnectingToWrongServer() throws IOException, InterruptedException {
        final int port = 34567;
        final String message = "MYMESSAGE";

        try (ServerSocket socket = new ServerSocket(port)) {
            new Thread() {

                @Override
                public void run() {
                    try (Socket socket2 = socket.accept(); OutputStream os = socket2.getOutputStream()) {
                        os.write("whatever".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        // Ignored
                    }
                }
            }.start();
            Thread.sleep(100);
            assertFalse(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
        }
    }
}
