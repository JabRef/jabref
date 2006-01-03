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
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.*;

import net.sf.jabref.groups.EntryTableTransferHandler;

public class EntryTable extends JTable {

    final int PREFERRED_WIDTH = 400, PREFERRED_HEIGHT = 30;

    // We use a subclassed JScrollPane with setBorder() overridden as
    // a no-op. This is done to avoid the JTable setting its border,
    // which it does whether we want it or not. And we don't. :)
    JScrollPane sp = new JScrollPane((JTable)this) {
            public void setBorder(Border b) {}
        };

    JPopupMenu rightClickMenu = null;
    EntryTableModel tableModel;
    JabRefPreferences prefs;
    protected boolean showingSearchResults = false,
        showingGroup = false;
    private boolean antialiasing = Globals.prefs.getBoolean("antialias"),
        ctrlClick = false,
        selectionListenerOn = true,
        tableColorCodes = true;
    //RenderingHints renderingHints;
    private BasePanel panel;
    Set lastSelection = new HashSet();

    private ListSelectionListener previewListener = null;
    private int activeRow = -1;
    
    ListSelectionListener groupsHighlightListener;
    
    public EntryTable(EntryTableModel tm_, BasePanel panel_, JabRefPreferences prefs_) {
        super(tm_);
        this.tableModel = tm_;
        setBorder(null);
        panel = panel_;
        // Add the global focus listener, so a menu item can see if this table was focused when
        // an action was called.
        addFocusListener(Globals.focusListener);

        // enable DnD
        setDragEnabled(true);
        // The following line is commented because EntryTableTransferHandler's
	// constructor now only accepts MainTable which has replaced EntryTable.
	// setTransferHandler(new EntryTableTransferHandler(this));

  //renderingHints = g2.getRenderingHints();
         //renderingHints.put(RenderingHints.KEY_ANTIALIASING,
        //		   RenderingHints.VALUE_ANTIALIAS_ON);
        //renderingHints.put(RenderingHints.KEY_RENDERING,
        //		   RenderingHints.VALUE_RENDER_QUALITY);
        prefs = prefs_;
        //antialiasing =
        //System.out.println(antialiasing);
        ctrlClick = prefs.getBoolean("ctrlClick");
        tableColorCodes = prefs.getBoolean("tableColorCodesOn");
        getTableHeader().setReorderingAllowed(false); // To prevent color bugs. Must be fixed.
        setGridColor(Globals.prefs.getColor("gridColor"));
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
          public void mouseClicked(MouseEvent e)
          {
            int col = getTableHeader().columnAtPoint(e.getPoint());
            if (col >= 1) { //tableModel.padleft) { // A valid column, but not the first.
              String s = tableModel.getFieldName(col);
              /*
               * If the user adjusts the header size the sort event is
               * always triggered.
               * To avoid this behaviour we check if the mouse is
               * inside the label's bounds and has a certain distance (offset)
               * to the label border.
               *
               * Sascha Hunold <hunoldinho@users.sourceforge.net>
               */

              Point p = e.getPoint();
              int colindex = getTableHeader().columnAtPoint(p);
              if( colindex >= 0 ) {
                  final int initoffset = 3;
                  int xoffset = initoffset;
                  for (int i = 0; i < colindex; i++) {
                      xoffset += getColumnModel().getColumn(i).getWidth();
                  }
                  TableColumn column = getColumnModel().getColumn(col);
                  int cw = column.getWidth();
                  int ch = getTableHeader().getHeight();

                  Rectangle r = new Rectangle();

                  r.setBounds(xoffset, 0/*offset*/, cw-2*initoffset, ch/*-2*offset*/);

                  if (!r.contains(p)) {
                      return;
                  }
              }

              if (!s.equals(prefs.get("priSort"))) {
                prefs.put("priSort", s);
                  // Now, if the selected column is an icon column, set the sort to binary mode,
                  // meaning that it only separates set fields from empty fields, and does no
                  // internal sorting of set fields:
                  if (tableModel.getIconTypeForColumn(col) == null)
                      prefs.putBoolean("priBinary", false);
                  else
                      prefs.putBoolean("priBinary", true);
              }
                // ... or change sort direction
              else prefs.putBoolean("priDescending",
                                    !prefs.getBoolean("priDescending"));
              tableModel.remap();
              
            }
          }
        });

        addMouseListener(new TableClickListener()); // Add the listener that responds to clicks on the table.

        // Trying this to get better handling of the row selection stuff.
        setSelectionModel(new javax.swing.DefaultListSelectionModel() {
          public void setSelectionInterval(int index0, int index1) {
            // Prompt user here
            //Util.pr("Selection model: "+panel.entryEditorAllowsChange());
            if (panel.entryEditorAllowsChange() == false) {
              panel.moveFocusToEntryEditor();
              return;
            }
            super.setSelectionInterval(index0, index1);
          }
        });

        addSelectionListener(); // Add the listener that responds to new entry selection.

        groupsHighlightListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                /*
                if (Globals.prefs.getBoolean("highlightGroupsMatchingAny"))
                    panel.getGroupSelector().showMatchingGroups(
                            panel.getSelectedEntries(), false);
                else if (Globals.prefs.getBoolean("highlightGroupsMatchingAll"))
                    panel.getGroupSelector().showMatchingGroups(
                            panel.getSelectedEntries(), true);
                else // no highlight
                    panel.getGroupSelector().showMatchingGroups(null, true);
                    */
            }
        };
        getSelectionModel().addListSelectionListener(groupsHighlightListener);
        
        // (to update entry editor or preview)
        setWidths();
        sp.getViewport().setBackground(Globals.prefs.getColor("tableBackground"));
        updateFont();
      }

    /**
     * Get the row number for the row that is active, in the sense that the preview or
     * entry editor should show the corresponding entry.
     * @return The active row number, or -1 if no row is active.
     */
    public int getActiveRow() {
        return activeRow;
    }


    /**
     * Get the active entry, in the sense that the preview or entry editor should
     * show it.
     * @return The active entry, or null if no row is active.
     */

    public BibtexEntry getActiveEntry() {
        //System.out.println("EntryTable.getActiveEntry: "+activeRow);
        return ((activeRow >= 0) && (activeRow < getRowCount())) ? tableModel.getEntryForRow(activeRow) : null;
    }


    /**
     * Updates our Set containing the last row selection. Ckecks which rows were ADDED
     * to the selection, to see what new entry should be previewed.
     * Returns the number of the row that should be considered active, or -1 if none.
     *
     * This method may have some potential for optimization.
     *
     * @param rows
     * @return
     */
    private int resolveNewSelection(int[] rows) {
        HashSet newSel = new HashSet();
        for (int i=0; i<rows.length; i++) {
            Integer row = new Integer(rows[i]);
            newSel.add(row);
        }
        // Store a clone of this Set:
        HashSet tmp = new HashSet(newSel);
        newSel.removeAll(lastSelection);
        // Set the new selection as the last:
        lastSelection = tmp;
        // We return an appropriate row number if a single additional entry was selected:
        int result = -1;
        if (newSel.size()==1)
            result = ((Integer)newSel.iterator().next()).intValue();

        // .. or if the current selection is only one entry:
        if ((result<0) && (rows.length == 1))
            result = rows[0];
        return result;
    }

      /**
       * A ListSelectionListener for updating the preview panel when the user selects an
       * entry. Should only be active when preview is enabled.
       */
      public void addSelectionListener() {
        if (previewListener == null)
          previewListener = new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
              if (!selectionListenerOn) return;
              if (!e.getValueIsAdjusting()) {
                  // We must use invokeLater() to postpone the updating. This is because of
                  // the situation where an EntryEditor has changes in one of the FieldEditors
                  // that need to be stored. This storage is initiated by a focusLost() call,
                  // and results in a call to refreshTable() in BasePanel, which messes
                  // with the table selection. After that chain has finished, the selection
                  // will have been reset correctly, so we make sure everything is ok by
                  // putting the updating based on table selection behind it in the event queue.
                  SwingUtilities.invokeLater(new Thread() {
                          public void run() {
                              // If a single new row was selected, set it as the active row:
                              activeRow = resolveNewSelection(getSelectedRows());

                              if (getSelectedRowCount() == 1) {
                                  //int row = getSelectedRow(); //e.getFirstIndex();
                                  //if (row >= 0) {
                                      // Update the value for which entry is shown:
                                    //  activeRow = row;

                                    //panel.updateViewToSelected();
                                    // guarantee that the the entry is visible
                                    ensureVisible(activeRow);

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
                                  // We want the entry preview to update when the user expands the
                                  // selection one entry at a time:
                                  //if ((e.getLastIndex()-e.getFirstIndex()) <= 1) {
                                  //if (activeRow >= 0)
                                    //panel.updateViewToSelected();
                                  //}
                                  // 2. Do nothing.
                              }

                              if (Globals.prefs.getBoolean("highlightGroupsMatchingAny"))
                                panel.getGroupSelector().showMatchingGroups(
                                    panel.getSelectedEntries(), false);
                            else if (Globals.prefs.getBoolean("highlightGroupsMatchingAll"))
                                panel.getGroupSelector().showMatchingGroups(
                                    panel.getSelectedEntries(), true);
                            else // no highlight
                                panel.getGroupSelector().showMatchingGroups(null, true);
                          }
                      });
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
        // Introducing a try-catch here to maybe track down the preview update bug
        // that occurs sometimes (20050405 M. Alver):
        try {
            super.setRowSelectionInterval(row1, row2);
            activeRow = resolveNewSelection(getSelectedRows());
            selectionListenerOn = oldState;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            System.out.println("Error occured. Trying to recover...");
            // Maybe try to remap the entry table:
            tableModel.remap();
            clearSelection();
            selectionListenerOn = oldState;
        }
      }

      public void addRowSelectionIntervalQuietly(int row1, int row2) {
          boolean oldState = selectionListenerOn;
          selectionListenerOn = false;
          //if (row2 < getModel().getRowCount()) {
          try {
            super.addRowSelectionInterval(row1, row2);
            selectionListenerOn = oldState;
          } catch (IllegalArgumentException ex) {
              ex.printStackTrace();
              System.out.println("Error occured. Trying to recover...");
            // Maybe try to remap the entry table:
            tableModel.remap();
            clearSelection();
              selectionListenerOn = oldState;
          }

      }

    /*public boolean surrendersFocusOnKeystroke() {
        return true;
        }*/

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
      public void mouseReleased(MouseEvent e) {
          // First find the column on which the user has clicked.
          final int col = columnAtPoint(e.getPoint()),
              row = rowAtPoint(e.getPoint());
          // Check if the user has right-clicked. If so, open the right-click menu.
          if (e.isPopupTrigger()) {
            processPopupTrigger(e, row, col);
            return;
          }
      }
      protected void processPopupTrigger(MouseEvent e, int row, int col) {
          int selRow = getSelectedRow();
          if (selRow == -1 ||// (getSelectedRowCount() == 0))
                  !isRowSelected(rowAtPoint(e.getPoint()))) {
            setRowSelectionInterval(row, row);
            //panel.updateViewToSelected();
          }
          rightClickMenu = new RightClickMenu(panel, panel.metaData);
          rightClickMenu.show(EntryTable.this, e.getX(), e.getY());
      }
      public void mousePressed(MouseEvent e) {

        // First find the column on which the user has clicked.
        final int col = columnAtPoint(e.getPoint()),
            row = rowAtPoint(e.getPoint());


        // A double click on an entry should open the entry's editor.
        if (/*(col == 0)*/!isCellEditable(row, col) && (e.getClickCount() == 2)) {
          try{ panel.runCommand("edit");
              return;
              /*showEntry(be);

                    if (splitPane.getBottomComponent() != null) {
                        new FocusRequester(splitPane.getBottomComponent());
                    }                                                      */
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        }

        // Check if the user has right-clicked. If so, open the right-click menu.
        if (e.isPopupTrigger()) {
          processPopupTrigger(e, row, col);
          return;
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
                      getIdForRow(row));
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
            if (tableModel.isComplete(row))
                renderer = defRenderer;
            else {
              //if (tableModel.hasCrossRef(row))
              //  renderer = maybeIncRenderer;
              //else
              renderer = incRenderer;//incompleteEntryRenderer;
            }

            //return (tableModel.isComplete(row) ? defRenderer: incRenderer);
        }
        else if (status == EntryTableModel.REQUIRED)
            renderer = reqRenderer;
        else if (status == EntryTableModel.OPTIONAL)
            renderer = optRenderer;
        else if (status == EntryTableModel.BOOLEAN)
          renderer = getDefaultRenderer(Boolean.class);
        else renderer = defRenderer;
        //Util.pr("("+row+","+column+"). "+status+" "+renderer.toString());

        // For MARKED feature:
        if (tableModel.isMarked(row) && (renderer != incRenderer)) {
          return markedRenderer;
        }

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
                bes[i] = tableModel.db.getEntryById(tableModel.getIdForRow(rows[i]));
            }
        }
        return bes;
    }


    // The following classes define the renderers used to render required
    // and optional fields in the table. The purpose of these renderers is
    // to visualize which fields are needed for each entry.
   private GeneralRenderer defRenderer = new GeneralRenderer(Globals.prefs.getColor("tableBackground"),
            Globals.prefs.getColor("tableText"), antialiasing),
        reqRenderer = new GeneralRenderer(Globals.prefs.getColor("tableReqFieldBackground"), Globals.prefs.getColor("tableText"), antialiasing),
        optRenderer = new GeneralRenderer(Globals.prefs.getColor("tableOptFieldBackground"), Globals.prefs.getColor("tableText"), antialiasing),
        incRenderer = new IncompleteRenderer(this, antialiasing),
            //new Renderer(GUIGlobals.tableIncompleteEntryBackground),
            //Globals.lang("This entry is incomplete")),
        grayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("grayedOutBackground"),
                                         Globals.prefs.getColor("grayedOutText"), antialiasing),
        veryGrayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("veryGrayedOutBackground"),
                                             Globals.prefs.getColor("veryGrayedOutText"), antialiasing),
        markedRenderer = new GeneralRenderer(Globals.prefs.getColor("markedEntryBackground"),
                Globals.prefs.getColor("tableText"), antialiasing);

    class IncompleteRenderer extends GeneralRenderer {
        public IncompleteRenderer(JTable table, boolean antialiasing) {
            super(Globals.prefs.getColor("incompleteEntryBackground"), antialiasing);
        }
        protected void setValue(Object value) {
            super.setValue(value);
            super.setToolTipText(Globals.lang("This entry is incomplete"));
        }
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
     * This is done for performance reasons.
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

