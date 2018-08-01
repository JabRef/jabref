package org.jabref.logic.remote;

import java.io.IOException;

import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.remote.server.MessageHandler;
import org.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import org.jabref.support.DisabledOnCIServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the case where the client and server are set-up correctly.
 * Testing the exceptional cases happens in {@link RemoteSetupTest}.
 */
@DisabledOnCIServer("Tests fails sporadically on CI server")
class RemoteCommunicationTest {

    private RemoteClient client;
    private RemoteListenerServerLifecycle serverLifeCycle;
    private MessageHandler server;

    @BeforeEach
    void setUp() {
        final int port = 34567;

        server = mock(MessageHandler.class);
        serverLifeCycle = new RemoteListenerServerLifecycle();
        serverLifeCycle.openAndStart(server, port);

        client = new RemoteClient(port);
    }

    @AfterEach
    void tearDown() {
        serverLifeCycle.close();
    }

    @Test
    void pingReturnsTrue() throws IOException, InterruptedException {
        assertTrue(client.ping());
    }

    @Test
    void commandLineArgumentSinglePassedToServer() {
        final String[] message = new String[]{"my message"};

        client.sendCommandLineArguments(message);

        verify(server).handleCommandLineArguments(message);
    }

    @Test
    void commandLineArgumentTwoPassedToServer() {
        final String[] message = new String[]{"my message", "second"};

        client.sendCommandLineArguments(message);

        verify(server).handleCommandLineArguments(message);
    }

    @Test
    void commandLineArgumentMultiLinePassedToServer() {
        final String[] message = new String[]{"my message\n second line", "second \r and third"};

        client.sendCommandLineArguments(message);

        verify(server).handleCommandLineArguments(message);
    }
}
