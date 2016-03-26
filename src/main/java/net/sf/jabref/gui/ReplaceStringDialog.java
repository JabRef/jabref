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

import javax.swing.*;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Dialog for creating or modifying groups. Operates directly on the
 * Vector containing group information.
 */
class ReplaceStringDialog extends JDialog {

    private final JTextField fields = new JTextField("", 30);
    private final JTextField from = new JTextField("", 30);
    private final JTextField to = new JTextField("", 30);

    private final JCheckBox selOnly = new JCheckBox(Localization.lang("Limit to selected entries"), false);
    private final JRadioButton allFi = new JRadioButton(Localization.lang("All fields"), true);
    private final JRadioButton field = new JRadioButton(Localization.lang("Limit to fields") + ":", false);
    private boolean okPressed;
    private String[] flds;
    private String s1;
    private String s2;


    public ReplaceStringDialog(JabRefFrame parent) {
        super(parent, Localization.lang("Replace string"), true);

        ButtonGroup bg = new ButtonGroup();
        bg.add(allFi);
        bg.add(field);
        ActionListener okListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                s1 = from.getText();
                s2 = to.getText();
                if ("".equals(s1)) {
                    return;
                }
                okPressed = true;
                flds = fields.getText().toLowerCase().split(";");
                dispose();
            }
        };
        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(okListener);
        to.addActionListener(okListener);
        fields.addActionListener(okListener);
        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(cancelAction);

        // Key bindings:
        JPanel settings = new JPanel();
        ActionMap am = settings.getActionMap();
        InputMap im = settings.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        // Layout starts here.
        GridBagLayout gbl = new GridBagLayout();
        settings.setLayout(gbl);
        JPanel opt = new JPanel();
        opt.setLayout(gbl);
        JPanel main = new JPanel();
        main.setLayout(gbl);

        settings.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createEtchedBorder(),
                        Localization.lang("Replace string")));
        main.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createEtchedBorder(),
                        Localization.lang("Strings")));

        // Settings panel:
        /*
        con.weightx = 0;
        con.insets = new Insets(3, 5, 3, 5);
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        con.gridx = 0;
        con.gridy = 2;
        gbl.setConstraints(nf, con);
        settings.add(nf);*/
        //con.weightx = 1;
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.HORIZONTAL;
        //JSeparator sep = new JSeparator()
        con.gridwidth = 2;
        con.weightx = 0;
        con.anchor = GridBagConstraints.WEST;
        con.gridy = 0;
        con.gridx = 0;
        con.insets = new Insets(3, 5, 3, 5);
        gbl.setConstraints(selOnly, con);
        settings.add(selOnly);
        con.gridy = 1;
        con.insets = new Insets(13, 5, 3, 5);
        gbl.setConstraints(allFi, con);
        settings.add(allFi);
        con.gridwidth = 1;
        con.gridy = 2;
        con.gridx = 0;
        con.insets = new Insets(3, 5, 3, 5);
        gbl.setConstraints(field, con);
        settings.add(field);
        con.gridx = 1;
        con.weightx = 1;
        //con.insets = new Insets(3, 5, 3, 5);
        gbl.setConstraints(fields, con);
        settings.add(fields);

        con.weightx = 0;
        con.gridx = 0;
        con.gridy = 0;
        JLabel fl = new JLabel(Localization.lang("Search for") + ":");
        gbl.setConstraints(fl, con);
        main.add(fl);
        con.gridy = 1;
        JLabel tl = new JLabel(Localization.lang("Replace with") + ":");
        gbl.setConstraints(tl, con);
        main.add(tl);
        con.weightx = 1;
        con.gridx = 1;
        con.gridy = 0;
        gbl.setConstraints(from, con);
        main.add(from);
        con.gridy = 1;
        gbl.setConstraints(to, con);
        main.add(to);

        // Option buttons:
        con.gridx = GridBagConstraints.RELATIVE;
        con.gridy = GridBagConstraints.RELATIVE;
        con.weightx = 1;
        con.gridwidth = 1;
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        gbl.setConstraints(ok, con);
        opt.add(ok);
        con.anchor = GridBagConstraints.WEST;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(cancel, con);
        opt.add(cancel);

        getContentPane().add(main, BorderLayout.NORTH);
        getContentPane().add(settings, BorderLayout.CENTER);
        getContentPane().add(opt, BorderLayout.SOUTH);

        pack();
        //setSize(400, 170);

        this.setLocationRelativeTo(parent);
    }

    public boolean okPressed() {
        return okPressed;
    }

    private boolean allFields() {
        return allFi.isSelected();
    }

    public boolean selOnly() {
        return selOnly.isSelected();
    }

    /**
     * Does the actual operation on a Bibtex entry based on the
     * settings specified in this same dialog. Returns the number of
     * occurences replaced.
     */
    public int replace(BibEntry be, NamedCompound ce) {
        int counter = 0;
        if (allFields()) {

            for (String s : be.getFieldNames()) {
                if (!s.equals(BibEntry.KEY_FIELD)) {
                    counter += replaceField(be, s, ce);
                }
            }
        } else {
            for (String fld : flds) {
                if (!fld.equals(BibEntry.KEY_FIELD)) {
                    counter += replaceField(be, fld, ce);
                }
            }

        }
        return counter;
    }

    private int replaceField(BibEntry be, String fieldname, NamedCompound ce) {
        if (!be.hasField(fieldname)) {
            return 0;
        }
        String txt = be.getField(fieldname);
        StringBuilder sb = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = s1.length();
        while ((ind = txt.indexOf(s1, piv)) >= 0) {
            counter++;
            sb.append(txt.substring(piv, ind)); // Text leading up to s1
            sb.append(s2); // Insert s2
            piv = ind + len1;
        }
        sb.append(txt.substring(piv));
        String newStr = sb.toString();
        be.setField(fieldname, newStr);
        ce.addEdit(new UndoableFieldChange(be, fieldname, txt, newStr));
        return counter;
    }
}
