package org.jabref.logic.shared;

import java.sql.SQLException;

/**
 * Processes all incoming or outgoing bib data to MySQL Database and manages its structure.
 */
public class MySQLProcessor extends DBMSProcessor {

    public MySQLProcessor(DatabaseConnection connection) {
        super(connection);
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     *
     * @throws SQLException
     */
    @Override
    public void setUp() throws SQLException {
        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS `ENTRY` (" +
                        "`SHARED_ID` INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                        "`TYPE` VARCHAR(255) NOT NULL, " +
                        "`VERSION` INT(11) DEFAULT 1)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS `FIELD` (" +
                        "`ENTRY_SHARED_ID` INT(11) NOT NULL, " +
                        "`NAME` VARCHAR(255) NOT NULL, " +
                        "`VALUE` TEXT DEFAULT NULL, " +
                        "FOREIGN KEY (`ENTRY_SHARED_ID`) REFERENCES `ENTRY`(`SHARED_ID`) ON DELETE CASCADE)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS `METADATA` (" +
                        "`KEY` varchar(255) NOT NULL," +
                        "`VALUE` text NOT NULL)");
    }

    @Override
    String escape(String expression) {
        return "`" + expression + "`";
    }
}
