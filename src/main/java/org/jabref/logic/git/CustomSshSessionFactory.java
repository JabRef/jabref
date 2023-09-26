package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

public final class CustomSshSessionFactory extends SshdSessionFactory {
    private Path sshDir;

    public CustomSshSessionFactory(Path sshDir) {
        this.sshDir = sshDir;
    }

    @Override
    public File getSshDirectory() {
        try {
            return Files.createDirectories(sshDir).toFile();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
