package org.jabref.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.stage.FileChooser;

import org.jabref.logic.util.FileType;

public class FileDialogConfiguration {

    private final List<FileChooser.ExtensionFilter> extensionFilters;
    private final Path initialDirectory;
    private final FileChooser.ExtensionFilter defaultExtension;
    private final String initialFileName;
    private FileChooser.ExtensionFilter selectedExtensionFilter;

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

    public FileChooser.ExtensionFilter getSelectedExtensionFilter() {
        return selectedExtensionFilter;
    }

    public void setSelectedExtensionFilter(FileChooser.ExtensionFilter selectedExtensionFilter) {
        this.selectedExtensionFilter = selectedExtensionFilter;
    }

    public static class Builder {

        private final List<FileChooser.ExtensionFilter> extensionFilters = new ArrayList<>();
        private Path initialDirectory;
        private FileChooser.ExtensionFilter defaultExtension;
        private String initialFileName;

        public FileDialogConfiguration build() {
            return new FileDialogConfiguration(initialDirectory, extensionFilters, defaultExtension, initialFileName);
        }

        public Builder withInitialDirectory(Path directory) {
            if (directory == null) { // It could be that somehow the path is null, for example if it got deleted in the meantime
                initialDirectory = null;
            } else { // Dir must be a folder, not a file
                if (!Files.isDirectory(directory)) {
                    directory = directory.getParent();
                }
                // The lines above work also if the dir does not exist at all!
                // NULL is accepted by the filechooser as no inital path
                // Explicit null check, if somehow the parent is null, as Files.exists throws an NPE otherwise
                if ((directory != null) && !Files.exists(directory)) {
                    directory = null;
                }
                initialDirectory = directory;
            }
            return this;
        }

        public Builder withInitialDirectory(String directory) {
            if (directory != null) {
                withInitialDirectory(Path.of(directory));
            } else {
                initialDirectory = null;
            }
            return this;
        }

        public Builder withInitialFileName(String initialFileName) {
            this.initialFileName = initialFileName;
            return this;
        }

        public Builder withDefaultExtension(FileChooser.ExtensionFilter extensionFilter) {
            defaultExtension = extensionFilter;
            return this;
        }

        public Builder withDefaultExtension(FileType fileType) {
            defaultExtension = FileFilterConverter.toExtensionFilter(fileType);
            return this;
        }

        public Builder withDefaultExtension(String description, FileType fileType) {
            defaultExtension = FileFilterConverter.toExtensionFilter(description, fileType);
            return this;
        }

        public Builder withDefaultExtension(String fileTypeDescription) {
            extensionFilters.stream()
                            .filter(type -> type.getDescription().equalsIgnoreCase(fileTypeDescription))
                            .findFirst()
                            .ifPresent(extensionFilter -> defaultExtension = extensionFilter);

            return this;
        }

        public Builder addExtensionFilter(FileChooser.ExtensionFilter filter) {
            extensionFilters.add(filter);
            return this;
        }

        public Builder addExtensionFilter(List<FileChooser.ExtensionFilter> filters) {
            extensionFilters.addAll(filters);
            return this;
        }

        public Builder addExtensionFilter(FileType... fileTypes) {
            Stream.of(fileTypes)
                  .map(FileFilterConverter::toExtensionFilter)
                  .forEachOrdered(this::addExtensionFilter);
            return this;
        }

        public Builder addExtensionFilter(String description, FileType fileType) {
            extensionFilters.add(FileFilterConverter.toExtensionFilter(description, fileType));
            return this;
        }
    }
}
