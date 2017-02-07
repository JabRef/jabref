package net.sf.jabref.gui.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import net.sf.jabref.logic.util.FileExtensions;

public class FileDialogConfiguration {

    private final List<FileChooser.ExtensionFilter> extensionFilters;
    private final Path initialDirectory;

    public Optional<Path> getInitialDirectory() {
        return Optional.ofNullable(initialDirectory);
    }

    public FileChooser.ExtensionFilter getDefaultExtension() {
        return defaultExtension;
    }

    private FileChooser.ExtensionFilter defaultExtension;

    private FileDialogConfiguration(Path initialDirectory, List<FileChooser.ExtensionFilter> extensionFilters, FileChooser.ExtensionFilter defaultExtension) {
        this.initialDirectory = initialDirectory;
        this.extensionFilters = Objects.requireNonNull(extensionFilters);
        this.defaultExtension = defaultExtension;
    }

    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    public static class Builder {
        List<FileChooser.ExtensionFilter> extensionFilter = new ArrayList<>();
        private Path initialDirectory;
        private FileChooser.ExtensionFilter defaultExtension;

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
            return new FileDialogConfiguration(initialDirectory, extensionFilter, defaultExtension);
        }

        public Builder withInitialDirectory(Path directory) {
            initialDirectory = directory;
            return this;
        }

        public Builder withDefaultExtension(FileExtensions extension) {
            defaultExtension = toFilter(extension);
            return this;
        }
    }
}
