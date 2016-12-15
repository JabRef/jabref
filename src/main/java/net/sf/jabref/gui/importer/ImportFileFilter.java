package net.sf.jabref.gui.importer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.stage.FileChooser;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.util.FileExtensions;

class ImportFileFilter {

    public static FileChooser.ExtensionFilter convert(Importer format) {
        return new FileChooser.ExtensionFilter(format.getExtensions().getDescription(), format.getExtensions().getExtensions());
    }

    public static FileChooser.ExtensionFilter convert(String description, Collection<Importer> formats) {
        List<FileExtensions> extensions = formats.stream().map(Importer::getExtensions).collect(Collectors.toList());
        List<String> flatExtensions = extensions.stream().flatMap(extList -> Stream.of(extList.getExtensions())).collect(Collectors.toList());
        return new FileChooser.ExtensionFilter(description, flatExtensions.toArray(new String[flatExtensions.size()]));
    }
}
