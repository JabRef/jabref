/*
 * Copyright (C) 2003 Morten O. Alver, Nizar N. Batada
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class EntryEditorTab {

	private JPanel panel = new JPanel();

	private String[] fields;

	private EntryEditor parent;

	private HashMap editors = new HashMap();

	private FieldEditor activeField = null;

	public EntryEditorTab(List fields, EntryEditor parent, boolean addKeyField, String name) {
		if (fields != null)
			this.fields = (String[]) fields.toArray(new String[0]);
		else
			this.fields = new String[] {};

		this.parent = parent;

		setupPanel(addKeyField, name);

		/*
		 * The following line makes sure focus cycles inside tab instead of
		 * being lost to other parts of the frame:
		 */
		panel.setFocusCycleRoot(true);
	}

	void setupPanel(boolean addKeyField, String title) {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints con = new GridBagConstraints();
		panel.setLayout(gbl);
		double totalWeight = 0;

		for (int i = 0; i < fields.length; i++) {
			// Create the text area:
			final FieldTextArea ta = new FieldTextArea(fields[i], null);
			JComponent ex = parent.getExtra(fields[i], ta);
			setupJTextComponent(ta);

			// Store the editor for later reference:
			editors.put(fields[i], ta);
			if (i == 0)
				activeField = ta;

			// The label for this field:
			con.insets = new Insets(5, 5, 0, 0);
			con.anchor = GridBagConstraints.WEST;
			con.fill = GridBagConstraints.BOTH;
			con.gridwidth = 1;
			con.weightx = 0;
			con.weighty = 0;
			con.anchor = GridBagConstraints.NORTH;
			con.fill = GridBagConstraints.BOTH;
			gbl.setConstraints(ta.getLabel(), con);
			panel.add(ta.getLabel());

			// The field itself:
			con.fill = GridBagConstraints.BOTH;
			con.weightx = 1;
			con.weighty = BibtexFields.getFieldWeight(fields[i]);
			totalWeight += con.weighty;
			// The gridwidth depends on whether we will add an extra component
			// to the right:
			if (ex != null)
				con.gridwidth = 1;
			else
				con.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(ta.getPane(), con);
			panel.add(ta.getPane());

			// Possibly an extra component:
			if (ex != null) {
				con.gridwidth = GridBagConstraints.REMAINDER;
				con.anchor = GridBagConstraints.NORTH;
				con.fill = GridBagConstraints.HORIZONTAL;
				con.weightx = 0;
				gbl.setConstraints(ex, con);
				panel.add(ex);
			}
			panel.setName(title);
		}

		// Add the edit field for Bibtex-key.
		if (addKeyField) {
			con.insets.top += 25;
			con.insets.bottom = 10;
			con.gridwidth = 1;
			con.weighty = 0;
			con.weightx = 0;
			con.fill = GridBagConstraints.HORIZONTAL;
			con.anchor = GridBagConstraints.SOUTHWEST;

			final FieldTextField tf = new FieldTextField(BibtexFields.KEY_FIELD, (String) parent
				.getEntry().getField(BibtexFields.KEY_FIELD), true);
			setupJTextComponent(tf);

			editors.put("bibtexkey", tf);
			/*
			 * If the key field is the only field, we should have only one
			 * editor, and this one should be set as active initially:
			 */
			if (editors.size() == 1)
				activeField = tf;

			gbl.setConstraints(tf.getLabel(), con);
			panel.add(tf.getLabel());
			con.gridwidth = GridBagConstraints.REMAINDER;
			con.weightx = 1;

			gbl.setConstraints(tf, con);
			panel.add(tf);
		}
	}

	BibtexEntry entry;

	public BibtexEntry getEntry() {
		return entry;
	}

	boolean isFieldModified(FieldEditor f) {
		String text = f.getText().trim();

		if (text.length() == 0) {
			return getEntry().getField(f.getFieldName()) != null;
		} else {
			Object entryValue = getEntry().getField(f.getFieldName());
			return entryValue == null || !entryValue.toString().equals(text);
		}
	}

	public void markIfModified(FieldEditor f) {
		// Only mark as changed if not already is and the field was indeed
		// modified
		if (!updating && !parent.panel.isBaseChanged() && isFieldModified(f)) {
			markBaseChanged();
		}
	}

	void markBaseChanged() {
		parent.panel.markBaseChanged();
	}

	/**
	 * Only sets the activeField variable but does not focus it.
	 * 
	 * Call activate afterwards.
	 * 
	 * @param c
	 */
	public void setActive(FieldEditor c) {
		activeField = c;
	}

	public FieldEditor getActive() {
		return activeField;
	}

	public List getFields() {
		return java.util.Arrays.asList(fields);
	}

	public void activate() {
		if (activeField != null){
			activeField.requestFocus();
		}
	}

	/**
	 * Reset all fields from the data in the BibtexEntry.
	 * 
	 */
	public void updateAll() {
		setEntry(getEntry());
	}

	protected boolean updating = false;

	public void setEntry(BibtexEntry entry) {
		try {
			updating = true;
			Iterator i = editors.values().iterator();
			while (i.hasNext()) {
				FieldEditor editor = (FieldEditor) i.next();
				Object content = entry.getField(editor.getFieldName());
				editor.setText((content == null) ? "" : content.toString());
			}
			this.entry = entry;
		} finally {
			updating = false;
		}
	}

	public boolean updateField(String field, String content) {
		if (!editors.containsKey(field))
			return false;
		FieldEditor ed = (FieldEditor) editors.get(field);
		ed.setText(content);
		return true;
	}

	public void validateAllFields() {
		for (Iterator i = editors.keySet().iterator(); i.hasNext();) {
			String field = (String) i.next();
			FieldEditor ed = (FieldEditor) editors.get(field);
			ed.setEnabled(true);
			if (((Component) ed).hasFocus())
				ed.setBackground(GUIGlobals.activeEditor);
			else
				ed.setBackground(GUIGlobals.validFieldBackground);
		}
	}

	public void setEnabled(boolean enabled) {
		Iterator i = editors.values().iterator();
		while (i.hasNext()) {
			FieldEditor editor = (FieldEditor) i.next();
			editor.setEnabled(enabled);
		}
	}

	public Component getPane() {
		return panel;
	}

	/**
	 * Set up key bindings and focus listener for the FieldEditor.
	 * 
	 * @param component
	 */
	public void setupJTextComponent(final JTextComponent component) {

		component.addFocusListener(fieldListener);

		InputMap im = component.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = component.getActionMap();

		im.put(Globals.prefs.getKey("Entry editor, previous entry"), "prev");
		am.put("prev", parent.prevEntryAction);
		im.put(Globals.prefs.getKey("Entry editor, next entry"), "next");
		am.put("next", parent.nextEntryAction);

		im.put(Globals.prefs.getKey("Entry editor, store field"), "store");
		am.put("store", parent.storeFieldAction);
		im.put(Globals.prefs.getKey("Entry editor, next panel"), "right");
		im.put(Globals.prefs.getKey("Entry editor, next panel 2"), "right");
		am.put("left", parent.switchLeftAction);
		im.put(Globals.prefs.getKey("Entry editor, previous panel"), "left");
		im.put(Globals.prefs.getKey("Entry editor, previous panel 2"), "left");
		am.put("right", parent.switchRightAction);
		im.put(Globals.prefs.getKey("Help"), "help");
		am.put("help", parent.helpAction);
		im.put(Globals.prefs.getKey("Save database"), "save");
		am.put("save", parent.saveDatabaseAction);
		im.put(Globals.prefs.getKey("Next tab"), "nexttab");
		am.put("nexttab", parent.frame.nextTab);
		im.put(Globals.prefs.getKey("Previous tab"), "prevtab");
		am.put("prevtab", parent.frame.prevTab);

		try {
			HashSet keys = new HashSet(component
				.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
			keys.clear();
			keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
			component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
			keys = new HashSet(component
				.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
			keys.clear();
			keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
			component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
		} catch (Throwable t) {
			System.err.println(t);
		}
	}

	/*
	 * Focus listener that fires the storeFieldAction when a FieldTextArea loses
	 * focus.
	 * 
	 * TODO: It would be nice to test this thoroughly.
	 */
	FocusListener fieldListener = new FocusListener() {
	
		JTextComponent c;

		DocumentListener d;

		public void focusGained(FocusEvent e) {

			synchronized (this){
				if (c != null) {
					c.getDocument().removeDocumentListener(d);
					c = null;
					d = null;
				}

				if (e.getSource() instanceof JTextComponent) {

					c = (JTextComponent) e.getSource();
					/**
					 * [ 1553552 ] Not properly detecting changes to flag as
					 * changed
					 */
					d = new DocumentListener() {

						void fire(DocumentEvent e) {
							if (c.isFocusOwner()) {
								markIfModified((FieldEditor) c);
							}
						}

						public void changedUpdate(DocumentEvent e) {
							fire(e);
						}

						public void insertUpdate(DocumentEvent e) {
							fire(e);
						}

						public void removeUpdate(DocumentEvent e) {
							fire(e);
						}
					};
					c.getDocument().addDocumentListener(d);
				}
			}

			setActive((FieldEditor) e.getSource());

		}

		public void focusLost(FocusEvent e) {
			synchronized (this) {
				if (c != null) {
					c.getDocument().removeDocumentListener(d);
					c = null;
					d = null;
				}
			}
			if (!e.isTemporary())
				parent.updateField((FieldEditor) e.getSource());
		}
	};
}
