package net.sf.jabref.logic.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import net.sf.jabref.logic.remote.client.RemoteListenerClient;
import net.sf.jabref.logic.remote.server.RemoteListenerServerLifecycle;

import org.junit.Assert;
import org.junit.Test;

public class RemoteTest {

    @Test
    public void testGoodCase() {
        final int port = 34567;
        final String message = "MYMESSAGE";


        try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
            Assert.assertFalse(server.isOpen());
            server.openAndStart(msg -> Assert.assertEquals(message, msg), port);
            Assert.assertTrue(server.isOpen());
            Assert.assertTrue(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
            server.stop();
            Assert.assertFalse(server.isOpen());
        }

    }

    @Test
    public void testGoodCaseWithAllLifecycleMethods() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
            Assert.assertFalse(server.isOpen());
            Assert.assertTrue(server.isNotStartedBefore());
            server.stop();
            Assert.assertFalse(server.isOpen());
            Assert.assertTrue(server.isNotStartedBefore());
            server.open(msg -> Assert.assertEquals(message, msg), port);
            Assert.assertTrue(server.isOpen());
            Assert.assertTrue(server.isNotStartedBefore());
            server.start();
            Assert.assertTrue(server.isOpen());
            Assert.assertFalse(server.isNotStartedBefore());

            Assert.assertTrue(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
            server.stop();
            Assert.assertFalse(server.isOpen());
            Assert.assertTrue(server.isNotStartedBefore());
        }
    }

    @Test
    public void testPortAlreadyInUse() throws IOException {
        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            Assert.assertTrue(socket.isBound());

            try (RemoteListenerServerLifecycle server = new RemoteListenerServerLifecycle()) {
                Assert.assertFalse(server.isOpen());
                server.openAndStart(msg -> Assert.fail("should not happen"), port);
                Assert.assertFalse(server.isOpen());
            } catch (Exception e) {
                Assert.fail("Exception: " + e.getMessage());
            }
        }
    }

    @Test
    public void testClientTimeout() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        Assert.assertFalse(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
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
                        os.write("whatever".getBytes("UTF8"));
                    } catch (IOException e) {
                        // Ignored
                    }
                }
            }.start();
            Thread.sleep(100);
            Assert.assertFalse(RemoteListenerClient.sendToActiveJabRefInstance(new String[]{message}, port));
        }
    }

}
