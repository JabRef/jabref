package org.jabref.gui.importer;

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

    private FileFilterConverter() {
    }

    private static FileChooser.ExtensionFilter convertImporter(String description, Collection<Importer> formats) {
        List<FileType> fileTypes = formats.stream().map(Importer::getFileType).collect(Collectors.toList());
        List<String> flatExtensions = fileTypes.stream().flatMap(extList -> extList.getExtensions().stream())
                .map(ending -> "*." + ending)
                .collect(Collectors.toList());

        return new FileChooser.ExtensionFilter(description, flatExtensions.toArray(new String[flatExtensions.size()]));
    }

    public static Optional<Importer> getImporter(FileChooser.ExtensionFilter extensionFilter, Collection<Importer> importers) {
        return importers.stream().filter(importer -> importer.getDescription().equals(extensionFilter.getDescription())).findFirst();
    }

    public static Optional<Exporter> getExporter(FileChooser.ExtensionFilter extensionFilter, Collection<Exporter> exporters) {
        return exporters.stream().filter(exporter -> exporter.getDescription().equals(extensionFilter.getDescription())).findFirst();
    }

    public static FileChooser.ExtensionFilter forAllImporters(SortedSet<Importer> importers) {
        return convertImporter(Localization.lang("Available import formats"), importers);
    }
}
