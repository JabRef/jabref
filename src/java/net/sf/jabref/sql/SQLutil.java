/*
 * SQLutil.java
 *
 * Created on October 4, 2007, 5:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jabref.sql;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.groups.ExplicitGroup;
import net.sf.jabref.groups.GroupTreeNode;

/**
 *
 * @author pattonlk
 */
public class SQLutil {

    public enum DBTYPE {
        MYSQL, DERBY
    } 

    private static ArrayList<String> fields = null;
    private static String fieldstr = null;

    /**
     * returns the DBTYPE associated with a DBStrings ServerType
     *
     * @param dbstrings
     *          The DBStrings to query
     * @return The DBTYPE associated withthe dbstrings ServerType
     */
    public static DBTYPE getDBType (DBStrings dbstrings) {

        DBTYPE dbtype = null;
        String srvtype = dbstrings.getServerType();

        if (srvtype.equalsIgnoreCase("mysql")) {
            dbtype = DBTYPE.MYSQL;
        }
        if (srvtype.equalsIgnoreCase("derby")) {
            dbtype = DBTYPE.DERBY;
        }

        return dbtype;
    }

    /**
     * This routine returns the JDBC url corresponding to the DBStrings input.
     *
     * @param dbstrings
     *          The DBStrings to use to make the connection
     * @return The JDBC url corresponding to the input DBStrings
     */
    public static String createJDBCurl (DBStrings dbs) {

        String url = "";
        String servertype = dbs.getServerType();

        if (servertype.equalsIgnoreCase("mysql")) {
            url = "jdbc:" + dbs.getServerType().toLowerCase() + "://" 
                          + dbs.getServerHostname() + "/" 
                          + dbs.getDatabase();
        }

        if (servertype.equalsIgnoreCase("derby")) {
            url = "jdbc:" + dbs.getServerType().toLowerCase() + ":"
                          + dbs.getDatabase() + ";create=true";
        }

        return url;
    }

    public static String getJDBCdriver (DBStrings dbstrings) {

        String driver = "";
        String servertype = dbstrings.getServerType();

        if (servertype.equalsIgnoreCase("mysql")) {
            driver ="com.mysql.jdbc.Driver";
        }

        if (servertype.equalsIgnoreCase("derby")) {
            driver = "org.apache.derby.jdbc.EmbeddedDriver";
        }

        return driver;
            
    }

    /**
     * This routine accepts the location of a MySQL database specified as a url as 
     * well as the username and password for the MySQL user with appropriate access
     * to this database.  The routine returns a valid Connection object if the MySQL 
     * database is successfully opened. It returns a null object otherwise.
     */
    public static Connection connectToDB (DBStrings dbstrings)
        throws Exception {

        String url = createJDBCurl(dbstrings);
        String drv = getJDBCdriver(dbstrings);

        Class.forName (drv).newInstance ();
        Connection conn = DriverManager.getConnection (url,
                           dbstrings.getUsername(), dbstrings.getPassword());
       
        return conn;

    }    
   

    /**
     * Utility method for processing DML with proper output
     *
     * @param out
     *          The output (PrintStream or Connection) object to which the DML should be sent
     * @param dml
     *          The DML statements to be processed
     */
    private static void processDML ( Object out, String dml) 
                            throws SQLException {

        if ( out instanceof PrintStream) {
            PrintStream fout = (PrintStream) out;
            fout.println(dml);
        }

        if ( out instanceof Connection) {
            Connection conn = (Connection) out;
            execDML(conn, dml);
        }

    }


    /**
     * Utility method for executing DML
     *
     * @param conn
     *          The DML Connection object that will execute the SQL
     * @param sql
     *          The DML statements to be executed
     */
    public static void execDML(Connection conn, String dml) throws SQLException {
        // System.out.println(dml); // remove
        Statement stmnt = conn.createStatement();
        stmnt.execute(dml);
        SQLWarning warn = stmnt.getWarnings();
        if (warn!=null) {
            //TODO handle SQL warnings
            System.out.println(warn.toString());
            System.out.println("("+dml+")");
        }
        stmnt.close();
    }

    
    public static ArrayList<String> getFields() {
        if (fields == null) {
            refreshFields();
        }
        return fields;
    }


    /**
     * loop through entry types to get required, optional, general and utility 
     * fields for this type.
     */
    public static void refreshFields() {

        if (fields==null) {
            fields = new ArrayList<String>();
        } else {
            fields.clear();
        }

        for (BibtexEntryType val : BibtexEntryType.ALL_TYPES.values()) {
            fields = uniqueInsert(fields, val.getRequiredFields());
            fields = uniqueInsert(fields, val.getOptionalFields());
            fields = uniqueInsert(fields, val.getGeneralFields());
            fields = uniqueInsert(fields, val.getUtilityFields());
        }

        // create comma separated list of field names
        fieldstr = "";
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0)
                fieldstr = fieldstr + ", ";
            fieldstr = fieldstr + fields.get(i);
        }

    }


    /**
     * Inserts the elements of a String array into an ArrayList making sure not
     * to duplicate entries in the ArrayList
     * 
     * @param list
     *            The ArrayList containing unique entries
     * @param array
     *            The String array to be inserted into the ArrayList
     * @return The updated ArrayList with new unique entries
     */
    private static ArrayList<String> uniqueInsert(ArrayList<String> list, 
            String[] array) {

        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (!list.contains(array[i]))
                    list.add(array[i]);
            }
        }
        return list;
    }


    /**
     * Accepts the BibtexDatabase and MetaData, generates the DML required to
     * create and populate SQL database tables, and writes this DML to the 
     * specified output file.
     *
     * @param database
     *          The BibtexDatabase to export
     * @param metaData
     *          The MetaData object containing the groups information
     * @param keySet
     *          The set of IDs of the entries to export.
     * @param file
     *          The name of the file to which the DML should be written
     */
    public static void exportDatabase(final BibtexDatabase database,
        final MetaData metaData, Set<String> keySet, String file, DBTYPE dbtype ) 
        throws Exception {

        // open output file
        File outfile = new File(file);
        if (outfile.exists())
            outfile.delete();

        PrintStream fout = null;
        fout = new PrintStream(outfile);

        exportDatabase_worker(dbtype, database, metaData, keySet, fout);

        fout.close();

    }


    /**
     * Accepts the BibtexDatabase and MetaData, generates the DML required to
     * create and populate SQL database tables, and writes this DML to the 
     * specified SQL database.
     *
     * @param database
     *          The BibtexDatabase to export
     * @param metaData
     *          The MetaData object containing the groups information
     * @param keySet
     *          The set of IDs of the entries to export.
     * @param dbStrings
     *          The necessary database connection information
     */
    public static void exportDatabase(final BibtexDatabase database,
        final MetaData metaData, Set<String> keySet, DBStrings dbStrings)
        throws Exception {

        DBTYPE dbtype = getDBType(dbStrings);

        Connection conn = null;

        try {

            conn = SQLutil.connectToDB(dbStrings);

            // conn.setAutoCommit(false);

            exportDatabase_worker(dbtype, database, metaData, keySet, conn);

            if (!conn.getAutoCommit()) {
                conn.commit();
                conn.setAutoCommit(true);
            }

            conn.close();

        } catch (SQLException ex) {

            if (conn != null) {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            }

            throw ex;
        }
    }


   /**
     * Worker method for the exportDatabase methods.
     *
     * @param dbtype
     *          The DBTYPE of the database
     * @param database
     *          The BibtexDatabase to export
     * @param metaData
     *          The MetaData object containing the groups information
     * @param keySet
     *            The set of IDs of the entries to export.
     * @param out
     *          The output (PrintStream or Connection) object to which the DML should be written.
     */
    private static void exportDatabase_worker (DBTYPE dbtype, 
            final BibtexDatabase database, final MetaData metaData, 
            Set<String> keySet, Object out) throws Exception{

        List<BibtexEntry> entries = FileActions.getSortedEntries(database,
            keySet, false);

        // create MySQL tables 
        dmlCreateTables(dbtype,out);

        // populate entry_type table
        dmlPopTab_ET(out);

        // populate entries table
        dmlPopTab_FD(entries,out);

		GroupTreeNode gtn = metaData.getGroups();

		// populate groups table
        dmlPopTab_GP(gtn,out);
        
		// populate entry_group table
        dmlPopTab_EG(gtn,out);
    };

    
    /**
     * Writes the table creation DML to the specififed file.
     * 
     * @param dbtype
     *          Indicates the type of database to be written to 
     * @param fout
     *          The output (PrintStream or Connection) object to which the DML should be written
     */
    private static void dmlCreateTables(DBTYPE dbtype, Object out)
                                throws SQLException{

        // make sure fields are initialized
        if (fields==null) {
            refreshFields();
        }

        // build the DML tables specification
        String dml1 = "", dml2 = "";
        switch (dbtype) {
            case MYSQL:

                // drop tables
                processDML(out,"DROP TABLE IF EXISTS entry_types;");
                processDML(out,"DROP TABLE IF EXISTS entries;");
                processDML(out,"DROP TABLE IF EXISTS groups;");
                processDML(out,"DROP TABLE IF EXISTS entry_group;");

                // generate DML that specifies DB columns corresponding to fields
                dml1 = SQLutil.fieldsAsCols(fields, " VARCHAR(3) DEFAULT NULL");
                dml2 = SQLutil.fieldsAsCols(fields, " TEXT DEFAULT NULL");

                // create tables
                dmlTable_mysql(dml1, dml2, out);

                break;

            case DERBY:

                // drop tables
                if (out instanceof Connection) {

                    Connection conn = (Connection) out;
                    boolean commitNow = conn.getAutoCommit();
                    conn.setAutoCommit(true);

                    //TODO: determine which tables are present, and drop them

                    conn.setAutoCommit(commitNow);

                }

                // generate DML that specifies DB columns corresponding to fields
                dml1 = SQLutil.fieldsAsCols(fields, " VARCHAR(3) DEFAULT NULL");
                dml2 = SQLutil.fieldsAsCols(fields, " LONG VARCHAR DEFAULT NULL");

                // create tables
                dmlTable_derby(dml1, dml2, out);

                break;

            default:
                System.err.println("Error: Do not recognize database enumeration.");
                System.exit(0);
        }

        return;
    }


    /**
     * Generates DML specifying table columns and their datatypes. The output of
     * this routine should be used within a CREATE TABLE statement.
     * 
     * @param fields
     *            Contains unique field names
     * @param datatype
     *            Specifies the SQL data type that the fields should take on.
     * @return The DML code to be included in a CREATE TABLE statement.
     */
    private static String fieldsAsCols(ArrayList<String> fields, String datatype) {
        String str = "";
        ListIterator<String> li = fields.listIterator();
        while (li.hasNext()) {
            str = str + li.next() + " " + datatype;
            if (li.hasNext())
                str = str + ", ";
        }
        return str;
    }

    /**
     * Generates DML code necessary to create all tables in a MySQL database, 
     * and writes it to appropriate output.
     *
     * @param dml1
     *            Column specifications for fields in entry_type table.
     * @param dml2
     *            Column specifications for fields in entries table.
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     * @return DML to create all MySQL tables.
     */
    private static void dmlTable_mysql(String dml1, String dml2, Object out)
            throws SQLException {

        processDML(out,"CREATE TABLE entry_types ( \n"
            + "entry_types_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT, \n"
            + "label			 TEXT, \n"
            + dml1
            + ", \n"
            + "PRIMARY KEY (entry_types_id) \n"
            + ");" );
           
        processDML(out,"CREATE TABLE entries ( \n"
            + "entries_id      INTEGER         NOT NULL AUTO_INCREMENT, \n"
			+ "jabref_eid      VARCHAR("
			+  Util.getMinimumIntegerDigits()
		    + ")   DEFAULT NULL, \n"
            + "entry_types_id  INTEGER         DEFAULT NULL, \n"
            + "cite_key        VARCHAR(30)     DEFAULT NULL, \n"
            + dml2
            + ",\n"
            + "PRIMARY KEY (entries_id), \n"
            + "FOREIGN KEY (entry_types_id) REFERENCES entry_type(entry_types_id) \n"
            + ");");
           
        processDML(out,"CREATE TABLE groups ( \n"
            + "groups_id       INTEGER         NOT NULL AUTO_INCREMENT, \n"
            + "label           VARCHAR(100)     DEFAULT NULL, \n"
            + "parent_id       INTEGER          DEFAULT NULL, \n"
            + "PRIMARY KEY (groups_id) \n"
            + ");");
           
        processDML(out,"CREATE TABLE entry_group ( \n"
            + "entries_id       INTEGER        NOT NULL AUTO_INCREMENT, \n"
            + "groups_id        INTEGER        DEFAULT NULL, \n"
            + "FOREIGN KEY (entries_id) REFERENCES entry_fields(entries_id), \n"
            + "FOREIGN KEY (groups_id)  REFERENCES groups(groups_id) \n"
            + ");");

        return;

    }

    /**
     * Generates DML code necessary to create all tables in a Derby database, 
     * and writes it to appropriate output.
     *
     * @param dml1
     *            Column specifications for fields in entry_type table.
     * @param dml2
     *            Column specifications for fields in entries table.
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     * @return DML to create all Derby tables.
     */
    private static void dmlTable_derby(String dml1, String dml2, Object out)
            throws SQLException {

        processDML(out,"CREATE TABLE entry_types ( "
            + "entry_types_id INT  NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
            + dml1 + ", "
            + "label LONG VARCHAR"
            + ")" );

        processDML(out,"CREATE TABLE entries ( "
            + "entries_id      INTEGER         NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
			+ "jabref_eid      VARCHAR("
			+  Util.getMinimumIntegerDigits()
		    + ")   DEFAULT NULL, "
            + "entry_types_id  INTEGER         DEFAULT NULL, "
            + "cite_key        VARCHAR(30)     DEFAULT NULL, "
            + dml2
            + ")");
          
        processDML(out,"ALTER TABLE entries ADD CONSTRAINT entries_fk "
                     + "FOREIGN KEY (\"entry_types_id\") REFERENCES \"entry_type\" (\"entry_types_id\")");

        processDML(out,"CREATE TABLE groups ( "
            + "groups_id       INTEGER         NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
            + "label           VARCHAR(100)     DEFAULT NULL, "
            + "parent_id       INTEGER          DEFAULT NULL  "
            + ")");
           
        processDML(out,"CREATE TABLE entry_group ( "
            + "entries_id       INTEGER        NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
            + "groups_id        INTEGER        DEFAULT NULL "
            + ")");

        processDML(out,"ALTER TABLE entry_group ADD CONSTRAINT entries_group_fk"
                     + "FOREIGN KEY (\"entries_id\") REFERENCES \"entry_fields\" (\"entries_id\")");

        processDML(out,"ALTER TABLE entry_group ADD CONSTRAINT groups_fk"
                     + "FOREIGN KEY (\"groups_id\") REFERENCES \"groups\" (\"groups_id\")");

        return;

    }

     /**
     * Generates the DML required to populate the entry_types table with jabref
     * data.
     * 
     * @param out
     *          The output (PrintSream or Connection) object to which the DML should be written.
     */
    private static void dmlPopTab_ET( Object out) throws SQLException{

        String dml = "";
        String insert = "INSERT INTO entry_types (label, "+fieldstr+") VALUES (";

        ArrayList<String> fieldID = new ArrayList<String>();
        for (int i = 0; i < fields.size(); i++)
            fieldID.add(null);

        // loop through entry types
        for (BibtexEntryType val : BibtexEntryType.ALL_TYPES.values()) {

            // set ID for each field corresponding to its relationship to the
            // entry type
            for (int i = 0; i < fieldID.size(); i++) {
                fieldID.set(i, "");
            }
            fieldID = setFieldID(fields, fieldID, val.getRequiredFields(),
                "req");
            fieldID = setFieldID(fields, fieldID, val.getOptionalFields(),
                "opt");
            fieldID = setFieldID(fields, fieldID, val.getGeneralFields(), "gen");
            fieldID = setFieldID(fields, fieldID, val.getUtilityFields(), "uti");

            // build DML insert statement
            dml = insert + "\"" + val.getName().toLowerCase() + "\"";
            for (int i = 0; i < fieldID.size(); i++) {
                dml = dml + ", ";
                if (fieldID.get(i) != "") {
                    dml = dml + "\"" + fieldID.get(i) + "\"";
                } else {
                    dml = dml + "NULL";
                }
            }
            dml = dml + ");";

            // handle DML according to output type
            processDML(out, dml);

        }

        return;

    }


     /**
     * A utility function for facilitating the assignment of a code to each
     * field name that represents the relationship of that field to a specific
     * entry type.
     * 
     * @param fields
     *            A list of all fields.
     * @param fieldID
     *            A list for holding the codes.
     * @param fieldstr
     *            A String array containing the fields to be coded.
     * @param ID
     *            The code that should be assigned to the specified fields.
     * @return The updated code list.
     */
    private static ArrayList<String> setFieldID(ArrayList<String> fields,
        ArrayList<String> fieldID, String[] fieldstr, String ID) {
        if (fieldstr != null) {
            for (int i = 0; i < fieldstr.length; i++) {
                fieldID.set(fields.indexOf(fieldstr[i]), ID);
            }
        }
        return fieldID;
    }


     /**
     * Generates the DML required to populate the entries table with jabref
     * data and writes it to the output PrintStream.
     * 
     * @param entries
     *          The BibtexEntries to export     
     * @param out
     *          The output (PrintStream or Connection) object to which the DML should be written.
     */
    private static void dmlPopTab_FD(List<BibtexEntry> entries, Object out) 
                            throws SQLException {

        String dml = "";
        String val = "";
        String insert = "INSERT INTO entries (jabref_eid, entry_types_id, cite_key, "
            + fieldstr
            + ") VALUES (";

        // loop throught the entries that are to be exported
        for (BibtexEntry entry : entries) {

            // build DML insert statement
            dml = insert 
			      + "\"" + entry.getId() + "\""
			      + ", (SELECT entry_types_id FROM entry_types WHERE label=\""
			      + entry.getType().getName().toLowerCase() + "\"), \""
                  + entry.getCiteKey() + "\"";

            for (int i = 0; i < fields.size(); i++) {
                dml = dml + ", ";
                val = entry.getField(fields.get(i));
                if (val != null) {
                    dml = dml + "\"" + val.replaceAll("\"", "\\\\\"") + "\"";
                } else {
                    dml = dml + "NULL";
                }
            }
            dml = dml + ");";

            // handle DML according to output type
            processDML(out, dml);

        }

        return;

    }

     /**
     * Generates the DML required to populate the groups table with jabref
     * data, and writes this DML to the output file.
     * 
     * @param cursor
     *            The current GroupTreeNode in the GroupsTree
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     */
	private static int dmlPopTab_GP (GroupTreeNode cursor, Object out) 
                        throws Exception {
        int cnt = dmlPopTab_GP_worker(cursor, 1, 1, out);
        return cnt;
    }

    /**
     * Recursive worker method for the dmlPopTab_GP methods.
     *
     * @param cursor
     *            The current GroupTreeNode in the GroupsTree
     * @param parentID
     *            The integer ID associated with the cursors's parent node
     * @param ID
     *            The integer value to associate with the cursor
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     */
	private static int dmlPopTab_GP_worker (GroupTreeNode cursor, int parentID,
            int ID, Object out) throws SQLException{

        // handle DML according to output type
        processDML(out, "INSERT INTO groups (groups_id, label, parent_id) " 
				      + "VALUES (" + ID + ", \"" + cursor.getGroup().getName() 
				      + "\", " + parentID + ");");

		// recurse on child nodes (depth-first traversal)
	    int myID = ID;
	    for (Enumeration<GroupTreeNode> e = cursor.children(); e.hasMoreElements();) 
			ID = dmlPopTab_GP_worker(e.nextElement(),myID,++ID,out);
	    return ID;
	}


    /**
     * Generates the DML required to populate the entry_group table with jabref
     * data, and writes the DML to the PrintStream.
     * 
     * @param cursor
     *            The current GroupTreeNode in the GroupsTree
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     */
	private static int dmlPopTab_EG(GroupTreeNode cursor, Object fout) 
                        throws SQLException{

            int cnt = dmlPopTab_EG_worker(cursor, 1, 1, fout);
            return cnt;
    }

    /**
     * Recursive worker method for the dmlPopTab_EG methods.
     * 
     * @param cursor
     *            The current GroupTreeNode in the GroupsTree
     * @param parentID
     *            The integer ID associated with the cursors's parent node
     * @param ID
     *            The integer value to associate with the cursor
     * @param out
     *            The output (PrintStream or Connection) object to which the DML should be written.
     */

	private static int dmlPopTab_EG_worker(GroupTreeNode cursor, int parentID, int ID, 
			Object out) throws SQLException{

		// if this group contains entries...
		if ( cursor.getGroup() instanceof ExplicitGroup) {

			// build INSERT statement for each entry belonging to this group
			ExplicitGroup grp = (ExplicitGroup)cursor.getGroup();
			
			for (BibtexEntry be : grp.getEntries()){

                // handle DML according to output type
                processDML(out, "INSERT INTO entry_group (entries_id, groups_id) " 
						   + "VALUES (" 
						   + "(SELECT entries_id FROM entries WHERE jabref_eid="
						   + "\"" + be.getId() + "\""
						   + "), "
						   + "(SELECT groups_id FROM groups WHERE groups_id=" 
						   + "\"" + ID + "\")"
						   + ");");
			}
		}

		// recurse on child nodes (depth-first traversal)
	    int myID = ID;
	    for (Enumeration<GroupTreeNode> e = cursor.children(); e.hasMoreElements();) 
			ID = dmlPopTab_EG_worker(e.nextElement(),myID,++ID,out);

	    return ID;
	}

    /**
     * Processes a SQLException, and returns a more user-friendly message
     * 
     * @param ex
     *            The SQLException raised
     * @param dbtype
     *            DBTYPE specifying the type of database that raised the exception
     */

    public static String getExceptionMessage (Exception ex, DBTYPE dbtype) {
        
        String errorMessage = null;

        switch (dbtype) {
            case MYSQL:
                errorMessage = getExceptionMessage_MySQL(ex);
                break;
            case DERBY:
                errorMessage = getExceptionMessage_MySQL(ex);
                break;
            default:
                errorMessage = Globals.lang("Could not determine exception message.");
                break;
        }

        return errorMessage;

    }

    /**
     * Handles work for getExceptionMessage when dbtype is MYSQL
     * 
     * @param ex
     *            The SQLException raised
     */
    public static String getExceptionMessage_MySQL (Exception ex) {
      
        String msg = null;

        // handle case where exception is SQL related
        if (ex instanceof SQLException) {

            SQLException sqlex = (SQLException) ex;

            // desc  : Unkown DB
            // code  : 1049
            // state : 42000
            // msg   : Unkown database 'database_name'
            // type  : SQLException
            // tested with MySQL

            if (sqlex.getSQLState() == "42000") {
                msg = Globals.lang(sqlex.getMessage());
            }


            // desc  : Invalid username and/or password
            // code  : 1045
            // state : 28000
            // msg   : Access denied for user 'username'@'hostname' (using password: ...) 
            // type  : SQLException
            // tested with MySQL

            if (sqlex.getSQLState() == "28000") {
                msg = Globals.lang(sqlex.getMessage());
            }


            // desc  : Cannot connect to SQL server
            // code  : 0
            // state : 08S01
            // msg   : Communications link failure due to underlying exception
            // type  : java.net.UnknownHostException
            // tested with MySQL
            if (sqlex.getSQLState() == "08S01") {
                msg = Globals.lang("Cannot connect to SQL server at the specified host.");
            } 

            // for debugging...
            if (false) {
                System.out.println("-------------------------------------");
                System.out.println(sqlex.getErrorCode());
                System.out.println(sqlex.getSQLState());
                System.out.println(sqlex.getMessage());
                System.out.println("-------------------------------------");
            }

        } 
        
        // handle case where exception is non-SQL related
        if (msg == null) {

            if (ex.getMessage()==null) {
                msg = ex.toString();
            } else {
                msg = ex.getMessage();
            }

        }


        return msg;

    }

}
