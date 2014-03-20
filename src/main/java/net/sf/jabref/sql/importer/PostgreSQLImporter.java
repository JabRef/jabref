/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

/**
 * 
 * @author ifsteinm.
 * 
 *  Jan 20th	Extends DBExporter to provide features specific for PostgreSQL
 *  			Created after a refactory on SQLUtil. 
 *          
 */
public class PostgreSQLImporter extends DBImporter {

	private static PostgreSQLImporter instance = null;

	private PostgreSQLImporter() {
	}

	/**
	 * 
	 * @return The singleton instance of the MySQLImporter
	 */
	public static PostgreSQLImporter getInstance() {
		if (instance == null)
			instance = new PostgreSQLImporter();
		return instance;
	}

	@Override
	protected ResultSet readColumnNames(Connection conn) throws SQLException {
		Statement statement = (Statement) SQLUtil.processQueryWithResults(conn,
				"SELECT column_name FROM information_schema.columns WHERE table_name ='entries';");
		ResultSet rs = statement.getResultSet();
		return rs;
	}

	@Override
	protected Connection connectToDB(DBStrings dbstrings) throws Exception{
		String url = SQLUtil.createJDBCurl(dbstrings, true);
		String drv = "org.postgresql.Driver";

		Class.forName(drv).newInstance();
		Connection conn = DriverManager.getConnection(url,
				dbstrings.getUsername(), dbstrings.getPassword());
		return conn;
	}

}
