/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.nio.charset.Charset;
import java.util.Set;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.sql.DBExporterAndImporterFactory;

/**
 * MySQLExport contributed by Lee Patton.
 */
public class MySQLExport extends ExportFormat {

    public MySQLExport() {
        super(Localization.lang("MySQL database"), "mysql", null, null, ".sql");
    }

    /**
     * First method called when user starts the export.
     *
     * @param database The bibtex database from which to export.
     * @param file The filename to which the export should be writtten.
     * @param encodingToUse The encoding to use.
     * @param keySet The set of IDs of the entries to export.
     * @throws java.lang.Exception If something goes wrong, feel free to throw an exception. The error message is shown
     *             to the user.
     */
    @Override
    public void performExport(final BibDatabase database, final MetaData metaData, final String file,
            final Charset encodingToUse, Set<String> keySet) throws Exception {

        new DBExporterAndImporterFactory().getExporter("MYSQL").exportDatabaseAsFile(database, metaData, keySet, file,
                encodingToUse);

    }

}
