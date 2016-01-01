/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General public static License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General public static License for more details.

    You should have received a copy of the GNU General public static License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.sql.importer;

import net.sf.jabref.MetaData;
import net.sf.jabref.model.database.BibDatabase;

public class DBImporterResult {

    public final BibDatabase database;
    public final MetaData metaData;
    public final String name;

    public DBImporterResult(final BibDatabase database, final MetaData metaData, final String name) {
        this.database = database;
        this.metaData = metaData;
        this.name = name;
    }

}
