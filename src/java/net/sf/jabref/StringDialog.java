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
import javax.swing.table.*;
import java.util.*;
import net.sf.jabref.undo.*;
import javax.swing.undo.CompoundEdit;

public class StringDialog extends JDialog {

    // A reference to the entry this object works on.
    BibtexDatabase base;
    JabRefFrame frame;
    BasePanel panel;
    JabRefPreferences prefs;

    // Layout objects.
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JLabel lab;
    Container conPane = getContentPane();
    JToolBar tlb = new JToolBar();
    JPanel pan = new JPanel();
    StringTable table;
    HelpAction helpAction;

    public StringDialog(JabRefFrame frame, BasePanel panel,
			BibtexDatabase base, JabRefPreferences prefs) {
	super(frame);
	this.frame = frame;
	this.panel = panel;
	this.base = base;
	this.prefs = prefs;

	helpAction = new HelpAction
	    (frame.helpDiag, GUIGlobals.stringEditorHelp, "Help");


	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    closeAction.actionPerformed(null);
		}
	    });

	// We replace the default FocusTraversalPolicy with a subclass
	// that only allows the StringTable to gain keyboard focus.
	setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
		protected boolean accept(Component c) {
		    return (super.accept(c) && (c instanceof StringTable));
		}
	    });

	setLocation(prefs.getInt("stringsPosX"), prefs.getInt("stringsPosY"));
	setSize(prefs.getInt("stringsSizeX"), prefs.getInt("stringsSizeY"));

	pan.setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	con.weighty = 1;
	con.weightx = 1;
	StringTableModel stm = new StringTableModel(this, base);
	table = new StringTable(stm);
	if (base.getStringCount() > 0)
	    table.setRowSelectionInterval(0,0);

	gbl.setConstraints(table.getPane(), con);
	pan.add(table.getPane());

	InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	ActionMap am = tlb.getActionMap();
	im.put(prefs.getKey("String dialog, add string"), "add");
	am.put("add", newStringAction);
	im.put(prefs.getKey("String dialog, remove string"), "remove");
	am.put("remove", removeStringAction);
	im.put(prefs.getKey("String dialog, move string up"), "up");
	am.put("up", stringUpAction);
	im.put(prefs.getKey("String dialog, move string down"), "down");
	am.put("down", stringDownAction);
	im.put(prefs.getKey("Close dialog"), "close");
	am.put("close", closeAction);
	im.put(prefs.getKey("Help"), "help");
	am.put("help", helpAction);
	im.put(prefs.getKey("Undo"), "undo");
	am.put("undo", undoAction);
	im.put(prefs.getKey("Redo"), "redo");
	am.put("redo", redoAction);

	//tlb.add(closeAction);
	//tlb.addSeparator();
	tlb.add(newStringAction);
	tlb.add(removeStringAction);
	tlb.addSeparator();
	tlb.add(stringUpAction);
	tlb.add(stringDownAction);
	tlb.addSeparator();
	tlb.add(helpAction);
	conPane.add(tlb, BorderLayout.NORTH);
	conPane.add(pan, BorderLayout.CENTER);

	if (panel.file != null)
	    setTitle(Globals.lang(GUIGlobals.stringsTitle)+": "+panel.file.getName());
	else
	    setTitle(Globals.lang(GUIGlobals.stringsTitle)+": "+Globals.lang(GUIGlobals.untitledTitle));

    }

    class StringTable extends JTable {
	JScrollPane sp = new JScrollPane((JTable)this);
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
	    sp.getViewport().setBackground(GUIGlobals.tableBackground);
	    // getInputMap().remove(GUIGlobals.exitDialog);
	    getInputMap().put(frame.prefs.getKey("Close dialog"), "close");
	    getActionMap().put("close", closeAction);
	    getInputMap().put(frame.prefs.getKey("Help"), "help");
	    getActionMap().put("help", helpAction);

	}

	public JComponent getPane() {
	    return sp;
	}

    }

    public void refreshTable() {
	table.revalidate();
	table.clearSelection();
	table.repaint();
    }

    class StringTableModel extends AbstractTableModel {

	BibtexDatabase base;
	StringDialog parent;

	public StringTableModel(StringDialog parent, BibtexDatabase base) {
	    this.parent = parent;
	    this.base = base;
	}

	public Object getValueAt(int row, int col) {
	    return ((col == 0) ?
		    base.getString(row).getName() :
		    base.getString(row).getContent());
	}

	public void setValueAt(Object value, int row, int col) {
	    //	    if (row >= base.getStringCount())
	    //	return; // After a Remove operation the program somehow
	                // thinks the user is still editing an entry,
	                // which might now be outside
	    if (col == 0) {
		// Change name of string.
		if (!((String)value).equals(base.getString(row).getName())) {
		    if (base.hasStringLabel((String)value))
			JOptionPane.showMessageDialog(parent,
						      Globals.lang("A string with that label "
								   +"already exists"),
						      Globals.lang("Label"),
						      JOptionPane.ERROR_MESSAGE);
		    else if ((value != null) && isNumber((String)value)) {
			JOptionPane.showMessageDialog
			    (parent,
			     Globals.lang("The label of the string can not be a number."),
			     Globals.lang("Label"),
			     JOptionPane.ERROR_MESSAGE);
		    }
		    else {
			// Store undo information.
			panel.undoManager.addEdit
			    (new UndoableStringChange
			     (panel, base.getString(row), true,
			      base.getString(row).getName(), (String)value));
			base.getString(row).setName((String)value);
			panel.markBaseChanged();
		    }
		}
	    } else {
		// Change content of string.
		if (!((String)value).equals(base.getString(row).getContent())) {
		    // Store undo information.
		    panel.undoManager.addEdit
			(new UndoableStringChange
			 (panel, base.getString(row), false,
			  base.getString(row).getContent(), (String)value));

		    base.getString(row).setContent((String)value);
		    panel.markBaseChanged();
		}
	    }
	}

	public int getColumnCount() {
	    return 2;
	}

	public int getRowCount() {
	    return base.getStringCount();
	}

	public String getColumnName(int col) {
	    return ((col == 0) ?
		    Globals.lang("Name") : Globals.lang("Content"));
	}

	public boolean isCellEditable(int row, int col) {
	    return true;
	}
    }

    protected boolean isNumber(String name) {
	// A pure integer number can not be used as a string label,
	// since Bibtex will read it as a number.
	try {
	    Integer.parseInt(name);
	    return true;
	} catch (NumberFormatException ex) {
	    return false;
	}

    }

    protected void assureNotEditing() {
	if (table.isEditing()) {
	    int col = table.getEditingColumn(),
		row = table.getEditingRow();
	    table.getCellEditor(row, col).stopCellEditing();
	}
    }

    // The action concerned with closing the window.
    CloseAction closeAction = new CloseAction(this);
    class CloseAction extends AbstractAction {
	StringDialog parent;
	public CloseAction(StringDialog parent) {
	    super("Close window");
	    //, new ImageIcon(GUIGlobals.closeIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Close dialog"));
	    this.parent = parent;
	}
	public void actionPerformed(ActionEvent e) {
	    panel.stringsClosing();
	    dispose();
	    Point p = getLocation();
	    Dimension d = getSize();
	    prefs.putInt("stringsPosX", p.x);
	    prefs.putInt("stringsPosY", p.y);
	    prefs.putInt("stringsSizeX", d.width);
	    prefs.putInt("stringsSizeY", d.height);
	}
    }

    NewStringAction newStringAction = new NewStringAction(this);
    class NewStringAction extends AbstractAction {
	StringDialog parent;
	public NewStringAction(StringDialog parent) {
	    super("New string",
		  new ImageIcon(GUIGlobals.addIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("New string"));
	    this.parent = parent;
	}
	public void actionPerformed(ActionEvent e) {
	    String name =
		JOptionPane.showInputDialog(parent,
					    Globals.lang("Please enter the string's label"));
	    if (name == null)
		return;
	    if (isNumber(name)) {
		JOptionPane.showMessageDialog
		    (parent,
		     Globals.lang("The label of the string can not be a number."),
		     Globals.lang("Label"),
		     JOptionPane.ERROR_MESSAGE);
		return;
	    }

	    try {
		BibtexString bs = new BibtexString(name, "");

		// Store undo information:
		panel.undoManager.addEdit
		    (new UndoableInsertString
		     (panel, panel.database, bs, base.getStringCount()));

		base.addString(bs, base.getStringCount());
		table.revalidate();
		panel.markBaseChanged();
	    } catch (KeyCollisionException ex) {
		JOptionPane.showMessageDialog(parent,
					      Globals.lang("A string with that label "
							   +"already exists"),
					      Globals.lang("Label"),
					      JOptionPane.ERROR_MESSAGE);
	    }
	}
    }

    StoreContentAction storeContentAction = new StoreContentAction(this);
    class StoreContentAction extends AbstractAction {
	StringDialog parent;
	public StoreContentAction(StringDialog parent) {
	    super("Store string",
		  new ImageIcon(GUIGlobals.addIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Store string"));
	    this.parent = parent;
	}
	public void actionPerformed(ActionEvent e) {
	}
    }

    RemoveStringAction removeStringAction = new RemoveStringAction(this);
    class RemoveStringAction extends AbstractAction {
	StringDialog parent;
	public RemoveStringAction(StringDialog parent) {
	    super("Remove selected strings",
		  new ImageIcon(GUIGlobals.removeIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Remove selected strings"));
	    this.parent = parent;
	}
	public void actionPerformed(ActionEvent e) {
	    int[] sel = table.getSelectedRows();
	    if (sel.length > 0) {

		// Make sure no cell is being edited, as caused by the
		// keystroke. This makes the content hang on the screen.
		assureNotEditing();

		String msg = Globals.lang("Really delete the selected")+" "+
		    ((sel.length>1) ? sel.length+" "+Globals.lang("entries")
		     : Globals.lang("entry"))+"?";
		int answer = JOptionPane.showConfirmDialog(parent, msg, Globals.lang("Delete strings"),
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE);
		if (answer == JOptionPane.YES_OPTION) {
		    CompoundEdit ce = new CompoundEdit();
		    for (int i=sel.length-1; i>=0; i--) {
			// Delete the strings backwards to avoid moving indexes.

			// Store undo information:
			ce.addEdit(new UndoableRemoveString
				   (panel, base,
				    base.getString(sel[i]), sel[i]));

			base.removeString(sel[i]);
		    }
		    ce.end();
		    panel.undoManager.addEdit(ce);

		    table.revalidate();
		    if (base.getStringCount() > 0)
			table.setRowSelectionInterval(0,0);
		    table.repaint();
		    panel.markBaseChanged();
		}
	    }
	}
    }

    StringUpAction stringUpAction = new StringUpAction();
    class StringUpAction extends AbstractAction {
	public StringUpAction() {
	    super("Move string up",
		  new ImageIcon(GUIGlobals.upIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Move string up"));
	}
	public void actionPerformed(ActionEvent e) {
	    int[] sel = table.getSelectedRows();
	    if ((sel.length == 1) && (sel[0] > 0)) {

		// Make sure no cell is being edited, as caused by the
		// keystroke. This makes the content hang on the screen.
		assureNotEditing();
		/*		int col = table.getEditingColumn(),
		    row = table.getEditingRow();
		    table.getCellEditor(row, col).stopCellEditing();*/

		// Store undo information:
		panel.undoManager.addEdit(new UndoableMoveString
					      (panel, base, sel[0], true));

		BibtexString bs = base.getString(sel[0]);
		base.removeString(sel[0]);
		try {
		    base.addString(bs, sel[0]-1);
		} catch (KeyCollisionException ex) {}
		table.revalidate();
		table.setRowSelectionInterval(sel[0]-1, sel[0]-1);
		table.repaint();
		panel.markBaseChanged();
	    }
	}
    }

    StringDownAction stringDownAction = new StringDownAction();
    class StringDownAction extends AbstractAction {
	public StringDownAction() {
	    super("Move string down",
		  new ImageIcon(GUIGlobals.downIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Move string down"));
	}
	public void actionPerformed(ActionEvent e) {
	    int[] sel = table.getSelectedRows();
	    if ((sel.length == 1) && (sel[0]+1 < base.getStringCount())) {

		// Make sure no cell is being edited, as caused by the
		// keystroke. This makes the content hang on the screen.
		assureNotEditing();

		/*int col = table.getEditingColumn(),
		    row = table.getEditingRow();
		    table.getCellEditor(row, col).stopCellEditing();*/

		// Store undo information:
		panel.undoManager.addEdit(new UndoableMoveString
					      (panel, base, sel[0], false));


		BibtexString bs = base.getString(sel[0]);
		base.removeString(sel[0]);
		try {
		    base.addString(bs, sel[0]+1);
		} catch (KeyCollisionException ex) {}
		table.revalidate();
		table.setRowSelectionInterval(sel[0]+1, sel[0]+1);
		table.repaint();
		panel.markBaseChanged();
	    }

	}
    }

    UndoAction undoAction = new UndoAction();
    class UndoAction extends AbstractAction {
	public UndoAction() {
	    super("Undo", new ImageIcon(GUIGlobals.undoIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Undo"));
	}
	public void actionPerformed(ActionEvent e) {
	    try {
		panel.runCommand("undo");
	    } catch (Throwable ex) {}
	}
    }

    RedoAction redoAction = new RedoAction();
    class RedoAction extends AbstractAction {
	public RedoAction() {
	    super("Undo", new ImageIcon(GUIGlobals.redoIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Redo"));
	}
	public void actionPerformed(ActionEvent e) {
	    try {
		panel.runCommand("redo");
	    } catch (Throwable ex) {}
	}
    }


}
