package net.sf.jabref.gui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import net.sf.jabref.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 *
 */
public class FetcherPreviewDialog extends JDialog {

    protected EventList<TableEntry> entries = new BasicEventList<TableEntry>();
    //protected SortedList<TableEntry> sortedList;
    protected JTable glTable;

    public static void main(String[] args) {
        FetcherPreviewDialog diag = new FetcherPreviewDialog();
        diag.addEntry("en", new JLabel("Dette er en prøve"));
        diag.addEntry("to", new JLabel("Dette er en prøve"));

        diag.setVisible(true);
    }

    public FetcherPreviewDialog(/*JabRefFrame frame*/) {
        super((JFrame)null, Globals.lang("Title"), true);

        //sortedList = new SortedList<TableEntry>(entries);
        EventTableModel<TableEntry> tableModelGl = new EventTableModel<TableEntry>(entries,
                    new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        getContentPane().add(new JScrollPane(glTable), BorderLayout.CENTER);

        pack();
    }

        /* (non-Javadoc)
	 * @see net.sf.jabref.gui.ImportInspection#addEntry(net.sf.jabref.BibtexEntry)
	 */
    public void addEntry(String entryId, JLabel preview) {
        TableEntry entry = new TableEntry(entryId, preview);
        this.entries.getReadWriteLock().writeLock().lock();
        this.entries.add(entry);
        this.entries.getReadWriteLock().writeLock().unlock();
    }


    class TableEntry {
        private String id;
        private JLabel preview;
        private boolean wanted = true;
        public TableEntry(String id, JLabel preview) {
            this.id = id;
            this.preview = preview;
        }

        public boolean isWanted() {
            return wanted;
        }

        public void setWanted(boolean wanted) {
            this.wanted = wanted;
        }

        public JLabel getPreview() {
            return preview;
        }

    }

    class PreviewRenderer implements TableCellRenderer {
        JLabel label = new JLabel();
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel)value;
            this.label.setText(label.getText());
            return this.label;
        }
    }

    class EntryTable extends JTable {

        PreviewRenderer renderer = new PreviewRenderer();

        public EntryTable(TableModel model) {
            super(model);
            getTableHeader().setReorderingAllowed(false);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            return column == 0 ? getDefaultRenderer(Boolean.class) : renderer;
        }

        /*
         * public TableCellEditor getCellEditor() { return
         * getDefaultEditor(Boolean.class); }
         */

        public Class<?> getColumnClass(int col) {
            if (col == 0)
                return Boolean.class;
            else
                return JLabel.class;
        }

        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        public void setValueAt(Object value, int row, int column) {
            // Only column 0, which is controlled by BibtexEntry.searchHit, is
            // editable:
            entries.getReadWriteLock().writeLock().lock();
            TableEntry entry = entries.get(row);
            entry.setWanted(((Boolean) value).booleanValue());
            entries.getReadWriteLock().writeLock().unlock();
        }
    }

    class EntryTableFormat implements TableFormat<TableEntry> {

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int i) {
            if (i == 0)
                return Globals.lang("Keep");
            else
                return Globals.lang("Preview");
        }

        public Object getColumnValue(TableEntry entry, int i) {
            if (i == 0)
                return entry.isWanted() ? Boolean.TRUE : Boolean.FALSE;
            else return entry.getPreview();
        }

    }

}
