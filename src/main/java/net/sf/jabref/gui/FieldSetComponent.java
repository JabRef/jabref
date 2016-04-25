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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;

/**
 *
 * @author alver
 */
class FieldSetComponent extends JPanel implements ActionListener {

    private final Set<ActionListener> additionListeners = new HashSet<>();
    protected final JList<String> list;
    private final JScrollPane sp;
    protected DefaultListModel<String> listModel;
    private JComboBox<String> sel;
    private JTextField input;
    private final JButton add;
    protected final JButton remove;
    private JButton up;
    private JButton down;
    protected final GridBagLayout gbl = new GridBagLayout();
    protected final GridBagConstraints con = new GridBagConstraints();
    protected final boolean forceLowerCase;
    protected boolean changesMade;
    private final Set<ListDataListener> modelListeners = new HashSet<>();


    /**
     * Creates a new instance of FieldSetComponent, with preset selection
     * values. These are put into a JComboBox.
     */
    public FieldSetComponent(String title, List<String> fields, List<String> preset, boolean arrows, boolean forceLowerCase) {
        this(title, fields, preset, Localization.lang("Add"),
                Localization.lang("Remove"), arrows, forceLowerCase);
    }

    /**
     * Creates a new instance of FieldSetComponent without preset selection
     * values. Replaces the JComboBox with a JTextField.
     */
    FieldSetComponent(String title, List<String> fields, boolean arrows, boolean forceLowerCase) {
        this(title, fields, null, Localization.lang("Add"),
                Localization.lang("Remove"), arrows, forceLowerCase);
    }

    private FieldSetComponent(String title, List<String> fields, List<String> preset, String addText, String removeText,
                              boolean arrows, boolean forceLowerCase) {
        this.forceLowerCase = forceLowerCase;
        add = new JButton(addText);
        remove = new JButton(removeText);
        listModel = new DefaultListModel<>();
        JLabel title1 = null;
        if (title != null) {
            title1 = new JLabel(title);
        }

        for (String field : fields) {
            listModel.addElement(field);
        }
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Set up GUI:
        add.addActionListener(this);
        remove.addActionListener(this);

        setLayout(gbl);
        con.insets = new Insets(1, 1, 1, 1);
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
        con.gridwidth = GridBagConstraints.REMAINDER;
        if (title1 != null) {
            gbl.setConstraints(title1, con);
            add(title1);
        }

        con.weighty = 1;
        sp = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbl.setConstraints(sp, con);
        add(sp);
        con.weighty = 0;
        con.gridwidth = 1;
        if (arrows) {
            con.weightx = 0;
            up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());
            down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
            up.addActionListener(this);
            down.addActionListener(this);
            up.setToolTipText(Localization.lang("Move up"));
            down.setToolTipText(Localization.lang("Move down"));
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
        if (preset == null) {
            input = new JTextField(20);
            input.addActionListener(this);
            gbl.setConstraints(input, con);
            add(input);
        } else {
            sel = new JComboBox<>(preset.toArray(new String[preset.size()]));
            sel.setEditable(true);
            //sel.addActionListener(this);
            gbl.setConstraints(sel, con);
            add(sel);
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
        if (idx >= 0) {
            list.setSelectedIndex(idx);
        }

        // Make sure it is visible:
        JViewport viewport = sp.getViewport();
        Rectangle rectangle = list.getCellBounds(idx, idx);
        if(rectangle != null) {
            viewport.scrollRectToVisible(rectangle);
        }

    }

    public String getFirstSelected() {
        Object o = list.getSelectedValue();
        if (o == null) {
            return null;
        }
        return (String) o;
    }

    @Override
    public void setEnabled(boolean en) {
        if (input != null) {
            input.setEnabled(en);
        }
        if (sel != null) {
            sel.setEnabled(en);
        }
        if (up != null) {
            up.setEnabled(en);
            down.setEnabled(en);
        }
        add.setEnabled(en);
        remove.setEnabled(en);
    }

    public void setFields(List<String> fields) {
        DefaultListModel<String> newListModel = new DefaultListModel<>();
        for (String field : fields) {
            newListModel.addElement(field);
        }
        this.listModel = newListModel;
        for (ListDataListener modelListener : modelListeners) {
            newListModel.addListDataListener(modelListener);
        }
        list.setModel(newListModel);
    }

    /**
     * This method is called when a new field should be added to the list. Performs validation of the
     * field.
     */
    protected void addField(String str) {
        String s = str.trim();
        if (forceLowerCase) {
            s = s.toLowerCase();
        }
        if ("".equals(s) || listModel.contains(s)) {
            return;
        }

        String testString = LabelPatternUtil.checkLegalKey(s);
        if (!testString.equals(s) || (s.indexOf('&') >= 0)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Localization.lang("Field names are not allowed to contain white space or the following "
                            + "characters") + ": # { } ~ , ^ &",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);

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
        for (ActionListener additionListener : additionListeners) {
            additionListener.actionPerformed(new ActionEvent(this, 0, s));
        }

    }

    protected void removeSelected() {
        int[] selected = list.getSelectedIndices();
        if (selected.length > 0) {
            changesMade = true;
        }
        for (int i = 0; i < selected.length; i++) {
            listModel.removeElementAt(selected[selected.length - 1 - i]);
        }

    }

    /**
     * Return the current list.
     */
    public List<String> getFields() {
        List<String> res = new ArrayList<>(listModel.getSize());
        Enumeration<String> elements = listModel.elements();
        while (elements.hasMoreElements()) {
            res.add(elements.nextElement());
        }
        return res;
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

    public void addListDataListener(ListDataListener l) {
        listModel.addListDataListener(l);
        modelListeners.add(l);
    }

    /**
     * If a field is selected in the list, move it dy positions.
     */
    private void move(int dy) {
        int oldIdx = list.getSelectedIndex();
        if (oldIdx < 0) {
            return;
        }
        String o = listModel.get(oldIdx);
        // Compute the new index:
        int newInd = Math.max(0, Math.min(listModel.size() - 1, oldIdx + dy));
        listModel.remove(oldIdx);
        listModel.add(newInd, o);
        list.setSelectedIndex(newInd);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (Objects.equals(src, add)) {
            // Selection has been made, or add button pressed:
            if ((sel != null) && (sel.getSelectedItem() != null)) {
                String s = sel.getSelectedItem().toString();
                addField(s);
            } else if ((input != null) && !"".equals(input.getText())) {
                addField(input.getText());
            }
        } else if (Objects.equals(src, input)) {
            addField(input.getText());
        } else if (Objects.equals(src, remove)) {
            // Remove button pressed:
            removeSelected();
        } else if (Objects.equals(src, sel)) {
            if ("comboBoxChanged".equals(e.getActionCommand()) && (e.getModifiers() == 0)) {
                // These conditions signify arrow key navigation in the dropdown list, so we should
                // not react to it. I'm not sure if this is well defined enough to be guaranteed to work
                // everywhere.
                return;
            }
            String s = sel.getSelectedItem().toString();
            addField(s);
            sel.getEditor().selectAll();
        } else if (Objects.equals(src, up)) {
            move(-1);
        } else if (Objects.equals(src, down)) {
            move(1);
        }
    }

}
