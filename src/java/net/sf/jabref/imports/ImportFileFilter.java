package net.sf.jabref.imports;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the ExportFormat in question.
 */
public class ImportFileFilter extends FileFilter implements Comparable<ImportFileFilter> {
    private ImportFormat format;
    private String name;

    public ImportFileFilter(ImportFormat format) {
        this.format = format;
        this.name = format.getFormatName();
    }

    public ImportFormat getImportFormat() {
        return format;
    }

    public boolean accept(File file) {
        return true;
        /*if (file.isDirectory())
            return true;
        else
            return file.getPath().toLowerCase().endsWith(extension);*/
    }

    public String getDescription() {
        return name;
    }

    public int compareTo(ImportFileFilter o) {
        return name.compareTo(o.name);
    }
}