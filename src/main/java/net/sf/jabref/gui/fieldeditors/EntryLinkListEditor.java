package net.sf.jabref.gui.fieldeditors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.autocompleter.AutoCompleteListener;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryLinkList;
import net.sf.jabref.model.entry.ParsedEntryLink;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class EntryLinkListEditor extends JTable implements FieldEditor {

    private static final Log LOGGER = LogFactory.getLog(EntryLinkListEditor.class);

    private final FieldNameLabel label;
    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final String fieldName;
    private final EntryEditor entryEditor;
    private final JPanel panel;
    private final EntryLinkListTableModel tableModel;
    private final JPopupMenu menu = new JPopupMenu();
    private final boolean singleEntry;
    private final JButton add = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
    private final JButton remove = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());

    private static final String layoutFormat = "\\begin{author}\\format[Authors(2,1),LatexToUnicode]{\\author}\\end{author}\\begin{title}, \"\\format[LatexToUnicode]{\\title}\"\\end{title}\\begin{year}, \\year\\end{year}";

    public EntryLinkListEditor(JabRefFrame frame, BibDatabaseContext databaseContext, String fieldName, String content,
            EntryEditor entryEditor, boolean singleEntry) {
        this.frame = frame;
        this.databaseContext = databaseContext;
        this.fieldName = fieldName;
        this.entryEditor = entryEditor;
        this.singleEntry = singleEntry;
        label = new FieldNameLabel(fieldName);
        tableModel = new EntryLinkListTableModel(EntryLinkList.parse(content, databaseContext.getDatabase()));
        setText(content);
        setModel(tableModel);
        JScrollPane sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());

        add.setToolTipText(("New entry link (INSERT)"));
        remove.setToolTipText(("Remove entry link (DELETE)"));
        add.setMargin(new Insets(0, 0, 0, 0));
        remove.setMargin(new Insets(0, 0, 0, 0));
        add.addActionListener(e -> addEntry());
        remove.addActionListener(e -> removeEntries());

        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("fill:pref:grow,1dlu,fill:pref:grow",
                        "fill:pref,fill:pref,1dlu,fill:pref"));

        if (!singleEntry) {
            JButton up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());

            JButton down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
            up.setMargin(new Insets(0, 0, 0, 0));
            down.setMargin(new Insets(0, 0, 0, 0));
            up.addActionListener(e -> moveEntry(-1));
            down.addActionListener(e -> moveEntry(1));
            builder.add(up).xy(1, 1);
            builder.add(down).xy(1, 2);
        }
        builder.add(add).xy(3, 1);
        builder.add(remove).xy(3, 2);
        JButton button = new JButton(Localization.lang("Select"));
        button.addActionListener(e -> selectEntry());
        builder.add(button).xyw(1, 4, 3);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sPane, BorderLayout.CENTER);
        panel.add(builder.getPanel(), BorderLayout.EAST);

        // Add an input/action pair for deleting entries:
        getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getActionMap().put("delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int row = getSelectedRow();
                removeEntries();
                row = Math.min(row, getRowCount() - 1);
                if (row >= 0) {
                    setRowSelectionInterval(row, row);
                }
            }
        });

        // Add an input/action pair for inserting an entry:
        getInputMap().put(KeyStroke.getKeyStroke("INSERT"), "insert");
        getActionMap().put("insert", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int row = getSelectedRow();
                addEntry();
                setRowSelectionInterval(row, row);
            }
        });

        // Add input/action pair for moving an entry up:
        getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.FILE_LIST_EDITOR_MOVE_ENTRY_UP), "move up");
        getActionMap().put("move up", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(-1);
            }
        });

        // Add input/action pair for moving an entry down:
        getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.FILE_LIST_EDITOR_MOVE_ENTRY_DOWN), "move down");
        getActionMap().put("move down", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(1);
            }
        });

        JMenuItem openLink = new JMenuItem(Localization.lang("Select"));
        menu.add(openLink);
        openLink.addActionListener(e -> selectEntry());

        // Set table row height
        FontMetrics metrics = getFontMetrics(getFont());
        setRowHeight(Math.max(getRowHeight(), metrics.getHeight()));

        updateButtonStates();
    }


    private void selectEntry() {
        int selectedRow = getSelectedRow();

        if (selectedRow != -1) {
            String crossref = tableModel.getEntry(selectedRow).getKey();

            frame.getCurrentBasePanel().getDatabase().getEntryByKey(crossref)
                    .ifPresent(entry -> frame.getCurrentBasePanel().highlightEntry(entry));
        }
    }

    public void adjustColumnWidth() {
        for (int column = 0; column < this.getColumnCount(); column++) {
            int width = 0;
            for (int row = 0; row < this.getRowCount(); row++) {
                TableCellRenderer renderer = this.getCellRenderer(row, column);
                Component comp = this.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            this.columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    /*
      * Returns the component to be added to a container. Might be a JScrollPane
    * or the component itself.
    */
    @Override
    public JComponent getPane() {
        return panel;
    }

    /*
     * Returns the text component itself.
    */
    @Override
    public JComponent getTextComponent() {
        return this;
    }

    @Override
    public JLabel getLabel() {
        return label;
    }

    @Override
    public void setLabelColor(Color color) {
        label.setForeground(color);
    }

    @Override
    public String getText() {
        return tableModel.getText();
    }

    @Override
    public void setText(String newText) {
        tableModel.setContent(EntryLinkList.parse(newText, databaseContext.getDatabase()));
        adjustColumnWidth();
        updateButtonStates();
    }

    @Override
    public void append(String text) {
        // Do nothing
    }

    @Override
    public void updateFont() {
        // Do nothing
    }

    @Override
    public void paste(String textToInsert) {
        // Do nothing
    }

    @Override
    public String getSelectedText() {
        return null;
    }


    private void addEntry() {
        int row = getSelectedRow();
        if (row == -1) {
            row = 0;
        }
        ParsedEntryLink entry = new ParsedEntryLink("", databaseContext.getDatabase());
        tableModel.addEntry(row, entry);
        entryEditor.updateField(this);
        adjustColumnWidth();
        updateButtonStates();
    }


    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null) {
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.removeEntry(rows[i]);
            }
        }
        entryEditor.updateField(this);
        adjustColumnWidth();
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (singleEntry) {
            if (tableModel.isEmpty()) {
                add.setEnabled(true);
                remove.setEnabled(false);
            } else {
                add.setEnabled(false);
                remove.setEnabled(true);
            }
        }
    }
    private void moveEntry(int i) {
        int[] sel = getSelectedRows();
        if ((sel.length != 1) || (tableModel.getRowCount() < 2)) {
            return;
        }
        int toIdx = sel[0] + i;
        if (toIdx >= tableModel.getRowCount()) {
            toIdx -= tableModel.getRowCount();
        }
        if (toIdx < 0) {
            toIdx += tableModel.getRowCount();
        }
        ParsedEntryLink entry = tableModel.getEntry(sel[0]);
        tableModel.removeEntry(sel[0]);
        tableModel.addEntry(toIdx, entry);
        entryEditor.updateField(this);
        setRowSelectionInterval(toIdx, toIdx);
        adjustColumnWidth();
    }


    class TableClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0) {
                    Optional<BibEntry>  entry = tableModel.getEntry(row).getLinkedEntry();
                    if (entry.isPresent()) {
                        // Select entry in main table
                        frame.getCurrentBasePanel().highlightEntry(entry.get());
                    } else {
                        // Focus BibTeX key field
                    }
                }
            } else if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        private void processPopupTrigger(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row >= 0) {
                setRowSelectionInterval(row, row);
                menu.show(EntryLinkListEditor.this, e.getX(), e.getY());
            }
        }
    }

    @Override
    public void undo() {
        // Do nothing
    }

    @Override
    public void redo() {
        // Do nothing
    }

    @Override
    public void setAutoCompleteListener(AutoCompleteListener listener) {
        // Do nothing
    }

    @Override
    public void clearAutoCompleteSuggestion() {
        // Do nothing
    }

    @Override
    public void setActiveBackgroundColor() {
        // Do nothing
    }

    @Override
    public void setValidBackgroundColor() {
        // Do nothing
    }

    @Override
    public void setInvalidBackgroundColor() {
        // Do nothing
    }

    @Override
    public void updateFontColor() {
        // Do nothing
    }


    private class EntryLinkListTableModel extends DefaultTableModel {

        private final List<ParsedEntryLink> internalList = Collections.synchronizedList(new ArrayList<>());


        public EntryLinkListTableModel(List<ParsedEntryLink> originalList) {
            addEntries(originalList);
        }

        public String getText() {
            synchronized (internalList) {
                String result = EntryLinkList.serialize(internalList);
                return result;
            }
        }

        public void addEntries(List<ParsedEntryLink> newList) {
            internalList.addAll(newList);
            if (SwingUtilities.isEventDispatchThread()) {
                fireTableDataChanged();
            } else {
                SwingUtilities.invokeLater(() -> fireTableDataChanged());
            }

        }

        public void setContent(List<ParsedEntryLink> newList) {

            internalList.clear();
            internalList.addAll(newList);
            if (SwingUtilities.isEventDispatchThread()) {
                fireTableDataChanged();
            } else {
                SwingUtilities.invokeLater(() -> fireTableDataChanged());
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            if (internalList == null) {
                return 0;
            }
            synchronized (internalList) {
                return internalList.size();
            }
        }

        @Override
        public Class<String> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            synchronized (internalList) {
                ParsedEntryLink entry = internalList.get(rowIndex);
                switch (columnIndex) {
                case 0:
                    return entry.getKey();
                case 1:
                    return entry.getLinkedEntry()
                            .map(bibEntry -> formatEntry(bibEntry, entry.getDataBase()))
                            .orElse("Unknown entry");
                default:
                    return null;
                }
            }
        }

        public ParsedEntryLink getEntry(int index) {
            synchronized (internalList) {
                return internalList.get(index);
            }
        }

        public void removeEntry(int index) {
            internalList.remove(index);
            if (SwingUtilities.isEventDispatchThread()) {
                fireTableRowsDeleted(index, index);
            } else {
                SwingUtilities.invokeLater(() -> fireTableRowsDeleted(index, index));
            }
        }

        public boolean isEmpty() {
            return internalList.isEmpty();
        }

        /**
         * Add an entry to the table model, and fire a change event. The change event
         * is fired on the event dispatch thread.
         * @param index The row index to insert the entry at.
         * @param entry The entry to insert.
         */
        public void addEntry(final int index, final ParsedEntryLink entry) {
            synchronized (internalList) {
                internalList.add(index, entry);
                if (SwingUtilities.isEventDispatchThread()) {
                    fireTableDataChanged();
                } else {
                    SwingUtilities.invokeLater(() -> fireTableDataChanged());
                }
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return (column == 0);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            synchronized (internalList) {
                if (columnIndex == 0) {
                    internalList.get(rowIndex).setKey((String) aValue);
                    if (SwingUtilities.isEventDispatchThread()) {
                        fireTableRowsUpdated(rowIndex, rowIndex);
                    } else {
                        SwingUtilities.invokeLater(() -> fireTableRowsUpdated(rowIndex, rowIndex));
                    }

                }
            }
        }
    }

    private static String formatEntry(BibEntry entry, BibDatabase database) {
        StringReader sr = new StringReader(layoutFormat);
        try {
            Layout layout = new LayoutHelper(sr,
                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
                            .getLayoutFromText();
            return layout.doLayout(entry, database);
        } catch (IOException e) {
            LOGGER.warn("Problem generating entry layout", e);
        }
        return "";
    }

}
