/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class PrefsDialog extends JDialog {

    private String[] _choices;
    private Boolean[] _sel;
    private JabRefPreferences _prefs;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    private JCheckBox colorCodes, autoResizeMode, autoOpenForm,
	backup, openLast, secDesc, terDesc, defSource, editSource,
	autoComplete, groupsVisible;
    private JTextField groupsField;
    private JPopupMenu complFields = new JPopupMenu();
    private JCheckBoxMenuItem[] sel_ac = 
	new JCheckBoxMenuItem[GUIGlobals.ALL_FIELDS.length];
    private JButton openAutoComp = new JButton(Globals.lang("Choose fields"));
    private JabRefFrame parent;

    private String[] sizes = new String[] {
	Globals.lang("Small"),
	Globals.lang("Medium"),
	Globals.lang("Large") };
							  
    private JComboBox 
	formWidth = new JComboBox(sizes),
	formHeight = new JComboBox(sizes),
	secSort = new JComboBox(GUIGlobals.ALL_FIELDS),
	terSort = new JComboBox(GUIGlobals.ALL_FIELDS);

    public static void showPrefsDialog(JabRefFrame parent, 
				       JabRefPreferences prefs) {
	PrefsDialog cd = new PrefsDialog(parent, prefs);
	Dimension ds = cd.getSize(), df = parent.getSize();		
	Point pf = parent.getLocation();
	cd.setLocation(new Point(pf.x+(df.width-ds.width)/2,
				  pf.y+(df.height-ds.height)/2));

	cd.setVisible(true);
    }

    public PrefsDialog(JabRefFrame parent, JabRefPreferences prefs) {
	super(parent, Globals.lang("JabRef preferences"), true);
	this.parent = parent;
	_prefs = prefs;

	colorCodes = new JCheckBox("Use color codes for required and optional fields",_prefs.getBoolean("tableColorCodesOn"));
	autoResizeMode = new JCheckBox("Always resize table horizontally to fit on screen",(_prefs.getInt("autoResizeMode")==JTable.AUTO_RESIZE_ALL_COLUMNS));
	autoOpenForm = new JCheckBox("Automatically open editor when creating a new entry",_prefs.getBoolean("autoOpenForm"));
	backup = new JCheckBox("Backup old file when saving",_prefs.getBoolean("backup"));
	openLast = new JCheckBox("As default, open latest edited database on startup",_prefs.getBoolean("openLastEdited"));
	secDesc = new JCheckBox("Descending",_prefs.getBoolean("secDescending"));
	terDesc = new JCheckBox("Descending",_prefs.getBoolean("terDescending"));
	defSource = new JCheckBox("Show source by default",_prefs.getBoolean("defaultShowSource"));
	editSource = new JCheckBox("Enable source editing",_prefs.getBoolean("enableSourceEditing"));
	autoComplete = new JCheckBox("Enable autocompletion",_prefs.getBoolean("autoComplete"));
	groupsVisible = new JCheckBox("Show groups interface if "
				      +"database has groups defined",
				      _prefs.getBoolean("groupSelectorVisible"));
	groupsField = new JTextField(_prefs.get("groupsDefaultField"));
	// Make ESC close dialog, equivalent to clicking Cancel.
	colorCodes.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
	    .put(GUIGlobals.exitDialog, "close");
	colorCodes.getActionMap().put("close", new CancelAction());

	formWidth.setEditable(false);

	Boolean[] sel = new Boolean[GUIGlobals.ALL_FIELDS.length];
	boolean found, found_ac;
	_choices = GUIGlobals.ALL_FIELDS;
	_sel = sel;
	byte[] autoCompFields = prefs.getByteArray("autoCompFields");
	String[] columns = prefs.getStringArray("columnNames");
	for (int i=0; i<_choices.length; i++) {
	    found = false;
	    found_ac = false;
	    for (int j=0; j<columns.length; j++)
		if (columns[j].equals(_choices[i])) 
		    found = true;
	    for (int j=0; j<autoCompFields.length; j++) 
		if (GUIGlobals.ALL_FIELDS[autoCompFields[j]].equals(_choices[i])) 
		    found_ac = true;
	    
	    if (found)
		sel[i] = new Boolean(true);
	    else
		sel[i] = new Boolean(false);
	    if (found_ac)
		sel_ac[i] = new JCheckBoxMenuItem
		    (GUIGlobals.ALL_FIELDS[i], true);
	    else
		sel_ac[i] = new JCheckBoxMenuItem
		    (GUIGlobals.ALL_FIELDS[i], false);
	}

	// Set up the popupmenu for choosing autocompleted fields.
	for (int i=0; i<_choices.length; i++) {
	    String text = sel_ac[i].getText();
	    if ((!text.equals("bibtexkey")) && (!text.equals("search"))) 
		complFields.add(sel_ac[i]);
	}
	openAutoComp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JButton src = (JButton)e.getSource();
		    complFields.show(src, 0, 0);
		}
	    });


	// Set the correct value for the primary sort JComboBox.
	String sec = prefs.get("secSort"),
	    ter = prefs.get("terSort");
	for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++) {
	    if (sec.equals(GUIGlobals.ALL_FIELDS[i]))
		secSort.setSelectedIndex(i);
	    if (ter.equals(GUIGlobals.ALL_FIELDS[i]))
		terSort.setSelectedIndex(i);
	}


	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    (new CancelAction()).actionPerformed(null);
		}	
	    });

	TableModel tm = new AbstractTableModel() {
		public int getRowCount() { return _choices.length; }
		public int getColumnCount() { return 2; }
		public Object getValueAt(int row, int column) {
		    if (column == 0)
			return _choices[row];
		    else return _sel[row];
		}
		public Class getColumnClass(int column) {
		    if (column == 0) return String.class;
		    else return Boolean.class;
		}
		public boolean isCellEditable(int row, int col) {
		    if (col > 0) return true;
		    else return false;
		}
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		    if (columnIndex == 1)
			_sel[rowIndex] = (Boolean)aValue;
		}

	    };

	JTable table = new JTable(tm);
	table.setRowSelectionAllowed(false);
	table.setColumnSelectionAllowed(false);
	table.getInputMap().put(GUIGlobals.exitDialog, "close");
	table.getActionMap().put("close", new CancelAction());

	TableColumnModel cm = table.getColumnModel();
	cm.getColumn(0).setPreferredWidth(90);
	cm.getColumn(1).setPreferredWidth(25);

	JLabel lab;
	JPanel 
	    upper = new JPanel(),
	    lower = new JPanel(),
	    right = new JPanel(),
	    general = new JPanel(),
	    rightlow = new JPanel(),
	    tablePanel = new JPanel(),
	    innerTablePanel = new JPanel(),
	    sort = new JPanel(),
	    source = new JPanel(),
	    autoComp = new JPanel(),
	    groups = new JPanel();
	//table.setBorder(BorderFactory.createEtchedBorder());
	table.setShowVerticalLines(false);
	innerTablePanel.setBorder(BorderFactory.createEtchedBorder());
	//innerTablePanel.setBorder(BorderFactory.createLoweredBevelBorder());
	tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Visible fields"));
	right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Table appearance"));
	general.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "General options"));
	sort.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sort options"));
	source.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Edit/view bibtex source"));
	autoComp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Autocomplete options"));
	groups.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Groups options"));
	formHeight.setSelectedIndex(prefs.getInt("entryTypeFormHeightFactor"));
	formWidth.setSelectedIndex(prefs.getInt("entryTypeFormWidth"));
	innerTablePanel.setLayout(new GridLayout(1,1));
	innerTablePanel.add(table);
	tablePanel.add(innerTablePanel);
	upper.setLayout(gbl);
	right.setLayout(gbl);
	general.setLayout(gbl);
	sort.setLayout(gbl);
	source.setLayout(gbl);
	autoComp.setLayout(gbl);
	groups.setLayout(gbl);

	con.gridheight = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(tablePanel, con);
	upper.add(tablePanel);
	con.gridheight = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(right, con);
	upper.add(right);
	gbl.setConstraints(general, con);
	upper.add(general);
	gbl.setConstraints(sort, con);
	upper.add(sort);
     	gbl.setConstraints(source, con);
	upper.add(source);
     	gbl.setConstraints(autoComp, con);
	upper.add(autoComp);
     	gbl.setConstraints(groups, con);
	upper.add(groups);

	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(colorCodes, con);
	right.add(colorCodes);
	gbl.setConstraints(autoResizeMode, con);
	right.add(autoResizeMode);
	gbl.setConstraints(autoOpenForm, con);
	general.add(autoOpenForm);
	con.gridwidth = 1;
	con.anchor = GridBagConstraints.WEST;
	con.weightx = 0;
	gbl.setConstraints(formWidth, con);
	general.add(formWidth);
	lab = new JLabel("Secondary sort criterion");
	gbl.setConstraints(lab, con);
	sort.add(lab);

	con.weightx = 1;
	con.insets = new Insets(0,5,0,0);
	gbl.setConstraints(secSort, con);
	sort.add(secSort);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(secDesc, con);
	sort.add(secDesc);

	lab = new JLabel("Default width of entry editor");
	gbl.setConstraints(lab, con);
	general.add(lab);
	con.gridwidth = 1;
	con.anchor = GridBagConstraints.WEST;
	con.weightx = 0;
	con.insets = new Insets(0,0,0,0);
	gbl.setConstraints(formHeight, con);
	general.add(formHeight);
	con.weightx = 1;
 	lab = new JLabel("Tertiary sort criterion");
	gbl.setConstraints(lab, con);
	sort.add(lab);
	con.weightx = 0;
	con.insets = new Insets(0,5,0,0);
	gbl.setConstraints(terSort, con);
	sort.add(terSort);
	con.weightx = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(terDesc, con);
	sort.add(terDesc);
	lab = new JLabel("Default height of entry editor");
	gbl.setConstraints(lab, con);
	general.add(lab);

	con.insets = new Insets(0,0,0,0);
	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(backup, con);
	general.add(backup);
	gbl.setConstraints(openLast, con);
	general.add(openLast);
	lab = new JLabel("Primary sort criterion is set by clicking on column headers.");
	gbl.setConstraints(lab, con);
	sort.add(lab);

	con.insets = new Insets(0,0,0,0);
	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(defSource, con);
	source.add(defSource);
	gbl.setConstraints(editSource, con);
	source.add(editSource);

	con.gridwidth = 1;
	gbl.setConstraints(autoComplete, con);
	autoComp.add(autoComplete);       
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(openAutoComp, con);
	autoComp.add(openAutoComp);

	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(groupsVisible, con);
	groups.add(groupsVisible);       	
	lab = new JLabel("Default search field for new group:");
	con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(lab, con);
	groups.add(lab);
	gbl.setConstraints(groupsField, con);
	groups.add(groupsField);


	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(rightlow, con);
	upper.add(rightlow);

	JButton 
	    ok = new JButton("Ok"),
	    cancel = new JButton("Cancel");
	ok.addActionListener(new OkAction());
	cancel.addActionListener(new CancelAction());
	lower.add(ok);
	lower.add(cancel);
	//upper.setBackground(Color.white);
	//lower.setBackground(Color.white);
	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(upper, BorderLayout.CENTER);
	getContentPane().add(lower, BorderLayout.SOUTH);
	pack();
	setResizable(false);
    }

    public String[] getChoices() {

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

    public byte] getAcChoices() {

	// First we count how many checkboxes the user has selected
	// for which fields to autocomplete.
	int count = 0;
	for (int i=0; i<sel_ac.length; i++)
	    if (sel_ac[i].isSelected()) count++;      

	// Then we build the byte array.
	byte[] choices = new byte[count];
	count = 0;
	for (int i=0; i<sel_ac.length; i++)
	    if (sel_ac[i].isSelected()) {
		choices[count] = (byte)i;
		count++;
	    }
	
	return choices;
    }

    class OkAction extends AbstractAction {
	//PrefsDialog parent;
	public OkAction(/*PrefsDialog parent*/) {
	    super("Ok");
	    //this.parent = parent;
	}    
	public void actionPerformed(ActionEvent e) {
	    // Record changes.
	    _prefs.putStringArray("columnNames", getChoices());
	    _prefs.putBoolean("tableColorCodesOn", colorCodes.isSelected());
	    _prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
	    _prefs.putInt("autoResizeMode",
			  autoResizeMode.isSelected() ?
			  JTable.AUTO_RESIZE_ALL_COLUMNS :
			  JTable.AUTO_RESIZE_OFF);
	    _prefs.putInt("entryTypeFormHeightFactor", formHeight.getSelectedIndex());
    	    _prefs.putInt("entryTypeFormWidth", formWidth.getSelectedIndex());
	    _prefs.putBoolean("backup", backup.isSelected());
	    _prefs.putBoolean("openLastEdited", openLast.isSelected());
	    _prefs.putBoolean("secDescending", secDesc.isSelected());
	    _prefs.putBoolean("terDescending", terDesc.isSelected());
	    _prefs.put("secSort", GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
	    _prefs.put("terSort", GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);
	    _prefs.putBoolean("defaultShowSource", defSource.isSelected());
	    _prefs.putBoolean("enableSourceEditing", editSource.isSelected());
	    _prefs.putBoolean("autoComplete", autoComplete.isSelected());
	    /*if (autoComplete.isSelected()) {
		//parent.database.setCompleters(parent.autoCompleters);	    
		_prefs.putByteArray("autoCompFields", getAcChoices());
		parent.assignAutoCompleters();
		parent.updateAutoCompleters();
		}*/
	    _prefs.putBoolean("groupSelectorVisible",
			      groupsVisible.isSelected());
	    _prefs.put("groupsDefaultField", groupsField.getText());
	    parent.output("Preferences recorded.");

	    // Close dialog.
	    dispose();
	}
    }

    class CancelAction extends AbstractAction {
	public CancelAction() {
	    super("Cancel");
		  //		  new ImageIcon(_prefs.getImagePath()+GUIGlobals.closeIconFile));
	}    
	public void actionPerformed(ActionEvent e) {
	    // Just close dialog without recording changes.
	    dispose();
	}
    }
  
}
