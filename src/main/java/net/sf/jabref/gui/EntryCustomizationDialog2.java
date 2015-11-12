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
import net.sf.jabref.gui.keyboard.KeyBinds;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;

public class EntryCustomizationDialog2 extends JDialog implements ListSelectionListener, ActionListener {

    private final JabRefFrame frame;
    protected GridBagLayout gbl = new GridBagLayout();
    protected GridBagConstraints con = new GridBagConstraints();
    private FieldSetComponent reqComp;
    private FieldSetComponent optComp;
    private FieldSetComponent optComp2;
    private EntryTypeList typeComp;
    private JButton ok;
    private JButton cancel;
    private JButton apply;
    protected JButton helpButton;
    protected JButton delete;
    protected JButton importTypes;
    protected JButton exportTypes;
    private final List<String> preset = java.util.Arrays.asList(BibtexFields.getAllFieldNames());
    private String lastSelected;
    private final Map<String, List<String>> reqLists = new HashMap<>();
    private final Map<String, List<String>> optLists = new HashMap<>();
    private final Map<String, List<String>> opt2Lists = new HashMap<>();
    private final Set<String> defaulted = new HashSet<>();
    private final Set<String> changed = new HashSet<>();

    private boolean biblatexMode;


    /**
     * Creates a new instance of EntryCustomizationDialog2
     */
    public EntryCustomizationDialog2(JabRefFrame frame) {
        super(frame, Localization.lang("Customize entry types"), false);

        this.frame = frame;
        initGui();
    }

    private void initGui() {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        biblatexMode = Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);

        JPanel main = new JPanel();
        JPanel buttons = new JPanel();
        JPanel right = new JPanel();
        main.setLayout(new BorderLayout());
        right.setLayout(new GridLayout(biblatexMode ? 2 : 1, 2));

        java.util.List<String> entryTypes = new ArrayList<>();
        for (String s : BibtexEntryType.getAllTypes()) {
            entryTypes.add(s);
        }

        typeComp = new EntryTypeList(entryTypes);
        typeComp.addListSelectionListener(this);
        typeComp.addAdditionActionListener(this);
        typeComp.addDefaultActionListener(new DefaultListener());
        typeComp.setListSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //typeComp.setEnabled(false);
        reqComp = new FieldSetComponent(Localization.lang("Required fields"), new ArrayList<String>(), preset, true, true);
        reqComp.setEnabled(false);
        reqComp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        ListDataListener dataListener = new DataListener();
        reqComp.addListDataListener(dataListener);
        optComp = new FieldSetComponent(Localization.lang("Optional fields"), new ArrayList<String>(), preset, true, true);
        optComp.setEnabled(false);
        optComp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        optComp.addListDataListener(dataListener);
        right.add(reqComp);
        right.add(optComp);

        if (biblatexMode) {
            optComp2 = new FieldSetComponent(Localization.lang("Optional fields") + " 2", new ArrayList<String>(), preset, true, true);
            optComp2.setEnabled(false);
            optComp2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            optComp2.addListDataListener(dataListener);
            right.add(new JPanel());
            right.add(optComp2);
        }

        //right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Globals.lang("Fields")));
        right.setBorder(BorderFactory.createEtchedBorder());
        ok = new JButton(Localization.lang("Ok"));
        cancel = new JButton(Localization.lang("Cancel"));
        apply = new JButton(Localization.lang("Apply"));
        ok.addActionListener(this);
        apply.addActionListener(this);
        cancel.addActionListener(this);
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(apply);
        bb.addButton(cancel);
        bb.addGlue();

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

        //con.fill = GridBagConstraints.BOTH;
        //con.weightx = 0.3;
        //con.weighty = 1;
        //gbl.setConstraints(typeComp, con);
        main.add(typeComp, BorderLayout.WEST);
        main.add(right, BorderLayout.CENTER);
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pane.add(main, BorderLayout.CENTER);
        pane.add(buttons, BorderLayout.SOUTH);
        pack();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (lastSelected != null) {
            // The entry type lastSelected is now unselected, so we store the current settings
            // for that type in our two maps.
            reqLists.put(lastSelected, reqComp.getFields());
            optLists.put(lastSelected, optComp.getFields());
            if (biblatexMode) {
                opt2Lists.put(lastSelected, optComp2.getFields());
            }
        }

        String s = typeComp.getFirstSelected();
        if (s == null) {
            return;
        }
        List<String> rl = reqLists.get(s);
        if (rl == null) {
            BibtexEntryType type = BibtexEntryType.getType(s);
            if (type != null) {
                List<String> req = type.getRequiredFieldsForCustomization();

                List<String> opt;
                if (!biblatexMode) {
                    opt = type.getOptionalFields();
                } else {
                    opt = type.getPrimaryOptionalFields();

                    List<String> opt2 = type.getSecondaryOptionalFields();

                    optComp2.setFields(opt2);
                    optComp2.setEnabled(true);
                }
                reqComp.setFields(req);
                reqComp.setEnabled(true);
                optComp.setFields(opt);
                optComp.setEnabled(true);
            } else {
                // New entry
                reqComp.setFields(new ArrayList<String>());
                reqComp.setEnabled(true);
                optComp.setFields(new ArrayList<String>());
                optComp.setEnabled(true);
                if (biblatexMode) {
                    optComp2.setFields(new ArrayList<String>());
                    optComp2.setEnabled(true);
                }
                new FocusRequester(reqComp);
            }
        } else {
            reqComp.setFields(rl);
            optComp.setFields(optLists.get(s));
            if (biblatexMode) {
                optComp2.setFields(opt2Lists.get(s));
            }
        }

        lastSelected = s;
        typeComp.enable(s, changed.contains(lastSelected) && !defaulted.contains(lastSelected));
    }

    private void applyChanges() {
        valueChanged(new ListSelectionEvent(new JList<>(), 0, 0, false));
        // Iterate over our map of required fields, and list those types if necessary:

        List<String> types = typeComp.getFields();
        for (Map.Entry<String, List<String>> stringListEntry : reqLists.entrySet()) {
            if (!types.contains(stringListEntry.getKey())) {
                continue;
            }

            List<String> reqStr = stringListEntry.getValue();
            List<String> optStr = optLists.get(stringListEntry.getKey());
            List<String> opt2Str = opt2Lists.get(stringListEntry.getKey());

            if (opt2Str != null) {
                opt2Str = new ArrayList<>(0);
            }

            // If this type is already existing, check if any changes have
            // been made
            boolean changesMade = true;

            if (defaulted.contains(stringListEntry.getKey())) {
                // This type should be reverted to its default setup.
                //System.out.println("Defaulting: "+typeName);
                String nm = StringUtil.capitalizeFirst(stringListEntry.getKey());
                BibtexEntryType.removeType(nm);

                updateTypesForEntries(nm);
                continue;
            }

            BibtexEntryType oldType = BibtexEntryType.getType(stringListEntry.getKey());
            if (oldType != null) {
                List<String> oldReq = oldType.getRequiredFields();
                List<String> oldOpt = oldType.getOptionalFields();
                if (biblatexMode) {
                    List<String> oldPriOpt = oldType.getPrimaryOptionalFields();
                    List<String> oldSecOpt = oldType.getSecondaryOptionalFields();
                    if (equalLists(oldReq, reqStr) && equalLists(oldPriOpt, optStr) &&
                            equalLists(oldSecOpt, opt2Str)) {
                        changesMade = false;
                    }
                } else if (equalLists(oldReq, reqStr) && equalLists(oldOpt, optStr)) {
                    changesMade = false;
                }
            }

            if (changesMade) {
                //System.out.println("Updating: "+typeName);
                CustomEntryType typ = biblatexMode ?
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), reqStr, optStr, opt2Str) :
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), reqStr, optStr);

                BibtexEntryType.addOrModifyCustomEntryType(typ);
                updateTypesForEntries(typ.getName());
            }
        }

        Set<Object> toRemove = new HashSet<>();
        for (String o : BibtexEntryType.getAllTypes()) {
            if (!types.contains(o)) {
                toRemove.add(o);
            }
        }

        // Remove those that should be removed:
        if (!toRemove.isEmpty()) {
            for (Object aToRemove : toRemove) {
                typeDeletion((String) aToRemove);
            }
        }

        updateTables();
    }

    private void typeDeletion(String name) {
        BibtexEntryType type = BibtexEntryType.getType(name);

        if (type instanceof CustomEntryType) {
            if (BibtexEntryType.getStandardType(name) == null) {
                int reply = JOptionPane.showConfirmDialog
                        (frame, Localization.lang("All entries of this "
                                        + "type will be declared "
                                        + "typeless. Continue?"),
                                Localization.lang("Delete custom format") +
                                        " '" + StringUtil.capitalizeFirst(name) + '\'', JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                if (reply != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            BibtexEntryType.removeType(name);
            updateTypesForEntries(StringUtil.capitalizeFirst(name));
            changed.remove(name);
            reqLists.remove(name);
            optLists.remove(name);
            if (biblatexMode) {
                opt2Lists.remove(name);
            }
        }
        //messageLabel.setText("'"+type.getName()+"' "+
        //        Globals.lang("is a standard type."));

    }

    private static boolean equalLists(List<String> one, List<String> two) {
        if ((one == null) && (two == null)) {
            return true; // Both null.
        }
        if ((one == null) || (two == null)) {
            return false; // One of them null, the other not.
        }
        if (one.size() != two.size()) {
            return false; // Different length.
        }
        // If we get here, we know that both are non-null, and that they have the same length.
        for (int i = 0; i < one.size(); i++) {
            if (!one.get(i).equals(two.get(i))) {
                return false;
            }
        }
        // If we get here, all entries have matched.
        return true;
    }

    @Override
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
        if (frame.getTabbedPane().getTabCount() == 0) {
            return;
        }
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            BasePanel bp = (BasePanel) frame.getTabbedPane().getComponentAt(i);

            // Invalidate associated cached entry editor
            bp.entryEditors.remove(typeName);

            for (BibtexEntry entry : bp.database().getEntries()) {
                BibtexEntryType newType = BibtexEntryType.getType(entry.getType().getName());
                if (newType != null) {
                    entry.setType(newType);
                } else {
                    entry.setType(BibtexEntryTypes.TYPELESS);
                }
            }
        }

    }

    private void updateTables() {
        if (frame.getTabbedPane().getTabCount() == 0) {
            return;
        }
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            frame.getTabbedPane().getComponentAt(i);
        }

    }


    // DEFAULT button pressed. Remember that this entry should be reset to default,
    // unless changes are made later.
    private class DefaultListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (lastSelected == null) {
                return;
            }
            defaulted.add(lastSelected);

            BibtexEntryType type = BibtexEntryType.getStandardType(lastSelected);
            if (type != null) {
                List<String> of = type.getOptionalFields();
                List<String> req = type.getRequiredFieldsForCustomization();
                List<String> opt1 = new ArrayList<>();
                List<String> opt2 = new ArrayList<>();

                if (biblatexMode) {
                    if (of.size() != 0) {
                        opt1 = type.getPrimaryOptionalFields();
                        opt2 = type.getSecondaryOptionalFields();
                    }
                } else {
                    if (of.size() != 0) {
                        opt1 = of;
                    }
                }

                reqComp.setFields(req);
                reqComp.setEnabled(true);
                optComp.setFields(opt1);
                if (biblatexMode) {
                    optComp2.setFields(opt2);
                }
            }
        }
    }

    class DataListener implements ListDataListener {

        @Override
        public void intervalAdded(javax.swing.event.ListDataEvent e) {
            record();
        }

        @Override
        public void intervalRemoved(javax.swing.event.ListDataEvent e) {
            record();
        }

        @Override
        public void contentsChanged(javax.swing.event.ListDataEvent e) {
            record();
        }

        private void record() {
            if (lastSelected == null) {
                return;
            }
            defaulted.remove(lastSelected);
            changed.add(lastSelected);
            typeComp.enable(lastSelected, true);
        }

    }
}
