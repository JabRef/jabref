package net.sf.jabref;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

class TablePrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private String[] _choices;
    private Boolean[] _sel;
    private JCheckBox colorCodes, autoResizeMode;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();


    public TablePrefsTab(JabRefPreferences prefs) {
	_prefs = prefs;

	setLayout(gbl);

	colorCodes = new JCheckBox(Globals.lang
				   ("Use color codes for required and optional fields")
				   ,_prefs.getBoolean("tableColorCodesOn"));
	autoResizeMode = new JCheckBox(Globals.lang
				       ("Fit table horizontally on screen"),
				       (_prefs.getInt("autoResizeMode")==JTable.AUTO_RESIZE_ALL_COLUMNS));

	JPanel upper = new JPanel();
	upper.setBorder(BorderFactory.createTitledBorder
			(BorderFactory.createEtchedBorder(), 
			 Globals.lang("Table appearance")));
	upper.setLayout(gbl);
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
	tablePanel.setBorder(BorderFactory.createTitledBorder
			     (BorderFactory.createEtchedBorder(),
			      Globals.lang("Visible fields")));
	innerTablePanel.add(table);
	tablePanel.add(innerTablePanel);


	TableColumnModel cm = table.getColumnModel();
	cm.getColumn(0).setPreferredWidth(90);
	cm.getColumn(1).setPreferredWidth(25);
	cm.getColumn(2).setPreferredWidth(90);
	cm.getColumn(3).setPreferredWidth(25);

	gbl.setConstraints(tablePanel, con);
	add(tablePanel);
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

	    _prefs.putStringArray("columnNames", getChoices());
	    _prefs.putBoolean("tableColorCodesOn", colorCodes.isSelected());
	    _prefs.putInt("autoResizeMode",
			  autoResizeMode.isSelected() ?
			  JTable.AUTO_RESIZE_ALL_COLUMNS :
			  JTable.AUTO_RESIZE_OFF);

    }
}
