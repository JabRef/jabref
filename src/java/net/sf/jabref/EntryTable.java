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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

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
	//setColumnSelectionAllowed(true);
	setColumnSelectionAllowed(false);
	setRowSelectionAllowed(true);
	setAutoResizeMode(prefs.getInt("autoResizeMode"));
	DefaultCellEditor dce = new DefaultCellEditor(new JTextField());
	dce.setClickCountToStart(2);
	setDefaultEditor(String.class, dce);
	getTableHeader().addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    int col = getTableHeader().columnAtPoint(e.getPoint());
		    if (col > 0) { // A valid column, but not the first.
			String s = tableModel.getColumnName(col).toLowerCase();
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
	updateFont();
    }

    public void setWidths() {
	// Setting column widths:
	String[] widths = prefs.getStringArray("columnWidths");
	TableColumnModel cm = getColumnModel();
	cm.getColumn(0).setPreferredWidth(GUIGlobals.NUMBER_COL_LANGTH);
	for (int i=1; i<getModel().getColumnCount(); i++) {
	    try {
		cm.getColumn(i).setPreferredWidth(Integer.parseInt(widths[i-1]));
	    } catch (Throwable ex) {
		Globals.logger("Exception while setting column widths. Choosing default.");
		cm.getColumn(i).setPreferredWidth(GUIGlobals.DEFAULT_FIELD_LENGTH);
	    }
	    //cm.getColumn(i).setPreferredWidth(GUIGlobals.getPreferredFieldLength(getModel().getColumnName(i)));
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
	Renderer renderer;

	int status;
	try { // This try clause is here to contain a bug.
	    status = tableModel.getCellStatus(row, column);
	} catch (ArrayIndexOutOfBoundsException ex) {
	    Globals.logger("Error happened in getCellRenderer method of EntryTable.");
	    return defRenderer; // This should not occur.
	}


	if (!showingSearchResults ||
	    tableModel.nonZeroField(row, Globals.SEARCH))
	    score++;
	if (!showingGroup ||
	    tableModel.nonZeroField(row, Globals.GROUPSEARCH))
	    score+=2;

	// Now, a grayed out renderer is for entries with -1, and
	// a very grayed out one for entries with -2
	if (score < -1)
	    renderer = veryGrayedOutRenderer;
	else if (score == -1)
	    renderer = grayedOutRenderer;

	else if (!prefs.getBoolean("tableColorCodesOn"))
	    renderer = defRenderer;
	else if (column == 0) {
	    // Return a renderer with red background if the entry is incomplete.
	    if (tableModel.isComplete(row))
		renderer = defRenderer;
	    else {
		if (tableModel.hasCrossRef(row))
		    renderer = maybeIncRenderer;
		else
		    renderer = incRenderer;
	    }
	    //return (tableModel.isComplete(row) ? defRenderer: incRenderer);
	}

	else if (status == EntryTableModel.REQUIRED)
	    renderer = reqRenderer;
	else if (status == EntryTableModel.OPTIONAL)
	    renderer = optRenderer;
	else renderer = defRenderer;
       
	return renderer;
	
	/*
	int test = row - 4*(row/4);	
	if (test <= 1)
	    return renderer;
	else {
	    return renderer.darker();
	    }*/
    }

    public void scrollTo(int y) {
	JScrollBar scb = sp.getVerticalScrollBar();
	scb.setValue(y*scb.getUnitIncrement(1));
    }

    public BibtexEntry[] getSelectedEntries() {
	BibtexEntry[] bes = null;
	int[] rows = getSelectedRows();
	//int[] cols = getSelectedColumns();

	// Entries are selected if only the first or multiple
	// columns are selected.
	//if (((cols.length == 1) && (cols[0] == 0)) ||
	//(cols.length > 1)) { // entryTable.getColumnCount())) {
	if (rows.length > 0) {
	    bes = new BibtexEntry[rows.length];
	    for (int i=0; i<rows.length; i++) {
		bes[i] = tableModel.db.getEntryById(tableModel.getNameFromNumber(rows[i]));
	    }
	}
	return bes;
    }


    // The following classes define the renderers used to render required
    // and optional fields in the table. The purpose of these renderers is
    // to visualize which fields are needed for each entry.
    private Renderer defRenderer = new Renderer(GUIGlobals.tableBackground),
	reqRenderer = new Renderer(GUIGlobals.tableReqFieldBackground),
	optRenderer = new Renderer(GUIGlobals.tableOptFieldBackground),
	incRenderer = new Renderer(GUIGlobals.tableIncompleteEntryBackground),
	grayedOutRenderer = new Renderer(GUIGlobals.grayedOutBackground,
					 GUIGlobals.grayedOutText),
	veryGrayedOutRenderer = new Renderer(GUIGlobals.veryGrayedOutBackground,
					     GUIGlobals.veryGrayedOutText),
	maybeIncRenderer = new Renderer(GUIGlobals.maybeIncompleteEntryBackground);

    private class Renderer extends DefaultTableCellRenderer {
	//private DefaultTableCellRenderer darker;
	public Renderer(Color c) {
	    super();
	    setBackground(c);


	    /*
	    darker = new DefaultTableCellRenderer();
	    double adj = 0.9;
	    darker.setBackground(new Color((int)((double)c.getRed()*adj),
					   (int)((double)c.getGreen()*adj),
					   (int)((double)c.getBlue()*adj)));
	    */
	}
	public Renderer(Color c, Color fg) {
	    this(c);
	    setForeground(fg);
	}
	
    public void paint(Graphics g) {
	//Util.pr("her");
	
	Graphics2D g2 = (Graphics2D)g;
	Font f = g2.getFont();//new Font("Plain", Font.PLAIN, 24);
	g2.setColor(getBackground());
	g2.fill(g2.getClipBounds());
	g2.setColor(getForeground());
	//g2.setFont(f);
	RenderingHints rh = g2.getRenderingHints();
	rh.put(RenderingHints.KEY_ANTIALIASING,
		RenderingHints.VALUE_ANTIALIAS_ON);
	g2.setRenderingHints(rh);
	//g2.drawString(getText(), 3, f.getSize());
	super.paint(g2);
	}

	//public DefaultTableCellRenderer darker() { return darker; }
    }


	public void scrollToCenter( int rowIndex, int vColIndex) {
        if (!(this.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport)this.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = this.getCellRect(rowIndex, vColIndex, true);

        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x-viewRect.x, rect.y-viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width-rect.width)/2;
        int centerY = (viewRect.height-rect.height)/2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);

	revalidate();
	repaint();
    }

  /**
   * updateFont
   */
  public void updateFont() {
      setFont(GUIGlobals.CURRENTFONT);
      setRowHeight(GUIGlobals.TABLE_ROW_PADDING+GUIGlobals.CURRENTFONT.getSize());
  }
	
}
