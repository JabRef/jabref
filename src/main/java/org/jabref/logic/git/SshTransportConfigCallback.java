package org.jabref.logic.git;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.util.FS;

public class SshTransportConfigCallback implements TransportConfigCallback {
    private Path sshDir = new File(FS.DETECTED.userHome(), "/.ssh").toPath();
    private SshdSessionFactory sshSessionFactory;

    public SshTransportConfigCallback(File sshDirectory) {
        this.sshSessionFactory = new CustomSshSessionFactory(sshDir);
    }

    @Override
    public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(this.sshSessionFactory);
    }
}
