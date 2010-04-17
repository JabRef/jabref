package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class TableColumnsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private boolean tableChanged = false;
    private JTable colSetup;
    private int rowCount = -1, ncWidth = -1;
    private Vector<TableRow> tableRows = new Vector<TableRow>(10);
    private JabRefFrame frame;

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
    public TableColumnsTab(JabRefPreferences prefs, JabRefFrame frame) {
        _prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        TableModel tm = new AbstractTableModel() {
                public int getRowCount() { return rowCount; }
                public int getColumnCount() { return 2; }
                public Object getValueAt(int row, int column) {
                  if (row == 0)
                    return (column==0 ? GUIGlobals.NUMBER_COL : ""+ncWidth);
                  row--;
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
                      return ((tr.length > 0) ? Integer.toString(tr.length) : "");
                  }
                  return null; // Unreachable.
                }

                public String getColumnName(int col) {
                    return (col == 0 ? Globals.lang("Field name") : Globals.lang("Column width"));
                }
                public Class<?> getColumnClass(int column) {
                    if (column == 0)
                    	return String.class;
                    else
                    	return Integer.class;
                }
                public boolean isCellEditable(int row, int col) {
                    return !((row == 0) && (col == 0));
                }
                public void setValueAt(Object value, int row, int col) {
                    tableChanged = true;
                    // Make sure the vector is long enough.
                    while (row >= tableRows.size())
                        tableRows.add(new TableRow("", -1));

                        if ((row == 0) && (col == 1)) {
                          ncWidth = Integer.parseInt(value.toString());
                          return;
                        }

                    TableRow rowContent = tableRows.elementAt(row-1);

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
        cm.getColumn(0).setPreferredWidth(140);
        cm.getColumn(1).setPreferredWidth(80);

        FormLayout layout = new FormLayout
            ("1dlu, 8dlu, left:pref, 4dlu, fill:pref",//, 4dlu, fill:60dlu, 4dlu, fill:pref",
             "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane
            (colSetup, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        colSetup.setPreferredScrollableViewportSize(new Dimension(250,200));
        sp.setMinimumSize(new Dimension(250,300));
        tabPanel.add(sp, BorderLayout.CENTER);
        JToolBar tlb = new JToolBar(SwingConstants.VERTICAL);
        tlb.setFloatable(false);
        //tlb.setRollover(true);
        //tlb.setLayout(gbl);
        AddRowAction ara = new AddRowAction();
        DeleteRowAction dra = new DeleteRowAction();
        MoveRowUpAction moveUp = new MoveRowUpAction();
        MoveRowDownAction moveDown = new MoveRowDownAction();
        tlb.setBorder(null);
        tlb.add(ara);
        tlb.add(dra);
        tlb.addSeparator();
        tlb.add(moveUp);
        tlb.add(moveDown);
        //tlb.addSeparator();
        //tlb.add(new UpdateWidthsAction());
        tabPanel.add(tlb, BorderLayout.EAST);

        builder.appendSeparator(Globals.lang("Entry table columns"));
        builder.nextLine();
        builder.append(pan); builder.append(tabPanel); builder.nextLine();
//	lab = new JLabel("<HTML>("+Globals.lang("this button will update the column width settings<BR>"
//						+"to match the current widths in your table")+")</HTML>");
//        lab = new JLabel("<HTML>("+Globals.lang("this_button_will_update") +")</HTML>") ;
        builder.append(pan);
        JButton buttonWidth = new JButton(new UpdateWidthsAction());
        JButton buttonOrder = new JButton(new UpdateOrderAction());
        builder.append(buttonWidth);builder.nextLine();
        builder.append(pan);
        builder.append(buttonOrder);builder.nextLine();
        builder.append(pan);
        //builder.append(lab);
        builder.nextLine();
        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(pan, BorderLayout.CENTER);
    }

    public void setValues() {
	tableRows.clear();
	String[] names = _prefs.getStringArray("columnNames"),
            lengths = _prefs.getStringArray("columnWidths");
        for (int i=0; i<names.length; i++) {
            if (i<lengths.length)
                tableRows.add(new TableRow(names[i], Integer.parseInt(lengths[i])));
            else
                tableRows.add(new TableRow(names[i]));
        }
        rowCount = tableRows.size()+5;
        ncWidth = _prefs.getInt("numberColWidth");

    }


    class DeleteRowAction extends AbstractAction {
        public DeleteRowAction() {
          super("Delete row", GUIGlobals.getImage("remove"));
          putValue(SHORT_DESCRIPTION, Globals.lang("Delete rows"));
        }
        public void actionPerformed(ActionEvent e) {
          int[] rows = colSetup.getSelectedRows();
          if (rows.length == 0)
            return;
          int offs = 0;
          for (int i=rows.length-1; i>=0; i--) {
            if ((rows[i] <= tableRows.size()) && (rows[i] != 0)) {
                tableRows.remove(rows[i]-1);
                offs++;
            }
          }
          rowCount -= offs;
          if (rows.length > 1) colSetup.clearSelection();
          colSetup.revalidate();
          colSetup.repaint();
          tableChanged = true;
        }
      }

    class AddRowAction extends AbstractAction {
        public AddRowAction() {
          super("Add row", GUIGlobals.getImage("add"));
          putValue(SHORT_DESCRIPTION, Globals.lang("Insert rows"));
        }
        public void actionPerformed(ActionEvent e) {
            int[] rows = colSetup.getSelectedRows();
            if (rows.length == 0) {
                // No rows selected, so we just add one at the end.
                rowCount++;
                colSetup.revalidate();
                colSetup.repaint();
                return;
            }
            for (int i=0; i<rows.length; i++) {
                if (rows[i]+i-1 < tableRows.size())
                    tableRows.add(Math.max(0, rows[i]+i-1), new TableRow(GUIGlobals.DEFAULT_FIELD_LENGTH));
            }
            rowCount += rows.length;
            if (rows.length > 1) colSetup.clearSelection();
            colSetup.revalidate();
            colSetup.repaint();
            tableChanged = true;
        }
    }

    abstract class AbstractMoveRowAction extends AbstractAction {
		public AbstractMoveRowAction(String string, ImageIcon image) {
			super(string, image);
		}

		protected void swap(int i, int j) {
			if (i < 0 || i >= tableRows.size())
				return;
			if (j < 0 || j >= tableRows.size())
				return;
			TableRow tmp = tableRows.get(i);
			tableRows.set(i, tableRows.get(j));
			tableRows.set(j, tmp);
		}
	}

	class MoveRowUpAction extends AbstractMoveRowAction {
		public MoveRowUpAction() {
			super("Up", GUIGlobals.getImage("up"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Move up"));
		}

		public void actionPerformed(ActionEvent e) {
			int selected[] = colSetup.getSelectedRows();
			Arrays.sort(selected);
			// first element (#) not inside tableRows
			// don't move if a selected element is at bounce
			if (selected.length > 0 && selected[0] > 1) {
				boolean newSelected[] = new boolean[colSetup.getRowCount()];
				for (int i : selected) {
					swap(i - 1, i - 2);
					newSelected[i - 1] = true;
				}
				// select all and remove unselected
				colSetup.setRowSelectionInterval(0, colSetup.getRowCount() - 1);
				for (int i = 0; i < colSetup.getRowCount(); i++) {
					if (!newSelected[i])
						colSetup.removeRowSelectionInterval(i, i);
				}
				colSetup.revalidate();
				colSetup.repaint();
				tableChanged = true;
			}
		}
	}

	class MoveRowDownAction extends AbstractMoveRowAction {
		public MoveRowDownAction() {
			super("Down", GUIGlobals.getImage("down"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Down"));
		}

		public void actionPerformed(ActionEvent e) {
			int selected[] = colSetup.getSelectedRows();
			Arrays.sort(selected);
			final int last = selected.length - 1;
			boolean newSelected[] = new boolean[colSetup.getRowCount()];
			// don't move if a selected element is at bounce
			if (selected.length > 0 && selected[last] < tableRows.size()) {
				for (int i = last; i >= 0; i--) {
					swap(selected[i] - 1, selected[i]);
					newSelected[selected[i] + 1] = true;
				}
				// select all and remove unselected
				colSetup.setRowSelectionInterval(0, colSetup.getRowCount() - 1);
				for (int i = 0; i < colSetup.getRowCount(); i++) {
					if (!newSelected[i])
						colSetup.removeRowSelectionInterval(i, i);
				}
				colSetup.revalidate();
				colSetup.repaint();
				tableChanged = true;
			}
		}
	}

	class UpdateOrderAction extends AbstractAction {
		public UpdateOrderAction() {
			super(Globals.lang("Update to current column order"));
		}

		public void actionPerformed(ActionEvent e) {
			BasePanel panel = frame.basePanel();
			if (panel == null) {
				return;
			}
			// idea: sort elements according to value stored in hash, keep
			// everything not inside hash/mainTable as it was
			final HashMap<String, Integer> map = new HashMap<String, Integer>();

			// first element (#) not inside tableRows
			for (int i = 1; i < panel.mainTable.getColumnCount(); i++) {
				String name = panel.mainTable.getColumnName(i);
				if (name != null && name.length() != 0) {
					map.put(name.toLowerCase(), i);
				}
			}
			Collections.sort(tableRows, new Comparator<TableRow>() {
				public int compare(TableRow o1, TableRow o2) {
					Integer n1 = map.get(o1.name);
					Integer n2 = map.get(o2.name);
					if (n1 == null || n2 == null) {
						return 0;
					}
					return n1.compareTo(n2);
				}
			});

			colSetup.revalidate();
			colSetup.repaint();
			tableChanged = true;
		}
	}

    class UpdateWidthsAction extends AbstractAction {
        public UpdateWidthsAction() {
          //super(Globals.lang("Update to current column widths"));
          super(Globals.lang("Update to current column widths"));
          //putValue(SHORT_DESCRIPTION, Globals.lang("Update to current column widths"));
        }
        public void actionPerformed(ActionEvent e) {
            BasePanel panel = frame.basePanel();
            if (panel == null) return;
            TableColumnModel colMod = panel.mainTable.getColumnModel();
            colSetup.setValueAt(""+colMod.getColumn(0).getWidth(), 0, 1);
            for (int i=1; i<colMod.getColumnCount(); i++) {
            try {
                String name = panel.mainTable.getColumnName(i).toLowerCase();
                int width = colMod.getColumn(i).getWidth();
                //Util.pr(":"+((String)colSetup.getValueAt(i-1, 0)).toLowerCase());
                //Util.pr("-"+name);
                if ((i <= tableRows.size()) && (((String)colSetup.getValueAt(i, 0)).toLowerCase()).equals(name))
                    colSetup.setValueAt(""+width, i, 1);
                else { // Doesn't match; search for a matching col in our table
                    for (int j=0; j<colSetup.getRowCount(); j++) {
                        if ((j < tableRows.size()) &&
                            (((String)colSetup.getValueAt(j, 0)).toLowerCase()).equals(name)) {
                            colSetup.setValueAt(""+width, j, 1);
                            break;
                        }
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            colSetup.revalidate();
            colSetup.repaint();
        }

        }
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
                if (tableRows.elementAt(i).name.equals(""))
                    tableRows.removeElementAt(i);
                else i++;
            }
            // Then we make arrays
            String[] names = new String[tableRows.size()],
                widths = new String[tableRows.size()];
            int[] nWidths = new int[tableRows.size()];

            _prefs.putInt("numberColWidth", ncWidth);
            for (i=0; i<tableRows.size(); i++) {
                TableRow tr = tableRows.elementAt(i);
                names[i] = tr.name;
                nWidths[i] = tr.length;
                widths[i] = ""+tr.length;
                //Util.pr(names[i]+"   "+widths[i]);
            }

            // Finally, we store the new preferences.
            _prefs.putStringArray("columnNames", names);
            _prefs.putStringArray("columnWidths", widths);
        }

    }

    public boolean readyToClose() {
        return true;
    }

	public String getTabName() {
	    return Globals.lang("Entry table columns");
	}
}
