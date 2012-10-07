package net.sf.jabref.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;

import net.sf.jabref.MetaData;
import net.sf.jabref.sql.DBImportExportDialog.DialogType;

public class DBImporterExporter {

	public void removeDB(DBImportExportDialog dialogo, String dbName,
			Connection conn, MetaData metadata) throws SQLException {
		if (dialogo.removeAction) {
			if ((dialogo.selectedInt <= 0)
					&& (dialogo.getDialogType().equals(DialogType.EXPORTER))) {
				JOptionPane.showMessageDialog(dialogo.getDiag(),
						"Please select a DB to be removed", "SQL Export",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				removeAGivenDB(conn,
						getDatabaseIDByName(metadata, conn, dbName));
			}
		}
	}

	/**
	 * Returns a Jabref Database ID from the database in case the DB is already
	 * exported. In case the bib was already exported before, the method returns
	 * the id, otherwise it calls the method that inserts a new row and returns
	 * the ID for this new database
	 * 
	 * @param metaData
	 *            The MetaData object containing the database information
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @return The ID of database row of the jabref database being exported
	 * @throws SQLException
	 */
	public int getDatabaseIDByName(MetaData metaData, Object out, String dbName)
			throws SQLException {

		if (out instanceof Connection) {
			Object response = SQLUtil.processQueryWithResults(out,
					"SELECT database_id FROM jabref_database WHERE database_name='"
							+ dbName + "';");
			ResultSet rs = ((Statement) response).getResultSet();
			if (rs.next())
				return rs.getInt("database_id");
			else {
				insertJabRefDatabase(metaData, out, dbName);
				return getDatabaseIDByName(metaData, out, dbName);
			}
		}
		// in case of text export there will be only 1 bib exported
		else {
			insertJabRefDatabase(metaData, out, dbName);
			return 1;
		}
	}

	public void removeAGivenDB(Object out, int database_id) throws SQLException {
		removeAllRecordsForAGivenDB(out, database_id);
		SQLUtil.processQuery(out,
				"DELETE FROM jabref_database WHERE database_id='" + database_id
						+ "';");
	}

	/**
	 * Removes all records for the database being exported in case it was
	 * exported before.
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @param database_id
	 *            Id of the database being exported.
	 * @throws SQLException
	 */
	public void removeAllRecordsForAGivenDB(Object out, int database_id)
			throws SQLException {
		SQLUtil.processQuery(out, "DELETE FROM entries WHERE database_id='"
				+ database_id + "';");
		SQLUtil.processQuery(out, "DELETE FROM groups WHERE database_id='"
				+ database_id + "';");
		SQLUtil.processQuery(out, "DELETE FROM strings WHERE database_id='"
				+ database_id + "';");
	}

	/**
	 * This method creates a new row into jabref_database table enabling to
	 * export more than one .bib
	 * 
	 * @param metaData
	 *            The MetaData object containing the groups information
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * 
	 * @throws SQLException
	 */
	private void insertJabRefDatabase(final MetaData metaData, Object out,
			String dbName) throws SQLException {
		String path = null;
		if (null == metaData.getFile())
			path = dbName;
		else
			path = metaData.getFile().getAbsolutePath();
		SQLUtil.processQuery(out,
				"INSERT INTO jabref_database(database_name, md5_path) VALUES ('"
						+ dbName + "', md5('" + path + "'));");
	}

}
