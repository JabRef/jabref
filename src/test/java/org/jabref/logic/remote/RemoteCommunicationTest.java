package org.jabref.logic.remote;

import java.io.IOException;

import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.support.DisabledOnCIServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the case where the client and server are set-up correctly. Testing the exceptional cases happens in {@link
 * RemoteSetupTest}.
 */
@DisabledOnCIServer("Tests fails sporadically on CI server")
class RemoteCommunicationTest {

    private RemoteClient client;
    private RemoteListenerServerManager serverLifeCycle;
    private RemoteMessageHandler server;

    @BeforeEach
    void setUp() {
        final int port = 34567;

        server = mock(RemoteMessageHandler.class);
        serverLifeCycle = new RemoteListenerServerManager();
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

    @Test
    void commandLineArgumentEncodingAndDecoding() {
        final String[] message = new String[]{"D:\\T EST\\测试te st.bib"};

        // will be encoded as "D%3A%5CT+EST%5C%E6%B5%8B%E8%AF%95te+st.bib"
        client.sendCommandLineArguments(message);

        verify(server).handleCommandLineArguments(message);
    }
}
