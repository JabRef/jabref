package org.jabref.logic.git;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.jabref.logic.shared.security.Password;

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
    private static final Path HOME_DIRECTORY = Path.of(Optional.of(System.getProperty("user.home")).orElse(""));
    private final GitPreferences preferences;

    GitAuthenticator(GitPreferences preferences) {
        this.preferences = preferences;
    }

    <Command extends TransportCommand<Command, ?>> void authenticate(Command transportCommand) {
        transportCommand.setCredentialsProvider(getCredentialsProvider());
        transportCommand.setTransportConfigCallback(this::transportConfigCallback);
    }

    private CredentialsProvider getCredentialsProvider() {
        String password = preferences.getPassword();
        try {
            password = new Password(
                    preferences.getPassword().toCharArray(),
                    GitPreferences.getPasswordEncryptionKey().orElse(preferences.getUsername())
            ).decrypt();
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.warn("Error while decrypting git password", e);
        }
        return new UsernamePasswordCredentialsProvider(preferences.getUsername(), password);
    }

    private void transportConfigCallback(Transport transport) {
        if (!(transport instanceof SshTransport sshTransport)) {
            LOGGER.debug("git repository does not use a SSH protocol");
            return;
        }
        SshSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey")
                .setHomeDirectory(HOME_DIRECTORY.toFile())
                .setSshDirectory(Path.of(preferences.getSshDirPath()).toFile())
                .setKeyPasswordProvider(cp -> new IdentityPasswordProvider(cp) {
                    @Override
                    protected char[] getPassword(URIish uri, String message) {
                        return GitPreferences.getSshPassphrase().orElse("").toCharArray();
                    }
                }).build(null);
        sshTransport.setSshSessionFactory(sshSessionFactory);
    }
}
