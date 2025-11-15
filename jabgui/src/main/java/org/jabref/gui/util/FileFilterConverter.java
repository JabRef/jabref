package org.jabref.gui.util;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;

public class FileFilterConverter {

    public static FileChooser.ExtensionFilter ANY_FILE = new FileChooser.ExtensionFilter(Localization.lang("Any file"), "*.*");

    private FileFilterConverter() {
    }

    public static FileChooser.ExtensionFilter toExtensionFilter(FileType fileType) {
        String fileList = String.join(", ", fileType.getExtensionsWithAsteriskAndDot());
        String description = Localization.lang("%0 file (%1)", fileType.getName(), fileList);
        return new FileChooser.ExtensionFilter(description, fileType.getExtensionsWithAsteriskAndDot());
    }

    public static FileChooser.ExtensionFilter toExtensionFilter(String description, FileType fileType) {
        return new FileChooser.ExtensionFilter(description, fileType.getExtensionsWithAsteriskAndDot());
    }

    /**
     * Determines the appropriate file extension filter based on the given file.
     * If the file is recognized as a BibTeX file, it returns a BibTeX-specific extension filter.
     * Otherwise, it returns a generic filter.
     *
     * @param file The file to check.
     * @return The corresponding Extension Filter for the file type.
     */
    public static FileChooser.ExtensionFilter determineExtensionFilter(Path file) {
        if (FileUtil.isBibFile(file)) {
            return toExtensionFilter("BibTeX", StandardFileType.BIBTEX_DB);
        }
        return FileFilterConverter.ANY_FILE;
    }

    public static Optional<Importer> getImporter(FileChooser.ExtensionFilter extensionFilter, Collection<Importer> importers) {
        return importers.stream().filter(importer -> importer.getName().equals(extensionFilter.getDescription())).findFirst();
    }

    public static Optional<Exporter> getExporter(FileChooser.ExtensionFilter extensionFilter, Collection<Exporter> exporters) {
        return exporters.stream().filter(exporter -> exporter.getName().equals(extensionFilter.getDescription())).findFirst();
    }

    public static FileChooser.ExtensionFilter forAllImporters(SortedSet<Importer> importers) {
        List<FileType> fileTypes = importers.stream().map(Importer::getFileType).toList();
        List<String> flatExtensions = fileTypes.stream()
                                               .flatMap(type -> type.getExtensionsWithAsteriskAndDot().stream())
                                               .collect(Collectors.toList());

        return new FileChooser.ExtensionFilter(Localization.lang("Available import formats"), flatExtensions);
    }

    public static List<FileChooser.ExtensionFilter> importerToExtensionFilter(Collection<Importer> importers) {
        return importers.stream()
                        .map(importer -> toExtensionFilter(importer.getName(), importer.getFileType()))
                        .collect(Collectors.toList());
    }

    public static List<FileChooser.ExtensionFilter> exporterToExtensionFilter(Collection<Exporter> exporters) {
        return exporters.stream()
                        .map(exporter -> toExtensionFilter(exporter.getName(), exporter.getFileType()))
                        .collect(Collectors.toList());
    }

    public static FileFilter toFileFilter(FileChooser.ExtensionFilter extensionFilter) {
        return toFileFilter(extensionFilter.getExtensions());
    }

    public static FileFilter toFileFilter(List<String> extensions) {
        Filter<Path> filter = toDirFilter(extensions);
        return file -> {
            try {
                return filter.accept(file.toPath());
            } catch (IOException e) {
                return false;
            }
        };
    }

    public static Filter<Path> toDirFilter(List<String> extensions) {
        List<String> extensionsCleaned = extensions.stream()
                                                   .map(extension -> extension.replace(".", "").replace("*", ""))
                                                   .filter(StringUtil::isNotBlank)
                                                   .toList();
        if (extensionsCleaned.isEmpty()) {
            // Except every file
            return _ -> true;
        } else {
            return path -> FileUtil.getFileExtension(path)
                                   .map(extensionsCleaned::contains)
                                   .orElse(false);
        }
    }
}
