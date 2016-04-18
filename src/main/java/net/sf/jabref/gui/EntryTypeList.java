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

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.entry.EntryType;

/**
 * This class extends FieldSetComponent to provide some required functionality for the
 * list of entry types in EntryCustomizationDialog.
 * @author alver
 */
public class EntryTypeList extends FieldSetComponent implements ListSelectionListener {

    private final JButton def = new JButton(Localization.lang("Default"));
    private final BibDatabaseMode mode;

    /** Creates a new instance of EntryTypeList */
    public EntryTypeList(List<String> fields, BibDatabaseMode mode) {
        super(Localization.lang("Entry types"), fields, false, true);
        this.mode = mode;

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

    @Override
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
            JOptionPane.showMessageDialog(this, Localization.lang("Entry type names are not allowed to contain white space or the following "
                            + "characters") + ": # { } ~ , ^ &",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if ("comment".equalsIgnoreCase(s)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Localization.lang("The name 'comment' cannot be used as an entry type name."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        addFieldUncritically(s);
    }

    @Override
    protected void removeSelected() {
        //super.removeSelected();
        int[] selected = list.getSelectedIndices();
        if (selected.length > 0) {
            changesMade = true;
        }
        for (int i = 0; i < selected.length; i++) {
            String typeName = listModel.get(selected[selected.length - 1 - i]);
            Optional<EntryType> type = EntryTypes.getType(typeName, this.mode);

            // If it is a custom entry type, we can remove it. If type == null, it means
            // the user must have added it and not yet applied it, so we can remove it
            // in this case as well. If it is a standard type it cannot be removed.
            if (type.isPresent() && (type.get() instanceof CustomEntryType)) {
                listModel.removeElementAt(selected[selected.length - 1 - i]);
            } else {
                // This shouldn't happen, since the Remove button should be disabled.
                JOptionPane.showMessageDialog(null, Localization.lang("This entry type cannot be removed."),
                        Localization.lang("Remove entry type"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // Do nothing
    }

    public void enable(String typeName, boolean isChanged) {
        //String s = (String)list.getSelectedValue();

        if (EntryTypes.getStandardType(typeName, mode).isPresent()) {
            Optional<EntryType> entryType = EntryTypes.getType(typeName, mode);
            if (isChanged || (entryType.isPresent() && (entryType.get() instanceof CustomEntryType))) {
                def.setEnabled(true);
            } else {
                def.setEnabled(false);
            }

            remove.setEnabled(false);
        } else {
            def.setEnabled(false);
            remove.setEnabled(true);
        }
    }

    public void addDefaultActionListener(ActionListener l) {
        def.addActionListener(l);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Default button pressed.
        if (e.getSource() == def) {
            def.setEnabled(false);
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    public void setEnabled(boolean en) {
        super.setEnabled(en);
        def.setEnabled(en);
    }
}
