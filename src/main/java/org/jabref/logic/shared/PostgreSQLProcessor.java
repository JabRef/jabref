package org.jabref.logic.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.jabref.gui.JabRefExecutorService;
import org.jabref.logic.shared.listener.PostgresSQLNotificationListener;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.postgresql.PGConnection;

/**
 * Processes all incoming or outgoing bib data to PostgreSQL database and manages its structure.
 */
public class PostgreSQLProcessor extends DBMSProcessor {

    private PostgresSQLNotificationListener listener;
    private Integer CURRENT_VERSION_DB_STRUCT = 1;

    public PostgreSQLProcessor(DatabaseConnection connection) {
        super(connection);
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     *
     * @throws SQLException
     */
    @Override
    public void setUp() throws SQLException {
		connection.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS jabref");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("ENTRY") + " (" +
                        "\"SHARED_ID\" SERIAL PRIMARY KEY, " +
                        "\"TYPE\" VARCHAR, " +
                        "\"VERSION\" INTEGER DEFAULT 1)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("FIELD") + " (" +
                        "\"ENTRY_SHARED_ID\" INTEGER REFERENCES " + escape_Table("ENTRY") + "(\"SHARED_ID\") ON DELETE CASCADE, " +
                        "\"NAME\" VARCHAR, " +
                        "\"VALUE\" TEXT)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("METADATA") + " ("
                        + "\"KEY\" VARCHAR,"
                        + "\"VALUE\" TEXT)");

		Map<String, String> metadata = getSharedMetaData();

        if(metadata.get(MetaData.VERSION_DB_STRUCT) != null){
            try {
                VERSION_DB_STRUCT_DEFALUT = Integer.valueOf(metadata.get(MetaData.VERSION_DB_STRUCT));
            } catch (Exception e) {
                // TODO: handle exception
                LOGGER.error("[VERSION_DB_STRUCT_DEFALUT] not Integer!");
            }
        }

        if(VERSION_DB_STRUCT_DEFALUT < CURRENT_VERSION_DB_STRUCT){
            //We can to migrate from old table in new table
            if(VERSION_DB_STRUCT_DEFALUT==-1 && CURRENT_VERSION_DB_STRUCT == 1){
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("ENTRY") + " SELECT * FROM \"ENTRY\"");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("FIELD") + " SELECT * FROM \"FIELD\"");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("METADATA") + " SELECT * FROM \"METADATA\"");
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
		boolean value = true;
		if(value){
			Map<String, String> metadata = getSharedMetaData();
			if(metadata.get(MetaData.VERSION_DB_STRUCT) == null){
				value = false;
			}else{
				try {
					CURRENT_VERSION_DB_STRUCT = Integer.valueOf(metadata.get(MetaData.VERSION_DB_STRUCT));
					if(VERSION_DB_STRUCT_DEFALUT < CURRENT_VERSION_DB_STRUCT){
						value = false;
                        //We can to migrate from old table in new table
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
    protected void insertIntoEntryTable(List<BibEntry> bibEntries) {
        StringBuilder insertIntoEntryQuery = new StringBuilder()
                .append("INSERT INTO ")
                .append(escape_Table("ENTRY"))
                .append("(")
                .append(escape("TYPE"))
                .append(") VALUES(?)");
        // Number of commas is bibEntries.size() - 1
        for (int i = 0; i < bibEntries.size() - 1; i++) {
            insertIntoEntryQuery.append(", (?)");
        }
        try (PreparedStatement preparedEntryStatement = connection.prepareStatement(insertIntoEntryQuery.toString(),
                Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < bibEntries.size(); i++) {
                preparedEntryStatement.setString(i + 1, bibEntries.get(i).getType().getName());
            }
            preparedEntryStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedEntryStatement.getGeneratedKeys()) {
                // The following assumes that we get the generated keys in the order the entries were inserted
                // This should be the case
                for (BibEntry bibEntry : bibEntries) {
                    generatedKeys.next();
                    bibEntry.getSharedBibEntryData().setSharedID(generatedKeys.getInt(1));
                }
                if (generatedKeys.next()) {
                    LOGGER.error("Error: Some shared IDs left unassigned");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    String escape(String expression) {
        return "\"" + expression + "\"";
    }

	@Override
    String escape_Table(String expression) {
        return "jabref." + escape(expression);
    }

    @Override
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        // Disable cleanup output of ThreadedHousekeeper
        // Logger.getLogger(ThreadedHousekeeper.class.getName()).setLevel(Level.SEVERE);
        try {
            connection.createStatement().execute("LISTEN jabrefLiveUpdate");
            // Do not use `new PostgresSQLNotificationListener(...)` as the object has to exist continuously!
            // Otherwise the listener is going to be deleted by GC.
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            listener = new PostgresSQLNotificationListener(dbmsSynchronizer, pgConnection);
            JabRefExecutorService.INSTANCE.execute(listener);
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void stopNotificationListener() {
        try {
            listener.stop();
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void notifyClients() {
        try {
            connection.createStatement().execute("NOTIFY jabrefLiveUpdate, '" + PROCESSOR_ID + "';");
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }
}
