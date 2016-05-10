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
package net.sf.jabref.importer;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.sf.jabref.importer.fileformat.ImportFormat;

/**
 * File filter that lets the user choose export format while choosing file to
 * export to. Contains a reference to the ExportFormat in question.
 */
class ImportFileFilter extends FileFilter implements Comparable<ImportFileFilter> {

    private final ImportFormat format;
    private final String name;


    public ImportFileFilter(ImportFormat format) {
        this.format = format;
        this.name = format.getFormatName();
    }

    public ImportFormat getImportFormat() {
        return format;
    }

    @Override
    public boolean accept(File file) {
        return true;
        /*if (file.isDirectory())
            return true;
        else
            return file.getPath().toLowerCase().endsWith(extension);*/
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public int compareTo(ImportFileFilter o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImportFileFilter) {
            return name.equals(((ImportFileFilter) o).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
