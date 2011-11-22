/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.gui.AutoCompleteListener;
import net.sf.jabref.gui.FileListEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 *
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class CompressedEntryEditorTab extends EntryEditorTab {

	private JPanel panel = new JPanel();

    private JScrollPane scrollPane = new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

	private String[] fields;

	private EntryEditor parent;

	private HashMap<String, FieldEditor> editors = new HashMap<String, FieldEditor>();

	private FieldEditor activeField = null;

	public CompressedEntryEditorTab(JabRefFrame frame, BasePanel panel, List<String> fields, EntryEditor parent,
                          boolean addKeyField, String name) {
		if (fields != null)
			this.fields = fields.toArray(new String[0]);
		else
			this.fields = new String[] {};

		this.parent = parent;

		setupPanel(frame, panel, addKeyField, name);

		/*
		 * The following line makes sure focus cycles inside tab instead of
		 * being lost to other parts of the frame:
		 */
		scrollPane.setFocusCycleRoot(true);
	}


    void setupPanel(JabRefFrame frame, BasePanel bPanel, boolean addKeyField, String title) {

    	InputMap im = panel.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = panel.getActionMap();

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


        panel.setName(title);
        //String rowSpec = "left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref";
        String colSpec = "fill:pref, 1dlu, fill:10dlu:grow, 1dlu, fill:pref, "
                +"8dlu, fill:pref, 1dlu, fill:10dlu:grow, 1dlu, fill:pref";
        StringBuffer sb = new StringBuffer();
        int rows = (int)Math.ceil((double)fields.length/2.0);
        for (int i = 0; i < rows; i++) {
            sb.append("fill:pref:grow, ");
        }
        if (addKeyField)
            sb.append("4dlu, fill:pref");
        else
            if (sb.length() >= 2)
                sb.delete(sb.length() - 2, sb.length());
        String rowSpec = sb.toString();

        DefaultFormBuilder builder = new DefaultFormBuilder
                (new FormLayout(colSpec, rowSpec), panel);

        for (int i = 0; i < fields.length; i++) {
            // Create the text area:
            int editorType = BibtexFields.getEditorType(fields[i]);

            final FieldEditor ta;
            if (editorType == GUIGlobals.FILE_LIST_EDITOR)
                ta = new FileListEditor(frame, bPanel.metaData(), fields[i], null, parent);
            else{
                ta = new FieldTextArea(fields[i], null);
                //inform the fieldtextarea of search events to highlight searchstrings
                frame.getSearchManager().addSearchListener((FieldTextArea)ta);                
            }
            //ta.addUndoableEditListener(bPanel.undoListener);

            JComponent ex = parent.getExtra(fields[i], ta);

            // Add autocompleter listener, if required for this field:
            AbstractAutoCompleter autoComp = bPanel.getAutoCompleter(fields[i]);
            AutoCompleteListener acl = null;
            if (autoComp != null) {
                acl = new AutoCompleteListener(autoComp);
            }
            setupJTextComponent(ta.getTextComponent(), acl);
            ta.setAutoCompleteListener(acl);

            // Store the editor for later reference:
            editors.put(fields[i], ta);
            if (i == 0)
                activeField = ta;
            //System.out.println(fields[i]+": "+BibtexFields.getFieldWeight(fields[i]));
            //ta.getPane().setPreferredSize(new Dimension(100,
            //        (int)(50.0*BibtexFields.getFieldWeight(fields[i]))));
            builder.append(ta.getLabel());
            if (ex == null)
                builder.append(ta.getPane(), 3);
            else {
                builder.append(ta.getPane());
                JPanel pan = new JPanel();
                pan.setLayout(new BorderLayout());
                pan.add(ex, BorderLayout.NORTH);
                builder.append(pan);
            }
            if (i%2 == 1) builder.nextLine();
        }

        // Add the edit field for Bibtex-key.
		if (addKeyField) {
			final FieldTextField tf = new FieldTextField(BibtexFields.KEY_FIELD, parent
				.getEntry().getField(BibtexFields.KEY_FIELD), true);
            //tf.addUndoableEditListener(bPanel.undoListener);
			setupJTextComponent(tf, null);

			editors.put("bibtexkey", tf);
			/*
			 * If the key field is the only field, we should have only one
			 * editor, and this one should be set as active initially:
			 */
			if (editors.size() == 1)
				activeField = tf;
            builder.nextLine();
			builder.append(tf.getLabel());
			builder.append(tf, 3);
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

	public List<String> getFields() {
		return java.util.Arrays.asList(fields);
	}

	public void activate() {
		if (activeField != null){
			/**
			 * Corrected to fix [ 1594169 ] Entry editor: navigation between panels
			 */
			new FocusRequester(activeField.getTextComponent());
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
			Iterator<FieldEditor> i = editors.values().iterator();
			while (i.hasNext()) {
				FieldEditor editor = i.next();
				Object content = entry.getField(editor.getFieldName());
                String toSet = (content == null) ? "" : content.toString();
                if (!toSet.equals(editor.getText()))
				    editor.setText(toSet);
			}
			this.entry = entry;
		} finally {
			updating = false;
		}
	}

	public boolean updateField(String field, String content) {
		if (!editors.containsKey(field))
			return false;
		FieldEditor ed = editors.get(field);
		ed.setText(content);
		return true;
	}

	public void validateAllFields() {
		for (Iterator<String> i = editors.keySet().iterator(); i.hasNext();) {
			String field = i.next();
			FieldEditor ed = editors.get(field);
			ed.setEnabled(true);
			if (((Component) ed).hasFocus())
				ed.setActiveBackgroundColor();
			else
				ed.setValidBackgroundColor();
		}
	}

	public void setEnabled(boolean enabled) {
		Iterator<FieldEditor> i = editors.values().iterator();
		while (i.hasNext()) {
			FieldEditor editor = i.next();
			editor.setEnabled(enabled);
		}
	}

	public Component getPane() {
		return scrollPane;
	}

	/**
	 * Set up key bindings and focus listener for the FieldEditor.
	 *
	 * @param component
	 */
	public void setupJTextComponent(final JComponent component, final AutoCompleteListener acl) {

        // Here we add focus listeners to the component. The funny code is because we need
        // to guarantee that the AutoCompleteListener - if used - is called before fieldListener
        // on a focus lost event. The AutoCompleteListener is responsible for removing any
        // current suggestion when focus is lost, and this must be done before fieldListener
        // stores the current edit. Swing doesn't guarantee the order of execution of event
        // listeners, so we handle this by only adding the AutoCompleteListener and telling
        // it to call fieldListener afterwards. If no AutoCompleteListener is used, we
        // add the fieldListener normally.
        if (acl != null) {
            component.addKeyListener(acl);
            component.addFocusListener(acl);
            acl.setNextFocusListener(fieldListener);
        }
        else
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
			HashSet<AWTKeyStroke> keys = new HashSet<AWTKeyStroke>(component
				.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
			keys.clear();
			keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
			component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
			keys = new HashSet<AWTKeyStroke>(component
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

                    /**
                     * Makes the vertical scroll panel view follow the focus
                     */
                    Component cScrollPane = c.getParent().getParent();
                    if (cScrollPane instanceof JScrollPane) {
                        JScrollPane componentPane = (JScrollPane) cScrollPane;
                        Component cPane = componentPane.getParent();
                        if (cPane instanceof JPanel) {
                            JPanel panel = (JPanel) cPane;
                            Rectangle bounds = componentPane.getBounds();
                            panel.scrollRectToVisible(bounds);
                        }
                    }
				
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
				parent.updateField(e.getSource());
		}
	};
}
