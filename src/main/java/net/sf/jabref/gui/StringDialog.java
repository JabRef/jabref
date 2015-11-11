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
package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
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

import net.sf.jabref.*;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.gui.undo.UndoableRemoveString;
import net.sf.jabref.gui.undo.UndoableStringChange;
import net.sf.jabref.bibtex.comparator.BibtexStringComparator;
import net.sf.jabref.logic.id.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexString;

class StringDialog extends JDialog {

    // A reference to the entry this object works on.
    private final BibtexDatabase base;
    private final JabRefFrame frame;
    private final BasePanel panel;
    private final JabRefPreferences prefs;
    private Object[] strings;

    JLabel lab;
    private final StringTable table;
    private final HelpAction helpAction;


    public StringDialog(JabRefFrame frame, BasePanel panel, BibtexDatabase base, JabRefPreferences prefs) {
        super(frame);
        this.frame = frame;
        this.panel = panel;
        this.base = base;
        this.prefs = prefs;

        sortStrings();

        helpAction = new HelpAction(frame.helpDiag, GUIGlobals.stringEditorHelp, Localization.lang("Help"));

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

        setLocation(prefs.getInt(JabRefPreferences.STRINGS_POS_X), prefs.getInt(JabRefPreferences.STRINGS_POS_Y));
        setSize(prefs.getInt(JabRefPreferences.STRINGS_SIZE_X), prefs.getInt(JabRefPreferences.STRINGS_SIZE_Y));

        JPanel pan = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        pan.setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        con.weighty = 1;
        con.weightx = 1;

        StringTableModel stm = new StringTableModel(this, base);
        table = new StringTable(stm);
        if (base.getStringCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        gbl.setConstraints(table.getPane(), con);
        pan.add(table.getPane());

        JToolBar tlb = new JToolBar();
        InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = tlb.getActionMap();
        im.put(prefs.getKey(KeyBinds.STRING_DIALOG_ADD_STRING), "add");
        NewStringAction newStringAction = new NewStringAction(this);
        am.put("add", newStringAction);
        im.put(prefs.getKey(KeyBinds.STRING_DIALOG_REMOVE_STRING), "remove");
        RemoveStringAction removeStringAction = new RemoveStringAction(this);
        am.put("remove", removeStringAction);
        im.put(prefs.getKey(KeyBinds.SAVE_DATABASE), "save");
        am.put("save", saveAction);
        im.put(prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        am.put("close", closeAction);
        im.put(prefs.getKey(KeyBinds.HELP), "help");
        am.put("help", helpAction);
        im.put(prefs.getKey(KeyBinds.UNDO), "undo");
        UndoAction undoAction = new UndoAction();
        am.put("undo", undoAction);
        im.put(prefs.getKey(KeyBinds.REDO), "redo");
        RedoAction redoAction = new RedoAction();
        am.put("redo", redoAction);

        tlb.add(newStringAction);
        tlb.add(removeStringAction);
        tlb.addSeparator();
        tlb.add(helpAction);
        Container conPane = getContentPane();
        conPane.add(tlb, BorderLayout.NORTH);
        conPane.add(pan, BorderLayout.CENTER);

        if (panel.getFile() != null) {
            setTitle(GUIGlobals.stringsTitle + ": " + panel.getFile().getName());
        } else {
            setTitle(GUIGlobals.stringsTitle + ": " + GUIGlobals.untitledTitle);
        }
    }


    class StringTable extends JTable {

        final JScrollPane sp = new JScrollPane(this);


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
            getInputMap().put(frame.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
            getActionMap().put("close", closeAction);
            getInputMap().put(frame.prefs.getKey(KeyBinds.HELP), "help");
            getActionMap().put("help", helpAction);
        }

        public JComponent getPane() {
            return sp;
        }

    }


    private void sortStrings() {
        // Rebuild our sorted set of strings:
        TreeSet<BibtexString> stringsSet = new TreeSet<>(new BibtexStringComparator(false));
        for (String s : base.getStringKeySet()) {
            stringsSet.add(base.getString(s));
        }
        strings = stringsSet.toArray();
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


    class StringTableModel extends AbstractTableModel {

        final BibtexDatabase tbase;
        final StringDialog parent;


        public StringTableModel(StringDialog parent, BibtexDatabase base) {
            this.parent = parent;
            this.tbase = base;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return col == 0 ? ((BibtexString) strings[row]).getName() : ((BibtexString) strings[row]).getContent();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                // Change name of string.
                if (!value.equals(((BibtexString) strings[row]).getName())) {
                    if (tbase.hasStringLabel((String) value)) {
                        // @formatter:off
                        JOptionPane.showMessageDialog(parent, Localization.lang("A string with that label "
                                + "already exists"),
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
                        // @formatter:on
                    } else {
                        // Store undo information.
                        BibtexString subject = (BibtexString) strings[row];
                        panel.undoManager.addEdit(
                                new UndoableStringChange(panel, subject, true, subject.getName(), (String) value));
                        subject.setName((String) value);
                        panel.markBaseChanged();
                        refreshTable();
                    }
                }
            } else {
                // Change content of string.
                BibtexString subject = (BibtexString) strings[row];

                if (!value.equals(subject.getContent())) {
                    try {
                        new LatexFieldFormatter().format((String) value, "__dummy");
                    } catch (IllegalArgumentException ex) {
                        return;
                    }
                    // Store undo information.
                    panel.undoManager.addEdit(
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
            return strings.length;
        }

        @Override
        public String getColumnName(int col) {
            // @formatter:off
            return col == 0 ? Localization.lang("Name") :
                Localization.lang("Content");
            // @formatter:on
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }
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

    void assureNotEditing() {
        if (table.isEditing()) {
            int col = table.getEditingColumn();
            int row = table.getEditingRow();
            table.getCellEditor(row, col).stopCellEditing();
        }
    }


    // The action concerned with closing the window.
    private final CloseAction closeAction = new CloseAction(this);


    class CloseAction extends AbstractAction {

        final StringDialog parent;


        public CloseAction(StringDialog parent) {
            super("Close window");
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close dialog"));
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.stringsClosing();
            dispose();
            Point p = getLocation();
            Dimension d = getSize();
            prefs.putInt(JabRefPreferences.STRINGS_POS_X, p.x);
            prefs.putInt(JabRefPreferences.STRINGS_POS_Y, p.y);
            prefs.putInt(JabRefPreferences.STRINGS_SIZE_X, d.width);
            prefs.putInt(JabRefPreferences.STRINGS_SIZE_Y, d.height);
        }
    }

    class NewStringAction extends AbstractAction {

        final StringDialog parent;


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
                // @formatter:off
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot be a number."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                // @formatter:on
                return;
            }
            if (name.contains("#")) {
                // @formatter:off
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain the '#' character."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                // @formatter:on
                return;
            }
            if (name.contains(" ")) {
                // @formatter:off
                JOptionPane.showMessageDialog(parent, Localization.lang("The label of the string cannot contain spaces."),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                // @formatter:on
                return;
            }
            try {
                String newId = IdGenerator.next();
                BibtexString bs = new BibtexString(newId, name, "");

                // Store undo information:
                panel.undoManager.addEdit(new UndoableInsertString(panel, panel.database, bs));

                base.addString(bs);
                refreshTable();
                //		table.revalidate();
                panel.markBaseChanged();
            } catch (KeyCollisionException ex) {
                // @formatter:off
                JOptionPane.showMessageDialog(parent, Localization.lang("A string with that label "
                        + "already exists"),
                        Localization.lang("Label"), JOptionPane.ERROR_MESSAGE);
                // @formatter:on
            }
        }
    }


    SaveDatabaseAction saveAction = new SaveDatabaseAction(this);


    static class SaveDatabaseAction extends AbstractAction {

        final StringDialog parent;


        public SaveDatabaseAction(StringDialog parent) {
            super("Save database", IconTheme.JabRefIcon.SAVE.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Save database"));
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            parent.saveDatabase();
        }
    }

    class RemoveStringAction extends AbstractAction {

        final StringDialog parent;


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

                // @formatter:off
                String msg = Localization.lang("Really delete the selected") + ' '
                        + (sel.length > 1 ? sel.length + " " + Localization.lang("entries")
                        : Localization.lang("entry")) + '?';
                int answer = JOptionPane.showConfirmDialog(parent, msg, Localization.lang("Delete strings"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                // @formatter:on
                if (answer == JOptionPane.YES_OPTION) {
                    CompoundEdit ce = new CompoundEdit();
                    for (int i = sel.length - 1; i >= 0; i--) {
                        // Delete the strings backwards to avoid moving indexes.

                        BibtexString subject = (BibtexString) strings[sel[i]];

                        // Store undo information:
                        ce.addEdit(new UndoableRemoveString(panel, base, subject));

                        base.removeString(subject.getId());
                    }
                    ce.end();
                    panel.undoManager.addEdit(ce);

                    //table.revalidate();
                    refreshTable();
                    if (base.getStringCount() > 0) {
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
            try {
                panel.runCommand(Actions.UNDO);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo", IconTheme.JabRefIcon.REDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Redo"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand(Actions.REDO);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }
}
