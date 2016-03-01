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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.sql.exporter.DBExporter;
import net.sf.jabref.sql.exporter.MySQLExporter;
import net.sf.jabref.sql.exporter.PostgreSQLExporter;
import net.sf.jabref.sql.importer.DBImporter;
import net.sf.jabref.sql.importer.MySQLImporter;
import net.sf.jabref.sql.importer.PostgreSQLImporter;

/**
 * Created by ifsteinm
 *
 * Jan 20th 	This class is a factory that creates DBImporter and DBExporters
 * 				when the user wishes to import or export a bib file to DBMS
 *
 */
public class DBExporterAndImporterFactory {

    private static final Log LOGGER = LogFactory.getLog(DBExporterAndImporterFactory.class);


    /**
     * All DBTypes must appear here. The enum items must be the
     * names that appear in the combobox used to select the DB,
     * because this text is used to choose which DBImporter/Exporter
     * will be sent back to the requester
     *
     */
    public enum DBType {
        MYSQL("MYSQL"), POSTGRESQL("POSTGRESQL");

        private final String dataBaseType;


        DBType(String dbType) {
            this.dataBaseType = dbType;
        }

        public String getDBType() {
            return dataBaseType;
        }
    }


    /**
     * Returns a DBExporter object according to a given DBType
     *
     * @param type
     * 		The type of the database selected
     * @return The DBExporter object instance
     */
    private DBExporter getExporter(DBType type) {
        DBExporter exporter = null;
        switch (type) {
        case MYSQL:
            exporter = MySQLExporter.getInstance();
            break;
        case POSTGRESQL:
            exporter = PostgreSQLExporter.getInstance();
            break;
        default:
            LOGGER.warn("Unkown database type");
            break;
        }
        return exporter;
    }

    /**
     * Returns a DBExporter object according the type given as a String
     *
     * @param type
     * 		The type of the DB as a String. (e.g. Postgresql, MySQL)
     * @return The DBExporter object instance
     */
    public DBExporter getExporter(String type) {
        return this.getExporter(DBType.valueOf(type.toUpperCase()));
    }

    /**
     * Returns a DBImporter object according to a given DBType
     *
     * @param type
     * 		The type of the database selected
     * @return The DBImporter object instance
     */
    private DBImporter getImporter(DBType type) {
        DBImporter importer = null;
        switch (type) {
        case MYSQL:
            importer = MySQLImporter.getInstance();
            break;
        case POSTGRESQL:
            importer = PostgreSQLImporter.getInstance();
            break;
        default:
            LOGGER.warn("Unknown database type");
            break;
        }
        return importer;
    }

    /**
     * Returns a DBImporter object according the type given as a String
     *
     * @param type
     * 		The type of the DB as a String. (e.g. Postgresql, MySQL)
     * @return The DBImporter object instance
     */
    public DBImporter getImporter(String type) {
        return this.getImporter(DBType.valueOf(type.toUpperCase()));
    }
}