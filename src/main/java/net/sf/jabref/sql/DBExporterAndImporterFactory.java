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
package net.sf.jabref.sql;

import net.sf.jabref.sql.database.MySQL;
import net.sf.jabref.sql.database.PostgreSQL;
import net.sf.jabref.sql.exporter.DatabaseExporter;
import net.sf.jabref.sql.importer.DatabaseImporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Optional;

public class DBExporterAndImporterFactory {

    private static final Log LOGGER = LogFactory.getLog(DBExporterAndImporterFactory.class);

    private static final DatabaseImporter MYSQL_IMPORTER = new DatabaseImporter(new MySQL());
    private static final DatabaseImporter POSTGRESQL_IMPORTER = new DatabaseImporter(new PostgreSQL());

    private static final DatabaseExporter MYSQL_EXPORTER = new DatabaseExporter(new MySQL());
    private static final DatabaseExporter POSTGRESQL_EXPORTER = new DatabaseExporter(new PostgreSQL());

    /**
     * Returns a DatabaseExporter object according the type given as a String
     *
     * @param type The type of the DB as a String. (e.g. Postgresql, MySQL)
     * @return The DatabaseExporter object instance
     */
    public Optional<DatabaseExporter> getExporter(String type) {
        return DatabaseType.build(type).map(t -> {
            if (t == DatabaseType.MYSQL) {
                return MYSQL_EXPORTER;
            } else if (t == DatabaseType.POSTGRESQL) {
                return POSTGRESQL_EXPORTER;
            } else {
                return null;
            }
        });
    }

    /**
     * Returns a DatabaseImporter object according the type given as a String
     *
     * @param type The type of the DB as a String. (e.g. Postgresql, MySQL)
     * @return The DatabaseImporter object instance
     */
    public Optional<DatabaseImporter> getImporter(String type) {
        return DatabaseType.build(type).map(t -> {
            if (t == DatabaseType.MYSQL) {
                return MYSQL_IMPORTER;
            } else if (t == DatabaseType.POSTGRESQL) {
                return POSTGRESQL_IMPORTER;
            } else {
                return null;
            }
        });
    }
}
