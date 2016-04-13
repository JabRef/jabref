/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Role of an importer for JabRef.
 */
public abstract class ImportFormat implements Comparable<ImportFormat> {

    /**
     * Using this when I have no database open or when I read
     * non bibtex file formats (used by the ImportFormatReader.java)
     *
     * TODO: Is this field really needed or would calling IdGenerator.next() suffice?
     */
    public static final String DEFAULT_BIBTEXENTRY_ID = "__ID";

    /**
     * Check whether the source is in the correct format for this importer.
     *
     * The effect of this method is primarily to avoid unnecessary processing of
     * files when searching for a suitable import format. If this method returns
     * false, the import routine will move on to the next import format.
     *
     * Thus the correct behaviour is to return false if it is certain that the file is
     * not of the suitable type, and true otherwise. Returning true is the safe choice if not certain.
     */
    public abstract boolean isRecognizedFormat(InputStream in) throws IOException;

    public boolean isRecognizedFormat(Path filePath) throws IOException {
        try (InputStream stream = new FileInputStream(filePath.toFile());
                BufferedInputStream bufferedStream = new BufferedInputStream(stream)) {

            bufferedStream.mark(Integer.MAX_VALUE);

            return isRecognizedFormat(bufferedStream);
        }
    }

    /**
     * Parse the database in the source.
     *
     * This method can be called in two different contexts - either when importing in
     * a specified format, or when importing in unknown format. In the latter case,
     * JabRef cycles through all available import formats. No error messages or feedback
     * is displayed from individual import formats in this case.
     *
     * If importing in a specified format and an empty database is returned, JabRef reports
     * that no entries were found.
     *
     * This method should never return null.
     *
     * @param in the input stream to read from
     */
    public abstract ParserResult importDatabase(InputStream in) throws IOException ;

    public ParserResult importDatabase(Path filePath) throws IOException {
        try (InputStream stream = new FileInputStream(filePath.toFile());
                BufferedInputStream bufferedStream = new BufferedInputStream(stream)) {

            return importDatabase(bufferedStream);
        }
    }

    /**
     * Returns the name of this import format.
     *
     * <p>The name must be unique.</p>
     *
     * @return format name, must be unique and not <code>null</code>
     */
    public abstract String getFormatName();

    /**
     * Returns the file extensions that this importer can read.
     * The extension should contain the leading dot, so for example ".bib"
     *
     * @return list of supported file extensions (not null but may be empty)
     */
    public abstract List<String> getExtensions();

    /**
     * Returns a one-word ID which identifies this import format.
     * Used for example, to identify the format when used from the command line.
     *
     * @return ID, must be unique and not <code>null</code>
     */
    public String getId() {
        String id = getFormatName();
        StringBuilder result = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Returns the description of the import format.
     *
     * The description should specify
     * <ul><li>
     *   what kind of entries from what sources and based on what specification it is able to import
     * </li><li>
     *   by what criteria it {@link #isRecognizedFormat(InputStream) recognizes} an import format
     * </li></ul>
     *
     * @return description of the import format
     */
    public abstract String getDescription();

    @Override
    public int hashCode() {
        return getFormatName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof ImportFormat)) {
            return false;
        }
        ImportFormat other = (ImportFormat)obj;
        return Objects.equals(this.getFormatName(), other.getFormatName());
    }

    @Override
    public String toString() {
        return getFormatName();
    }

    @Override
    public int compareTo(ImportFormat o) {
        return getFormatName().compareTo(o.getFormatName());
    }
}
