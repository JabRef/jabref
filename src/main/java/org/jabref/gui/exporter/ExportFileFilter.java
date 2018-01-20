package org.jabref.gui.exporter;

import java.io.File;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.util.FileType;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the TemplateExporter in question.
 */
public class ExportFileFilter extends FileFilter implements Comparable<ExportFileFilter> {

    private final Exporter format;
    private final FileType extension;
    private final String name;

    public ExportFileFilter(Exporter format) {
        this.format = format;
        this.extension = format.getFileType();
        this.name = format.getDisplayName() + " (*" + extension
                + ')';
    }

    public Exporter getExportFormat() {
        return format;
    }

    public FileType getExtension() {
        return extension;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            return file.getPath().toLowerCase(Locale.ROOT).endsWith(extension.getExtensions().toString());
        }
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public int compareTo(ExportFileFilter o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ExportFileFilter) {
            return name.equals(((ExportFileFilter) o).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
