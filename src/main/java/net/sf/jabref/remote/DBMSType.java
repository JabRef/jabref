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
package net.sf.jabref.remote;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Enumerates all supported database systems (DBMS) by JabRef.
 */
public enum DBMSType {

    MYSQL("MySQL", "com.mysql.jdbc.Driver"),
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver");

    private String type;
    private String driverPath;

    private DBMSType(String type, String driverPath) {
        this.type = type;
        this.driverPath = driverPath;
    }

    @Override
    public String toString() {
        return this.type;
    }

    /**
     * @return Java Class path for establishing JDBC connection.
     */
    public String getDriverClassPath() throws Error {
        return this.driverPath;
    }

    public static Optional<DBMSType> fromString(String typeName) {
        for (DBMSType dbmsType : EnumSet.allOf(DBMSType.class)) {
            if (typeName.equals(dbmsType.toString())) {
                return Optional.ofNullable(dbmsType);
            }
        }
        return Optional.empty();
    }


}
