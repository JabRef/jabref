/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.exporter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the ExportFormat in question.
 */
class ExportFileFilter extends FileFilter implements Comparable<ExportFileFilter> {

    private final IExportFormat format;
    private final String extension;
    private final String name;


    public ExportFileFilter(IExportFormat format, String extension) {
        this.format = format;
        this.extension = extension;
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
