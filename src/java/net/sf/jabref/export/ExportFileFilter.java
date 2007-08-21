package net.sf.jabref.export;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the ExportFormat in question.
 */
public class ExportFileFilter extends FileFilter implements Comparable<ExportFileFilter> {
    private IExportFormat format;
    private String extension, name;

    public ExportFileFilter(IExportFormat format, String extension) {
		this.format = format;
		this.extension = extension;
		this.name = format.getDisplayName() + " (*" + extension
				+ ")";
	}

    public IExportFormat getExportFormat() {
        return format;
    }
    
    public String getExtension(){
    	return extension;
    }

    public boolean accept(File file) {
        if (file.isDirectory())
            return true;
        else
            return file.getPath().toLowerCase().endsWith(extension);
    }

    public String getDescription() {
        return name;
    }

    public int compareTo(ExportFileFilter o) {
        return name.compareTo(o.name);
    }
}
