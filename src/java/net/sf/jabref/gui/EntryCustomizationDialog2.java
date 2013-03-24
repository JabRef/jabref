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
package net.sf.jabref.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class EntryCustomizationDialog2 extends JDialog implements ListSelectionListener, ActionListener {

    protected JabRefFrame frame;
    protected GridBagLayout gbl = new GridBagLayout();
    protected GridBagConstraints con = new GridBagConstraints();
    protected FieldSetComponent reqComp, optComp, optComp2;
    protected EntryTypeList typeComp;
    protected JButton ok, cancel, apply, helpButton, delete, importTypes, exportTypes;
    protected final List<String> preset = java.util.Arrays.asList(BibtexFields.getAllFieldNames());
    protected String lastSelected = null;
    protected Map<String, List<String>> reqLists = new HashMap<String, List<String>>(),
            optLists = new HashMap<String, List<String>>(),
            opt2Lists = new HashMap<String, List<String>>();
    protected Set<String> defaulted = new HashSet<String>(), changed = new HashSet<String>();

    boolean biblatexMode;

    /** Creates a new instance of EntryCustomizationDialog2 */
    public EntryCustomizationDialog2(JabRefFrame frame) {
        super(frame, Globals.lang("Customize entry types"), false);

        this.frame = frame;
        initGui();
    }

    protected final void initGui() {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        biblatexMode = Globals.prefs.getBoolean("biblatexMode");

        JPanel main = new JPanel(), buttons = new JPanel(),
                right = new JPanel();
        main.setLayout(new BorderLayout());
        right.setLayout(new GridLayout(biblatexMode ? 2 : 1, 2));

        java.util.List<String> entryTypes = new ArrayList<String>();
        for (Iterator<String> i=BibtexEntryType.ALL_TYPES.keySet().iterator(); i.hasNext();) {
            entryTypes.add(i.next());
        }

        typeComp = new EntryTypeList(entryTypes);
        typeComp.addListSelectionListener(this);
        typeComp.addAdditionActionListener(this);
        typeComp.addDefaultActionListener(new DefaultListener());
        typeComp.setListSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //typeComp.setEnabled(false);
        reqComp = new FieldSetComponent(Globals.lang("Required fields"), new ArrayList<String>(), preset, true, true);
        reqComp.setEnabled(false);
        reqComp.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        ListDataListener dataListener = new DataListener();
        reqComp.addListDataListener(dataListener);
        optComp = new FieldSetComponent(Globals.lang("Optional fields"), new ArrayList<String>(), preset, true, true);
        optComp.setEnabled(false);
        optComp.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        optComp.addListDataListener(dataListener);
        right.add(reqComp);
        right.add(optComp);

        if (biblatexMode) {
            optComp2 = new FieldSetComponent(Globals.lang("Optional fields")+" 2", new ArrayList<String>(), preset, true, true);
            optComp2.setEnabled(false);
            optComp2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            optComp2.addListDataListener(dataListener);
            right.add(new JPanel());
            right.add(optComp2);
        }

        //right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Globals.lang("Fields")));
        right.setBorder(BorderFactory.createEtchedBorder());
        ok = new JButton("OK");
        cancel = new JButton(Globals.lang("Cancel"));
        apply = new JButton(Globals.lang("Apply"));
        ok.addActionListener(this);
        apply.addActionListener(this);
        cancel.addActionListener(this);
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(apply);
        bb.addButton(cancel);
        bb.addGlue();
                
        AbstractAction closeAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        };
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);

        //con.fill = GridBagConstraints.BOTH;
        //con.weightx = 0.3;
        //con.weighty = 1;
        //gbl.setConstraints(typeComp, con);
        main.add(typeComp, BorderLayout.WEST);
        main.add(right, BorderLayout.CENTER);
        main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pane.add(main, BorderLayout.CENTER);
        pane.add(buttons, BorderLayout.SOUTH);
        pack();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }


        if (lastSelected != null) {
            // The entry type lastSelected is now unselected, so we store the current settings
            // for that type in our two maps.
            reqLists.put(lastSelected, reqComp.getFields());
            optLists.put(lastSelected, optComp.getFields());
            if (biblatexMode)
                opt2Lists.put(lastSelected, optComp2.getFields());
        }

        String s = typeComp.getFirstSelected();
        if (s == null)
            return;
        List<String> rl = reqLists.get(s);
        if (rl == null) {
            BibtexEntryType type = BibtexEntryType.getType(s);
            if (type != null) {
                String[] rf = type.getRequiredFieldsForCustomization(),
                        of = type.getOptionalFields();
                List<String> req, opt;
                if (rf != null)
                    req = java.util.Arrays.asList(rf);
                else
                    req = new ArrayList<String>();

                if (!biblatexMode) {
                    if (of != null)
                        opt = java.util.Arrays.asList(of);
                    else
                        opt = new ArrayList<String>();
                } else {
                    String[] priOf = type.getPrimaryOptionalFields();
                    if (priOf != null)
                        opt = java.util.Arrays.asList(priOf);
                    else
                        opt = new ArrayList<String>();
                    List<String> opt2 = new ArrayList<String>();
                    if (of != null)
                        for (int i=0; i<of.length; i++)
                            if (!opt.contains(of[i]))
                                opt2.add(of[i]);
                    optComp2.setFields(opt2);
                    optComp2.setEnabled(true);
                }

                reqComp.setFields(req);
                reqComp.setEnabled(true);
                optComp.setFields(opt);
                optComp.setEnabled(true);
            } else {
                // New entry, veintle
                reqComp.setFields(new ArrayList<String>());
                reqComp.setEnabled(true);
                optComp.setFields(new ArrayList<String>());
                optComp.setEnabled(true);
                new FocusRequester(reqComp);
            }
        } else {
            reqComp.setFields(rl);
            optComp.setFields(optLists.get(s));
        }

        lastSelected = s;
        typeComp.enable(s, changed.contains(lastSelected) && !defaulted.contains(lastSelected));
    }

    protected void applyChanges() {
        valueChanged(new ListSelectionEvent(new JList(), 0, 0, false));
        // Iterate over our map of required fields, and list those types if necessary:

        List<String> types = typeComp.getFields();
        for (Iterator<String> i=reqLists.keySet().iterator(); i.hasNext();) {
            String typeName = i.next();
            if (!types.contains(typeName))
                continue;

            List<String> reqFields = reqLists.get(typeName);
            List<String> optFields = optLists.get(typeName);
            List<String> opt2Fields = opt2Lists.get(typeName);
            String[] reqStr = new String[reqFields.size()];
            reqStr = reqFields.toArray(reqStr);
            String[] optStr = new String[optFields.size()];
            optStr = optFields.toArray(optStr);
            String[] opt2Str;
            if (opt2Fields != null)
                opt2Str = opt2Fields.toArray(new String[opt2Fields.size()]);
            else
                opt2Str = new String[0];

            // If this type is already existing, check if any changes have
            // been made
            boolean changesMade = true;

            if (defaulted.contains(typeName)) {
                // This type should be reverted to its default setup.
                //System.out.println("Defaulting: "+typeName);
                String nm = Util.nCase(typeName);
                BibtexEntryType.removeType(nm);

                updateTypesForEntries(nm);
                continue;
            }

            BibtexEntryType oldType = BibtexEntryType.getType(typeName);
            if (oldType != null) {
                String[] oldReq = oldType.getRequiredFields(),
                        oldOpt = oldType.getOptionalFields();
                if (biblatexMode) {
                    String[] priOpt = oldType.getPrimaryOptionalFields();
                    ArrayList<String> secOpt = new ArrayList<String>();
                    for (int j = 0; j < oldOpt.length; j++) {
                        boolean inPri = false;
                        for (int k = 0; k < priOpt.length; k++) {
                            if (priOpt[k].equals(oldOpt[j])) {
                                inPri = true;
                                break;
                            }
                        }
                        if (!inPri)
                            secOpt.add(oldOpt[j]);
                    }
                    String[] secOptArray = secOpt.toArray(new String[secOpt.size()]);
                    if (equalArrays(oldReq, reqStr) && equalArrays(oldOpt, optStr) &&
                            equalArrays(secOptArray, opt2Str))
                        changesMade = false;
                } else
                    if (equalArrays(oldReq, reqStr) && equalArrays(oldOpt, optStr))
                        changesMade = false;
            }

            if (changesMade) {
                //System.out.println("Updating: "+typeName);
                CustomEntryType typ = biblatexMode ?
                        new CustomEntryType(Util.nCase(typeName), reqStr, optStr, opt2Str) :
                        new CustomEntryType(Util.nCase(typeName), reqStr, optStr);

                BibtexEntryType.ALL_TYPES.put(typeName.toLowerCase(), typ);
                updateTypesForEntries(typ.getName());
            }
        }


        Set<Object> toRemove = new HashSet<Object>();
        for (Iterator<String> i=BibtexEntryType.ALL_TYPES.keySet().iterator(); i.hasNext();) {
            Object o = i.next();
            if (!types.contains(o)) {
                toRemove.add(o);
            }
        }

        // Remove those that should be removed:
        if (toRemove.size() > 0) {
            for (Iterator<Object> i=toRemove.iterator(); i.hasNext();)
                typeDeletion((String)i.next());
        }

        updateTables();
    }

    protected void typeDeletion(String name) {
        BibtexEntryType type = BibtexEntryType.getType(name);

        if (type instanceof CustomEntryType) {
            if (BibtexEntryType.getStandardType(name) == null) {
                int reply = JOptionPane.showConfirmDialog
                        (frame, Globals.lang("All entries of this "
                        +"type will be declared "
                        +"typeless. Continue?"),
                        Globals.lang("Delete custom format")+
                        " '"+Util.nCase(name)+"'", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (reply != JOptionPane.YES_OPTION)
                    return;
            }
            BibtexEntryType.removeType(name);
            updateTypesForEntries(Util.nCase(name));
            changed.remove(name);
            reqLists.remove(name);
            optLists.remove(name);
        }
        //messageLabel.setText("'"+type.getName()+"' "+
        //        Globals.lang("is a standard type."));

    }


protected boolean equalArrays(String[] one, String[] two) {
    if ((one == null) && (two == null))
        return true; // Both null.
    if ((one == null) || (two == null))
        return false; // One of them null, the other not.
    if (one.length != two.length)
        return false; // Different length.
    // If we get here, we know that both are non-null, and that they have the same length.
    for (int i=0; i<one.length; i++) {
        if (!one[i].equals(two[i]))
            return false;
    }
    // If we get here, all entries have matched.
    return true;
}

public void actionPerformed(ActionEvent e) {
    if (e.getSource() == ok) {
        applyChanges();
        dispose();
    } else if (e.getSource() == cancel) {
        dispose();
    } else if (e.getSource() == apply) {
        applyChanges();
    } else if (e.getSource() == typeComp) {
        //System.out.println("add: "+e.getActionCommand());
        typeComp.selectField(e.getActionCommand());
    }
}

/**
 * Cycle through all databases, and make sure everything is updated with
 * the new type customization. This includes making sure all entries have
 * a valid type, that no obsolete entry editors are around, and that
 * the right-click menus' change type menu is up-to-date.
 */
private void updateTypesForEntries(String typeName) {
    if (frame.getTabbedPane().getTabCount() == 0)
        return;
    for (int i=0; i<frame.getTabbedPane().getTabCount(); i++) {
        BasePanel bp = (BasePanel)frame.getTabbedPane().getComponentAt(i);

        // Invalidate associated cached entry editor
        bp.entryEditors.remove(typeName);

        for (BibtexEntry entry : bp.database().getEntries()){
            entry.updateType();
        }
    }

}

private void updateTables() {
    if (frame.getTabbedPane().getTabCount() == 0)
        return;
    for (int i=0; i<frame.getTabbedPane().getTabCount(); i++) {
        frame.getTabbedPane().getComponentAt(i);
    }

}

// DEFAULT button pressed. Remember that this entry should be reset to default,
// unless changes are made later.
class DefaultListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        if (lastSelected == null)
            return;
        defaulted.add(lastSelected);

        BibtexEntryType type = BibtexEntryType.getStandardType(lastSelected);
        if (type != null) {
            String[] rf = type.getRequiredFieldsForCustomization(),
                    of = type.getOptionalFields();
            List<String> req, opt;
            if (rf != null)
                req = java.util.Arrays.asList(rf);
            else
                req = new ArrayList<String>();
            if (of != null)
                opt = java.util.Arrays.asList(of);
            else
                opt = new ArrayList<String>();

            reqComp.setFields(req);
            reqComp.setEnabled(true);
            optComp.setFields(opt);
        }
    }
}

class DataListener implements ListDataListener {


    public void intervalAdded(javax.swing.event.ListDataEvent e) {
        record();
    }

    public void intervalRemoved(javax.swing.event.ListDataEvent e) {
        record();
    }

    public void contentsChanged(javax.swing.event.ListDataEvent e) {
        record();
    }

    private void record() {
        if (lastSelected == null)
            return;
        defaulted.remove(lastSelected);
        changed.add(lastSelected);
        typeComp.enable(lastSelected, true);
    }

}
}
