package net.sf.jabref;

import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;
import net.sf.jabref.gui.ColorSetupPanel;

class TablePrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private String[] _choices;
    private Boolean[] _sel;
    private JCheckBox colorCodes, autoResizeMode, secDesc, terDesc,
    antialias, pdfColumn, urlColumn, citeseerColumn, abbrNames;
    private JRadioButton namesAsIs, namesFf, namesFl, namesNatbib;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    private JComboBox
    secSort = new JComboBox(GUIGlobals.ALL_FIELDS),
    terSort = new JComboBox(GUIGlobals.ALL_FIELDS);
    private JTextField secField, terField, fontSize;
    private JButton fontButton = new JButton(Globals.lang("Set table font"));
    private ColorSetupPanel colorPanel = new ColorSetupPanel();
    private boolean tableChanged = false;
    private Font font = GUIGlobals.CURRENTFONT,	menuFont;
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



        colorCodes = new JCheckBox(Globals.lang
                   ("Color codes for required and optional fields"));
        antialias = new JCheckBox(Globals.lang
                  ("Use antialiasing font in table"));
        autoResizeMode = new JCheckBox(Globals.lang
                       ("Fit table horizontally on screen"));

        namesAsIs = new JRadioButton(Globals.lang("Show names unchanged"));
        namesFf = new JRadioButton(Globals.lang("Show 'Firstname Lastname'"));
        namesFl = new JRadioButton(Globals.lang("Show 'Lastname, Firstname'"));
        namesNatbib = new JRadioButton(Globals.lang("Natbib style"));
        abbrNames = new JCheckBox(Globals.lang("Abbreviate names"));
        pdfColumn = new JCheckBox(Globals.lang("Show PDF/PS column"));
        urlColumn = new JCheckBox(Globals.lang("Show URL/DOI column"));
        citeseerColumn = new JCheckBox(Globals.lang("Show CiteSeer column"));

        secField = new JTextField(10);
        terField = new JTextField(10);

        secSort.insertItemAt(Globals.lang("<select>"), 0);
        terSort.insertItemAt(Globals.lang("<select>"), 0);

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
    builder.append(pan); builder.append(namesAsIs); builder.nextLine();
    builder.append(pan); builder.append(namesFf); builder.nextLine();
    builder.append(pan); builder.append(namesFl); builder.nextLine();
    builder.append(pan); builder.append(namesNatbib); builder.nextLine();
    builder.append(pan); builder.append(abbrNames); builder.nextLine();
    builder.appendSeparator(Globals.lang("Sort options"));
    // Create a new panel with its own FormLayout for these items:
    FormLayout layout2 = new FormLayout
        ("left:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
    DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
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
    builder.appendSeparator(Globals.lang("Table appearance"));
    builder.append(pan); builder.append(colorCodes); builder.nextLine();
    builder.append(pan); builder.append(autoResizeMode); builder.nextLine();
    builder.append(pan); builder.append(antialias); builder.nextLine();
    builder.append(pan); builder.append(fontButton); builder.nextLine();
    builder.append(pan); builder.append(colorPanel);
    //	builder.append(pan); builder.append(); builder.nextLine();

    JPanel upper = new JPanel(),
        sort = new JPanel(),
        namesp = new JPanel(),
            iconCol = new JPanel();
    upper.setLayout(gbl);
    sort.setLayout(gbl);
        namesp.setLayout(gbl);
        iconCol.setLayout(gbl);
    /*
     con.gridwidth = GridBagConstraints.REMAINDER;
     con.fill = GridBagConstraints.NONE;
     con.anchor = GridBagConstraints.WEST;
     gbl.setConstraints(colorCodes, con);
     upper.add(colorCodes);
     gbl.setConstraints(autoResizeMode, con);
     upper.add(autoResizeMode);
         gbl.setConstraints(antialias, con);
         upper.add(antialias);
         con.gridwidth = 1;
     lab = new JLabel(Globals.lang("Menu and label font size"));
     gbl.setConstraints(lab, con);
     upper.add(lab);
     Insets old = con.insets;
     con.insets = new Insets(0, 5, 0, 5);
     gbl.setConstraints(fontSize, con);
     upper.add(fontSize);
     con.insets = old;
         con.gridwidth = GridBagConstraints.REMAINDER;
     lab = new JLabel("("+Globals.lang("non-Mac only")+")");
     gbl.setConstraints(lab, con);
     upper.add(lab);
     //gbl.setConstraints(menuFontButton, con);
     //upper.add(menuFontButton);
     //con.anchor = GridBagConstraints.EAST;
         con.gridwidth = GridBagConstraints.REMAINDER;
     gbl.setConstraints(fontButton, con);
     upper.add(fontButton);
     con.anchor = GridBagConstraints.WEST;
     con.fill = GridBagConstraints.BOTH;
         con.gridwidth = 1;
         con.gridheight = 2;
     gbl.setConstraints(upper, con);
     //add(upper);
         con.gridheight = 1;
         con.gridwidth = GridBagConstraints.REMAINDER;
         gbl.setConstraints(pdfColumn, con);
         iconCol.add(pdfColumn);
         gbl.setConstraints(urlColumn, con);
         iconCol.add(urlColumn);
         gbl.setConstraints(citeseerColumn, con);
         iconCol.add(citeseerColumn);
         con.fill = GridBagConstraints.BOTH;
         gbl.setConstraints(iconCol, con);
     add(iconCol);


     con.gridwidth = GridBagConstraints.REMAINDER;
     con.fill = GridBagConstraints.NONE;
     con.anchor = GridBagConstraints.WEST;
     gbl.setConstraints(namesAsIs, con);
     namesp.add(namesAsIs);
     gbl.setConstraints(namesFf, con);
     namesp.add(namesFf);
     gbl.setConstraints(namesFl, con);
     namesp.add(namesFl);
         con.fill = GridBagConstraints.BOTH;
         gbl.setConstraints(namesp, con);
     add(namesp);*/



    // Set the correct value for the primary sort JComboBox.
    /*String sec = prefs.get("secSort"),
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
         gbl.setConstraints(secField, con);
         sort.add(secField);
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
         gbl.setConstraints(terField, con);
         sort.add(terField);
     con.weightx = 1;
     con.gridwidth = GridBagConstraints.REMAINDER;
     gbl.setConstraints(terDesc, con);
     sort.add(terDesc);*/


    fontButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // JDialog dl = new EntryCustomizationDialog(ths);
            Font f=new FontSelectorDialog
                (null, GUIGlobals.CURRENTFONT).getSelectedFont();
            if(f==null)
                return;
            else
                font = f;
        }
        });
    /*menuFontButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             Font f=new FontSelectorDialog
                 (null, menuFont).getSelectedFont();
             if(f==null)
                 return;
             else
                 menuFont = f;
         }
         });*/

    pan = builder.getPanel();
    pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    add(pan, BorderLayout.CENTER);
    }

    public void setValues() {
    menuFont = new Font
        (_prefs.get("menuFontFamily"), _prefs.getInt("menuFontStyle"),
         _prefs.getInt("menuFontSize"));
    colorCodes.setSelected(_prefs.getBoolean("tableColorCodesOn"));
    antialias.setSelected(_prefs.getBoolean("antialias"));
    autoResizeMode.setSelected((_prefs.getInt("autoResizeMode")==JTable.AUTO_RESIZE_ALL_COLUMNS));
    pdfColumn.setSelected(_prefs.getBoolean("pdfColumn"));
    urlColumn.setSelected(_prefs.getBoolean("urlColumn"));
    citeseerColumn.setSelected(_prefs.getBoolean("citeseerColumn"));

    secField.setText(_prefs.get("secSort"));
    terField.setText(_prefs.get("terSort"));
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
    abbrNames.setSelected(_prefs.getBoolean("abbrAuthorNames"));
    secDesc.setSelected(_prefs.getBoolean("secDescending"));
    terDesc.setSelected(_prefs.getBoolean("terDescending"));
        colorPanel.setValues();
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    public void storeSettings() {

    _prefs.putBoolean("tableColorCodesOn", colorCodes.isSelected());
    _prefs.putBoolean("namesAsIs", namesAsIs.isSelected());
    _prefs.putBoolean("namesFf", namesFf.isSelected());
    _prefs.putBoolean("namesNatbib", namesNatbib.isSelected());
    _prefs.putBoolean("abbrAuthorNames", abbrNames.isSelected());

        _prefs.putBoolean("antialias", antialias.isSelected());
        _prefs.putBoolean("pdfColumn", pdfColumn.isSelected());
        _prefs.putBoolean("urlColumn", urlColumn.isSelected());
        _prefs.putBoolean("citeseerColumn", citeseerColumn.isSelected());
    _prefs.putInt("autoResizeMode",
              autoResizeMode.isSelected() ?
              JTable.AUTO_RESIZE_ALL_COLUMNS :
              JTable.AUTO_RESIZE_OFF);
    _prefs.putBoolean("secDescending", secDesc.isSelected());
    _prefs.putBoolean("terDescending", terDesc.isSelected());
    //_prefs.put("secSort", GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
    //_prefs.put("terSort", GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);
        _prefs.put("secSort", secField.getText().toLowerCase().trim());
        _prefs.put("terSort", terField.getText().toLowerCase().trim());
    // updatefont
    _prefs.put("fontFamily", font.getFamily());
    _prefs.putInt("fontStyle", font.getStyle());
    _prefs.putInt("fontSize", font.getSize());
    //_prefs.put("menuFontFamily", menuFont.getFamily());
    //_prefs.putInt("menuFontStyle", menuFont.getStyle());
    //_prefs.putInt("menuFontSize", menuFont.getSize());

    GUIGlobals.CURRENTFONT = font;
        colorPanel.storeSettings();
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
