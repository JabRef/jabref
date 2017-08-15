package org.jabref.gui.importer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.util.FileExtensions;

class ImportFileFilter {

    private ImportFileFilter() {
    }

    public static FileChooser.ExtensionFilter convert(Importer format) {
        return new FileChooser.ExtensionFilter(format.getExtensions().getDescription(),
                format.getExtensions().getExtensions());
    }

    public static FileChooser.ExtensionFilter convert(String description, Collection<Importer> formats) {
        List<FileExtensions> extensions = formats.stream().map(Importer::getExtensions).collect(Collectors.toList());
        List<String> flatExtensions = extensions.stream().flatMap(extList -> Arrays.stream(extList.getExtensions()))
                .collect(Collectors.toList());
        return new FileChooser.ExtensionFilter(description, flatExtensions.toArray(new String[flatExtensions.size()]));
    }

    public static Optional<Importer> convert(FileChooser.ExtensionFilter extensionFilter,
            Collection<Importer> formats) {
        return formats.stream().filter(format -> format.getDescription().equals(extensionFilter.getDescription()))
                .findFirst();

    }

}
