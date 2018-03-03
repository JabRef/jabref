package org.jabref.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableColumnModel;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.TransferableBibtexEntry;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileMenuItem;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.MainTableNameFormatter;
import org.jabref.gui.renderer.GeneralRenderer;
import org.jabref.gui.util.comparator.IconComparator;
import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.SearchPreferences;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog to display search results, potentially from more than one BasePanel, with
 * possibility to preview and to locate each entry in the main window.
 */
public class SearchResultFrame {

    private static final String[] FIELDS = new String[] {
            FieldName.AUTHOR, FieldName.TITLE, FieldName.YEAR, FieldName.JOURNAL
    };
    private static final int DATABASE_COL = 0;
    private static final int FILE_COL = 1;
    private static final int URL_COL = 2;
    private static final int PAD = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultFrame.class);

    private final JabRefFrame frame;
    private JFrame searchResultFrame;
    private final JLabel fileLabel = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
    private final JLabel urlLabel = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());

    private final JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final EventList<BibEntry> entries = new BasicEventList<>();

    private final Map<BibEntry, BasePanel> entryHome = new HashMap<>();
    private DefaultEventTableModel<BibEntry> model;

    private SortedList<BibEntry> sortedEntries;
    private JTable entryTable;
    private PreviewPanel preview;

    private SearchQuery searchQuery;
    private boolean globalSearch;


    public SearchResultFrame(JabRefFrame frame, String title, SearchQuery searchQuery, boolean globalSearch) {
        this.frame = Objects.requireNonNull(frame);
        this.searchQuery = searchQuery;
        this.globalSearch = globalSearch;
        frame.getGlobalSearchBar().setSearchResultFrame(this);
        init(Objects.requireNonNull(title));
    }

    private void init(String title) {
        searchResultFrame = new JFrame();
        searchResultFrame.setTitle(title);
        searchResultFrame.setIconImages(IconTheme.getLogoSet());

        preview = new PreviewPanel(null, null);

        sortedEntries = new SortedList<>(entries, new EntryComparator(false, true, FieldName.AUTHOR));
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

        JFXPanel container = CustomJFXPanel.wrap(new Scene(preview));
        contentPane.setBottomComponent(container);

        // Key bindings:
        AbstractAction closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        ActionMap actionMap = contentPane.getActionMap();
        InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DATABASE), "close");
        actionMap.put("close", closeAction);

        actionMap = entryTable.getActionMap();
        inputMap = entryTable.getInputMap();
        //Override 'selectNextColumnCell' and 'selectPreviousColumnCell' to move rows instead of cells on TAB
        actionMap.put("selectNextColumnCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectNextEntry();
            }
        });
        actionMap.put("selectPreviousColumnCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectPreviousEntry();
            }
        });
        actionMap.put("selectNextRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectNextEntry();
            }
        });
        actionMap.put("selectPreviousRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectPreviousEntry();
            }
        });

        String selectFirst = "selectFirst";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SELECT_FIRST_ENTRY), selectFirst);
        actionMap.put(selectFirst, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectFirstEntry();
            }
        });

        String selectLast = "selectLast";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SELECT_LAST_ENTRY), selectLast);
        actionMap.put(selectLast, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectLastEntry();
            }
        });

        actionMap.put("copy", new AbstractAction() {
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

        // override standard enter-action; enter opens the selected entry
        entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        actionMap.put("Enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                BibEntry entry = sortedEntries.get(entryTable.getSelectedRow());
                selectEntryInBasePanel(entry);
            }
        });

        searchResultFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                contentPane.setDividerLocation(0.5f);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
            }
        });

        searchResultFrame.getContentPane().add(contentPane, BorderLayout.CENTER);

        // Remember and default to last size:
        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        searchResultFrame.setSize(searchPreferences.getSeachDialogWidth(), searchPreferences.getSeachDialogHeight());
        searchResultFrame.setLocation(searchPreferences.getSearchDialogPosX(), searchPreferences.getSearchDialogPosY());

        searchResultFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                new SearchPreferences(Globals.prefs)
                        .setSearchDialogWidth(searchResultFrame.getSize().width)
                        .setSearchDialogHeight(searchResultFrame.getSize().height);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                new SearchPreferences(Globals.prefs)
                        .setSearchDialogPosX(searchResultFrame.getLocation().x)
                        .setSearchDialogPosY(searchResultFrame.getLocation().y);
            }
        });
    }

    /**
     * Control the visibility of the dialog.
     * @param visible true to show dialog, false to hide.
     */
    public void setVisible(boolean visible) {
        searchResultFrame.setVisible(visible);
    }

    public void selectFirstEntry() {
        selectEntry(0);
    }

    public void selectLastEntry() {
        selectEntry(entryTable.getRowCount() - 1);
    }

    public void selectPreviousEntry() {
        selectEntry((entryTable.getSelectedRow() - 1 + entryTable.getRowCount()) % entryTable.getRowCount());
    }

    public void selectNextEntry() {
        selectEntry((entryTable.getSelectedRow() + 1) % entryTable.getRowCount());
    }

    public void selectEntry(int index) {
        if (index >= 0 && index < entryTable.getRowCount()) {
            entryTable.changeSelection(index, 0, false, false);
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
        List<Comparator> comparators;
        // Icon columns:
        for (int i = 0; i < PAD; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            if (i == FILE_COL) {
                comparators.add(new IconComparator(Collections.singletonList(FieldName.FILE)));
            } else if (i == URL_COL) {
                comparators.add(new IconComparator(Collections.singletonList(FieldName.URL)));
            } else if (i == DATABASE_COL) {
                comparators.add((entry1, entry2) -> {
                    String databaseTitle1 = entryHome.get(entry1).getTabTitle();
                    String databaseTitle2 = entryHome.get(entry2).getTabTitle();
                    return databaseTitle1.compareTo(databaseTitle2);
                });
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
        for (int i = 0; i < PAD + FIELDS.length; i++) {
            switch (i) {
                case FILE_COL:
                case URL_COL:
                    cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
                    cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
                    cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
                    break;
                case DATABASE_COL: {
                    int width = InternalBibtexFields.getFieldLength(FieldName.AUTHOR);
                    cm.getColumn(i).setPreferredWidth(width);
                    break;
                }
                default: {
                    int width = InternalBibtexFields.getFieldLength(FIELDS[i - PAD]);
                    cm.getColumn(i).setPreferredWidth(width);
                    break;
                }
            }
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

        if (preview.getEntry() == null || !preview.getBasePanel().isPresent()) {
            preview.setEntry(entry);
            preview.setBasePanel(panel);
            preview.setDatabaseContext(panel.getBibDatabaseContext());
        }
    }

    private void selectEntryInBasePanel(BibEntry entry) {
        BasePanel basePanel = entryHome.get(entry);
        frame.showBasePanel(basePanel);
        basePanel.requestFocus();
        basePanel.highlightEntry(entry);
    }

    public void dispose() {
        frame.getGlobalSearchBar().setSearchResultFrame(null);
        searchResultFrame.dispose();
        frame.getGlobalSearchBar().focus();
    }

    public void focus() {
        entryTable.requestFocus();
    }

    public SearchQuery getSearchQuery() {
        return searchQuery;
    }

    public boolean isGlobalSearch() {
        return globalSearch;
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
                selectEntryInBasePanel(model.getElementAt(row));
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
                    if (entry.hasField(FieldName.FILE)) {
                        FileListTableModel tableModel = new FileListTableModel();
                        entry.getField(FieldName.FILE).ifPresent(tableModel::setContent);
                        if (tableModel.getRowCount() == 0) {
                            return;
                        }
                        FileListEntry fl = tableModel.getEntry(0);
                        (new ExternalFileMenuItem(frame, entry, "", fl.getLink(), null,
                                p.getBibDatabaseContext(), fl.getType())).actionPerformed(null);
                    }
                    break;
                case URL_COL:
                    entry.getField(FieldName.URL).ifPresent(link -> { try {
                        JabRefDesktop.openExternalViewer(p.getBibDatabaseContext(), link, FieldName.URL);
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
                entry.getField(FieldName.FILE).ifPresent(fileList::setContent);
                // If there are one or more links, open the first one:
                for (int i = 0; i < fileList.getRowCount(); i++) {
                    FileListEntry flEntry = fileList.getEntry(i);
                    String description = flEntry.getDescription();
                    if ((description == null) || (description.trim().isEmpty())) {
                        description = flEntry.getLink();
                    }
                    menu.add(new ExternalFileMenuItem(p.frame(), entry, description, flEntry.getLink(),
                            flEntry.getType().get().getIcon(), p.getBibDatabaseContext(), flEntry.getType()));
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
                BasePanel basePanel = entryHome.get(entry);
                // Update the preview's database context:
                preview.setDatabaseContext(basePanel.getBibDatabaseContext());
                // Update the preview's entry:
                preview.setEntry(entry);
                preview.setBasePanel(entryHome.get(entry));
                preview.setDatabaseContext(entryHome.get(entry).getBibDatabaseContext());
                contentPane.setDividerLocation(0.5f);
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
                return StringUtil.capitalizeFirst(FIELDS[column - PAD]);
            } else if (column == DATABASE_COL) {
                return Localization.lang("Library");
            } else {
                return "";
            }
        }

        @Override
        public Object getColumnValue(BibEntry entry, int column) {
            if (column < PAD) {
                switch (column) {
                case DATABASE_COL:
                    return entryHome.get(entry).getTabTitle();
                case FILE_COL:
                    if (entry.hasField(FieldName.FILE)) {
                        FileListTableModel tmpModel = new FileListTableModel();
                        entry.getField(FieldName.FILE).ifPresent(tmpModel::setContent);
                        fileLabel.setToolTipText(tmpModel.getToolTipHTMLRepresentation());
                        if (tmpModel.getRowCount() > 0) {
                            if (tmpModel.getEntry(0).getType().isPresent()) {
                                fileLabel.setIcon(tmpModel.getEntry(0).getType().get().getIcon());
                            } else {
                                fileLabel.setIcon(IconTheme.JabRefIcon.FILE.getSmallIcon());
                            }
                        }
                        return fileLabel;
                    } else {
                        return null;
                    }
                case URL_COL: {
                    Optional<String> urlField = entry.getField(FieldName.URL);
                    if (urlField.isPresent()) {
                        urlLabel.setToolTipText(urlField.get());
                        return urlLabel;
                    }
                    return null;
                }
                default:
                    return null;
                }
            }
            else {
                String field = FIELDS[column - PAD];
                String fieldContent = entry.getLatexFreeField(field).orElse("");

                if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
                    // For name fields, tap into a MainTableFormat instance and use
                    // the same name formatting as is used in the entry table:
                    return MainTableNameFormatter.formatName(fieldContent);
                }
                return fieldContent;
            }
        }

        @Override
        public Class<?> getColumnClass(int i) {
            switch (i) {
                case FILE_COL:
                case URL_COL:
                    return JLabel.class;
                default:
                    return String.class;
            }
        }

        @Override
        public Comparator<?> getColumnComparator(int i) {
            return null;
        }
    }

}
