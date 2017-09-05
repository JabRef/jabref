package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.CompoundEdit;

import org.jabref.Globals;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableRemoveString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.gui.util.WindowLocation;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;
import org.jabref.preferences.JabRefPreferences;

class StringDialog extends JabRefDialog {

    private static final String STRINGS_TITLE = Localization.lang("Strings for library");
    // A reference to the entry this object works on.
    private final BibDatabase base;
    private final BasePanel panel;
    private final StringTable table;
    private final HelpAction helpAction;

    private final SaveDatabaseAction saveAction = new SaveDatabaseAction(this);

    // The action concerned with closing the window.
    private final CloseAction closeAction = new CloseAction();
    private List<BibtexString> strings;


    public StringDialog(JabRefFrame frame, BasePanel panel, BibDatabase base) {
        super(frame, StringDialog.class);
        this.panel = panel;
        this.base = base;

        sortStrings();

        helpAction = new HelpAction(Localization.lang("Help"), HelpFile.STRING_EDITOR);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeAction.actionPerformed(null);
            }
        });

        // We replace the default FocusTraversalPolicy with a subclass
        // that only allows the StringTable to gain keyboard focus.
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {

            @Override
            protected boolean accept(Component c) {
                return super.accept(c) && (c instanceof StringTable);
            }
        });

        JPanel pan = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        pan.setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        con.weighty = 1;
        con.weightx = 1;

        StringTableModel stm = new StringTableModel(this, base);
        table = new StringTable(stm);
        if (!base.hasNoStrings()) {
            table.setRowSelectionInterval(0, 0);
        }

        gbl.setConstraints(table.getPane(), con);
        pan.add(table.getPane());

        JToolBar tlb = new OSXCompatibleToolbar();
        InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = tlb.getActionMap();
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.STRING_DIALOG_ADD_STRING), "add");
        NewStringAction newStringAction = new NewStringAction(this);
        am.put("add", newStringAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.STRING_DIALOG_REMOVE_STRING), "remove");
        RemoveStringAction removeStringAction = new RemoveStringAction(this);
        am.put("remove", removeStringAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.SAVE_DATABASE), "save");
        am.put("save", saveAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        am.put("help", helpAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.UNDO), "undo");
        UndoAction undoAction = new UndoAction();
        am.put("undo", undoAction);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.REDO), "redo");
        RedoAction redoAction = new RedoAction();
        am.put("redo", redoAction);

        tlb.add(newStringAction);
        tlb.add(removeStringAction);
        tlb.addSeparator();
        tlb.add(helpAction);
        Container conPane = getContentPane();
        conPane.add(tlb, BorderLayout.NORTH);
        conPane.add(pan, BorderLayout.CENTER);

        setTitle(STRINGS_TITLE + ": "
                + panel.getBibDatabaseContext().getDatabaseFile().map(File::getName).orElse(GUIGlobals.UNTITLED_TITLE));
        WindowLocation pw = new WindowLocation(this, JabRefPreferences.STRINGS_POS_X, JabRefPreferences.STRINGS_POS_Y,
                JabRefPreferences.STRINGS_SIZE_X, JabRefPreferences.STRINGS_SIZE_Y);
        pw.displayWindowAtStoredLocation();
    }

    private static boolean isNumber(String name) {
        // A pure integer number cannot be used as a string label,
        // since Bibtex will read it as a number.
        try {
            Integer.parseInt(name);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void sortStrings() {
        // Rebuild our sorted set of strings:
        strings = new ArrayList<>();
        for (String s : base.getStringKeySet()) {
            strings.add(base.getString(s));
        }
        Collections.sort(strings, new BibtexStringComparator(false));
    }

    public void refreshTable() {
        sortStrings();
        table.revalidate();
        table.clearSelection();
        table.repaint();
    }

    public void saveDatabase() {
        panel.runCommand(Actions.SAVE);
    }

    public void assureNotEditing() {
        if (table.isEditing()) {
            int col = table.getEditingColumn();
            int row = table.getEditingRow();
            table.getCellEditor(row, col).stopCellEditing();
        }
    }

    static class SaveDatabaseAction extends AbstractAction {

        private final StringDialog parent;


        public SaveDatabaseAction(StringDialog parent) {
            super("Save library", IconTheme.JabRefIcon.SAVE.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Save library"));
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            parent.saveDatabase();
        }
    }

    class StringTable extends JTable {

        private final JScrollPane sp = new JScrollPane(this);


        public StringTable(StringTableModel stm) {
            super(stm);
            setShowVerticalLines(true);
            setShowHorizontalLines(true);
            setColumnSelectionAllowed(true);
            DefaultCellEditor dce = new DefaultCellEditor(new JTextField());
            dce.setClickCountToStart(2);
            setDefaultEditor(String.class, dce);
            TableColumnModel cm = getColumnModel();
            cm.getColumn(0).setPreferredWidth(800);
            cm.getColumn(1).setPreferredWidth(2000);
            sp.getViewport().setBackground(Globals.prefs.getColor(JabRefPreferences.TABLE_BACKGROUND));
            getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
            getActionMap().put("close", closeAction);
            getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
            getActionMap().put("help", helpAction);
        }

        public JComponent getPane() {
            return sp;
        }

    }

    class StringTableModel extends AbstractTableModel {

        private final BibDatabase tbase;
        private final StringDialog parent;


        public StringTableModel(StringDialog parent, BibDatabase base) {
            this.parent = parent;
            this.tbase = base;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return col == 0 ? strings.get(row).getName() : strings.get(row).getContent();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                // Change name of string.
                if (!value.equals(strings.get(row).getName())) {
                    if (tbase.hasStringLabel((String) value)) {
                        JOptionPane.showMessageDialog(parent, Localization.lang("A string with that label already exists"),
                                Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                    } else if (((String) value).contains(" ")) {
                        JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain spaces."),
                                Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                    } else if (((String) value).contains("#")) {
                        JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain the '#' character."),
                                Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                    } else if (isNumber((String) value)) {
                        JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot be a number."),
                                Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Store undo information.
                        BibtexString subject = strings.get(row);
                        panel.getUndoManager().addEdit(
                                new UndoableStringChange(panel, subject, true, subject.getName(), (String) value));
                        subject.setName((String) value);
                        panel.markBaseChanged();
                        refreshTable();
                    }
                }
            } else {
                // Change content of string.
                BibtexString subject = strings.get(row);

                if (!value.equals(subject.getContent())) {
                    try {
                        new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences())
                                .format((String) value, "__dummy");
                    } catch (InvalidFieldValueException ex) {
                        return;
                    }
                    // Store undo information.
                    panel.getUndoManager().addEdit(
                            new UndoableStringChange(panel, subject, false, subject.getContent(), (String) value));

                    subject.setContent((String) value);
                    panel.markBaseChanged();
                }
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return strings.size();
        }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? Localization.lang("Label") :
                Localization.lang("Content");
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }
    }

    class CloseAction extends AbstractAction {

        public CloseAction() {
            super("Close window");
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close dialog"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.stringsClosing();
            dispose();
        }
    }

    class NewStringAction extends AbstractAction {

        private final StringDialog parent;


        public NewStringAction(StringDialog parent) {
            super("New string", IconTheme.JabRefIcon.ADD.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("New string"));
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane.showInputDialog(parent, Localization.lang("Please enter the string's label"));
            if (name == null) {
                return;
            }
            if (isNumber(name)) {
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot be a number."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (name.contains("#")) {
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain the '#' character."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (name.contains(" ")) {
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain spaces."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                BibtexString bs = new BibtexString(name, "");

                // Store undo information:
                panel.getUndoManager().addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));

                base.addString(bs);
                refreshTable();
                panel.markBaseChanged();
            } catch (KeyCollisionException ex) {
                JOptionPane.showMessageDialog(parent,
                        Localization.lang("A string with that label already exists"),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class RemoveStringAction extends AbstractAction {

        private final StringDialog parent;


        public RemoveStringAction(StringDialog parent) {
            super("Remove selected strings", IconTheme.JabRefIcon.REMOVE.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Remove selected strings"));
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] sel = table.getSelectedRows();
            if (sel.length > 0) {

                // Make sure no cell is being edited, as caused by the
                // keystroke. This makes the content hang on the screen.
                assureNotEditing();

                String msg = (sel.length > 1 ? Localization.lang("Really delete the %0 selected entries?",
                        Integer.toString(sel.length)) : Localization.lang("Really delete the selected entry?"));
                int answer = JOptionPane.showConfirmDialog(parent, msg, Localization.lang("Delete strings"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    CompoundEdit ce = new CompoundEdit();
                    for (int i = sel.length - 1; i >= 0; i--) {
                        // Delete the strings backwards to avoid moving indexes.

                        BibtexString subject = strings.get(sel[i]);

                        // Store undo information:
                        ce.addEdit(new UndoableRemoveString(panel, base, subject));

                        base.removeString(subject.getId());
                    }
                    ce.end();
                    panel.getUndoManager().addEdit(ce);

                    refreshTable();
                    if (!base.hasNoStrings()) {
                        table.setRowSelectionInterval(0, 0);
                    }
                }
            }
        }
    }

    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Undo", IconTheme.JabRefIcon.UNDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Undo"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.UNDO);
        }
    }

    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo", IconTheme.JabRefIcon.REDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Redo"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.REDO);
        }
    }
}
