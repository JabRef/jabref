package org.jabref.logic.git;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitSshTest {

    @Test
    void shutdownClosesSshdSessionFactory() {
        SshSessionFactory original = SshSessionFactory.getInstance();
        AtomicInteger closeCalls = new AtomicInteger();
        // A private instance, so the process-wide factory used by other Git tests stays open
        SshSessionFactory.setInstance(new SshdSessionFactory() {
            @Override
            public void close() {
                closeCalls.incrementAndGet();
            }
        });
        try {
            GitSsh.shutdown();
        } finally {
            SshSessionFactory.setInstance(original);
        }
        assertEquals(1, closeCalls.get());
    }
}
