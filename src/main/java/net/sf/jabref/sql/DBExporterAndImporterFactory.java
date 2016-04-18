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

import java.util.Objects;

import net.sf.jabref.sql.database.MySQL;
import net.sf.jabref.sql.database.PostgreSQL;
import net.sf.jabref.sql.exporter.DatabaseExporter;
import net.sf.jabref.sql.importer.DatabaseImporter;

public class DBExporterAndImporterFactory {

    /**
     * ensuring that only one is used in the system
     */
    private static final DatabaseExporter MYSQL_EXPORTER = new DatabaseExporter(new MySQL());

    /**
     * ensuring that only one is used in the system
     */
    private static final DatabaseExporter POSTGRESQL_EXPORTER = new DatabaseExporter(new PostgreSQL());

    public DatabaseExporter getExporter(DatabaseType type) {
        Objects.requireNonNull(type);

        switch (type) {
            case POSTGRESQL:
                return POSTGRESQL_EXPORTER;
            case MYSQL:
            default:
                return MYSQL_EXPORTER;
        }
    }

    public DatabaseImporter getImporter(DatabaseType type) {
        Objects.requireNonNull(type);

        switch (type) {
            case POSTGRESQL:
                return new DatabaseImporter(new PostgreSQL());
            case MYSQL:
            default:
                return new DatabaseImporter(new MySQL());
        }
    }
}
