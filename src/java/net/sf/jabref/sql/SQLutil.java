/*
 * SQLutil.java
 *
 * Created on October 4, 2007, 5:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jabref.sql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Enumeration;
import java.util.Iterator;
import java.lang.System;
import java.io.PrintStream;
import java.lang.Exception;

import net.sf.jabref.Util;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.ExplicitGroup;


/**
 *
 * @author pattonlk
 */
public class SQLutil {

    public enum DBTYPE {
        MYSQL
    } 

    private static ArrayList<String> fields = null;
    private static String fieldstr = null;


    public static Connection connect_mysql(String url, String username, String password)
        throws Exception {
    /**
     * This routine accepts the location of a MySQL database specified as a url as 
     * well as the username and password for the MySQL user with appropriate access
     * to this database.  The routine returns a valid Connection object if the MySQL 
     * database is successfully opened. It returns a null object otherwise.
     */

        Class.forName ("com.mysql.jdbc.Driver").newInstance ();
        Connection conn = DriverManager.getConnection (url,username,password);
              
        return conn;

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
     * Writes the table creation DML to the specififed file.
     * 
     * @param dbtype
     *          Indicates the type of database to be written to 
     * @param fout
     *          The PrintStream to which the DML should be written
     */
    public static void dmlCreateTables(DBTYPE dbtype, PrintStream fout) 
                            throws SQLException{
        dmlCreateTables_worker(dbtype, fout);
    }


    /**
     * Worker method for the dbmlCreateTables methods.
     * 
     * @param dbtype
     *          Indicates the type of database to be written to 
     * @param fout
     *          The output (PrintStream or Connection) object to which the DML should be written
     */
    private static void dmlCreateTables_worker(DBTYPE dbtype, Object out)
                                throws SQLException{

        // make sure fields are initialized
        if (fields==null) {
            refreshFields();
        }

        // generate DML that specifies DB columns corresponding to fields
        String dml1 = SQLutil.fieldsAsCols(fields, "\tVARCHAR(3)\t\tDEFAULT NULL");
        String dml2 = SQLutil.fieldsAsCols(fields, "\tTEXT\t\tDEFAULT NULL");

        // build the DML tables specification
        String dml = "";
        switch (dbtype) {
            case MYSQL:
                dml = dmlTable_mysql(dml1, dml2);
                break;
            default:
                System.err.println("Error: Do not recognize database enumeration.");
                System.exit(0);
        }

        // handle DML according to output type
        processDML(out, dml);

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
        Statement stmnt = conn.createStatement();
        stmnt.execute(dml);
        SQLWarning warn = stmnt.getWarnings();
        if (warn!=null) {
            //TODO handle SQL warnings
            System.out.println(warn.toString());
        }
        stmnt.close();
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
            str = str + li.next() + "\t" + datatype;
            if (li.hasNext())
                str = str + ",\n";
        }
        return str;
    }



    /**
     * Returns DML code necessary to create all tables in a MySQL database.
     *
     * @param dml1
     *            Column specifications for fields in entry_type table.
     * @param dml2
     *            Column specifications for fields in entries table.
     * @return DML to create all MySQL tables.
     */
    private static String dmlTable_mysql(String dml1, String dml2) {
        String dml = "DROP TABLE IF EXISTS entry_types;\n"
            + "CREATE TABLE entry_types\n" + "(\n"
            + "entry_types_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,\n"
            + "label			 TEXT,\n"
            + dml1
            + ",\n"
            + "PRIMARY KEY (entry_types_id)\n"
            + ");\n"
            + "\n"
            + "DROP TABLE IF EXISTS entries;\n"
            + "create table entries\n"
            + "(\n"
            + "entries_id      INTEGER         NOT NULL AUTO_INCREMENT,\n"
			+ "jabref_eid      VARCHAR("
			+  Util.getMinimumIntegerDigits()
		    + ")   DEFAULT NULL,\n"
            + "entry_types_id  INTEGER         DEFAULT NULL,\n"
            + "cite_key        VARCHAR(30)     DEFAULT NULL,\n"
            + dml2
            + ",\n"
            + "PRIMARY KEY (entries_id),\n"
            + "FOREIGN KEY (entry_types_id) REFERENCES entry_type(entry_types_id)\n"
            + ");\n"
            + "\n"
            + "DROP TABLE IF EXISTS groups;\n"
            + "CREATE TABLE groups\n"
            + "(\n"
            + "groups_id       INTEGER         NOT NULL AUTO_INCREMENT,\n"
            + "label           VARCHAR(100)     DEFAULT NULL,\n"
            + "parent_id       INTEGER         DEFAULT NULL,\n"
            + "PRIMARY KEY (groups_id)\n"
            + ");\n"
            + "\n"
            + "DROP TABLE IF EXISTS entry_group;\n"
            + "CREATE TABLE entry_group\n"
            + "(\n"
            + "entries_id       INTEGER        NOT NULL AUTO_INCREMENT,\n"
            + "groups_id        INTEGER        DEFAULT NULL,\n"
            + "FOREIGN KEY (entries_id) REFERENCES entry_fields(entries_id),\n"
            + "FOREIGN KEY (groups_id)  REFERENCES groups(groups_id)\n"
            + ");\n";
        return dml;
    }


     /**
     * Generates the DML required to populate the entry_types table with jabref
     * data.
     * 
     * @param out
     *            The printstream to which the DML should be written.
     */
    public static void dmlPopTab_ET (PrintStream out) throws Exception{
        dmlPopTab_ET_worker(out);
    }

     /**
     * Worker for the dmlPopTab_ET methods.
     *
     * @param out
     *            The output (PrintSream or Connection) object to which the DML should be written.
     */
    private static void dmlPopTab_ET_worker( Object out) throws SQLException{

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
     *          The PrintStream to which the DML should be written.
     */
    public static void dmlPopTab_FD(List<BibtexEntry> entries, PrintStream out) 
                            throws Exception{
        dmlPopTab_FD_worker(entries, out);
    }

     /**
     * Workder method for the dmlPopTab_FD methods.
     * 
     * @param entries
     *          The BibtexEntries to export     
     * @param out
     *          The output (PrintStream or Connection) object to which the DML should be written.
     */
    private static void dmlPopTab_FD_worker (List<BibtexEntry> entries, Object out) 
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
     * @param fout
     *            The printstream to which the DML should be written.
     */
	public static int dmlPopTab_GP (GroupTreeNode cursor, PrintStream fout) 
                        throws Exception {
        int cnt = dmlPopTab_GP_worker(cursor, 1, 1, fout);
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
     * @param fout
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
     * @param fout
     *            The printstream to which the DML should be written.
     */
	public static int dmlPopTab_EG(GroupTreeNode cursor, PrintStream fout) 
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
     * @param fout
     *            The output (PrintStream or Connection) object to which the DML should be written.
     */

	private static int dmlPopTab_EG_worker(GroupTreeNode cursor, int parentID, int ID, 
			Object out) throws SQLException{

		// if this group contains entries...
		if ( cursor.getGroup() instanceof ExplicitGroup) {

			// build INSERT statement for each entry belonging to this group
			ExplicitGroup grp = (ExplicitGroup)cursor.getGroup();
			Iterator it = grp.getEntries().iterator();
			while (it.hasNext()) {

				BibtexEntry be = (BibtexEntry) it.next();

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

}
