package net.sf.jabref.gui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import com.jgoodies.uif_lite.component.UIFSplitPane;
import net.sf.jabref.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

/**
 * Dialog to display search results, potentially from more than one BasePanel, with
 * possibility to preview and to locate each entry in the main window.
 *
 * TODO: should be possible to save or export the list.
 */
public class SearchResultsDialog {
    private JabRefFrame frame;
    private String title;
    private JDialog diag;
    private String[] fields = new String[]{
            "author", "title", "year", "journal"
    };

    protected Rectangle toRect = new Rectangle(0, 0, 1, 1);

    private EventTableModel model;
    private EventList<BibtexEntry> entries = new BasicEventList<BibtexEntry>();
    private SortedList<BibtexEntry> sortedEntries;
    private HashMap<BibtexEntry, BasePanel> entryHome = new HashMap<BibtexEntry, BasePanel>();

    private JTable entryTable;
    protected UIFSplitPane contentPane = new UIFSplitPane(UIFSplitPane.VERTICAL_SPLIT);
    PreviewPanel preview;

    public SearchResultsDialog(JabRefFrame frame, String title) {

        this.frame = frame;

        init(title);
    }

    private void init(String title) {
        diag = new JDialog(frame, title, false);

        preview = new PreviewPanel(null, new MetaData(), Globals.prefs.get("preview1"));

        sortedEntries = new SortedList<BibtexEntry>(entries, new EntryComparator(false, true, "author"));
        model = new EventTableModel(sortedEntries,
                new EntryTableFormat());
        entryTable = new JTable(model);


        TableComparatorChooser<BibtexEntry> tableSorter =
                new TableComparatorChooser<BibtexEntry>(entryTable, sortedEntries, true);
        JScrollPane sp = new JScrollPane(entryTable);

        EventSelectionModel<BibtexEntry> selectionModel = new EventSelectionModel<BibtexEntry>(sortedEntries);
        entryTable.setSelectionModel(selectionModel);
        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
        entryTable.addMouseListener(new TableClickListener());

        contentPane.setTopComponent(sp);
        contentPane.setBottomComponent(new JScrollPane(preview));

        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            diag.dispose();
          }
        };
        ActionMap am = contentPane.getActionMap();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);
        
        diag.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                contentPane.setDividerLocation(0.5f);
            }
        });

        diag.getContentPane().add(contentPane, BorderLayout.CENTER);
        diag.pack();
        diag.setLocationRelativeTo(frame);
    }

    public void setVisible(boolean visible) {
        diag.setVisible(visible);
    }

    /**
     * Remove all entries from the table.
     */
    public synchronized void clear() {
        entries.clear();
        entryHome.clear();
    }

    /**
     * Add a list of entries to the table.
     * @param newEntries The list of entries.
     * @param panel A reference to the BasePanel where the entries belong.
     */
    public synchronized void addEntries(java.util.List<BibtexEntry> newEntries, BasePanel panel) {
        for (BibtexEntry entry : newEntries) {
            entries.add(entry);
            entryHome.put(entry, panel);
        }
    }

    /**
     * Add a single entry to the table.
     * @param entry The entry to add.
     * @param panel A reference to the BasePanel where the entry belongs.
     */
    public synchronized void addEntry(BibtexEntry entry, BasePanel panel) {
        entries.add(entry);
        entryHome.put(entry, panel);
    }

    /**
     * Mouse listener for the entry table.
     */
    class TableClickListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            // First find the row on which the user has clicked.
            final int row = entryTable.rowAtPoint(e.getPoint());

            // A double click on an entry should highlight the entry in its BasePanel:
            if (e.getClickCount() == 2) {
                // Get the selected entry:
                BibtexEntry toShow = (BibtexEntry)model.getElementAt(row);
                // Look up which BasePanel it belongs to:
                BasePanel p = entryHome.get(toShow);
                // Show the correct tab in the main window:
                frame.showBasePanel(p);
                // Highlight the entry:
                p.highlightEntry(toShow);
            }
        }


    }

    class EntrySelectionListener implements ListEventListener<BibtexEntry> {

            public void listChanged(ListEvent<BibtexEntry> listEvent) {
                if (listEvent.getSourceList().size() == 1) {
                    BibtexEntry entry = listEvent.getSourceList().get(0);
                    // Find out which BasePanel the selected entry belongs to:
                    BasePanel p = entryHome.get(entry);
                    // Update the preview's metadata reference:
                    preview.setMetaData(p.metaData());
                    // Update the preview's entry:
                    preview.setEntry(entry);
                    contentPane.setDividerLocation(0.5f);
                    SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                            preview.scrollRectToVisible(toRect);
                        }
                    });
                }
            }
        }

    /**
     * TableFormat for the table shown in the dialog.
     */
    public class EntryTableFormat implements TableFormat {

        public int getColumnCount() {
            return fields.length;
        }

        public String getColumnName(int column) {
            return Util.nCase(fields[column]);
        }

        public Object getColumnValue(Object o, int column) {
            BibtexEntry entry = (BibtexEntry) o;

            return entry.getField(fields[column]);
        }
    }
}
