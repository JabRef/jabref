package org.jabref.logic.git;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Guards the module-system wiring: jgit discovers its SSH backend via `ServiceLoader`, which silently yields nothing
/// (or a `ServiceConfigurationError`) if the synthesized module descriptors lack the `uses`/`provides` directives.
class SshSessionFactoryTest {

    @Test
    void sshBackendIsDiscoverable() {
        assertInstanceOf(SshdSessionFactory.class, SshSessionFactory.getInstance());
    }
}
