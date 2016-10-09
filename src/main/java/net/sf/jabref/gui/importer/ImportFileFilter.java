package net.sf.jabref.gui.importer;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.util.FileExtensions;

class ImportFileFilter extends FileFilter implements Comparable<ImportFileFilter> {
    private final String description;
    private final FileNameExtensionFilter fileFilter;

    public ImportFileFilter(Importer format) {
        FileExtensions extensions = format.getExtensions();
        this.description = extensions.getDescription();
        fileFilter = new FileNameExtensionFilter(extensions.getDescription(), extensions.getExtensions());
    }

    public ImportFileFilter(String description, Collection<Importer> formats) {
        this.description = description;

        List<FileExtensions> extensions = formats.stream().map(p -> p.getExtensions()).collect(Collectors.toList());
        List<String> flatExtensions = extensions.stream().flatMap(extList -> Stream.of(extList.getExtensions())).collect(Collectors.toList());
        fileFilter = new FileNameExtensionFilter(description, flatExtensions.toArray(new String[flatExtensions.size()]));
    }

    @Override
    public boolean accept(File file) {
        return (file != null) && (file.isDirectory() || fileFilter.accept(file));
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(ImportFileFilter o) {
        return description.compareTo(o.description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ImportFileFilter) {
            return description.equals(((ImportFileFilter) o).description);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }

}
