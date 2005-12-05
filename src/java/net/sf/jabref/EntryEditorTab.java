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

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;

public class EntryEditorTab {

    private JPanel panel = new JPanel();
    private String[] fields;
    private final static Object[] ARRAY = new String[0];
    private EntryEditor parent;
    private HashMap editors = new HashMap();
    private FieldEditor activeField = null;
    private JScrollPane sp = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    //    private BibtexEntry entry;

    public EntryEditorTab(List fields, EntryEditor parent, boolean addKeyField, String name) {
        if (fields != null)
	        this.fields = (String[])fields.toArray(ARRAY);
        else
            this.fields = new String[] {};
        this.parent = parent;
        setupPanel(addKeyField, name);

        // The following line makes sure focus cycles inside tab instead of being lost
        // to other parts of the frame:
        panel.setFocusCycleRoot(true);

    }


    private final void setupPanel(boolean addKeyField, String title) {
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints con = new GridBagConstraints();
	panel.setLayout(gbl);
	double totalWeight = 0;
	
	//panel.setOpaque(true);
	//panel.setBackground(java.awt.Color.white);

	for (int i=0; i<fields.length; i++) {

	    // Create the text area:
	    FieldTextArea ta = new FieldTextArea(fields[i], null);//stringContent);
        JComponent ex = parent.getExtra(fields[i], ta);
	    // Attach listeners and key bindings:
	    setupJTextComponent(ta);
	    ta.addFocusListener(new FieldListener(ta));

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
	    con.weighty = GUIGlobals.getFieldWeight(fields[i]);
	    totalWeight += con.weighty;
	    // The gridwidth depends on whether we will add an extra component to the right:
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
	    FieldTextField tf = new FieldTextField(Globals.KEY_FIELD, (String) parent.entry.getField(Globals.KEY_FIELD), true);//(String) entry.getField(Globals.KEY_FIELD));
        editors.put("bibtexkey", tf);

        // If the key field is the only field, we should have only one editor, and this one should be set
        // as active initially:
        if (editors.size() == 1)
            activeField = tf;
        
	    gbl.setConstraints(tf.getLabel(), con);
	    panel.add(tf.getLabel());
	    con.gridwidth = GridBagConstraints.REMAINDER;
	    con.weightx = 1;	    
	    setupJTextComponent(tf);
	    tf.addFocusListener(new FieldListener(tf));
	    gbl.setConstraints(tf, con);
	    panel.add(tf);
	}


    }


    public void setActive(FieldEditor c) {
	activeField = c;
	//System.out.println(c.toString());
    }
    
    public FieldEditor getActive() {
	return activeField;
    }

    public List getFields() {
	return java.util.Arrays.asList(fields);
    }

    public void activate() {
	if (activeField != null)
	    activeField.requestFocus();


	//System.out.println("Activate, hurra");
    }

    public void updateAll() {
        // Test: make sure all fields are correct:
        setEntry(parent.entry);
        /*for (int i=0; i<fields.length; i++) {
            FieldEditor fe = (FieldEditor)editors.get(fields[i]);
            fe.setText(e);
        }  */
    }

    public void setEntry(BibtexEntry entry) {
	for (Iterator i=editors.keySet().iterator(); i.hasNext();) {
	    String field = (String)i.next();
	    FieldEditor ed = (FieldEditor)editors.get(field);
	    Object content = entry.getField(ed.getFieldName());
	    ed.setText((content == null) ? "" : content.toString());
	}
    }

    public boolean updateField(String field, String content) {
	if (!editors.containsKey(field))
	    return false;
	FieldEditor ed = (FieldEditor)editors.get(field);
	ed.setText(content);
	return true;
    }

    public void validateAllFields() {
	for (Iterator i=editors.keySet().iterator(); i.hasNext();) {
	    String field = (String)i.next();
	    FieldEditor ed = (FieldEditor)editors.get(field);
        if (((Component)ed).hasFocus())
            ed.setBackground(GUIGlobals.activeEditor);
        else
	        ed.setBackground(GUIGlobals.validFieldBackground);
	}
    }

    public void setEnabled(boolean enabled) {
	for (Iterator i=editors.keySet().iterator(); i.hasNext();) {
	    String field = (String)i.next();
	    FieldEditor ed = (FieldEditor)editors.get(field);
	    ed.setEnabled(enabled);
	}
    }

    public Component getPane() {

	return panel;
    }

    public void setupJTextComponent(JTextComponent ta) {
	// Activate autocompletion if it should be used for this field.
	
	// Set up key bindings and focus listener for the FieldEditor.
	InputMap im = ta.getInputMap(JComponent.WHEN_FOCUSED);
	ActionMap am = ta.getActionMap();

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
	    HashSet keys =
		new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
	    keys.clear();
	    keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
	    ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
	    keys =
		new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
	    keys.clear();
	    keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
	    ta.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
	} catch (Throwable t) {
	    System.err.println(t);
	}
	
    }


    /*
     * Focus listener that fires the storeFieldAction when a FieldTextArea loses
     * focus.
     */
    class FieldListener extends FocusAdapter {

	FieldEditor fe;

	public FieldListener(FieldEditor fe) {
	    this.fe = fe;
	}

	public void focusGained(FocusEvent e) {
	    setActive(fe);
	}
	
	public void focusLost(FocusEvent e) {
  	    if (!e.isTemporary())
		parent.updateField(fe);
	}
    }
    
}

