package org.jabref.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DirectoryDialogConfiguration {

    private final Path initialDirectory;

    private DirectoryDialogConfiguration(Path initialDirectory) {
        this.initialDirectory = initialDirectory;
    }

    public Optional<Path> getInitialDirectory() {
        return Optional.ofNullable(initialDirectory);
    }

    public static class Builder {

        private Path initialDirectory;

        public DirectoryDialogConfiguration build() {
            return new DirectoryDialogConfiguration(initialDirectory);
        }

        public Builder withInitialDirectory(Path directory) {

            directory = directory.toAbsolutePath();
            // Dir must be a folder, not a file
            if (!Files.isDirectory(directory)) {
                directory = directory.getParent();
            }
            // The lines above work also if the dir does not exist at all!
            // NULL is accepted by the filechooser as no inital path

            if (!Files.exists(directory)) {

                directory = null;
            }
            initialDirectory = directory;
            return this;
        }

        public Builder withInitialDirectory(String directory) {
            withInitialDirectory(Path.of(directory));
            return this;
        }
    }
}
