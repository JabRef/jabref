/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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

import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.Dimension;
import java.io.*;

public class EntryTable extends JTable {

    final int PREFERRED_WIDTH = 400, PREFERRED_HEIGHT = 30;

    JScrollPane sp = new JScrollPane((JTable)this);
    JPopupMenu rightClickMenu = null;
    EntryTableModel tableModel;
    JabRefPreferences prefs;
    protected boolean showingSearchResults = false,
	showingGroup = false;    
    private EntryTable ths = this;

    public EntryTable(EntryTableModel tm_, JabRefPreferences prefs_) {
	super(tm_);
	this.tableModel = tm_;
	prefs = prefs_;

	getTableHeader().setReorderingAllowed(false); // To prevent color bugs. Must be fixed.
	setShowVerticalLines(true);
	setShowHorizontalLines(true);
	setColumnSelectionAllowed(true);
	setAutoResizeMode(prefs.getInt("autoResizeMode"));
	DefaultCellEditor dce = new DefaultCellEditor(new JTextField());
	dce.setClickCountToStart(2);
	setDefaultEditor(String.class, dce);
	getTableHeader().addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    int col = getTableHeader().columnAtPoint(e.getPoint());
		    if (col > 0) { // A valid column, but not the first.
			String s = tableModel.getColumnName(col).toLowerCase();

			// Change sort field ...
			if (!s.equals(prefs.get("priSort")))
			    prefs.put("priSort", s); 
			// ... or change sort direction
			else prefs.putBoolean("priDescending",
				       !prefs.getBoolean("priDescending"));
			tableModel.remap();
			repaint();
		    }

							     
		}
	    });
	addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    if (e.getButton() == MouseEvent.BUTTON3) {
			if (rightClickMenu != null)
			    rightClickMenu.show(ths, e.getX(), e.getY());
		    }
							     
		}
	    });
	setWidths();
	sp.getViewport().setBackground(GUIGlobals.tableBackground);
    }

    public void setWidths() {
	// Setting column widths:

	TableColumnModel cm = getColumnModel();
	for (int i=0; i<getModel().getColumnCount(); i++) {
	    cm.getColumn(i).setPreferredWidth(GUIGlobals.getPreferredFieldLength(getModel().getColumnName(i)));
	}
    }

    public JScrollPane getPane() {
	return sp;
    }

    public void setShowingSearchResults(boolean search,
					boolean group) {
	showingSearchResults = search;
	showingGroup = group;
    }

    public void setRightClickMenu(JPopupMenu rcm) {
	rightClickMenu = rcm;
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
	// This method asks the table model whether the given cell represents a
	// required or optional field, and returns the appropriate renderer.
	int score = -3;
	if (!showingSearchResults ||
	    tableModel.nonZeroField(row, Globals.SEARCH))
	    score++;
	if (!showingGroup ||
	    tableModel.nonZeroField(row, Globals.GROUPSEARCH))
	    score+=2;

	// Now, a grayed out renderer is for entries with -1, and
	// a very grayed out one for entries with -2
	if (score < -1)
	    return veryGrayedOutRenderer;
	if (score == -1)
	    return grayedOutRenderer;

	if (!prefs.getBoolean("tableColorCodesOn"))	
	    return defRenderer;
	if (column == 0) {
	    // Return a renderer with red background if the entry is incomplete.
	    if (tableModel.isComplete(row))
		return defRenderer;
	    else {
		if (tableModel.hasCrossRef(row))
		    return maybeIncRenderer;
		else
		    return incRenderer;
	    }
	    //return (tableModel.isComplete(row) ? defRenderer: incRenderer); 
	}
	int status;
	try { // This try clause is here to contain a bug.
	    status = tableModel.getCellStatus(row, column);
	} catch (ArrayIndexOutOfBoundsException ex) {
	    return defRenderer; // This should not occur.
	}

	//if (column == 1)
	//    Util.pr(""+status);
	if (status == EntryTableModel.REQUIRED)
	    return reqRenderer;
	else if (status == EntryTableModel.OPTIONAL)
	    return optRenderer;
	else return defRenderer;
    }

    public void scrollTo(int y) {
	JScrollBar scb = sp.getVerticalScrollBar();
	scb.setValue(y*scb.getUnitIncrement(1));
    }

    public BibtexEntry[] getSelectedEntries() {
	BibtexEntry[] bes = null;
	int[] rows = getSelectedRows();
	int[] cols = getSelectedColumns();	    
	// Entries are selected if only the first or multiple
	// columns are selected.
	if (((cols.length == 1) && (cols[0] == 0)) ||
	    (cols.length > 1)) { // entryTable.getColumnCount())) {
	    if (rows.length > 0) {
		bes = new BibtexEntry[rows.length];
		for (int i=0; i<rows.length; i++) {
		    bes[i] = tableModel.db.getEntryById(tableModel.getNameFromNumber(rows[i]));
		}
		

	    }
	}
	
	return bes;
    }
	

    // The following classes define the renderers used to render required
    // and optional fields in the table. The purpose of these renderers is
    // to visualize which fields are needed for each entry.
    private DefaultTableCellRenderer defRenderer = new DefaultTableCellRenderer(); 
    private RequiredRenderer reqRenderer = new RequiredRenderer();
    private OptionalRenderer optRenderer = new OptionalRenderer();
    private IncompleteEntryRenderer incRenderer = new IncompleteEntryRenderer();
    private GrayedOutRenderer grayedOutRenderer = new GrayedOutRenderer();
    private VeryGrayedOutRenderer veryGrayedOutRenderer 
	= new VeryGrayedOutRenderer();
    private MaybeIncompleteEntryRenderer 
	maybeIncRenderer = new MaybeIncompleteEntryRenderer();

    public class RequiredRenderer extends DefaultTableCellRenderer {
	public RequiredRenderer() {
	    super();
	    setBackground(GUIGlobals.tableReqFieldBackground);
	}
    }
    public class OptionalRenderer extends DefaultTableCellRenderer {
	public OptionalRenderer() {
	    super();
	    setBackground(GUIGlobals.tableOptFieldBackground);
	}
    }    
    public class IncompleteEntryRenderer extends DefaultTableCellRenderer {
	public IncompleteEntryRenderer() {
	    super();
	    setBackground(GUIGlobals.tableIncompleteEntryBackground);
	}
    }
    public class MaybeIncompleteEntryRenderer extends DefaultTableCellRenderer {
	public MaybeIncompleteEntryRenderer() {
	    super();
	    setBackground(GUIGlobals.maybeIncompleteEntryBackground);
	}
    }

    public class GrayedOutRenderer extends DefaultTableCellRenderer {
	public GrayedOutRenderer() {
	    super();
	    setBackground(GUIGlobals.grayedOutBackground);
	    setForeground(GUIGlobals.grayedOutText);
	}
    }

    public class VeryGrayedOutRenderer extends DefaultTableCellRenderer {
	public VeryGrayedOutRenderer() {
	    super();
	    setBackground(GUIGlobals.veryGrayedOutBackground);
	    setForeground(GUIGlobals.veryGrayedOutText);
	}
    }

}
