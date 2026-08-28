package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SshAgentConnectorFactoryTest {

    private static class FakeConnector implements Connector {
        boolean closed;
        private final boolean connects;
        private final boolean throwsOnConnect;

        FakeConnector(boolean connects, boolean throwsOnConnect) {
            this.connects = connects;
            this.throwsOnConnect = throwsOnConnect;
        }

        @Override
        public boolean connect() throws IOException {
            if (throwsOnConnect) {
                throw new IOException("agent missing");
            }
            return connects;
        }

        @Override
        public byte[] rpc(byte command, byte[] message) {
            return new byte[] {command};
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class RecordingFactory implements ConnectorFactory {
        final List<String> requestedAgents = new ArrayList<>();
        final FakeConnector pageant;
        final FakeConnector pipe;

        RecordingFactory(FakeConnector pageant, FakeConnector pipe) {
            this.pageant = pageant;
            this.pipe = pipe;
        }

        @Override
        public Connector create(String identityAgent, File homeDir) {
            requestedAgents.add(identityAgent == null ? "default" : identityAgent);
            return identityAgent == null ? pageant : pipe;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String getName() {
            return "fake";
        }

        @Override
        public Collection<ConnectorDescriptor> getSupportedConnectors() {
            return List.of();
        }

        @Override
        public ConnectorDescriptor getDefaultConnector() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void primaryAgentIsUsedWhenReachable() throws IOException {
        FakeConnector pageant = new FakeConnector(true, false);
        FakeConnector pipe = new FakeConnector(true, false);
        RecordingFactory jgit = new RecordingFactory(pageant, pipe);

        Connector connector = new SshAgentConnectorFactory(jgit).create(null, new File("."));

        assertTrue(connector.connect());
        assertEquals(List.of("default"), jgit.requestedAgents);
        assertFalse(pageant.closed);
    }

    @Test
    void fallsBackToOpenSshPipeWhenPrimaryRefuses() throws IOException {
        FakeConnector pageant = new FakeConnector(false, false);
        FakeConnector pipe = new FakeConnector(true, false);
        RecordingFactory jgit = new RecordingFactory(pageant, pipe);

        Connector connector = new SshAgentConnectorFactory(jgit).create(null, new File("."));

        assertTrue(connector.connect());
        assertEquals(List.of("default", SshAgentConnectorFactory.OPENSSH_AGENT_PIPE), jgit.requestedAgents);
        assertTrue(pageant.closed);
        assertEquals(1, connector.rpc((byte) 1, new byte[0])[0]);
    }

    @Test
    void fallsBackWhenPrimaryThrows() throws IOException {
        FakeConnector pageant = new FakeConnector(false, true);
        FakeConnector pipe = new FakeConnector(true, false);
        RecordingFactory jgit = new RecordingFactory(pageant, pipe);

        Connector connector = new SshAgentConnectorFactory(jgit).create(null, new File("."));

        assertTrue(connector.connect());
        assertTrue(pageant.closed);
    }

    @Test
    void reportsNoAgentWhenBothUnavailable() throws IOException {
        RecordingFactory jgit = new RecordingFactory(new FakeConnector(false, false), new FakeConnector(false, false));

        assertFalse(new SshAgentConnectorFactory(jgit).create(null, new File(".")).connect());
    }

    @Test
    void explicitIdentityAgentIsPassedThrough() throws IOException {
        FakeConnector pageant = new FakeConnector(true, false);
        FakeConnector pipe = new FakeConnector(true, false);
        RecordingFactory jgit = new RecordingFactory(pageant, pipe);

        new SshAgentConnectorFactory(jgit).create("pageant", new File("."));

        assertEquals(List.of("pageant"), jgit.requestedAgents);
    }
}
