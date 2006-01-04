package net.sf.jabref;

import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;
import net.sf.jabref.gui.ColorSetupPanel;

class TablePrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private JCheckBox autoResizeMode, priDesc, secDesc, terDesc,
    pdfColumn, urlColumn, citeseerColumn;
    private JRadioButton namesAsIs, namesFf, namesFl, namesNatbib, abbrNames, noAbbrNames, lastNamesOnly;
    private JComboBox
    priSort = new JComboBox(GUIGlobals.ALL_FIELDS),
    secSort = new JComboBox(GUIGlobals.ALL_FIELDS),
    terSort = new JComboBox(GUIGlobals.ALL_FIELDS);
    private JTextField priField, secField, terField;
    private JabRefFrame frame;


    /**
     * Customization of external program paths.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public TablePrefsTab(JabRefPreferences prefs, JabRefFrame frame) {
        _prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());



        autoResizeMode = new JCheckBox(Globals.lang
                       ("Fit table horizontally on screen"));

        namesAsIs = new JRadioButton(Globals.lang("Show names unchanged"));
        namesFf = new JRadioButton(Globals.lang("Show 'Firstname Lastname'"));
        namesFl = new JRadioButton(Globals.lang("Show 'Lastname, Firstname'"));
        namesNatbib = new JRadioButton(Globals.lang("Natbib style"));
        noAbbrNames = new JRadioButton(Globals.lang("Do not abbreviate names"));
        abbrNames = new JRadioButton(Globals.lang("Abbreviate names"));
        lastNamesOnly = new JRadioButton("Show last names only");
        pdfColumn = new JCheckBox(Globals.lang("Show PDF/PS column"));
        urlColumn = new JCheckBox(Globals.lang("Show URL/DOI column"));
        citeseerColumn = new JCheckBox(Globals.lang("Show CiteSeer column"));

        priField = new JTextField(10);
        secField = new JTextField(10);
        terField = new JTextField(10);

        priSort.insertItemAt(Globals.lang("<select>"), 0);
        secSort.insertItemAt(Globals.lang("<select>"), 0);
        terSort.insertItemAt(Globals.lang("<select>"), 0);

        priSort.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (priSort.getSelectedIndex() > 0) {
              priField.setText(GUIGlobals.ALL_FIELDS[priSort.getSelectedIndex() - 1]);
              priSort.setSelectedIndex(0);
            }
          }
        });
        secSort.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (secSort.getSelectedIndex() > 0) {
              secField.setText(GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex() - 1]);
              secSort.setSelectedIndex(0);
            }
          }
        });
        terSort.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (terSort.getSelectedIndex() > 0) {
              terField.setText(GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex() - 1]);
              terSort.setSelectedIndex(0);
            }
          }
        });

    ButtonGroup bg = new ButtonGroup();
    bg.add(namesAsIs);
    bg.add(namesNatbib);
    bg.add(namesFf);
    bg.add(namesFl);
    ButtonGroup bg2 = new ButtonGroup();
    bg2.add(lastNamesOnly);
    bg2.add(abbrNames);
    bg2.add(noAbbrNames);
    priDesc = new JCheckBox(Globals.lang("Descending"));
    secDesc = new JCheckBox(Globals.lang("Descending"));
    terDesc = new JCheckBox(Globals.lang("Descending"));

    FormLayout layout = new FormLayout
        ("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref",
         "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    JLabel lab;
    JPanel pan = new JPanel();
    builder.appendSeparator(Globals.lang("Special table columns"));
    builder.nextLine();
    builder.append(pan); builder.append(pdfColumn); builder.nextLine();
    builder.append(pan); builder.append(urlColumn); builder.nextLine();
    builder.append(pan); builder.append(citeseerColumn); builder.nextLine();
    builder.appendSeparator(Globals.lang("Format of author and editor names"));
    DefaultFormBuilder nameBuilder = new DefaultFormBuilder(
            new FormLayout("left:pref, 8dlu, left:pref",""));

    nameBuilder.append(namesAsIs); nameBuilder.append(noAbbrNames); nameBuilder.nextLine();
    nameBuilder.append(namesFf); nameBuilder.append(abbrNames); nameBuilder.nextLine();
    nameBuilder.append(namesFl); nameBuilder.append(lastNamesOnly); nameBuilder.nextLine();
    nameBuilder.append(namesNatbib);
        builder.append(pan); builder.append(nameBuilder.getPanel()); builder.nextLine();
    //builder.append(pan); builder.append(noAbbrNames); builder.nextLine();
    //builder.append(pan); builder.append(abbrNames); builder.nextLine();
    //builder.append(pan); builder.append(lastNamesOnly); builder.nextLine();

    builder.appendSeparator(Globals.lang("Default sort criteria"));
    // Create a new panel with its own FormLayout for these items:
    FormLayout layout2 = new FormLayout
        ("left:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
    DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
    lab = new JLabel(Globals.lang("Primary sort criterion"));
    builder2.append(lab);
    builder2.append(priSort);
    builder2.append(priField);
    builder2.append(priDesc);
    builder2.nextLine();
    lab = new JLabel(Globals.lang("Secondary sort criterion"));
    builder2.append(lab);
    builder2.append(secSort);
    builder2.append(secField);
    builder2.append(secDesc);
    builder2.nextLine();
    lab = new JLabel(Globals.lang("Tertiary sort criterion"));
    builder2.append(lab);
    builder2.append(terSort);
    builder2.append(terField);
    builder2.append(terDesc);
    builder.nextLine();
    builder.append(pan);
    builder.append(builder2.getPanel());
    builder.nextLine();
    builder.appendSeparator(Globals.lang("General"));
    builder.append(pan); builder.append(autoResizeMode); builder.nextLine();


    pan = builder.getPanel();
    pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    add(pan, BorderLayout.CENTER);

        namesNatbib.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                abbrNames.setEnabled(!namesNatbib.isSelected());
                lastNamesOnly.setEnabled(!namesNatbib.isSelected());
                noAbbrNames.setEnabled(!namesNatbib.isSelected());
            }
        });
    }

    public void setValues() {
    autoResizeMode.setSelected((_prefs.getInt("autoResizeMode")==JTable.AUTO_RESIZE_ALL_COLUMNS));
    pdfColumn.setSelected(_prefs.getBoolean("pdfColumn"));
    urlColumn.setSelected(_prefs.getBoolean("urlColumn"));
    citeseerColumn.setSelected(_prefs.getBoolean("citeseerColumn"));

    priField.setText(_prefs.get("priSort"));
    secField.setText(_prefs.get("secSort"));
    terField.setText(_prefs.get("terSort"));
        priSort.setSelectedIndex(0);
        secSort.setSelectedIndex(0);
        terSort.setSelectedIndex(0);

    if (_prefs.getBoolean("namesAsIs"))
        namesAsIs.setSelected(true);
    else if (_prefs.getBoolean("namesFf"))
        namesFf.setSelected(true);
    else if (_prefs.getBoolean("namesNatbib"))
        namesNatbib.setSelected(true);
    else
        namesFl.setSelected(true);
    if (_prefs.getBoolean("abbrAuthorNames"))
        abbrNames.setSelected(true);
    else if (_prefs.getBoolean("namesLastOnly"))
        lastNamesOnly.setSelected(true);
    else
        noAbbrNames.setSelected(true);
    priDesc.setSelected(_prefs.getBoolean("priDescending"));
    secDesc.setSelected(_prefs.getBoolean("secDescending"));
    terDesc.setSelected(_prefs.getBoolean("terDescending"));

        abbrNames.setEnabled(!namesNatbib.isSelected());
        lastNamesOnly.setEnabled(!namesNatbib.isSelected());
        noAbbrNames.setEnabled(!namesNatbib.isSelected());
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    public void storeSettings() {

    _prefs.putBoolean("namesAsIs", namesAsIs.isSelected());
    _prefs.putBoolean("namesFf", namesFf.isSelected());
    _prefs.putBoolean("namesNatbib", namesNatbib.isSelected());
    _prefs.putBoolean("namesLastOnly", lastNamesOnly.isSelected());
    _prefs.putBoolean("abbrAuthorNames", abbrNames.isSelected());

        _prefs.putBoolean("pdfColumn", pdfColumn.isSelected());
        _prefs.putBoolean("urlColumn", urlColumn.isSelected());
        _prefs.putBoolean("citeseerColumn", citeseerColumn.isSelected());
    _prefs.putInt("autoResizeMode",
              autoResizeMode.isSelected() ?
              JTable.AUTO_RESIZE_ALL_COLUMNS :
              JTable.AUTO_RESIZE_OFF);
    _prefs.putBoolean("priDescending", priDesc.isSelected());
    _prefs.putBoolean("secDescending", secDesc.isSelected());
    _prefs.putBoolean("terDescending", terDesc.isSelected());
    //_prefs.put("secSort", GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
    //_prefs.put("terSort", GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);
        _prefs.put("priSort", priField.getText().toLowerCase().trim());
        _prefs.put("secSort", secField.getText().toLowerCase().trim());
        _prefs.put("terSort", terField.getText().toLowerCase().trim());
    // updatefont
    }

    public boolean readyToClose() {
    return true;
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
