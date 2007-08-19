/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

/**
 * Dialog for creating or modifying groups. Operates directly on the
 * Vector containing group information.
 */
class ReplaceStringDialog extends JDialog {

    JTextField
        fields = new JTextField("", 30),
        from = new JTextField("", 30),
        to = new JTextField("", 30);
    JLabel
        fl = new JLabel(Globals.lang("Search for")+":"),
        tl = new JLabel(Globals.lang("Replace with")+":");

    JButton
        ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    JPanel
        settings = new JPanel(),
        main = new JPanel(),
        opt = new JPanel();
    JCheckBox
        selOnly = new JCheckBox(Globals.lang("Limit to selected entries"), false);
    JRadioButton
        allFi = new JRadioButton(Globals.lang("All fields"), true),
        field = new JRadioButton(Globals.lang("Limit to fields")+":", false);
    ButtonGroup bg = new ButtonGroup();
    private boolean ok_pressed = false;
    private JabRefFrame parent;
    String[] flds = null;
    String s1, s2;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    public ReplaceStringDialog(JabRefFrame parent_) {
        super(parent_, Globals.lang("Replace string"), true);
        parent = parent_;

        bg.add(allFi);
        bg.add(field);
        ActionListener okListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    s1 = from.getText();
                    s2 = to.getText();
                    if (s1.equals(""))
                        return;
                    ok_pressed = true;
                    flds = Util.delimToStringArray(fields.getText().toLowerCase(), ";");
                    dispose();
                }
            };
        ok.addActionListener(okListener);
        to.addActionListener(okListener);
        fields.addActionListener(okListener);
        AbstractAction cancelAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };
        cancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = settings.getActionMap();
        InputMap im = settings.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(parent.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        // Layout starts here.
        settings.setLayout(gbl);
        opt.setLayout(gbl);
        main.setLayout(gbl);

        settings.setBorder(BorderFactory.createTitledBorder
                       (BorderFactory.createEtchedBorder(),
                        Globals.lang("Replace string")));
        main.setBorder(BorderFactory.createTitledBorder
                       (BorderFactory.createEtchedBorder(),
                        Globals.lang("Strings")));
          

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
        gbl.setConstraints(fl, con);
        main.add(fl);
        con.gridy = 1;
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

        Util.placeDialog(this, parent);
    }

    public boolean okPressed() { return ok_pressed; }
    public boolean allFields() { return allFi.isSelected(); }
    public boolean selOnly() { return selOnly.isSelected(); }
    public String[] fields() { return Util.delimToStringArray(field.getText(), ";"); }

    /**
     * Does the actual operation on a Bibtex entry based on the
     * settings specified in this same dialog. Returns the number of
     * occurences replaced.
     */
    public int replace(BibtexEntry be, NamedCompound ce) {
        int counter = 0;
        if (allFields()) {
        	
        	for (String s : be.getAllFields()){
                if (!s.equals(BibtexFields.KEY_FIELD))
                    counter += replaceField(be, s, ce);
            }
        } else {
            for (int i=0; i<flds.length; i++) {
                if (!flds[i].equals(BibtexFields.KEY_FIELD))
                    counter += replaceField(be, flds[i], ce);
            }

        }
        return counter;
    }

    public int replaceField(BibtexEntry be, String field, NamedCompound ce) {
        Object o = be.getField(field);
        if (o == null) return 0;
        String txt = o.toString();
        StringBuffer sb = new StringBuffer();
        int ind = -1, piv = 0, counter = 0, len1 = s1.length();
        while ((ind=txt.indexOf(s1, piv)) >= 0) {
            counter++;
            sb.append(txt.substring(piv, ind)); // Text leading up to s1
            sb.append(s2);  // Insert s2
            piv = ind+len1;
        }
        sb.append(txt.substring(piv));
        String newStr = sb.toString();
        be.setField(field, newStr);
        ce.addEdit(new UndoableFieldChange(be, field, txt, newStr));
        return counter;
    }
}
