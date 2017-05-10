package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.text.JTextComponent;

import org.jabref.Globals;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.gui.fieldeditors.TextArea;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.gui.util.WindowLocation;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.preferences.JabRefPreferences;

class PreambleEditor extends JabRefDialog {
    // A reference to the entry this object works on.
    private final BibDatabase database;
    private final BasePanel panel;

    private final FieldEditor editor;

    private final UndoAction undoAction = new UndoAction();
    private final StoreFieldAction storeFieldAction = new StoreFieldAction();
    private final RedoAction redoAction = new RedoAction();
    // The action concerned with closing the window.
    private final CloseAction closeAction = new CloseAction();

    public PreambleEditor(JabRefFrame baseFrame, BasePanel panel, BibDatabase database) {
        super(baseFrame, PreambleEditor.class);
        this.panel = panel;
        this.database = database;

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeAction.actionPerformed(null);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                editor.requestFocus();
            }
        });
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {

            @Override
            protected boolean accept(Component c) {
                return super.accept(c) && (c instanceof FieldEditor);
            }
        });

        JPanel pan = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        pan.setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        con.weighty = 1;
        con.insets = new Insets(10, 5, 10, 5);

        editor = new TextArea(Localization.lang("Preamble"), database.getPreamble().orElse(""));

        // TODO: Reenable this
        //setupJTextComponent((TextArea) editor);

        //gbl.setConstraints(editor.getLabel(), con);
        //pan.add(editor.getLabel());

        con.weightx = 1;

        gbl.setConstraints(editor.getPane(), con);
        pan.add(editor.getPane());

        Container conPane = getContentPane();
        conPane.add(pan, BorderLayout.CENTER);
        setTitle(Localization.lang("Edit preamble"));

        WindowLocation pw = new WindowLocation(this, JabRefPreferences.PREAMBLE_POS_X, JabRefPreferences.PREAMBLE_POS_Y,
                JabRefPreferences.PREAMBLE_SIZE_X, JabRefPreferences.PREAMBLE_SIZE_Y);
        pw.displayWindowAtStoredLocation();
    }

    private void setupJTextComponent(JTextComponent ta) {
        // Set up key bindings and focus listener for the FieldEditor.
        ta.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        ta.getActionMap().put("close", closeAction);
        ta.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.PREAMBLE_EDITOR_STORE_CHANGES), "store");
        ta.getActionMap().put("store", storeFieldAction);

        ta.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.UNDO), "undo");
        ta.getActionMap().put(Actions.UNDO, undoAction);
        ta.getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.REDO), "redo");
        ta.getActionMap().put(Actions.REDO, redoAction);

        ta.addFocusListener(new FieldListener());
    }

    public void updatePreamble() {
        editor.setText(database.getPreamble().orElse(""));
    }

    public FieldEditor getFieldEditor() {
        return editor;
    }

    public void storeCurrentEdit() {
        storeFieldAction.actionPerformed(null);
    }

    private class FieldListener extends FocusAdapter {

        /*
         * Focus listener that fires the storeFieldAction when a TextArea
         * loses focus.
         */
        @Override
        public void focusLost(FocusEvent e) {
            if (!e.isTemporary()) {
                storeFieldAction.actionPerformed(new ActionEvent(e.getSource(), 0, ""));
            }
        }

    }

    class StoreFieldAction extends AbstractAction {

        public StoreFieldAction() {
            super("Store field value");
            putValue(Action.SHORT_DESCRIPTION, "Store field value");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String toSet = editor.getText();

            // We check if the field has changed, since we don't want to mark the
            // base as changed unless we have a real change.
            if (!database.getPreamble().orElse("").equals(toSet)) {
                panel.getUndoManager().addEdit(
                        new UndoablePreambleChange(database, panel, database.getPreamble().orElse(null), toSet));
                database.setPreamble(toSet);
                //if ((toSet == null) || toSet.isEmpty()) {
                //    editor.setLabelColor(GUIGlobals.NULL_FIELD_COLOR);
                //} else {
                //    editor.setLabelColor(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
                //}
                editor.setValidBackgroundColor();
                if (editor.hasFocus()) {
                    editor.setActiveBackgroundColor();
                }
                panel.markBaseChanged();
            }

        }
    }

    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Undo", IconTheme.JabRefIcon.UNDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Undo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.UNDO);
        }
    }

    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo", IconTheme.JabRefIcon.REDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Redo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.REDO);
        }
    }

    class CloseAction extends AbstractAction {

        public CloseAction() {
            super(Localization.lang("Close window"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            storeFieldAction.actionPerformed(null);
            panel.preambleEditorClosing();
            dispose();
        }
    }

}
