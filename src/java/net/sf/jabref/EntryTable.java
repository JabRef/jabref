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
import java.io.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTableUI;

public class EntryTable extends JTable {

    final int PREFERRED_WIDTH = 400, PREFERRED_HEIGHT = 30;

    JScrollPane sp = new JScrollPane((JTable)this);
    JPopupMenu rightClickMenu = null;
    EntryTableModel tableModel;
    JabRefPreferences prefs;
    protected boolean showingSearchResults = false,
	showingGroup = false;
    private EntryTable ths = this;
    private boolean antialiasing = true,
        ctrlClick = false,
        selectionListenerOn = true,
        tableColorCodes = true;
    //RenderingHints renderingHints;
    private BasePanel panel;

    private ListSelectionListener previewListener = null;

    public EntryTable(EntryTableModel tm_, BasePanel panel_, JabRefPreferences prefs_) {
	super(tm_);
	this.tableModel = tm_;
        sp.setBorder(null);
        panel = panel_;
        // Add the global focus listener, so a menu item can see if this table was focused when
        // an action was called.
        addFocusListener(Globals.focusListener);


  //renderingHints = g2.getRenderingHints();
 	//renderingHints.put(RenderingHints.KEY_ANTIALIASING,
	//		   RenderingHints.VALUE_ANTIALIAS_ON);
	//renderingHints.put(RenderingHints.KEY_RENDERING,
	//		   RenderingHints.VALUE_RENDER_QUALITY);
	prefs = prefs_;
        antialiasing = prefs.getBoolean("antialias");
        ctrlClick = prefs.getBoolean("ctrlClick");
        tableColorCodes = prefs.getBoolean("tableColorCodesOn");
	getTableHeader().setReorderingAllowed(false); // To prevent color bugs. Must be fixed.
        setGridColor(GUIGlobals.gridColor);
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
            if (col >= tableModel.padleft) { // A valid column, but not the first.
              String s = tableModel.getFieldName(col);
              if (s.equals("")) {
                // The user has clicked a column with an empty header, such as the icon columns.
                // We could add sorting for these columns, but currently we do nothing.
                return;
              }
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

        addMouseListener(new TableClickListener()); // Add the listener that responds to clicks on the table.

        addSelectionListener(); // Add the listener that responds to new entry selection.
        // (to update entry editor or preview)
        setWidths();
        sp.getViewport().setBackground(GUIGlobals.tableBackground);
        updateFont();
      }

      /**
       * A ListSelectionListener for updating the preview panel when the user selects an
       * entry. Should only be active when preview is enabled.
       */
      public void addSelectionListener() {
        if (previewListener == null)
          previewListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
              if (!selectionListenerOn) return;
              if (!e.getValueIsAdjusting()) {
                if (getSelectedRowCount() == 1) {
                  int row = getSelectedRow(); //e.getFirstIndex();
                  if (row >= 0) {
                    panel.updateViewToSelected();//panel.database().getEntryById(
                        //tableModel.getNameFromNumber(row)));
                  }
                } else {
                  /* With a multiple selection, there are three alternative behaviours:
                   1. Disable the entry editor. Do not update it.
                   2. Do not disable the entry editor, and do not update it.
                   3. Update the entry editor, and keep it enabled.

                   We currently implement 1 and 2, and choose between them based on
                   prefs.getBoolean("disableOnMultipleSelection");
                   */
                  if (prefs.getBoolean("disableOnMultipleSelection")) { // 1.
                    panel.setEntryEditorEnabled(false);
                  }
                  // 2. Do nothing.
                }
              }
            }
          };
        getSelectionModel().addListSelectionListener(previewListener);
      }

      /**
       * Remove the preview listener.
       */
      public void disablePreviewListener() {
        getSelectionModel().removeListSelectionListener(previewListener);
      }

      /**
       * This method overrides the superclass' to disable the selection listener while the
       * row selection is adjusted.
       */
      public void setRowSelectionInterval(int row1, int row2) {
        boolean oldState = selectionListenerOn;
        selectionListenerOn = false;
        super.setRowSelectionInterval(row1, row2);
        selectionListenerOn = oldState;
      }

      /**
       * This method overrides the superclass' to disable the selection listener while the
       * selection is cleared.
       */
      public void clearSelection() {
        boolean oldState = selectionListenerOn;
        selectionListenerOn = false;
        super.clearSelection();
        selectionListenerOn = oldState;
      }

      /**
       * Enables or disables the selectionlistener. Useful if the selection needs to be
       * updated in several steps, without the table responding between each.
       * @param enabled boolean
       */
      public void setSelectionListenerEnabled(boolean enabled) {
        selectionListenerOn = enabled;
      }

      /**
       * Turns off any cell editing going on.
       */
      protected void assureNotEditing() {
        if (isEditing()) {
          int col = getEditingColumn(),
              row = getEditingRow();
          getCellEditor(row, col).stopCellEditing();
        }
      }


    public void setWidths() {
	// Setting column widths:
        int ncWidth = prefs.getInt("numberColWidth");
	String[] widths = prefs.getStringArray("columnWidths");
        TableColumnModel cm = getColumnModel();
        cm.getColumn(0).setPreferredWidth(ncWidth);
        for (int i=1; i<tableModel.padleft; i++) {
          // Lock the width of icon columns.
          cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
          cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
          cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
        }
	for (int i=tableModel.padleft; i<getModel().getColumnCount(); i++) {
	    try {
		cm.getColumn(i).setPreferredWidth(Integer.parseInt(widths[i-tableModel.padleft]));
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

    /*public void setShowingSearchResults(boolean search,
					boolean group) {
	showingSearchResults = search;
	showingGroup = group;
    }
*/
    public void setRightClickMenu(JPopupMenu rcm) {
	rightClickMenu = rcm;
    }

  /**
   * This class handles clicks on the EntryTable that should trigger specific
   * events, like opening an entry editor, the context menu or a pdf file.
   */
  class TableClickListener extends MouseAdapter {
      public void mouseClicked(MouseEvent e) {

        // First find the column on which the user has clicked.
        final int col = columnAtPoint(e.getPoint()),
            row = rowAtPoint(e.getPoint());


	// A double click on an entry should open the entry's editor.
        if (/*(col == 0)*/!isCellEditable(row, col) && (e.getClickCount() == 2)) {
          try{ panel.runCommand("edit");
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        }

        // Check if the user has right-clicked. If so, open the right-click menu.
        if ( (e.getButton() == MouseEvent.BUTTON3) ||
             (ctrlClick && (e.getButton() == MouseEvent.BUTTON1) && e.isControlDown())) {
          rightClickMenu = new RightClickMenu(panel, panel.metaData);
          rightClickMenu.show(ths, e.getX(), e.getY());
        }

        // Check if the user has clicked on an icon cell to open url or pdf.
        if (tableModel.getCellStatus(0, col) == EntryTableModel.ICON_COL) {

          // Get the row number also:
          Object value = getValueAt(row, col);
          if (value == null) return; // No icon here, so we do nothing.
          /*Util.pr("eouaeou");
          JButton button = (JButton)value;

          MouseEvent buttonEvent =
              (MouseEvent)SwingUtilities.convertMouseEvent(ths, e, button);
          button.dispatchEvent(buttonEvent);
          // This is necessary so that when a button is pressed and released
          // it gets rendered properly.  Otherwise, the button may still appear
          // pressed down when it has been released.
          ths.repaint();

          */



          // Get the icon type. Corresponds to the field name.
          final String[] iconType = tableModel.getIconTypeForColumn(col);
          int hasField = -1;
          for (int i=iconType.length-1; i>= 0; i--)
            if (tableModel.hasField(row, iconType[i]))
              hasField = i;
          if (hasField == -1)
            return;
          final String fieldName = iconType[hasField];

          // Open it now. We do this in a thread, so the program won't freeze during the wait.
          (new Thread() {
            public void run() {
              panel.output(Globals.lang("External viewer called") + ".");
              BibtexEntry be = panel.database().getEntryById(tableModel.
                  getNameFromNumber(row));
              if (be == null) {
                Globals.logger("Error: could not find entry.");
                return;
              }

              Object link = be.getField(fieldName);
              if (iconType == null) {
                Globals.logger("Error: no link to " + fieldName + ".");
                return; // There is an icon, but the field is not set.
              }

              try {
                Util.openExternalViewer( (String) link, fieldName, prefs);
              }
              catch (IOException ex) {
                panel.output(Globals.lang("Error")+": "+ex.getMessage());
              }
            }

          }).start();
        }
      }
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
	// This method asks the table model whether the given cell represents a
	// required or optional field, and returns the appropriate renderer.
	int score = -3;
	TableCellRenderer renderer;

	int status;
	try { // This try clause is here to contain a bug.
          status = tableModel.getCellStatus(row, column);
	} catch (ArrayIndexOutOfBoundsException ex) {
	    Globals.logger("Error happened in getCellRenderer method of EntryTable, for cell ("+row+","+column+").");
	    return defRenderer; // This should not occur.
	}

        // For testing MARKED feature:
        if (tableModel.hasField(row, Globals.MARKED)) {
          return markedRenderer;
        }

	if (!panel.coloringBySearchResults ||
	    tableModel.nonZeroField(row, Globals.SEARCH))
	    score++;
	if (!panel.coloringByGroup ||
	    tableModel.nonZeroField(row, Globals.GROUPSEARCH))
	    score+=2;

	// Now, a grayed out renderer is for entries with -1, and
	// a very grayed out one for entries with -2
	if (score < -1)
	    renderer = veryGrayedOutRenderer;
	else if (score == -1)
	    renderer = grayedOutRenderer;

	else if (!tableColorCodes)
	    renderer = defRenderer;
	else if (column == 0) {
	    // Return a renderer with red background if the entry is incomplete.
	    renderer = defRenderer;
	    /*if (tableModel.isComplete(row))
		renderer = defRenderer;
	    else {
		if (tableModel.hasCrossRef(row))
		    renderer = maybeIncRenderer;
		else
		    renderer = incRenderer;
		    }*/





	    //return (tableModel.isComplete(row) ? defRenderer: incRenderer);
	}
        //else if (status == EntryTableModel.ICON_COL)
        //  renderer = iconRenderer;
	else if (status == EntryTableModel.REQUIRED)
	    renderer = reqRenderer;
	else if (status == EntryTableModel.OPTIONAL)
	    renderer = optRenderer;
	else renderer = defRenderer;
        //Util.pr("("+row+","+column+"). "+status+" "+renderer.toString());
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
        maybeIncRenderer = new Renderer(GUIGlobals.maybeIncompleteEntryBackground),
        markedRenderer = new Renderer(GUIGlobals.markedEntryBackground);

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

        public void firePropertyChange(String propertyName, boolean old, boolean newV) {
        }
        public void firePropertyChange(String propertyName, Object old, Object newV) {
        }

	/* For enabling the renderer to handle icons. */
        protected void setValue(Object value) {
            if (value instanceof Icon) {
                setIcon((Icon)value);
                super.setValue(null);
            } else if (value instanceof JLabel) {
              JLabel lab = (JLabel)value;
              super.setIcon(lab.getIcon());
              super.setToolTipText(lab.getToolTipText());
              super.setText(null);
            } else {
                setIcon(null);
                super.setToolTipText(null);
                super.setValue(value);
            }
	}

    //public void paintComponent(Graphics g) {
    public void paint(Graphics g) {
	//Util.pr("her");

	Graphics2D g2 = (Graphics2D)g;
	//Font f = g2.getFont();//new Font("Plain", Font.PLAIN, 24);
	//g2.setColor(getBackground());
	//g2.fill(g2.getClipBounds());
	//g2.setColor(getForeground());
	//g2.setFont(f);
	if (antialiasing) {
	    RenderingHints rh = g2.getRenderingHints();
	    rh.put(RenderingHints.KEY_ANTIALIASING,
		   RenderingHints.VALUE_ANTIALIAS_ON);
	    rh.put(RenderingHints.KEY_RENDERING,
		   RenderingHints.VALUE_RENDER_QUALITY);
	    g2.setRenderingHints(rh);
	}

        ui.update(g2, this);

	//super.paintComponent(g2);
      }

	//public DefaultTableCellRenderer darker() { return darker; }
    }

    /* public TableCellRenderer iconRenderer = new IconCellRenderer();
        //new JTableButtonRenderer(getDefaultRenderer(JButton.class));
    class IconCellRenderer extends DefaultTableCellRenderer {
        protected void setValue(Object value) {
            if (value instanceof Icon) {
                setIcon((Icon)value);
                super.setValue(null);
            } else {
                setIcon(null);
                super.setValue(value);
            }
        }
    }


   class JTableButtonRenderer implements TableCellRenderer {
      private TableCellRenderer __defaultRenderer;

      public JTableButtonRenderer(TableCellRenderer renderer) {
        __defaultRenderer = renderer;
      }

      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row, int column)
      {
        if(value instanceof Component)
          return (Component)value;
        return __defaultRenderer.getTableCellRendererComponent(
      table, value, isSelected, hasFocus, row, column);
      }
    }*/


    public void ensureVisible(int row) {
	JScrollBar vert = sp.getVerticalScrollBar();
	int y = row*getRowHeight();
	if ((y < vert.getValue()) || (y > vert.getValue()+vert.getVisibleAmount()))
	    scrollToCenter(row, 1);
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

  public void updateUI() {
      super.updateUI();
      setUI(new CustomTableUI());
  }



  class CustomTableUI extends BasicTableUI {
    public void installUI(JComponent c) {
      super.installUI(c);
      c.remove(rendererPane);
      rendererPane = new CustomCellRendererPane();
      c.add(rendererPane);
    }

    /**
     * Overrides paintComponent to NOT clone the Graphics
     * passed in and NOT validate the Component passed in.
     */
    private class CustomCellRendererPane extends CellRendererPane {
        private Rectangle tmpRect = new Rectangle();

        public void repaint() {
        }

        public void repaint(int x, int y, int width, int height) {
        }

        public void paintComponent(Graphics g, Component c, Container p,
                                   int x, int y, int w, int h,
                                   boolean shouldValidate) {
          if (c == null) {
            if (p != null) {
              Color oldColor = g.getColor();
              g.setColor(p.getBackground());
              g.fillRect(x, y, w, h);
              g.setColor(oldColor);
            }
            return;
          }
          if (c.getParent() != this) {
            this.add(c);
          }

          c.setBounds(x, y, w, h);

          boolean wasDoubleBuffered = false;
          JComponent jc = (c instanceof JComponent) ? (JComponent)c : null;
          if (jc != null && jc.isDoubleBuffered()) {
            wasDoubleBuffered = true;
            jc.setDoubleBuffered(false);
          }

          // Don't create a new Graphics, reset the clip and translate
          // the origin.
          Rectangle clip = g.getClipBounds(tmpRect);
          g.clipRect(x, y, w, h);
          g.translate(x, y);
          c.paint(g);
          g.translate(-x, -y);
          g.setClip(clip.x, clip.y, clip.width, clip.height);
          if (wasDoubleBuffered) {
            jc.setDoubleBuffered(true);
          }
          c.setBounds(-w, -h, 0, 0);
        }
      }

    }

  }

