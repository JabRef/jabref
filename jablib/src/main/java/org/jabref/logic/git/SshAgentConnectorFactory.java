package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.jabref.logic.util.strings.StringUtil;

import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;
import org.eclipse.jgit.util.SystemReader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// On Windows, jgit only talks to Pageant unless `IdentityAgent` is set in `~/.ssh/config`. This factory keeps that
/// default but falls back to the Windows OpenSSH agent's named pipe when Pageant is not running, so either agent
/// works without configuration. Explicitly configured agents are passed through untouched.
@NullMarked
public class SshAgentConnectorFactory implements ConnectorFactory {

    static final String OPENSSH_AGENT_PIPE = "\\\\.\\pipe\\openssh-ssh-agent";

    private static final Logger LOGGER = LoggerFactory.getLogger(SshAgentConnectorFactory.class);

    private final ConnectorFactory delegate;

    SshAgentConnectorFactory(ConnectorFactory delegate) {
        this.delegate = delegate;
    }

    /// Registers the fallback as jgit's default connector factory. No-op outside Windows, where jgit already honors
    /// `SSH_AUTH_SOCK`.
    public static void install() {
        ConnectorFactory jgitDefault = ConnectorFactory.getDefault();
        if (jgitDefault == null || !SystemReader.getInstance().isWindows()) {
            return;
        }
        if (!(jgitDefault instanceof SshAgentConnectorFactory)) {
            ConnectorFactory.setDefault(new SshAgentConnectorFactory(jgitDefault));
        }
    }

    @Override
    public Connector create(@Nullable String identityAgent, File homeDir) throws IOException {
        if (!StringUtil.isBlank(identityAgent)) {
            return delegate.create(identityAgent, homeDir);
        }
        return new FallbackConnector(delegate.create(null, homeDir), () -> delegate.create(OPENSSH_AGENT_PIPE, homeDir));
    }

    @Override
    public boolean isSupported() {
        return delegate.isSupported();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Collection<ConnectorDescriptor> getSupportedConnectors() {
        return delegate.getSupportedConnectors();
    }

    @Override
    public ConnectorDescriptor getDefaultConnector() {
        return delegate.getDefaultConnector();
    }

    @FunctionalInterface
    interface ConnectorSupplier {
        Connector get() throws IOException;
    }

    /// Connects to the primary agent; if that is unavailable, switches to the fallback for the rest of the session.
    static class FallbackConnector implements Connector {
        private final ConnectorSupplier fallback;
        private Connector active;

        FallbackConnector(Connector primary, ConnectorSupplier fallback) {
            this.active = primary;
            this.fallback = fallback;
        }

        @Override
        public boolean connect() throws IOException {
            try {
                if (active.connect()) {
                    return true;
                }
            } catch (IOException e) {
                LOGGER.debug("Primary SSH agent not reachable, trying fallback", e);
            }
            active.close();
            active = fallback.get();
            return active.connect();
        }

        @Override
        public byte[] rpc(byte command, byte[] message) throws IOException {
            return active.rpc(command, message);
        }

        @Override
        public void close() throws IOException {
            active.close();
        }
    }
}
