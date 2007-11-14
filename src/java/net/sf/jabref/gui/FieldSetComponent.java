/*
 * FieldSetComponent.java
 *
 * Created on February 10, 2005, 8:32 PM
 */

package net.sf.jabref.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 *
 * @author alver
 */
public class FieldSetComponent extends JPanel implements ActionListener {

    protected Set<ActionListener> additionListeners = new HashSet<ActionListener>();
    protected JList list;
    protected JScrollPane sp = null;
    protected DefaultListModel listModel;
    protected JComboBox sel;
    protected JTextField input;
    protected JLabel title = null;
    protected JButton add, remove, up=null, down=null;
    protected GridBagLayout gbl = new GridBagLayout();
    protected GridBagConstraints con = new GridBagConstraints();
    protected boolean forceLowerCase, changesMade = false;
    protected Set<ListDataListener> modelListeners = new HashSet<ListDataListener>();
    
    /** 
     * Creates a new instance of FieldSetComponent, with preset selection
     * values. These are put into a JComboBox.
     */
    public FieldSetComponent(String title, List<String> fields, List<String> preset, boolean arrows, boolean forceLowerCase) {
        this(title, fields, preset, "Add", "Remove", arrows, forceLowerCase);
    }
    
    /**
     * Creates a new instance of FieldSetComponent without preset selection
     * values. Replaces the JComboBox with a JTextField.
     */
    public FieldSetComponent(String title, List<String> fields, boolean arrows, boolean forceLowerCase) {
        this(title, fields, null, "Add", "Remove", arrows, forceLowerCase);
    }
    
    public FieldSetComponent(String title, List<String> fields, List<String> preset, String addText, String removeText, 
            boolean arrows, boolean forceLowerCase) {
        this.forceLowerCase = forceLowerCase;                
        add = new JButton(Globals.lang(addText));
        remove = new JButton(Globals.lang(removeText));
        listModel = new DefaultListModel();
        if (title != null)
            this.title = new JLabel(title);
        
        for (String field : fields)
            listModel.addElement(field);
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Set up GUI:
        add.addActionListener(this);
        remove.addActionListener(this);
        
        
        setLayout(gbl);
        con.insets = new Insets(1,1,1,1);
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
        con.gridwidth = GridBagConstraints.REMAINDER;
        if (this.title != null) {
            gbl.setConstraints(this.title, con);
            add(this.title);
        }
        
        con.weighty = 1;
        sp = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbl.setConstraints(sp, con);
        add(sp);
        con.weighty = 0;
        con.gridwidth = 1;
        if (arrows) {
            con.weightx = 0;
            up = new JButton(GUIGlobals.getImage("up"));
            down = new JButton(GUIGlobals.getImage("down"));
            up.addActionListener(this);
            down.addActionListener(this);
            up.setToolTipText(Globals.lang("Move up"));
            down.setToolTipText(Globals.lang("Move down"));
            gbl.setConstraints(up, con);
            add(up);
            gbl.setConstraints(down, con);
            add(down);
            con.weightx = 0;
        }
        
        Component strut = Box.createHorizontalStrut(5);
        gbl.setConstraints(strut, con);
        add(strut);        
        
        con.weightx = 1;
        con.gridwidth = GridBagConstraints.REMAINDER;
        
        //Component b = Box.createHorizontalGlue();
        //gbl.setConstraints(b, con);
        //add(b);
        
        //if (!arrows)
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(remove, con);
        add(remove);

        con.gridwidth = 3;
        con.weightx = 1;
        if (preset != null) {
            sel = new JComboBox(preset.toArray());
            sel.setEditable(true);
            //sel.addActionListener(this);
            gbl.setConstraints(sel, con);
            add(sel);
        } else {
            input = new JTextField(20);
            input.addActionListener(this);
            gbl.setConstraints(input, con);
            add(input);
        }
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weighty = 0;
        con.weightx = 0.5;
        con.gridwidth = 1;
        gbl.setConstraints(add, con);
        add(add);
        
    }
    
    public void setListSelectionMode(int mode) {
        list.setSelectionMode(mode);
    }
    
    public void selectField(String fieldName) {
        int idx = listModel.indexOf(fieldName);
        if (idx >= 0)
            list.setSelectedIndex(idx);
        
        // Make sure it is visible:
        JViewport viewport = sp.getViewport();
        viewport.scrollRectToVisible(list.getCellBounds(idx, idx));
        
    }
    
    public String getFirstSelected() {
        Object o = list.getSelectedValue();
        if (o == null)
            return null;
        return (String)o;
    }
    
    public void setEnabled(boolean en) {
        if (input != null)
            input.setEnabled(en);
        if (sel != null)
            sel.setEnabled(en);
        if (up != null) {
            up.setEnabled(en);
            down.setEnabled(en);
        }
        add.setEnabled(en);
        remove.setEnabled(en);
    }
    
    public void setFields(List<String> fields) {
        DefaultListModel newListModel = new DefaultListModel();
        for (String field : fields)
            newListModel.addElement(field);
        this.listModel = newListModel;
        for (Iterator<ListDataListener> i=modelListeners.iterator(); i.hasNext();)
            newListModel.addListDataListener(i.next());
        list.setModel(newListModel);
    }

    /**
     * This method is called when a new field should be added to the list. Performs validation of the 
     * field.
     */
    protected void addField(String s) {
        s = s.trim();
        if (forceLowerCase)
            s = s.toLowerCase();
        if (s.equals("") || listModel.contains(s))
            return;
        
        String testString = Util.checkLegalKey(s);
        if (!testString.equals(s) || (s.indexOf('&') >= 0)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Globals.lang("Field names are not allowed to contain white space or the following "
                    +"characters")+": # { } ~ , ^ &",
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        addFieldUncritically(s);
    }

    /**
     * This method adds a new field to the list, without any regard to validation. This method can be
     * useful for classes that overrides addField(s) to provide different validation.
     */
    protected void addFieldUncritically(String s) {
        listModel.addElement(s);
        changesMade = true;
        for (Iterator<ActionListener> i=additionListeners.iterator(); i.hasNext();) {
            i.next().actionPerformed(new ActionEvent(this, 0, s));
        }
        
    }
    
    protected void removeSelected() {
        int[] selected = list.getSelectedIndices();
        if (selected.length > 0)
            changesMade = true;
        for (int i=0; i<selected.length; i++)
            listModel.removeElementAt(selected[selected.length-1-i]);

    }
    
    public void activate() {
        sel.requestFocus();
    }
    
    /**
     * Returns true if there have been changes to the field list. Reports true
     * if changes have been made, regardless of whether the changes cancel each other.
     */
    public boolean changesMade() {
        return changesMade;
    }
    
    /**
     * Return the current list.
     */
    @SuppressWarnings("unchecked")
	public List<String> getFields() {
        Object[] o = listModel.toArray();
        return (List)java.util.Arrays.asList(o);
    }
    
    /**
     * Add a ListSelectionListener to the JList component displayed as part of this component.
     */
    public void addListSelectionListener(ListSelectionListener l) {
        list.addListSelectionListener(l);
    }
    
    /**
     * Adds an ActionListener that will receive events each time a field is added. The ActionEvent
     * will specify this component as source, and the added field as action command.
     */
    public void addAdditionActionListener(ActionListener l) {
        additionListeners.add(l);
    }
    
    public void removeAdditionActionListener(ActionListener l) {
        additionListeners.remove(l);
    }
    
    public void addListDataListener(ListDataListener l) {
        listModel.addListDataListener(l);
        modelListeners.add(l);
     }
    
    /**
     * If a field is selected in the list, move it dy positions.
     */
    public void move(int dy) {
        int oldIdx = list.getSelectedIndex();
        if  (oldIdx < 0)
            return;
        Object o = listModel.get(oldIdx);
        // Compute the new index:
        int newInd = Math.max(0, Math.min(listModel.size()-1, oldIdx+dy));
        listModel.remove(oldIdx);
        listModel.add(newInd, o);
        list.setSelectedIndex(newInd);
    }
    
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        
        if (src == add) {
            // Selection has been made, or add button pressed:
            if ((sel != null) && (sel.getSelectedItem() != null)) {
                String s = sel.getSelectedItem().toString();
                addField(s);
            } else if ((input != null) && !input.getText().equals("")) {
                addField(input.getText());
            }
        }
        else if (src == input) {
            addField(input.getText());
        }
        else if (src == remove) {
            // Remove button pressed:
            removeSelected();
        }
        else if (src == sel) {
            if (e.getActionCommand().equals("comboBoxChanged") && (e.getModifiers() == 0))
                // These conditions signify arrow key navigation in the dropdown list, so we should
                // not react to it. I'm not sure if this is well defined enough to be guaranteed to work
                // everywhere.
                return;
            String s = sel.getSelectedItem().toString();
            addField(s);
            sel.getEditor().selectAll();
        }
        else if (src == up) {
            move(-1);
        }
        else if (src == down) {
            move(1);
        }
    }
    
              
    
}
