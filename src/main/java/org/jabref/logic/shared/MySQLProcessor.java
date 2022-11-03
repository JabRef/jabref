package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Map;

import org.jabref.model.metadata.MetaData;

/**
 * Processes all incoming or outgoing bib data to MySQL Database and manages its structure.
 */
public class MySQLProcessor extends DBMSProcessor {

    private Integer CURRENT_VERSION_DB_STRUCT = 1;

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
                "CREATE TABLE IF NOT EXISTS `JABREF_ENTRY` (" +
                        "`SHARED_ID` INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                        "`TYPE` VARCHAR(255) NOT NULL, " +
                        "`VERSION` INT(11) DEFAULT 1)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS `JABREF_FIELD` (" +
                        "`ENTRY_SHARED_ID` INT(11) NOT NULL, " +
                        "`NAME` VARCHAR(255) NOT NULL, " +
                        "`VALUE` TEXT DEFAULT NULL, " +
                        "FOREIGN KEY (`ENTRY_SHARED_ID`) REFERENCES `JABREF_ENTRY`(`SHARED_ID`) ON DELETE CASCADE)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS `JABREF_METADATA` (" +
                        "`KEY` varchar(255) NOT NULL," +
                        "`VALUE` text NOT NULL)");

        Map<String, String> metadata = getSharedMetaData();

        if(metadata.get(MetaData.VERSION_DB_STRUCT) != null){
            try {
                VERSION_DB_STRUCT_DEFALUT = Integer.valueOf(metadata.get(MetaData.VERSION_DB_STRUCT));
            } catch (Exception e) {
                // TODO: handle exception
                LOGGER.error("[VERSION_DB_STRUCT_DEFALUT] not Integer!");
            }
        }else{
            LOGGER.error("[VERSION_DB_STRUCT_DEFALUT] not Exists!");
        }

        if(VERSION_DB_STRUCT_DEFALUT < CURRENT_VERSION_DB_STRUCT){
            //We can to migrate from old table in new table
            if(VERSION_DB_STRUCT_DEFALUT==-1 && CURRENT_VERSION_DB_STRUCT == 1){
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("ENTRY") + " SELECT * FROM `ENTRY`");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("FIELD") + " SELECT * FROM `FIELD`");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("METADATA") + " SELECT * FROM `METADATA`");
                metadata = getSharedMetaData();
            }

            metadata.put(MetaData.VERSION_DB_STRUCT, CURRENT_VERSION_DB_STRUCT.toString());
            setSharedMetaData(metadata);
        }
    }

	/**
     * Scans the database for required tables.
     *
     * @return <code>true</code> if the structure matches the requirements, <code>false</code> if not.
     * @throws SQLException
     */
	@Override
    public boolean checkBaseIntegrity() throws SQLException {
		// boolean value = checkTableAvailability(escape_Table("ENTRY"), escape_Table("FIELD"), escape_Table("METADATA"));
		boolean value =  true;
		if(value){
			Map<String, String> metadata = getSharedMetaData();
			if(metadata.get(MetaData.VERSION_DB_STRUCT) == null){
				value = false;
			}else{
				try {
					VERSION_DB_STRUCT_DEFALUT = Integer.valueOf(metadata.get(MetaData.VERSION_DB_STRUCT));
					if(VERSION_DB_STRUCT_DEFALUT < CURRENT_VERSION_DB_STRUCT){
						value = false;
					}
				} catch (Exception e) {
					// TODO: handle exception
					value = false;
				}
			}
		}
        return value;
    }

    @Override
    String escape(String expression) {
        return "`" + expression + "`";
    }

	@Override
    String escape_Table(String expression) {
        return escape("JABREF_"+expression);
    }
}
