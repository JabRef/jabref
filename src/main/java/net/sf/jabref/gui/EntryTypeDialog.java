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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.EntryUtil;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if cancelled.
 */
public class EntryTypeDialog extends JDialog implements ActionListener {
    private EntryType type;
    private static final int COLUMN = 3;

    private final CancelAction cancelAction = new CancelAction();

    static class TypeButton extends JButton implements Comparable<TypeButton> {
        final EntryType type;

        public TypeButton(String label, EntryType _type) {
            super(label);
            type = _type;
        }

        @Override
        public int compareTo(TypeButton o) {
            return type.getName().compareTo(o.type.getName());
        }
    }

    public EntryTypeDialog(JabRefFrame _baseFrame) {
        // modal dialog
        super(_baseFrame, true);

        setTitle(Localization.lang("Select entry type"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction.actionPerformed(null);
            }
        });

        getContentPane().setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(this);

        // Make ESC close dialog, equivalent to clicking Cancel.
        cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(_baseFrame.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        cancel.getActionMap().put("close", cancelAction);

        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        bb.addGlue();
        bb.addButton(cancel);
        bb.addGlue();
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // Entry types
        addEntryTypeGroup(panel, EntryTypes.getAllValues());

        pack();
        setResizable(false);
    }

    private void addEntryTypeGroup(JPanel panel, Collection<EntryType> entries) {
        GridBagLayout bagLayout = new GridBagLayout();
        panel.setLayout(bagLayout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 4, 4, 4);
        // column count
        int col = 0;

        for (EntryType entryType : entries) {
            TypeButton entryButton = new TypeButton(EntryUtil.capitalizeFirst(entryType.getName()), entryType);
            entryButton.addActionListener(this);
            // Check if we should finish the row.
            col++;
            if (col == EntryTypeDialog.COLUMN) {
                col = 0;
                constraints.gridwidth = GridBagConstraints.REMAINDER;
            } else {
                constraints.gridwidth = 1;
            }
            bagLayout.setConstraints(entryButton, constraints);
            panel.add(entryButton);
        }
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Localization.lang("Entry types")));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof TypeButton) {
            type = ((TypeButton) e.getSource()).type;
        }
        dispose();
    }

    public EntryType getChoice() {
        return type;
    }


    class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }

}
