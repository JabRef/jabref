/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.TransferableBibtexEntry;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.maintable.MainTableNameFormatter;
import net.sf.jabref.gui.renderer.GeneralRenderer;
import net.sf.jabref.gui.util.comparator.IconComparator;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.bibtex.comparator.EntryComparator;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;

/**
 * Dialog to display search results, potentially from more than one BasePanel, with
 * possibility to preview and to locate each entry in the main window.
 */
public class SearchResultsDialog {

    private static final Log LOGGER = LogFactory.getLog(SearchResultsDialog.class);

    private final JabRefFrame frame;

    private JDialog diag;
    private static final String[] FIELDS = new String[] {
            "author", "title", "year", "journal"
    };
    private static final int FILE_COL = 0;
    private static final int URL_COL = 1;
    private static final int PAD = 2;
    private final JLabel fileLabel = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
    private final JLabel urlLabel = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());

    private final JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final Rectangle toRect = new Rectangle(0, 0, 1, 1);
    private final EventList<BibEntry> entries = new BasicEventList<>();

    private final Map<BibEntry, BasePanel> entryHome = new HashMap<>();
    private DefaultEventTableModel<BibEntry> model;

    private SortedList<BibEntry> sortedEntries;
    private JTable entryTable;
    private PreviewPanel preview;


    public SearchResultsDialog(JabRefFrame frame, String title) {
        this.frame = Objects.requireNonNull(frame);
        init(Objects.requireNonNull(title));
    }

    private void init(String title) {
        diag = new JDialog(frame, title, false);

        int activePreview = Globals.prefs.getInt(JabRefPreferences.ACTIVE_PREVIEW);
        String layoutFile = activePreview == 0 ? Globals.prefs.get(JabRefPreferences.PREVIEW_0) : Globals.prefs
                .get(JabRefPreferences.PREVIEW_1);
        preview = new PreviewPanel(null, null, layoutFile);

        sortedEntries = new SortedList<>(entries, new EntryComparator(false, true, "author"));
        model = (DefaultEventTableModel<BibEntry>) GlazedListsSwing.eventTableModelWithThreadProxyList(sortedEntries,
                new EntryTableFormat());
        entryTable = new JTable(model);
        GeneralRenderer renderer = new GeneralRenderer(Color.white);
        entryTable.setDefaultRenderer(JLabel.class, renderer);
        entryTable.setDefaultRenderer(String.class, renderer);
        setWidths();
        TableComparatorChooser<BibEntry> tableSorter =
                TableComparatorChooser.install(entryTable, sortedEntries,
                        AbstractTableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);
        setupComparatorChooser(tableSorter);
        JScrollPane sp = new JScrollPane(entryTable);

        final DefaultEventSelectionModel<BibEntry> selectionModel = (DefaultEventSelectionModel<BibEntry>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(sortedEntries);
        entryTable.setSelectionModel(selectionModel);
        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
        entryTable.addMouseListener(new TableClickListener());

        contentPane.setTopComponent(sp);
        contentPane.setBottomComponent(preview);

        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        };
        ActionMap am = contentPane.getActionMap();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

        entryTable.getActionMap().put("copy", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!selectionModel.getSelected().isEmpty()) {
                    List<BibEntry> bes = selectionModel.getSelected();
                    TransferableBibtexEntry trbe = new TransferableBibtexEntry(bes);
                    // ! look at ClipBoardManager
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(trbe, frame.getCurrentBasePanel());
                    frame.output(Localization.lang("Copied") + ' ' + (bes.size() > 1 ? bes.size() + " "
                            + Localization.lang("entries")
                            : "1 " + Localization.lang("entry") + '.'));
                }
            }
        });

        diag.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                contentPane.setDividerLocation(0.5f);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                Globals.prefs.putInt(JabRefPreferences.SEARCH_DIALOG_WIDTH, diag.getSize().width);
                Globals.prefs.putInt(JabRefPreferences.SEARCH_DIALOG_HEIGHT, diag.getSize().height);
            }
        });

        diag.getContentPane().add(contentPane, BorderLayout.CENTER);
        // Remember and default to last size:
        diag.setSize(new Dimension(Globals.prefs.getInt(JabRefPreferences.SEARCH_DIALOG_WIDTH), Globals.prefs
                .getInt(JabRefPreferences.SEARCH_DIALOG_HEIGHT)));
        diag.setLocationRelativeTo(frame);
    }

    /**
     * Control the visibility of the dialog.
     * @param visible true to show dialog, false to hide.
     */
    public void setVisible(boolean visible) {
        diag.setVisible(visible);
    }

    public void selectFirstEntry() {
        if (entryTable.getRowCount() > 0) {
            entryTable.setRowSelectionInterval(0, 0);
        } else {
            contentPane.setDividerLocation(1.0f);
        }
    }

    /**
     * Set up the comparators for each column, so the user can modify sort order
     * by clicking the column labels.
     * @param comparatorChooser The comparator chooser controlling the sort order.
     */
    private void setupComparatorChooser(TableComparatorChooser<BibEntry> comparatorChooser) {
        // First column:
        List<Comparator> comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();

        comparators = comparatorChooser.getComparatorsForColumn(1);
        comparators.clear();

        // Icon columns:
        for (int i = 0; i < PAD; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            if (i == FILE_COL) {
                comparators.add(new IconComparator(Collections.singletonList(Globals.FILE_FIELD)));
            } else if (i == URL_COL) {
                comparators.add(new IconComparator(Collections.singletonList("url")));
            }

        }
        // Remaining columns:
        for (int i = PAD; i < (PAD + FIELDS.length); i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(FIELDS[i - PAD]));
        }

        sortedEntries.getReadWriteLock().writeLock().lock();
        comparatorChooser.appendComparator(PAD, 0, false);
        sortedEntries.getReadWriteLock().writeLock().unlock();

    }

    /**
     * Set column widths according to which field is shown, and lock icon columns
     * to a suitable width.
     */
    private void setWidths() {
        TableColumnModel cm = entryTable.getColumnModel();
        for (int i = 0; i < PAD; i++) {
            cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
        }

        for (int i = 0; i < FIELDS.length; i++) {
            int width = InternalBibtexFields.getFieldLength(FIELDS[i]);
            cm.getColumn(i + PAD).setPreferredWidth(width);
        }
    }

    /**
     * Add a list of entries to the table.
     * @param newEntries The list of entries.
     * @param panel A reference to the BasePanel where the entries belong.
     */
    public void addEntries(List<BibEntry> newEntries, BasePanel panel) {
        for (BibEntry entry : newEntries) {
            addEntry(entry, panel);
        }
    }

    /**
     * Add a single entry to the table.
     * @param entry The entry to add.
     * @param panel A reference to the BasePanel where the entry belongs.
     */
    private void addEntry(BibEntry entry, BasePanel panel) {
        entries.add(entry);
        entryHome.put(entry, panel);
    }

    /**
     * Mouse listener for the entry table. Processes icon clicks to open external
     * files or urls, as well as the opening of the context menu.
     */
    class TableClickListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
                return;
            }

            // First find the row on which the user has clicked.
            final int row = entryTable.rowAtPoint(e.getPoint());

            // A double click on an entry should highlight the entry in its BasePanel:
            if (e.getClickCount() == 2) {
                // Get the selected entry:
                BibEntry toShow = model.getElementAt(row);
                // Look up which BasePanel it belongs to:
                BasePanel p = entryHome.get(toShow);
                // Show the correct tab in the main window:
                frame.showBasePanel(p);
                // Highlight the entry:
                p.highlightEntry(toShow);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
                return;
            }
            //if (e.)
            final int col = entryTable.columnAtPoint(e.getPoint());
            final int row = entryTable.rowAtPoint(e.getPoint());
            if (col < PAD) {
                BibEntry entry = sortedEntries.get(row);
                BasePanel p = entryHome.get(entry);
                switch (col) {
                case FILE_COL:
                    if (entry.hasField(Globals.FILE_FIELD)) {
                        FileListTableModel tableModel = new FileListTableModel();
                        tableModel.setContent(entry.getField(Globals.FILE_FIELD));
                        if (tableModel.getRowCount() == 0) {
                            return;
                        }
                        FileListEntry fl = tableModel.getEntry(0);
                        (new ExternalFileMenuItem(frame, entry, "", fl.link, null,
                                p.getBibDatabaseContext(), fl.type)).actionPerformed(null);
                    }
                    break;
                case URL_COL:
                    entry.getFieldOptional("url").ifPresent(link -> { try {
                        JabRefDesktop.openExternalViewer(p.getBibDatabaseContext(), link, "url");
                    } catch (IOException ex) {
                            LOGGER.warn("Could not open viewer", ex);
                        }
                    });
                    break;
                default:
                    break;
                }
            }
        }

        /**
         * If the user has signalled the opening of a context menu, the event
         * gets redirected to this method. Here we open a file link menu if the
         * user is pointing at a file link icon. Otherwise a general context
         * menu should be shown.
         * @param e The triggering mouse event.
         */
        public void processPopupTrigger(MouseEvent e) {
            BibEntry entry = sortedEntries.get(entryTable.rowAtPoint(e.getPoint()));
            BasePanel p = entryHome.get(entry);
            int col = entryTable.columnAtPoint(e.getPoint());
            JPopupMenu menu = new JPopupMenu();
            int count = 0;

            if (col == FILE_COL) {
                // We use a FileListTableModel to parse the field content:
                FileListTableModel fileList = new FileListTableModel();
                fileList.setContent(entry.getField(Globals.FILE_FIELD));
                // If there are one or more links, open the first one:
                for (int i = 0; i < fileList.getRowCount(); i++) {
                    FileListEntry flEntry = fileList.getEntry(i);
                    String description = flEntry.description;
                    if ((description == null) || (description.trim().isEmpty())) {
                        description = flEntry.link;
                    }
                    menu.add(new ExternalFileMenuItem(p.frame(), entry, description, flEntry.link,
                            flEntry.type.get().getIcon(), p.getBibDatabaseContext(), flEntry.type));
                    count++;
                }

            }

            if (count > 0) {
                menu.show(entryTable, e.getX(), e.getY());
            }
        }
    }

    /**
     * The listener for the Glazed list monitoring the current selection.
     * When selection changes, we need to update the preview panel.
     */
    private class EntrySelectionListener implements ListEventListener<BibEntry> {

        @Override
        public void listChanged(ListEvent<BibEntry> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                BibEntry entry = listEvent.getSourceList().get(0);
                // Find out which BasePanel the selected entry belongs to:
                BasePanel p = entryHome.get(entry);
                // Update the preview's database context:
                preview.setDatabaseContext(p.getBibDatabaseContext());
                // Update the preview's entry:
                preview.setEntry(entry);
                contentPane.setDividerLocation(0.5f);
                SwingUtilities.invokeLater(() -> preview.scrollRectToVisible(toRect));
            }
        }
    }

    /**
     * TableFormat for the table shown in the dialog. Handles the display of entry
     * fields and icons for linked files and urls.
     */
    private class EntryTableFormat implements AdvancedTableFormat<BibEntry> {

        @Override
        public int getColumnCount() {
            return PAD + FIELDS.length;
        }

        @Override
        public String getColumnName(int column) {
            if (column >= PAD) {
                return EntryUtil.capitalizeFirst(FIELDS[column - PAD]);
            } else {
                return "";
            }
        }

        @Override
        public Object getColumnValue(BibEntry entry, int column) {
            if (column < PAD) {
                switch (column) {
                case FILE_COL:
                    if (entry.hasField(Globals.FILE_FIELD)) {
                        FileListTableModel tmpModel = new FileListTableModel();
                        tmpModel.setContent(entry.getField(Globals.FILE_FIELD));
                        fileLabel.setToolTipText(tmpModel.getToolTipHTMLRepresentation());
                        if (tmpModel.getRowCount() > 0) {
                            if (tmpModel.getEntry(0).type.isPresent()) {
                                fileLabel.setIcon(tmpModel.getEntry(0).type.get().getIcon());
                            } else {
                                fileLabel.setIcon(IconTheme.JabRefIcon.FILE.getSmallIcon());
                            }
                        }
                        return fileLabel;
                    } else {
                        return null;
                    }
                case URL_COL:
                    if (entry.hasField("url")) {
                        urlLabel.setToolTipText(entry.getField("url"));
                        return urlLabel;
                    } else {
                        return null;
                    }
                default:
                    return null;
                }
            }
            else {
                String field = FIELDS[column - PAD];
                if (InternalBibtexFields.getFieldExtras(field).contains(FieldProperties.PERSON_NAMES)) {
                    // For name fields, tap into a MainTableFormat instance and use
                    // the same name formatting as is used in the entry table:
                    if (frame.getCurrentBasePanel() != null) {
                        return MainTableNameFormatter.formatName(entry.getField(field));
                    }
                }
                return entry.getField(field);
            }
        }

        @Override
        public Class<?> getColumnClass(int i) {
            if (i < PAD) {
                return JLabel.class;
            } else {
                return String.class;
            }
        }

        @Override
        public Comparator<?> getColumnComparator(int i) {
            return null;
        }
    }
}
