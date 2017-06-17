package org.jabref.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.util.FileExtensions;

public class FileDialogConfiguration {

    private final List<FileChooser.ExtensionFilter> extensionFilters;
    private final Path initialDirectory;
    private final FileChooser.ExtensionFilter defaultExtension;
    private final String initialFileName;

    private FileDialogConfiguration(Path initialDirectory, List<FileChooser.ExtensionFilter> extensionFilters,
            FileChooser.ExtensionFilter defaultExtension, String initialFileName) {
        this.initialDirectory = initialDirectory;
        this.extensionFilters = Objects.requireNonNull(extensionFilters);
        this.defaultExtension = defaultExtension;
        this.initialFileName = initialFileName;
    }

    public Optional<Path> getInitialDirectory() {
        return Optional.ofNullable(initialDirectory);
    }

    public FileChooser.ExtensionFilter getDefaultExtension() {
        return defaultExtension;
    }

    public String getInitialFileName() {
        return initialFileName;
    }

    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    public static class Builder {

        private final List<FileChooser.ExtensionFilter> extensionFilter = new ArrayList<>();
        private Path initialDirectory;
        private FileChooser.ExtensionFilter defaultExtension;
        private String initialFileName;

        public Builder addExtensionFilter(FileExtensions extension) {
            extensionFilter.add(toFilter(extension));
            return this;
        }

        private FileChooser.ExtensionFilter toFilter(FileExtensions extension) {
            return new FileChooser.ExtensionFilter(extension.getDescription(),
                    extension.getExtensionsAsList().stream().map(ending -> "*." + ending).collect(Collectors.toList()));
        }

        public Builder addExtensionFilters(Collection<FileExtensions> extensions) {
            extensions.forEach(this::addExtensionFilter);
            return this;
        }

        public FileDialogConfiguration build() {
            return new FileDialogConfiguration(initialDirectory, extensionFilter, defaultExtension, initialFileName);
        }

        public Builder withInitialDirectory(Path directory) {
            if (directory == null) { //It could be that somehow the path is null, for example if it got deleted in the meantime
                initialDirectory = null;
            } else { //Dir must be a folder, not a file
                if (!Files.isDirectory(directory)) {
                    directory = directory.getParent();
                }
                //The lines above work also if the dir does not exist at all!
                //NULL is accepted by the filechooser as no inital path
                if (!Files.exists(directory)) {
                    directory = null;
                }
                initialDirectory = directory;
            }
            return this;
        }

        public Builder withInitialDirectory(String directory) {
            if (directory != null) {
                withInitialDirectory(Paths.get(directory));
            } else {
                initialDirectory = null;
            }
            return this;
        }

        public Builder withDefaultExtension(FileExtensions extension) {
            defaultExtension = toFilter(extension);
            return this;
        }

        public Builder withInitialFileName(String initialFileName) {
            this.initialFileName = initialFileName;
            return this;

        }

    }
}
