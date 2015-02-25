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
package net.sf.jabref.export;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.MetaData;

import java.util.Set;

import javax.swing.filechooser.FileFilter;

public interface IExportFormat {

	/**
	 * Name to call this format in the console.
	 */
	String getConsoleName();

	/**
	 * Name to display to the user (for instance in the Save file format drop
	 * down box.
	 */
	String getDisplayName();

	/**
	 * A file filter that accepts filetypes that this exporter would create.
	 */
	FileFilter getFileFilter();

	/**
	 * Perform the export.
	 * 
	 * @param database
	 *            The database to export from.
     * @param metaData
     *            The database's metadata.
	 * @param file
	 *            The filename to write to.
	 * @param encoding
	 *            The encoding to use.
	 * @param entryIds
	 *            (may be null) A Set containing the IDs of all entries that
	 *            should be exported. If null, all entries will be exported.
	 * @throws Exception
	 */
	void performExport(BibtexDatabase database, MetaData metaData,
                       String file, String encoding,
		Set<String> entryIds) throws Exception;

}
