package org.jabref.gui.util;

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

public class FileFilterConverter {

    public static FileChooser.ExtensionFilter ANY_FILE = new FileChooser.ExtensionFilter(Localization.lang("Any file"), "*.*");

    private FileFilterConverter() {
    }

    static FileChooser.ExtensionFilter toExtensionFilter(FileType fileType) {
        String description = Localization.lang("%0 file", fileType.toString());
        return new FileChooser.ExtensionFilter(description, fileType.getExtensionsWithDot());
    }

    static FileChooser.ExtensionFilter toExtensionFilter(String description, FileType fileType) {
        return new FileChooser.ExtensionFilter(description, fileType.getExtensionsWithDot());
    }

    public static Optional<Importer> getImporter(FileChooser.ExtensionFilter extensionFilter, Collection<Importer> importers) {
        return importers.stream().filter(importer -> importer.getName().equals(extensionFilter.getDescription())).findFirst();
    }

    public static Optional<Exporter> getExporter(FileChooser.ExtensionFilter extensionFilter, Collection<Exporter> exporters) {
        return exporters.stream().filter(exporter -> exporter.getName().equals(extensionFilter.getDescription())).findFirst();
    }

    public static FileChooser.ExtensionFilter forAllImporters(SortedSet<Importer> importers) {
        List<FileType> fileTypes = importers.stream().map(Importer::getFileType).collect(Collectors.toList());
        List<String> flatExtensions = fileTypes.stream()
                                               .flatMap(type -> type.getExtensionsWithDot().stream())
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
}
