/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import net.sf.jabref.undo.UndoablePreambleChange;

public class PreambleEditor extends JDialog {

    // A reference to the entry this object works on.
    BibtexDatabase base;
    BasePanel panel;
    JabRefFrame baseFrame;
    JabRefPreferences prefs;

    // Layout objects.
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JLabel lab;
    Container conPane = getContentPane();
    //JToolBar tlb = new JToolBar();
    JPanel pan = new JPanel();
    FieldEditor ed;


    public PreambleEditor(JabRefFrame baseFrame,
                          BasePanel panel, BibtexDatabase base,
                          JabRefPreferences prefs) {
        super(baseFrame);
        this.baseFrame = baseFrame;
        this.panel = panel;
        this.base = base;
        this.prefs = prefs;

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeAction.actionPerformed(null);
            }

            public void windowOpened(WindowEvent e) {
                ed.requestFocus();
            }
        });
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            protected boolean accept(Component c) {
                return (super.accept(c) && (c instanceof FieldEditor));
            }
        });

        int prefHeight = (int) (GUIGlobals.PE_HEIGHT * GUIGlobals.FORM_HEIGHT[prefs.getInt("entryTypeFormHeightFactor")]);
        setSize(GUIGlobals.FORM_WIDTH[prefs.getInt("entryTypeFormWidth")], prefHeight);

        pan.setLayout(gbl);
        con.fill = GridBagConstraints.BOTH;
        con.weighty = 1;
        con.insets = new Insets(10, 5, 10, 5);

        String content = base.getPreamble();

        ed = new FieldTextArea(Globals.lang("Preamble"), ((content != null) ? content : ""));
        //ed.addUndoableEditListener(panel.undoListener);
        setupJTextComponent((FieldTextArea) ed);

        gbl.setConstraints(ed.getLabel(), con);
        pan.add(ed.getLabel());

        con.weightx = 1;

        gbl.setConstraints(ed.getPane(), con);
        pan.add(ed.getPane());

        //tlb.add(closeAction);
        //conPane.add(tlb, BorderLayout.NORTH);
        conPane.add(pan, BorderLayout.CENTER);
        setTitle(Globals.lang("Edit preamble"));
    }

    private void setupJTextComponent(javax.swing.text.JTextComponent ta) {
        // Set up key bindings and focus listener for the FieldEditor.
        ta.getInputMap().put(prefs.getKey("Close preamble editor"), "close");
        ta.getActionMap().put("close", closeAction);
        ta.getInputMap().put(prefs.getKey("Preamble editor, store changes"), "store");
        ta.getActionMap().put("store", storeFieldAction);
        ta.getInputMap().put(prefs.getKey("Close preamble editor"), "close");
        ta.getActionMap().put("close", closeAction);

        ta.getInputMap().put(prefs.getKey("Undo"), "undo");
        ta.getActionMap().put("undo", undoAction);
        ta.getInputMap().put(prefs.getKey("Redo"), "redo");
        ta.getActionMap().put("redo", redoAction);


        ta.addFocusListener(new FieldListener());
    }

    public void updatePreamble() {
        ed.setText(base.getPreamble());
    }

    class FieldListener extends FocusAdapter {
        /*
       * Focus listener that fires the storeFieldAction when a FieldTextArea
       * loses focus.
       */
        public void focusLost(FocusEvent e) {
            if (!e.isTemporary())
                storeFieldAction.actionPerformed(new ActionEvent(e.getSource(), 0, ""));
        }

    }

    StoreFieldAction storeFieldAction = new StoreFieldAction();

    class StoreFieldAction extends AbstractAction {
        public StoreFieldAction() {
            super("Store field value");
            putValue(SHORT_DESCRIPTION, "Store field value");
        }

        public void actionPerformed(ActionEvent e) {
            String toSet = null;
            boolean set;
            if (ed.getText().length() > 0)
                toSet = ed.getText();
            // We check if the field has changed, since we don't want to mark the
            // base as changed unless we have a real change.
            if (toSet == null) {
                if (base.getPreamble() == null)
                    set = false;
                else
                    set = true;
            } else {
                if ((base.getPreamble() != null)
                        && toSet.equals(base.getPreamble()))
                    set = false;
                else
                    set = true;
            }

            if (set) {
                panel.undoManager.addEdit(new UndoablePreambleChange
                        (base, panel, base.getPreamble(), toSet));
                base.setPreamble(toSet);
                if ((toSet != null) && (toSet.length() > 0)) {
                    ed.setLabelColor(GUIGlobals.validFieldColor);
                    ed.setBackground(GUIGlobals.validFieldBackground);
                } else {
                    ed.setLabelColor(GUIGlobals.nullFieldColor);
                    ed.setBackground(GUIGlobals.validFieldBackground);
                }
                if (ed.getTextComponent().hasFocus())
                    ed.setBackground(GUIGlobals.activeEditor);
                panel.markBaseChanged();
            }

        }
    }

    UndoAction undoAction = new UndoAction();

    class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", GUIGlobals.getImage("undo"));
            putValue(SHORT_DESCRIPTION, "Undo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand("undo");
            } catch (Throwable ex) {
            }
        }
    }

    RedoAction redoAction = new RedoAction();

    class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Undo", GUIGlobals.getImage("redo"));
            putValue(SHORT_DESCRIPTION, "Redo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand("redo");
            } catch (Throwable ex) {
            }
        }
    }

    // The action concerned with closing the window.
    CloseAction closeAction = new CloseAction();

    class CloseAction extends AbstractAction {
        public CloseAction() {
            super(Globals.lang("Close window"));
            //, new ImageIcon(GUIGlobals.closeIconFile));
            //putValue(SHORT_DESCRIPTION, "Close window (Ctrl-Q)");
        }

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
