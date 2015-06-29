package net.sf.jabref.gui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import net.sf.jabref.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class FetcherPreviewDialog extends JDialog implements OutputPrinter {

    private final EventList<TableEntry> entries = new BasicEventList<TableEntry>();
    //protected SortedList<TableEntry> sortedList;
    private final JTable glTable;
    private boolean okPressed = false;
    private final JabRefFrame frame;
    private final int warningLimit;


    public FetcherPreviewDialog(JabRefFrame frame, int warningLimit, int tableRowHeight) {
        super(frame, Globals.lang("Title"), true);
        this.frame = frame;
        this.warningLimit = warningLimit;

        JButton ok = new JButton(Globals.lang("Ok"));
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (verifySelection()) {
                    okPressed = true;
                    dispose();
                }
            }
        });
        JButton cancel = new JButton(Globals.lang("Cancel"));
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed = false;
                dispose();
            }
        });
        JButton selectAll = new JButton(Globals.lang("Select all"));
        selectAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectionAll(true);
            }
        });
        JButton deselectAll = new JButton(Globals.lang("Deselect all"));
        deselectAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectionAll(false);
            }
        });

        EventTableModel<TableEntry> tableModelGl = new EventTableModel<TableEntry>(entries,
                new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        glTable.setRowHeight(tableRowHeight);
        glTable.getColumnModel().getColumn(0).setMaxWidth(45);
        glTable.setPreferredScrollableViewportSize(new Dimension(1100, 600));
        EventSelectionModel<TableEntry> selectionModel = new EventSelectionModel<TableEntry>(entries);
        glTable.setSelectionModel(selectionModel);
        ButtonStackBuilder builder = new ButtonStackBuilder();
        builder.addButton(selectAll);
        builder.addButton(deselectAll);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel centerPan = new JPanel();
        centerPan.setLayout(new BorderLayout());
        centerPan.add(new JScrollPane(glTable), BorderLayout.CENTER);
        centerPan.add(builder.getPanel(), BorderLayout.WEST);

        getContentPane().add(centerPan, BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = centerPan.getActionMap();
        InputMap im = centerPan.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);

        pack();

    }

    /**
     * Check whether a large number of entries are selected, and if so, ask the user whether
     * to go on.
     * @return true if we should go on
     */
    private boolean verifySelection() {
        int selected = 0;
        for (TableEntry entry : entries) {
            if (entry.isWanted()) {
                selected++;
            }
        }
        if (selected > warningLimit) {
            int result = JOptionPane.showConfirmDialog(this,
                    Globals.lang("You have selected more than %0 entries for download. Some web sites "
                            + "might block you if you make too many rapid downloads. Do you want to continue?",
                            String.valueOf(warningLimit)),
                    Globals.lang("Confirm selection"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return result == JOptionPane.YES_OPTION;
        } else {
            return true;
        }
    }

    public Map<String, Boolean> getSelection() {
        LinkedHashMap<String, Boolean> selection = new LinkedHashMap<String, Boolean>();
        for (TableEntry e : entries) {
            selection.put(e.id, e.isWanted());
        }
        return selection;
    }

    /* (non-Javadoc)
    * @see net.sf.jabref.gui.ImportInspection#addEntry(net.sf.jabref.BibtexEntry)
    */
    public void addEntry(String entryId, JLabel preview) {
        TableEntry entry = new TableEntry(entryId, preview);
        this.entries.getReadWriteLock().writeLock().lock();
        this.entries.add(entry);
        this.entries.getReadWriteLock().writeLock().unlock();
        glTable.repaint();
    }

    private void setSelectionAll(boolean select) {
        for (int i = 0; i < glTable.getRowCount(); i++) {
            glTable.setValueAt(select, i, 0);
        }
        glTable.repaint();
    }


    static class TableEntry {

        private final String id;
        private final JLabel preview;
        private boolean wanted = false;


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

    static class PreviewRenderer implements TableCellRenderer {

        final JLabel label = new JLabel();


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel label = (JLabel) value;
            this.label.setText(label.getText());
            return this.label;
        }
    }

    class EntryTable extends JTable {

        final PreviewRenderer renderer = new PreviewRenderer();


        public EntryTable(TableModel model) {
            super(model);
            getTableHeader().setReorderingAllowed(false);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            return column == 0 ? getDefaultRenderer(Boolean.class) : renderer;
        }

        /*
         * public TableCellEditor getCellEditor() { return
         * getDefaultEditor(Boolean.class); }
         */

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == 0) {
                return Boolean.class;
            } else {
                return JLabel.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            // Only column 0, which is controlled by BibtexEntry.searchHit, is
            // editable:
            entries.getReadWriteLock().writeLock().lock();
            TableEntry entry = entries.get(row);
            entry.setWanted((Boolean) value);
            entries.getReadWriteLock().writeLock().unlock();
        }
    }

    private static class EntryTableFormat implements TableFormat<TableEntry> {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int i) {
            if (i == 0) {
                return Globals.lang("Keep");
            } else {
                return Globals.lang("Preview");
            }
        }

        @Override
        public Object getColumnValue(TableEntry entry, int i) {
            if (i == 0) {
                return entry.isWanted() ? Boolean.TRUE : Boolean.FALSE;
            } else {
                return entry.getPreview();
            }
        }

    }


    public boolean isOkPressed() {
        return okPressed;
    }

    @Override
    public void setStatus(String s) {
        frame.setStatus(s);
    }

    @Override
    public void showMessage(Object message, String title, int msgType) {
        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
