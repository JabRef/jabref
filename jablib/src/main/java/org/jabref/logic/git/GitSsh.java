package org.jabref.logic.git;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.jspecify.annotations.NullMarked;

/// Lifecycle of jgit's shared SSH backend. The connector side is installed lazily from
/// [GitHandler] (see [SshAgentConnectorFactory]); this closes the session-factory side at a
/// controlled point.
@NullMarked
public final class GitSsh {

    private GitSsh() {
    }

    /// Closes jgit's shared sshd session factory so mina-sshd's asynchronous channel group is torn
    /// down while the JVM is still running. Left to JVM shutdown instead, a Windows IOCP socket read
    /// can complete after the group's executor is already gone and fail on a background thread with
    /// `IllegalStateException: Executor has been shut down`. Call once, during application shutdown.
    public static void shutdown() {
        if (SshSessionFactory.getInstance() instanceof SshdSessionFactory factory) {
            factory.close();
        }
    }
}
