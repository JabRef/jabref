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
package net.sf.jabref.gui.preftabs;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexSingleField;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

class TableColumnsTab extends JPanel implements PrefsTab {

    private static final Log LOGGER = LogFactory.getLog(TableColumnsTab.class);

    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private final JTable colSetup;
    private int rowCount = -1;
    private int ncWidth = -1;
    private final List<TableRow> tableRows = new ArrayList<>(10);
    private final JabRefFrame frame;

    private final JCheckBox urlColumn;
    private final JCheckBox fileColumn;
    private final JCheckBox arxivColumn;

    private final JCheckBox extraFileColumns;
    private final JList<String> listOfFileColumns;

    private final JRadioButton preferUrl;
    private final JRadioButton preferDoi;

    /*** begin: special fields ***/
    private final JCheckBox specialFieldsEnabled;
    private final JCheckBox rankingColumn;
    private final JCheckBox qualityColumn;
    private final JCheckBox priorityColumn;
    private final JCheckBox relevanceColumn;
    private final JCheckBox printedColumn;
    private final JCheckBox readStatusColumn;
    private final JRadioButton syncKeywords;
    private final JRadioButton writeSpecialFields;
    private boolean oldSpecialFieldsEnabled;
    private boolean oldRankingColumn;
    private boolean oldQualityColumn;
    private boolean oldPriorityColumn;
    private boolean oldRelevanceColumn;
    private boolean oldPrintedColumn;
    private boolean oldReadStatusColumn;
    private boolean oldSyncKeyWords;
    private boolean oldWriteSpecialFields;


    /*** end: special fields ***/

    static class TableRow {

        private String name;
        private int length;


        public TableRow() {
            name = "";
            length = BibtexSingleField.DEFAULT_FIELD_LENGTH;
        }

        public TableRow(String name) {
            this.name = name;
            length = BibtexSingleField.DEFAULT_FIELD_LENGTH;
        }

        public TableRow(String name, int length) {
            this.name = name;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }


    /**
     * Customization of external program paths.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public TableColumnsTab(JabRefPreferences prefs, JabRefFrame frame) {
        this.prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        TableModel tm = new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return rowCount;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int row, int column) {
                int internalRow = row;
                if (internalRow == 0) {
                    return column == 0 ? InternalBibtexFields.NUMBER_COL : String.valueOf(ncWidth);
                }
                internalRow--;
                if (internalRow >= tableRows.size()) {
                    return "";
                }
                Object rowContent = tableRows.get(internalRow);
                if (rowContent == null) {
                    return "";
                }
                TableRow tr = (TableRow) rowContent;
                // Only two columns
                if (column == 0) {
                    return tr.getName();
                } else {
                    return tr.getLength() > 0 ? Integer.toString(tr.getLength()) : "";
                }
            }

            @Override
            public String getColumnName(int col) {
                return col == 0 ? Localization.lang("Field name") :
                    Localization.lang("Column width");
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return String.class;
                }
                return Integer.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return !((row == 0) && (col == 0));
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                tableChanged = true;
                // Make sure the vector is long enough.
                while (row >= tableRows.size()) {
                    tableRows.add(new TableRow("", -1));
                }

                if ((row == 0) && (col == 1)) {
                    ncWidth = Integer.parseInt(value.toString());
                    return;
                }

                TableRow rowContent = tableRows.get(row - 1);

                if (col == 0) {
                    rowContent.setName(value.toString());
                    if ("".equals(getValueAt(row, 1))) {
                        setValueAt(String.valueOf(BibtexSingleField.DEFAULT_FIELD_LENGTH), row, 1);
                    }
                }
                else {
                    if (value == null) {
                        rowContent.setLength(-1);
                    } else {
                        rowContent.setLength(Integer.parseInt(value.toString()));
                    }
                }
            }

        };

        colSetup = new JTable(tm);
        TableColumnModel cm = colSetup.getColumnModel();
        cm.getColumn(0).setPreferredWidth(140);
        cm.getColumn(1).setPreferredWidth(80);

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:pref","");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane
                (colSetup, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        colSetup.setPreferredScrollableViewportSize(new Dimension(250, 200));
        sp.setMinimumSize(new Dimension(250, 300));
        tabPanel.add(sp, BorderLayout.CENTER);
        JToolBar toolBar = new OSXCompatibleToolbar(SwingConstants.VERTICAL);
        toolBar.setFloatable(false);
        AddRowAction addRow = new AddRowAction();
        DeleteRowAction deleteRow = new DeleteRowAction();
        MoveRowUpAction moveUp = new MoveRowUpAction();
        MoveRowDownAction moveDown = new MoveRowDownAction();
        toolBar.setBorder(null);
        toolBar.add(addRow);
        toolBar.add(deleteRow);
        toolBar.addSeparator();
        toolBar.add(moveUp);
        toolBar.add(moveDown);
        tabPanel.add(toolBar, BorderLayout.EAST);

        fileColumn = new JCheckBox(Localization.lang("Show file column"));
        urlColumn = new JCheckBox(Localization.lang("Show URL/DOI column"));
        preferUrl = new JRadioButton(Localization.lang("Show URL first"));
        preferDoi = new JRadioButton(Localization.lang("Show DOI first"));
        ButtonGroup preferUrlDoiGroup = new ButtonGroup();
        preferUrlDoiGroup.add(preferUrl);
        preferUrlDoiGroup.add(preferDoi);

        urlColumn.addChangeListener(arg0 -> {
            preferUrl.setEnabled(urlColumn.isSelected());
            preferDoi.setEnabled(urlColumn.isSelected());
        });
        arxivColumn = new JCheckBox(Localization.lang("Show ArXiv column"));

        Collection<ExternalFileType> fileTypes = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        String[] fileTypeNames = new String[fileTypes.size()];
        int i = 0;
        for (ExternalFileType fileType : fileTypes) {
            fileTypeNames[i++] = fileType.getName();
        }
        listOfFileColumns = new JList<>(fileTypeNames);
        JScrollPane listOfFileColumnsScrollPane = new JScrollPane(listOfFileColumns);
        listOfFileColumns.setVisibleRowCount(3);
        extraFileColumns = new JCheckBox(Localization.lang("Show Extra columns"));
        extraFileColumns.addChangeListener(arg0 -> listOfFileColumns.setEnabled(extraFileColumns.isSelected()));

        /*** begin: special table columns and special fields ***/

        JButton helpButton = new HelpAction(Localization.lang("Help on special fields"), HelpFiles.specialFieldsHelp).getHelpButton();

        rankingColumn = new JCheckBox(Localization.lang("Show rank"));
        qualityColumn = new JCheckBox(Localization.lang("Show quality"));
        priorityColumn = new JCheckBox(Localization.lang("Show priority"));
        relevanceColumn = new JCheckBox(Localization.lang("Show relevance"));
        printedColumn = new JCheckBox(Localization.lang("Show printed status"));
        readStatusColumn = new JCheckBox(Localization.lang("Show read status"));

        // "sync keywords" and "write special" fields may be configured mutually exclusive only
        // The implementation supports all combinations (TRUE+TRUE and FALSE+FALSE, even if the latter does not make sense)
        // To avoid confusion, we opted to make the setting mutually exclusive
        syncKeywords = new JRadioButton(Localization.lang("Synchronize with keywords"));
        writeSpecialFields = new JRadioButton(Localization.lang("Write values of special fields as separate fields to BibTeX"));
        ButtonGroup group = new ButtonGroup();
        group.add(syncKeywords);
        group.add(writeSpecialFields);

        specialFieldsEnabled = new JCheckBox(Localization.lang("Enable special fields"));
        specialFieldsEnabled.addChangeListener(event -> {
            boolean isEnabled = specialFieldsEnabled.isSelected();
            rankingColumn.setEnabled(isEnabled);
            qualityColumn.setEnabled(isEnabled);
            priorityColumn.setEnabled(isEnabled);
            relevanceColumn.setEnabled(isEnabled);
            printedColumn.setEnabled(isEnabled);
            readStatusColumn.setEnabled(isEnabled);
            syncKeywords.setEnabled(isEnabled);
            writeSpecialFields.setEnabled(isEnabled);
        });

        builder.appendSeparator(Localization.lang("Special table columns"));
        builder.nextLine();
        builder.append(pan);

        DefaultFormBuilder specialTableColumnsBuilder = new DefaultFormBuilder(new FormLayout(
                "8dlu, 8dlu, 8cm, 8dlu, 8dlu, left:pref:grow", "pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref"));
        CellConstraints cc = new CellConstraints();

        specialTableColumnsBuilder.add(specialFieldsEnabled, cc.xyw(1, 1, 3));
        specialTableColumnsBuilder.add(rankingColumn, cc.xyw(2, 2, 2));
        specialTableColumnsBuilder.add(relevanceColumn, cc.xyw(2, 3, 2));
        specialTableColumnsBuilder.add(qualityColumn, cc.xyw(2, 4, 2));
        specialTableColumnsBuilder.add(priorityColumn, cc.xyw(2, 5, 2));
        specialTableColumnsBuilder.add(printedColumn, cc.xyw(2, 6, 2));
        specialTableColumnsBuilder.add(readStatusColumn, cc.xyw(2, 7, 2));
        specialTableColumnsBuilder.add(syncKeywords, cc.xyw(2, 10, 2));
        specialTableColumnsBuilder.add(writeSpecialFields, cc.xyw(2, 11, 2));
        specialTableColumnsBuilder.add(helpButton, cc.xyw(1, 12, 2));

        specialTableColumnsBuilder.add(fileColumn, cc.xyw(5, 1, 2));
        specialTableColumnsBuilder.add(urlColumn, cc.xyw(5, 2, 2));
        specialTableColumnsBuilder.add(preferUrl, cc.xy(6, 3));
        specialTableColumnsBuilder.add(preferDoi, cc.xy(6, 4));
        specialTableColumnsBuilder.add(arxivColumn, cc.xyw(5, 5, 2));

        specialTableColumnsBuilder.add(extraFileColumns, cc.xyw(5, 6, 2));
        specialTableColumnsBuilder.add(listOfFileColumnsScrollPane, cc.xywh(5, 7, 2, 6));

        builder.append(specialTableColumnsBuilder.getPanel());
        builder.nextLine();

        /*** end: special table columns and special fields ***/

        builder.appendSeparator(Localization.lang("Entry table columns"));
        builder.nextLine();
        builder.append(pan);
        builder.append(tabPanel);
        builder.nextLine();
        builder.append(pan);
        JButton buttonWidth = new JButton(new UpdateWidthsAction());
        JButton buttonOrder = new JButton(new UpdateOrderAction());
        builder.append(buttonWidth);
        builder.nextLine();
        builder.append(pan);
        builder.append(buttonOrder);
        builder.nextLine();
        builder.append(pan);
        builder.nextLine();
        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        fileColumn.setSelected(prefs.getBoolean(JabRefPreferences.FILE_COLUMN));
        urlColumn.setSelected(prefs.getBoolean(JabRefPreferences.URL_COLUMN));
        preferUrl.setSelected(!prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI));
        preferDoi.setSelected(prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI));
        fileColumn.setSelected(prefs.getBoolean(JabRefPreferences.FILE_COLUMN));
        arxivColumn.setSelected(prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN));

        extraFileColumns.setSelected(prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS));
        if (extraFileColumns.isSelected()) {
            List<String> desiredColumns = prefs.getStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            int listSize = listOfFileColumns.getModel().getSize();
            int[] indicesToSelect = new int[listSize];
            for (int i = 0; i < listSize; i++) {
                indicesToSelect[i] = listSize + 1;
                for (String desiredColumn : desiredColumns) {
                    if (listOfFileColumns.getModel().getElementAt(i).equals(desiredColumn)) {
                        indicesToSelect[i] = i;
                        break;
                    }
                }
            }
            listOfFileColumns.setSelectedIndices(indicesToSelect);
        }
        else {
            listOfFileColumns.setSelectedIndices(new int[] {});
        }

        /*** begin: special fields ***/

        oldRankingColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING);
        rankingColumn.setSelected(oldRankingColumn);

        oldQualityColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY);
        qualityColumn.setSelected(oldQualityColumn);

        oldPriorityColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY);
        priorityColumn.setSelected(oldPriorityColumn);

        oldRelevanceColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE);
        relevanceColumn.setSelected(oldRelevanceColumn);

        oldPrintedColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED);
        printedColumn.setSelected(oldPrintedColumn);

        oldReadStatusColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ);
        readStatusColumn.setSelected(oldReadStatusColumn);

        oldSyncKeyWords = prefs.getBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
        syncKeywords.setSelected(oldSyncKeyWords);

        oldWriteSpecialFields = prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS);
        writeSpecialFields.setSelected(oldWriteSpecialFields);

        // has to be called as last to correctly enable/disable the other settings
        oldSpecialFieldsEnabled = prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED);
        specialFieldsEnabled.setSelected(!oldSpecialFieldsEnabled);
        specialFieldsEnabled.setSelected(oldSpecialFieldsEnabled); // Call twice to make sure the ChangeListener is triggered

        /*** end: special fields ***/

        tableRows.clear();
        List<String> names = prefs.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<String> lengths = prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS);
        for (int i = 0; i < names.size(); i++) {
            if (i < lengths.size()) {
                tableRows.add(new TableRow(names.get(i), Integer.parseInt(lengths.get(i))));
            } else {
                tableRows.add(new TableRow(names.get(i)));
            }
        }
        rowCount = tableRows.size() + 5;
        ncWidth = prefs.getInt(JabRefPreferences.NUMBER_COL_WIDTH);

    }


    class DeleteRowAction extends AbstractAction {

        public DeleteRowAction() {
            super("Delete row", IconTheme.JabRefIcon.REMOVE_NOBOX.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Delete rows"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = colSetup.getSelectedRows();
            if (rows.length == 0) {
                return;
            }
            int offs = 0;
            for (int i = rows.length - 1; i >= 0; i--) {
                if ((rows[i] <= tableRows.size()) && (rows[i] != 0)) {
                    tableRows.remove(rows[i] - 1);
                    offs++;
                }
            }
            rowCount -= offs;
            if (rows.length > 1) {
                colSetup.clearSelection();
            }
            colSetup.revalidate();
            colSetup.repaint();
            tableChanged = true;
        }
    }

    class AddRowAction extends AbstractAction {

        public AddRowAction() {
            super("Add row", IconTheme.JabRefIcon.ADD_NOBOX.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Insert rows"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = colSetup.getSelectedRows();
            if (rows.length == 0) {
                // No rows selected, so we just add one at the end.
                rowCount++;
                colSetup.revalidate();
                colSetup.repaint();
                return;
            }
            for (int i = 0; i < rows.length; i++) {
                if (((rows[i] + i) - 1) < tableRows.size()) {
                    tableRows.add(Math.max(0, (rows[i] + i) - 1), new TableRow());
                }
            }
            rowCount += rows.length;
            if (rows.length > 1) {
                colSetup.clearSelection();
            }
            colSetup.revalidate();
            colSetup.repaint();
            tableChanged = true;
        }
    }

    abstract class AbstractMoveRowAction extends AbstractAction {

        public AbstractMoveRowAction(String string, Icon image) {
            super(string, image);
        }

        public void swap(int i, int j) {
            if ((i < 0) || (i >= tableRows.size())) {
                return;
            }
            if ((j < 0) || (j >= tableRows.size())) {
                return;
            }
            TableRow tmp = tableRows.get(i);
            tableRows.set(i, tableRows.get(j));
            tableRows.set(j, tmp);
        }
    }

    class MoveRowUpAction extends AbstractMoveRowAction {

        public MoveRowUpAction() {
            super("Up", IconTheme.JabRefIcon.UP.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Move up"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = colSetup.getSelectedRows();
            Arrays.sort(selected);
            // first element (#) not inside tableRows
            // don't move if a selected element is at bounce
            if ((selected.length > 0) && (selected[0] > 1)) {
                boolean[] newSelected = new boolean[colSetup.getRowCount()];
                for (int i : selected) {
                    swap(i - 1, i - 2);
                    newSelected[i - 1] = true;
                }
                // select all and remove unselected
                colSetup.setRowSelectionInterval(0, colSetup.getRowCount() - 1);
                for (int i = 0; i < colSetup.getRowCount(); i++) {
                    if (!newSelected[i]) {
                        colSetup.removeRowSelectionInterval(i, i);
                    }
                }
                colSetup.revalidate();
                colSetup.repaint();
                tableChanged = true;
            }
        }
    }

    class MoveRowDownAction extends AbstractMoveRowAction {

        public MoveRowDownAction() {
            super("Down", IconTheme.JabRefIcon.DOWN.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Down"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = colSetup.getSelectedRows();
            Arrays.sort(selected);
            final int last = selected.length - 1;
            boolean[] newSelected = new boolean[colSetup.getRowCount()];
            // don't move if a selected element is at bounce
            if ((selected.length > 0) && (selected[last] < tableRows.size())) {
                for (int i = last; i >= 0; i--) {
                    swap(selected[i] - 1, selected[i]);
                    newSelected[selected[i] + 1] = true;
                }
                // select all and remove unselected
                colSetup.setRowSelectionInterval(0, colSetup.getRowCount() - 1);
                for (int i = 0; i < colSetup.getRowCount(); i++) {
                    if (!newSelected[i]) {
                        colSetup.removeRowSelectionInterval(i, i);
                    }
                }
                colSetup.revalidate();
                colSetup.repaint();
                tableChanged = true;
            }
        }
    }

    class UpdateOrderAction extends AbstractAction {

        public UpdateOrderAction() {
            super(Localization.lang("Update to current column order"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BasePanel panel = frame.getCurrentBasePanel();
            if (panel == null) {
                return;
            }
            // idea: sort elements according to value stored in hash, keep
            // everything not inside hash/mainTable as it was
            final HashMap<String, Integer> map = new HashMap<>();

            // first element (#) not inside tableRows
            for (int i = 1; i < panel.mainTable.getColumnCount(); i++) {
                String name = panel.mainTable.getColumnName(i);
                if ((name != null) && !name.isEmpty()) {
                    map.put(name.toLowerCase(), i);
                }
            }
            Collections.sort(tableRows, (o1, o2) -> {
                Integer n1 = map.get(o1.getName());
                Integer n2 = map.get(o2.getName());
                if ((n1 == null) || (n2 == null)) {
                    return 0;
                }
                return n1.compareTo(n2);
            });

            colSetup.revalidate();
            colSetup.repaint();
            tableChanged = true;
        }
    }

    class UpdateWidthsAction extends AbstractAction {

        public UpdateWidthsAction() {
            super(Localization.lang("Update to current column widths"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BasePanel panel = frame.getCurrentBasePanel();
            if (panel == null) {
                return;
            }
            TableColumnModel colMod = panel.mainTable.getColumnModel();
            colSetup.setValueAt(String.valueOf(colMod.getColumn(0).getWidth()), 0, 1);
            for (int i = 1; i < colMod.getColumnCount(); i++) {
                try {
                    String name = panel.mainTable.getColumnName(i).toLowerCase();
                    int width = colMod.getColumn(i).getWidth();
                    if ((i <= tableRows.size()) && ((String) colSetup.getValueAt(i, 0)).equalsIgnoreCase(name)) {
                        colSetup.setValueAt(String.valueOf(width), i, 1);
                    } else { // Doesn't match; search for a matching col in our table
                        for (int j = 0; j < colSetup.getRowCount(); j++) {
                            if ((j < tableRows.size()) && ((String) colSetup.getValueAt(j, 0)).equalsIgnoreCase(name)) {
                                colSetup.setValueAt(String.valueOf(width), j, 1);
                                break;
                            }
                        }
                    }
                } catch (Throwable ex) {
                    LOGGER.warn("Problem with table columns", ex);
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
    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.FILE_COLUMN, fileColumn.isSelected());
        prefs.putBoolean(JabRefPreferences.URL_COLUMN, urlColumn.isSelected());
        prefs.putBoolean(JabRefPreferences.PREFER_URL_DOI, preferDoi.isSelected());
        prefs.putBoolean(JabRefPreferences.ARXIV_COLUMN, arxivColumn.isSelected());

        prefs.putBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS, extraFileColumns.isSelected());
        if (extraFileColumns.isSelected() && !listOfFileColumns.isSelectionEmpty()) {
            int numberSelected = listOfFileColumns.getSelectedIndices().length;
            List<String> selections = new ArrayList<>(numberSelected);
            for (int i = 0; i < numberSelected; i++) {
                selections.add(listOfFileColumns.getModel().getElementAt(listOfFileColumns.getSelectedIndices()[i]));
            }
            prefs.putStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS, selections);
        } else {
            prefs.putStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS, new ArrayList<>());
        }

        /*** begin: special fields ***/

        boolean newSpecialFieldsEnabled = specialFieldsEnabled.isSelected();
        boolean newRankingColumn = rankingColumn.isSelected();
        boolean newQualityColumn = qualityColumn.isSelected();
        boolean newPriorityColumn = priorityColumn.isSelected();
        boolean newRelevanceColumn = relevanceColumn.isSelected();
        boolean newPrintedColumn = printedColumn.isSelected();
        boolean newReadStatusColumn = readStatusColumn.isSelected();
        boolean newSyncKeyWords = syncKeywords.isSelected();
        boolean newWriteSpecialFields = writeSpecialFields.isSelected();

        boolean restartRequired;
        restartRequired = (oldSpecialFieldsEnabled != newSpecialFieldsEnabled) ||
                (oldRankingColumn != newRankingColumn) ||
                (oldQualityColumn != newQualityColumn) ||
                (oldPriorityColumn != newPriorityColumn) ||
                (oldRelevanceColumn != newRelevanceColumn) ||
                (oldPrintedColumn != newPrintedColumn) ||
                (oldReadStatusColumn != newReadStatusColumn) ||
                (oldSyncKeyWords != newSyncKeyWords) ||
                (oldWriteSpecialFields != newWriteSpecialFields);
        if (restartRequired) {
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You have changed settings for special fields.")
                    .concat(" ")
                    .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                    Localization.lang("Changed special field settings"),
                    JOptionPane.WARNING_MESSAGE);
        }

        // restart required implies that the settings have been changed
        // the seetings need to be stored
        if (restartRequired) {
            prefs.putBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, newSpecialFieldsEnabled);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, newRankingColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, newPriorityColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, newQualityColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, newRelevanceColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED, newPrintedColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ, newReadStatusColumn);
            prefs.putBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, newSyncKeyWords);
            prefs.putBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, newWriteSpecialFields);
        }

        /*** end: special fields ***/

        if (colSetup.isEditing()) {
            int col = colSetup.getEditingColumn();
            int row = colSetup.getEditingRow();
            colSetup.getCellEditor(row, col).stopCellEditing();
        }

        // Now we need to make sense of the contents the user has made to the
        // table setup table.
        if (tableChanged) {
            // First we remove all rows with empty names.
            int i = 0;
            while (i < tableRows.size()) {
                if (tableRows.get(i).getName().isEmpty()) {
                    tableRows.remove(i);
                } else {
                    i++;
                }
            }
            // Then we make arrays
            List<String> names = new ArrayList<>(tableRows.size());
            List<String> widths = new ArrayList<>(tableRows.size());
            List<Integer> nWidths = new ArrayList<>(tableRows.size());

            prefs.putInt(JabRefPreferences.NUMBER_COL_WIDTH, ncWidth);
            for (TableRow tr : tableRows) {
                names.add(tr.getName().toLowerCase());
                nWidths.add(tr.getLength());
                widths.add(String.valueOf(tr.getLength()));
            }

            // Finally, we store the new preferences.
            prefs.putStringList(JabRefPreferences.COLUMN_NAMES, names);
            prefs.putStringList(JabRefPreferences.COLUMN_WIDTHS, widths);
        }

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table columns");
    }
}
