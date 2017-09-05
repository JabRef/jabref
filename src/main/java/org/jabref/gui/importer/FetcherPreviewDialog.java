package org.jabref.gui.importer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jabref.Globals;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.l10n.Localization;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 *
 */
public class FetcherPreviewDialog extends JabRefDialog implements OutputPrinter {

    private final EventList<TableEntry> entries = new BasicEventList<>();
    private final JTable glTable;
    private boolean okPressed;
    private final JabRefFrame frame;
    private final int warningLimit;


    public FetcherPreviewDialog(JabRefFrame frame, int warningLimit, int tableRowHeight) {
        super(frame, Localization.lang("Title"), true, FetcherPreviewDialog.class);
        this.frame = frame;
        this.warningLimit = warningLimit;

        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(e -> {
            if (verifySelection()) {
                okPressed = true;
                dispose();
            }
        });
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(e -> {
            okPressed = false;
            dispose();
        });
        JButton selectAll = new JButton(Localization.lang("Select all"));
        selectAll.addActionListener(e -> setSelectionAll(true));

        JButton deselectAll = new JButton(Localization.lang("Deselect all"));
        deselectAll.addActionListener(e -> setSelectionAll(false));

        DefaultEventTableModel<TableEntry> tableModelGl = (DefaultEventTableModel<TableEntry>) GlazedListsSwing
                .eventTableModelWithThreadProxyList(entries, new EntryTableFormat());
        glTable = new EntryTable(tableModelGl);
        glTable.setRowHeight(tableRowHeight);
        glTable.getColumnModel().getColumn(0).setMaxWidth(45);
        glTable.setPreferredScrollableViewportSize(new Dimension(1100, 600));
        DefaultEventSelectionModel<TableEntry> selectionModel = (DefaultEventSelectionModel<TableEntry>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(entries);
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
        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = centerPan.getActionMap();
        InputMap im = centerPan.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
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
                    Localization.lang("You have selected more than %0 entries for download. Some web sites "
                                    + "might block you if you make too many rapid downloads. Do you want to continue?",
                            String.valueOf(warningLimit)),
                    Localization.lang("Confirm selection"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return result == JOptionPane.YES_OPTION;
        } else {
            return true;
        }
    }

    public Map<String, Boolean> getSelection() {
        LinkedHashMap<String, Boolean> selection = new LinkedHashMap<>();
        for (TableEntry e : entries) {
            selection.put(e.id, e.isWanted());
        }
        return selection;
    }

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
        private boolean wanted;


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

        private final JLabel label = new JLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel valueLabel = (JLabel) value;
            label.setText(valueLabel.getText());
            return label;
        }
    }

    class EntryTable extends JTable {

        private final PreviewRenderer renderer = new PreviewRenderer();


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
            // Only column 0, which is controlled by BibEntry.searchHit, is
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
                return Localization.lang("Keep");
            } else {
                return Localization.lang("Preview");
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
    public void showMessage(String message, String title, int msgType) {
        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /**
     * Displays a dialog which tells the user that an error occurred while fetching entries
     */
    public void showErrorMessage(String fetcherTitle, String localizedMessage) {
        showMessage(Localization.lang("Error while fetching from %0", fetcherTitle) + "\n" +
                        Localization.lang("Please try again later and/or check your network connection.") + "\n" +
                        localizedMessage,
                Localization.lang("Search %0", fetcherTitle), JOptionPane.ERROR_MESSAGE);
    }
}
