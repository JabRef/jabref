package net.sf.jabref.gui;

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
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.text.JTextComponent;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextArea;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.UndoablePreambleChange;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.preferences.JabRefPreferences;

class PreambleEditor extends JDialog {
    // A reference to the entry this object works on.
    private final BibDatabase base;
    private final BasePanel panel;

    private final FieldEditor ed;

    private final UndoAction undoAction = new UndoAction();
    private final StoreFieldAction storeFieldAction = new StoreFieldAction();
    private final RedoAction redoAction = new RedoAction();
    // The action concerned with closing the window.
    private final CloseAction closeAction = new CloseAction();

    public PreambleEditor(JabRefFrame baseFrame, BasePanel panel, BibDatabase base) {
        super(baseFrame);
        this.panel = panel;
        this.base = base;

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeAction.actionPerformed(null);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                ed.requestFocus();
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

        String content = base.getPreamble();

        ed = new TextArea(Localization.lang("Preamble"), content == null ? "" : content);

        setupJTextComponent((TextArea) ed);

        gbl.setConstraints(ed.getLabel(), con);
        pan.add(ed.getLabel());

        con.weightx = 1;

        gbl.setConstraints(ed.getPane(), con);
        pan.add(ed.getPane());

        Container conPane = getContentPane();
        conPane.add(pan, BorderLayout.CENTER);
        setTitle(Localization.lang("Edit preamble"));

        PositionWindow pw = new PositionWindow(this, JabRefPreferences.PREAMBLE_POS_X, JabRefPreferences.PREAMBLE_POS_Y,
                JabRefPreferences.PREAMBLE_SIZE_X, JabRefPreferences.PREAMBLE_SIZE_Y);
        pw.setWindowPosition();
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
        ed.setText(base.getPreamble());
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
            String toSet = null;
            boolean set;
            if (!ed.getText().isEmpty()) {
                toSet = ed.getText();
            }
            // We check if the field has changed, since we don't want to mark the
            // base as changed unless we have a real change.
            if (toSet == null) {
                set = base.getPreamble() != null;
            } else {
                set = !((base.getPreamble() != null)
                        && toSet.equals(base.getPreamble()));
            }

            if (set) {
                panel.getUndoManager().addEdit(new UndoablePreambleChange
                        (base, panel, base.getPreamble(), toSet));
                base.setPreamble(toSet);
                if ((toSet == null) || toSet.isEmpty()) {
                    ed.setLabelColor(GUIGlobals.NULL_FIELD_COLOR);
                } else {
                    ed.setLabelColor(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
                }
                ed.setValidBackgroundColor();
                if (ed.getTextComponent().hasFocus()) {
                    ed.setActiveBackgroundColor();
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


    public FieldEditor getFieldEditor() {
        return ed;
    }

    public void storeCurrentEdit() {
        storeFieldAction.actionPerformed(null);
    }

}
