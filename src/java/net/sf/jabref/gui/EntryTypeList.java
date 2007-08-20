/*
 * EntryTypeList.java
 *
 * Created on February 13, 2005, 2:07 PM
 */

package net.sf.jabref.gui;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.CustomEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 * This class extends FieldSetComponent to provide some required functionality for the
 * list of entry types in EntryCustomizationDialog2.
 * @author alver
 */
public class EntryTypeList extends FieldSetComponent implements ListSelectionListener, ActionListener {
    
    protected JButton def = new JButton(Globals.lang("Default"));
    
    /** Creates a new instance of EntryTypeList */
    public EntryTypeList(List<String> fields) {
        super(Globals.lang("Entry types"), fields, false, true);
        
        con.gridx = 0;
        con.gridy = 2;
        con.fill = GridBagConstraints.VERTICAL;
        con.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(def, con);
        add(def);
        list.addListSelectionListener(this);
        def.addActionListener(this);
        def.setEnabled(false);
        remove.setEnabled(false);
    }

    protected void addField(String s) {
        s = s.trim();
        if (forceLowerCase)
            s = s.toLowerCase();
        if (s.equals("") || listModel.contains(s))
            return;
        
        String testString = Util.checkLegalKey(s);
        if (!testString.equals(s) || (s.indexOf('&') >= 0)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Globals.lang("Entry type names are not allowed to contain white space or the following "
                    +"characters")+": # { } ~ , ^ &",
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        addFieldUncritically(s);
    }
    
    protected void removeSelected() {
        //super.removeSelected();
        int[] selected = list.getSelectedIndices();
        if (selected.length > 0)
            changesMade = true;
        for (int i=0; i<selected.length; i++) {
            String typeName = (String)listModel.get(selected[selected.length-1-i]);
            BibtexEntryType type = BibtexEntryType.getType(typeName);
            
            // If it is a custom entry type, we can remove it. If type == null, it means
            // the user must have added it and not yet applied it, so we can remove it
            // in this case as well. If it is a standard type it cannot be removed.
            if ((type != null) && (type instanceof CustomEntryType)) {
                listModel.removeElementAt(selected[selected.length-1-i]);
            }
            else
                // This shouldn't happen, since the Remove button should be disabled.
                JOptionPane.showMessageDialog(null, Globals.lang("This entry type cannot be removed."),
                        Globals.lang("Remove entry type"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        
        
        //
        
    }

    public void enable(String typeName, boolean isChanged) {
        //String s = (String)list.getSelectedValue();
        
        if (BibtexEntryType.getStandardType(typeName) != null) {
            
            if (isChanged || (BibtexEntryType.getType(typeName) instanceof CustomEntryType)) {
                def.setEnabled(true);
            } else
                def.setEnabled(false);
            
            remove.setEnabled(false);
        } else {
            def.setEnabled(false);
            remove.setEnabled(true);
        }
    }
    
    public void addDefaultActionListener(ActionListener l) {
        def.addActionListener(l);
    }
    
    public void actionPerformed(ActionEvent e) {
        // Default button pressed.
        if (e.getSource() == def)
            def.setEnabled(false);
        else super.actionPerformed(e);
    }
    
    public void setEnabled(boolean en) {
        super.setEnabled(en);
        def.setEnabled(en);
    }
}
