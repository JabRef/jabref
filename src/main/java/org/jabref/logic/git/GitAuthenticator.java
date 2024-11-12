package org.jabref.logic.git;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.IdentityPasswordProvider;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GitAuthenticator {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitAuthenticator.class);
    // TODO: temp (get from preferences)
    private static String userName = Optional.ofNullable(System.getenv("GIT_USERNAME")).orElse("");
    // TODO: temp
    private static String password = Optional.ofNullable(System.getenv("GIT_PASSWORD")).orElse("");
    // TODO: temp
    private static String sshPassPhrase = Optional.ofNullable(System.getenv("GIT_SSH_PASSPHRASE")).orElse("");
    // TODO: temp
    private static Path homeDirectory = Path.of(Optional.of(System.getProperty("user.home")).orElse(""));
    // TODO: temp
    private static Path sshDirectory = homeDirectory.resolve(".ssh");

    static <Command extends TransportCommand<Command, ?>> void authenticate(Command transportCommand) {
        transportCommand.setCredentialsProvider(getCredentialsProvider());
        transportCommand.setTransportConfigCallback(GitAuthenticator::transportConfigCallback);
    }

    private static CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(userName, password);
    }

    private static void transportConfigCallback(Transport transport) {
        if (!(transport instanceof SshTransport sshTransport)) {
            LOGGER.debug("git repository does not use a SSH protocol");
            return;
        }
        SshSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey")
                .setHomeDirectory(homeDirectory.toFile())
                .setSshDirectory(sshDirectory.toFile())
                .setKeyPasswordProvider(cp -> new IdentityPasswordProvider(cp) {
                    @Override
                    protected char[] getPassword(URIish uri, String message) {
                        return sshPassPhrase.toCharArray();
                    }
                }).build(null);
        sshTransport.setSshSessionFactory(sshSessionFactory);
    }

    static void setUserName(String userName) {
        GitAuthenticator.userName = userName;
    }

    static void setPassword(String password) {
        GitAuthenticator.password = password;
    }

    static void setSshPassPhrase(String sshPassPhrase) {
        GitAuthenticator.sshPassPhrase = sshPassPhrase;
    }

    static void setHomeDirectory(Path homeDirectory) {
        GitAuthenticator.homeDirectory = homeDirectory;
    }

    static void setSshDirectory(Path sshDirectory) {
        GitAuthenticator.sshDirectory = sshDirectory;
    }
}
