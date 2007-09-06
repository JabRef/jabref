package net.sf.jabref.export;

import junit.awtui.Logo;
import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.MetaData;

import java.util.Set;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.ListIterator;


/**
 * Skeleton implementation of MySQLExporter.
 */
public class MySQLExport extends ExportFormat {

    public MySQLExport() {
        // You need to fill in the correct extension, and edit the name if necessary.
        // The second argument is the command-line name of the export format:
        super(Globals.lang("MySQL database"), "mysql", null, null, ".sql");
    }

	/**
	 * First method calledd when user starts the export.
	 *
	 * @param database The bibtex database from which to export.
	 * @param file The filename to which the export should be writtten.
     * @param encoding The encoding to use.
     * @param keySet The set of IDs of the entries to export.
	 * @throws java.lang.Exception 
	 */
    public void performExport(final BibtexDatabase database,
							  final MetaData metaData,
                              final String file, final String encoding,
                              Set keySet) throws Exception {

        // get TreeMap of all Bibtex Entry Types
		TreeMap<String, BibtexEntryType> ALL_TYPES = BibtexEntryType.ALL_TYPES;

		ArrayList<String> fields  = new ArrayList();
		Set mappings = ALL_TYPES.entrySet();

		// loop through entry types
		for( Iterator iter = mappings.iterator(); iter.hasNext(); )
		{

			// get the entry type
			Map.Entry me = (Map.Entry) iter.next();
			BibtexEntryType val = (BibtexEntryType)me.getValue();

			// get required, optional, general and utility fields for this type
			fields = processFields( fields, val.getRequiredFields() );
			fields = processFields( fields, val.getOptionalFields());
			fields = processFields( fields, val.getGeneralFields());
			fields = processFields( fields, val.getUtilityFields());

		}

		// open output file
		File outfile = new File(file);
		if( outfile.exists()) outfile.delete();
		PrintStream fout = new PrintStream(outfile);

		// generate SQL that specifies columns corresponding to fields
		String sql1 = sql_fieldColumns( fields, "\tVARCHAR(3)\t\tDEFAULT NULL");
		String sql2 = sql_fieldColumns( fields, "\tTEXT\t\tDEFAULT NULL");

		// generate MySQL tables using auto-generated SQL
		String sql = sql_createTables( sql1, sql2);
	
		// write SQL table creation to file
		fout.println(sql);

		// create comma separated list of field names for use in INSERT statements
		String fieldstr = "";
		for( int i=0; i<fields.size(); i++){
			if(i>0) fieldstr = fieldstr + ", ";
			fieldstr = fieldstr + fields.get(i);
		}

		// get entries selected for export
        List<BibtexEntry> entries = FileActions.getSortedEntries(database, keySet, false);
 
		// populate entry_type table
		sql_popTabET( mappings, fields, fieldstr, fout);

		// populate entries table
		sql_popTabFD( entries, fields, fieldstr, fout);

		fout.close();	
	
    }


	/**
	 * Inserts the elements of a String array into an ArrayList making sure not 
	 * to duplicate entries in the ArrayList
	 * 
	 * @param fields The ArrayList containing unique entries
	 * @param efields The String array to be inserted into the ArrayList
	 * @return The updated ArrayList with new unique entries
	 */
	private ArrayList<String> processFields( ArrayList<String> fields, String[] efields){
		if( efields != null)
		{
			for( int i=0; i<efields.length; i++ )
			{
				if(!fields.contains(efields[i])) fields.add(efields[i]);
			}
		}
		return fields;
	}

	/**
	 * Generates DML specifying table columns and thier datatypes.  The output
	 * of this routine should be used within a CREATE TABLE statement.
	 * 
	 * @param fields  Contains unique field names
	 * @param datatype Specifies the SQL data type that the fields should take on.
	 * @return The DML code to be included in a CREATE TABLE statement.
	 */
	private String sql_fieldColumns( ArrayList<String> fields, String datatype){
		String str = "";
		ListIterator<String> li=fields.listIterator();
		while (li.hasNext())
		{
			str = str + li.next() + "\t" + datatype;
			if( li.hasNext() ) str = str + ",\n";
		}
		return str;
	}

	/**
	 * Returns the DML code necessary to create all tables required for holding
	 * jabref database.
	 *
	 * @param sql1 Column specifications for fields in entry_type table.
	 * @param sql2 Column specifications for fields in entries table.
	 * @return DML to creat all tables.
	 */
	private String sql_createTables( String sql1, String sql2){
		String sql = 
			  "DROP TABLE IF EXISTS entry_type;\n"
			+ "CREATE TABLE entry_types\n"
			+ "(\n"
			+ "entry_types_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,\n"
			+ "label			 TEXT,\n"
			+ sql1 + ",\n"
			+ "PRIMARY KEY (entry_types_id)\n"
			+ ");\n"
			+ "\n"
			+ "DROP TABLE IF EXISTS entries;\n"
			+ "create table entries\n"
			+ "(\n"
			+ "entries_id      INTEGER         NOT NULL AUTO_INCREMENT,\n"
			+ "entry_types_id  INTEGER         DEFAULT NULL,\n"
			+ "cite_key 	   VARCHAR(30)     DEFAULT NULL,\n"
			+  sql2 + ",\n"
			+ "PRIMARY KEY (entries_id),\n"
			+ "FOREIGN KEY (entry_types_id) REFERENCES entry_type(entry_types_id)\n"
			+ ");\n"
			+ "\n"
			+ "DROP TABLE IF EXISTS groups;\n"
			+ "CREATE TABLE groups\n"
			+ "(\n"
			+ "groups_id       INTEGER         NOT NULL AUTO_INCREMENT,\n"
			+ "label           VARCHAR(30)     DEFAULT NULL,\n"
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
		return sql;
	 }

	/**
	 * Generates the DML required to populate the entry_types table with jabref data. 
	 *
	 * @param mappings A Set of bibtex entries that are to be exported.
	 * @param fields The fields to be specified.
	 * @param fieldstr A comma delimited string of all field names.
	 * @param out The printstream to which the DML should be written.
	 */
	private void sql_popTabET( Set mappings, ArrayList fields, String fieldstr, PrintStream out){

		String sql = "";
		String insert = "INSERT INTO entry_types (label, " + fieldstr + ") VALUES (";

		ArrayList fieldID = new ArrayList();
		for(int i=0; i<fields.size(); i++) fieldID.add(null);

		// loop through entry types
		Integer cnt = 0;
		for( Iterator iter = mappings.iterator(); iter.hasNext(); )
		{

			// get the entry type
			Map.Entry me = (Map.Entry) iter.next();
			BibtexEntryType val = (BibtexEntryType)me.getValue();

			// set ID for each field corresponding to its relationship to the entry type
			for( int i=0; i<fieldID.size(); i++){
				fieldID.set(i,"");
			}
			fieldID = setFieldID( fields, fieldID, val.getRequiredFields(), "req");
			fieldID = setFieldID( fields, fieldID, val.getOptionalFields(), "opt");
			fieldID = setFieldID( fields, fieldID, val.getGeneralFields(),  "gen");
			fieldID = setFieldID( fields, fieldID, val.getUtilityFields(),  "uti");

			// build SQL insert statement
			sql = insert + "\"" + val.getName().toLowerCase() + "\"";
			for( int i=0; i<fieldID.size(); i++){
				sql = sql + ", ";
				if( fieldID.get(i) != ""){
					sql = sql + "\"" + fieldID.get(i) + "\"";
				}else{
					sql = sql + "NULL";
				}
			}
			sql = sql + ");";

			// write SQL insert to file
			if( out instanceof PrintStream){
				out.println( sql);
			}

		}

		return;

	}

	/**
	 * A utility function for facilitating the assignment of a code to each field
	 * name that represents the relationship of that field to a specific entry type. 
	 *
	 * @param fields  A list of all fields.
	 * @param fieldID A list for holding the codes.
	 * @param fieldstr A String array containing the fields to be coded.
	 * @param ID The code that should be assigned to the specified fields.
	 * @return The updated code list.
	 */
	private ArrayList setFieldID( ArrayList fields, ArrayList fieldID, String[] fieldstr, String ID ){
		if( fieldstr != null)
		{
			for( int i=0; i<fieldstr.length; i++)
			{
				fieldID.set(fields.indexOf(fieldstr[i]), ID);
			}
		}
		return fieldID;
	}

	/**
	 * Generates the DML required to populate the entries table with jabref data. 
	 *
	 * @param entries A sorted list of all entries to be exported.
	 * @param fields The fields to be specified.
	 * @param fieldstr A comma delimited string of all field names.
	 * @param out The printstream to which the DML should be written.
	 */
	private void sql_popTabFD( List<BibtexEntry> entries, ArrayList fields, String fieldstr, PrintStream out){	

		String sql = "";
		String val = "";
		String insert = "INSERT INTO entries (entry_types_id, cite_key, " + fieldstr 
				      + ") VALUES ((SELECT entry_types_id FROM entry_types WHERE label=\"";
	
		// loop throught the entries that are to be exported
		for (BibtexEntry entry : entries) {

			// build SQL insert statement
			sql = insert + entry.getType().getName().toLowerCase() + "\")";
			sql = sql + ", \"" + entry.getCiteKey() + "\"";
			for( int i=0; i<fields.size(); i++){
				sql = sql + ", "; 
				val = entry.getField((String)fields.get(i));
				if( val != null){
					sql = sql + "\"" + val.replaceAll("\"", "\\\\\"") + "\"";
				}else{
					sql = sql + "NULL";
				}
			}
			sql = sql + ");";

			// write SQL insert to file
			if( out instanceof PrintStream){
				out.println( sql);
			}

		}

		return;

	}

}
