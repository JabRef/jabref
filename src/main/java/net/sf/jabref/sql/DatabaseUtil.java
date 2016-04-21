package net.sf.jabref.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;

import net.sf.jabref.BibDatabaseContext;

public class DatabaseUtil {

    public static void removeDB(DBImportExportDialog dialogo, String dbName, Connection conn, BibDatabaseContext databaseContext)
            throws SQLException {
        if (dialogo.removeAction) {
            if ((dialogo.selectedInt <= 0) && dialogo.isExporter()) {
                JOptionPane.showMessageDialog(dialogo.getDiag(), "Please select a DB to be removed", "SQL Export",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                removeDB(dbName, conn, databaseContext);
            }
        }
    }

    public static void removeDB(String dbName, Connection conn, BibDatabaseContext databaseContext)
            throws SQLException {
        removeAGivenDB(conn, getDatabaseIDByName(databaseContext, conn, dbName));
    }

    /**
     * Returns a Jabref Database ID from the database in case the DB is already exported. In case the bib was already
     * exported before, the method returns the id, otherwise it calls the method that inserts a new row and returns the
     * ID for this new database
     *
     * @param databaseContext the database
     * @param out             The output (PrintStream or Connection) object to which the DML should be written.
     * @return The ID of database row of the jabref database being exported
     * @throws SQLException
     */
    public static int getDatabaseIDByName(BibDatabaseContext databaseContext, Connection out, String dbName)
            throws SQLException {
        String query = "SELECT database_id FROM jabref_database WHERE database_name='" + dbName + "';";
        try (Statement statement = (Statement) ((Connection) out).createStatement();
                ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("database_id");
            } else {
                insertJabRefDatabase(databaseContext, out, dbName);
                return getDatabaseIDByName(databaseContext, out, dbName);
            }
        }
    }

    private static void removeAGivenDB(Connection out, final int database_id) throws SQLException {
        removeAllRecordsForAGivenDB(out, database_id);
        SQLUtil.processQuery(out, "DELETE FROM jabref_database WHERE database_id='" + database_id + "';");
    }

    /**
     * Removes all records for the database being exported in case it was exported before.
     *
     * @param out         The output (PrintStream or Connection) object to which the DML should be written.
     * @param database_id Id of the database being exported.
     * @throws SQLException
     */
    public static void removeAllRecordsForAGivenDB(Connection out, final int database_id) throws SQLException {
        SQLUtil.processQuery(out, "DELETE FROM entries WHERE database_id='" + database_id + "';");
        SQLUtil.processQuery(out, "DELETE FROM groups WHERE database_id='" + database_id + "';");
        SQLUtil.processQuery(out, "DELETE FROM strings WHERE database_id='" + database_id + "';");
    }

    /**
     * This method creates a new row into jabref_database table enabling to export more than one .bib
     *
     * @param databaseContext the database
     * @param out             The output (PrintStream or Connection) object to which the DML should be written.
     * @throws SQLException
     */
    private static void insertJabRefDatabase(final BibDatabaseContext databaseContext, Connection out, String dbName)
            throws SQLException {
        String path;
        if (databaseContext.getDatabaseFile() == null) {
            path = dbName;
        } else {
            path = databaseContext.getDatabaseFile().getAbsolutePath();
        }
        SQLUtil.processQuery(out,
                "INSERT INTO jabref_database(database_name, md5_path) VALUES ('" + dbName + "', md5('" + path + "'));");
    }

}
