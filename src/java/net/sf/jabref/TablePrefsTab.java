package net.sf.jabref;

import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

class TablePrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private String[] _choices;
    private Boolean[] _sel;
    private JCheckBox colorCodes, autoResizeMode, secDesc, terDesc;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    private JComboBox
	secSort = new JComboBox(GUIGlobals.ALL_FIELDS),
	terSort = new JComboBox(GUIGlobals.ALL_FIELDS);
    private JTextArea tableFields = new JTextArea();//"", 80, 5);

    private boolean tableChanged = false;
    private JTable colSetup;
    private int rowCount = -1;
    private Vector tableRows = new Vector(10);
    class TableRow {
	String name;
	int length;
	public TableRow(String name) {
	    this.name = name;
	    length = GUIGlobals.DEFAULT_FIELD_LENGTH;
	}
	public TableRow(int length) {
	    this.length = length;
	    name = "";
	}
	public TableRow(String name, int length) {
	    this.name = name;
	    this.length = length;
	}
    }


    /**
     * Customization of external program paths.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public TablePrefsTab(JabRefPreferences prefs) {
	_prefs = prefs;

	setLayout(gbl);

	colorCodes = new JCheckBox(Globals.lang
				   ("Use color codes for required and optional fields")
				   ,_prefs.getBoolean("tableColorCodesOn"));
	autoResizeMode = new JCheckBox(Globals.lang
				       ("Fit table horizontally on screen"),
				       (_prefs.getInt("autoResizeMode")==JTable.AUTO_RESIZE_ALL_COLUMNS));
	secDesc = new JCheckBox(Globals.lang("Descending"),
				_prefs.getBoolean("secDescending"));
	terDesc = new JCheckBox(Globals.lang("Descending"),
				_prefs.getBoolean("terDescending"));
	tableFields.setText(Util.stringArrayToDelimited
			    (_prefs.getStringArray("columnNames"), ";"));

	String[] names = _prefs.getStringArray("columnNames"),
	    lengths = _prefs.getStringArray("columnWidths");
	for (int i=0; i<names.length; i++) {
	    if (i<lengths.length)
		tableRows.add(new TableRow(names[i], Integer.parseInt(lengths[i])));
	    else 
		tableRows.add(new TableRow(names[i]));
	}
	rowCount = tableRows.size()+5;

	TableModel tm = new AbstractTableModel() {
		public int getRowCount() { return rowCount; }
		public int getColumnCount() { return 2; }
		public Object getValueAt(int row, int column) {
		    if (row >= tableRows.size())
			return "";
		    Object rowContent = tableRows.elementAt(row);
		    if (rowContent == null)
			return "";
		    TableRow tr = (TableRow)rowContent;
		    switch (column) {
		    case 0:
			return tr.name;
		    case 1:			
			return ((tr.length > 0) ? new Integer(tr.length).toString() : "");
		    }
		    return null; // Unreachable.
		}
		
		public String getColumnName(int col) {
		    return (col == 0 ? Globals.lang("Field name") : Globals.lang("Column width"));
		}
		public Class getColumnClass(int column) {
		    if (column == 0) return String.class;
		    else return Integer.class;
		}
		public boolean isCellEditable(int row, int col) {
		    return true;
		}
		public void setValueAt(Object value, int row, int col) {
		    tableChanged = true;
		    // Make sure the vector is long enough.
		    while (row >= tableRows.size())
			tableRows.add(new TableRow("", -1));

		    TableRow rowContent = (TableRow)tableRows.elementAt(row);

		    if (col == 0) {
			rowContent.name = value.toString();
			if (((String)getValueAt(row, 1)).equals(""))
			    setValueAt(""+GUIGlobals.DEFAULT_FIELD_LENGTH, row, 1);
		    }
		    else {
			if (value == null) rowContent.length = -1;
			else rowContent.length = Integer.parseInt(value.toString());
		    }
		}

	    };

	colSetup = new JTable(tm);	
	TableColumnModel cm = colSetup.getColumnModel();
	cm.getColumn(0).setPreferredWidth(90);
	cm.getColumn(1).setPreferredWidth(30);

	JLabel lab;
	JPanel upper = new JPanel(),
	    sort = new JPanel();
	upper.setLayout(gbl);
	sort.setLayout(gbl);

	upper.setBorder(BorderFactory.createTitledBorder
			(BorderFactory.createEtchedBorder(),
			 Globals.lang("Table appearance")));
	sort.setBorder(BorderFactory.createTitledBorder
		       (BorderFactory.createEtchedBorder(),
                        Globals.lang("Sort options")));

	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(colorCodes, con);
	upper.add(colorCodes);
	gbl.setConstraints(autoResizeMode, con);
	upper.add(autoResizeMode);
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(upper, con);
	add(upper);

	// Set the correct value for the primary sort JComboBox.
	String sec = prefs.get("secSort"),
	    ter = prefs.get("terSort");
	for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++) {
	    if (sec.equals(GUIGlobals.ALL_FIELDS[i]))
		secSort.setSelectedIndex(i);
	    if (ter.equals(GUIGlobals.ALL_FIELDS[i]))
		terSort.setSelectedIndex(i);
	}

	lab = new JLabel(Globals.lang("Secondary sort criterion"));
	con.gridwidth = 1;
	con.insets = new Insets(0,5,0,0);
	gbl.setConstraints(lab, con);
	sort.add(lab);
	con.weightx = 1;
	gbl.setConstraints(secSort, con);
	sort.add(secSort);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(secDesc, con);
	sort.add(secDesc);

	con.gridwidth = 1;

 	lab = new JLabel(Globals.lang("Tertiary sort criterion"));
	gbl.setConstraints(lab, con);
	sort.add(lab);
	con.weightx = 0;
	//con.insets = new Insets(0,5,0,0);
	gbl.setConstraints(terSort, con);
	sort.add(terSort);
	con.weightx = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(terDesc, con);
	sort.add(terDesc);

	con.insets = new Insets(0,0,0,0);
	gbl.setConstraints(sort, con);
	add(sort);

        tableFields.setBorder(BorderFactory.createEtchedBorder());
	JScrollPane sp = new JScrollPane
	    (colSetup, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	sp.setMinimumSize(new Dimension(200,300));
	sp.setBorder(BorderFactory.createTitledBorder
		     (BorderFactory.createEtchedBorder(),
		      Globals.lang("Visible fields")));
	con.weighty = 1;
	con.weightx = 0;
	con.fill = GridBagConstraints.NONE;
	gbl.setConstraints(sp, con);
	add(sp);

    }

    private String[] getChoices() {

	// First we count how many checkboxes the user has selected.
	int count = 0;
	for (int i=0; i<_sel.length; i++)
	    if (_sel[i].booleanValue()) count++;

	// Then we build the byte array.
	String[] choices = new String[count];
	count = 0;
	for (int i=0; i<_sel.length; i++)
	    if (_sel[i].booleanValue()) {
		choices[count] = GUIGlobals.ALL_FIELDS[i];
		count++;
	    }
	return choices;
    }


    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    public void storeSettings() {

	if (colSetup.isEditing()) {
	    int col = colSetup.getEditingColumn(),
		row = colSetup.getEditingRow();
	    colSetup.getCellEditor(row, col).stopCellEditing();
	}


	//_prefs.putStringArray("columnNames", getChoices());
	/*String[] cols = tableFields.getText().replaceAll("\\s+","")
	    .replaceAll("\\n+","").toLowerCase().split(";");
	if (cols.length > 0) for (int i=0; i<cols.length; i++)
	    cols[i] = cols[i].trim();
	    else cols = null;*/

	// Now we need to make sense of the contents the user has made to the
	// table setup table.
	if (tableChanged) {
	    // First we remove all rows with empty names.
	    int i=0;
	    while (i < tableRows.size()) {
		if (((TableRow)tableRows.elementAt(i)).name.equals(""))
		    tableRows.removeElementAt(i);
		else i++;		
	    }
	    // Then we make arrays 
	    String[] names = new String[tableRows.size()],
		widths = new String[tableRows.size()];
	    int[] nWidths = new int[tableRows.size()];
	    for (i=0; i<tableRows.size(); i++) {
		TableRow tr = (TableRow)tableRows.elementAt(i);
		names[i] = tr.name;
		nWidths[i] = tr.length;
		widths[i] = ""+tr.length;
		//Util.pr(names[i]+"   "+widths[i]);
	    }

	    // Finally, we store the new preferences.
	    _prefs.putStringArray("columnNames", names);
	    _prefs.putStringArray("columnWidths", widths);	    
	}

	//_prefs.putStringArray("columnNames", cols);

	_prefs.putBoolean("tableColorCodesOn", colorCodes.isSelected());
	_prefs.putInt("autoResizeMode",
		      autoResizeMode.isSelected() ?
		      JTable.AUTO_RESIZE_ALL_COLUMNS :
		      JTable.AUTO_RESIZE_OFF);
	_prefs.putBoolean("secDescending", secDesc.isSelected());
	_prefs.putBoolean("terDescending", terDesc.isSelected());
	_prefs.put("secSort", GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
	_prefs.put("terSort", GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);

    }

}

	/*
	Boolean[] sel = new Boolean[GUIGlobals.ALL_FIELDS.length];
	boolean found;
	_choices = GUIGlobals.ALL_FIELDS;
	_sel = sel;
	String[] columns = prefs.getStringArray("columnNames");
	for (int i=0; i<_choices.length; i++) {
	    found = false;
	    for (int j=0; j<columns.length; j++)
		if (columns[j].equals(_choices[i]))
		    found = true;
	    if (found)
		sel[i] = new Boolean(true);
	    else
		sel[i] = new Boolean(false);
	}

	TableModel tm = new AbstractTableModel() {
		public int getRowCount() { return (_choices.length-1)/2; }
		public int getColumnCount() { return 4; }
		public Object getValueAt(int row, int column) {
		    switch (column) {
		    case 0:
			return _choices[row];
		    case 1:
			return _sel[row];
		    case 2:
			return _choices[getRowCount()+row];
		    case 3:
			return _sel[getRowCount()+row];
		    }
		    return null; // Unreachable.
		}
		public Class getColumnClass(int column) {
		    if ((column == 0) || (column == 2)) return String.class;
		    else return Boolean.class;
		}
		public boolean isCellEditable(int row, int col) {
		    if ((col == 1) || (col == 3)) return true;
		    else return false;
		}
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		    if (columnIndex == 1)
			_sel[rowIndex] = (Boolean)aValue;
		    if (columnIndex == 3)
			_sel[getRowCount()+rowIndex] = (Boolean)aValue;
		}

	    };

	JTable table = new JTable(tm);
	table.setRowSelectionAllowed(false);
	table.setColumnSelectionAllowed(false);
	//table.getInputMap().put(GUIGlobals.exitDialog, "close");
	//table.getActionMap().put("close", new CancelAction());
	JPanel
	    tablePanel = new JPanel(),
	    innerTablePanel = new JPanel();

	table.setShowVerticalLines(false);
	innerTablePanel.setBorder(BorderFactory.createEtchedBorder());
	//innerTablePanel.setBorder(BorderFactory.createLoweredBevelBorder());
	innerTablePanel.add(table);
	tablePanel.add(innerTablePanel);


	TableColumnModel cm = table.getColumnModel();
	cm.getColumn(0).setPreferredWidth(90);
	cm.getColumn(1).setPreferredWidth(25);
	cm.getColumn(2).setPreferredWidth(90);
	cm.getColumn(3).setPreferredWidth(25);
	*/
