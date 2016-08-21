package net.sf.jabref.gui.exporter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.sf.jabref.logic.exporter.IExportFormat;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the ExportFormat in question.
 */
public class ExportFileFilter extends FileFilter implements Comparable<ExportFileFilter> {

    private final IExportFormat format;
    private final String extension;
    private final String name;


    public ExportFileFilter(IExportFormat format) {
        this.format = format;
        this.extension = format.getExtension();
        this.name = format.getDisplayName() + " (*" + extension
                + ')';
    }

    public IExportFormat getExportFormat() {
        return format;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            return file.getPath().toLowerCase().endsWith(extension);
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
