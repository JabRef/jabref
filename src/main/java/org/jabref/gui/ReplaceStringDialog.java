package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Dialog for replacing strings.
 */
class ReplaceStringDialog extends JabRefDialog {

    private final JTextField fieldsField = new JTextField("", 30);
    private final JTextField fromField = new JTextField("", 30);
    private final JTextField toField = new JTextField("", 30);

    private final JCheckBox selOnly = new JCheckBox(Localization.lang("Limit to selected entries"), false);
    private final JRadioButton allFi = new JRadioButton(Localization.lang("All fields"), true);
    private final JRadioButton field = new JRadioButton(Localization.lang("Limit to fields") + ":", false);
    private boolean okPressed;
    private String[] fieldStrings;
    private String fromString;
    private String toString;


    public ReplaceStringDialog(JabRefFrame parent) {
        super(parent, Localization.lang("Replace string"), true, ReplaceStringDialog.class);

        ButtonGroup bg = new ButtonGroup();
        bg.add(allFi);
        bg.add(field);
        ActionListener okListener = e -> {
            fromString = fromField.getText();
            toString = toField.getText();
            if ("".equals(fromString)) {
                return;
            }
            okPressed = true;
            fieldStrings = fieldsField.getText().toLowerCase(Locale.ROOT).split(";");
            dispose();
        };
        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(okListener);
        toField.addActionListener(okListener);
        fieldsField.addActionListener(okListener);
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
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.HORIZONTAL;
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
        gbl.setConstraints(fieldsField, con);
        settings.add(fieldsField);

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
        gbl.setConstraints(fromField, con);
        main.add(fromField);
        con.gridy = 1;
        gbl.setConstraints(toField, con);
        main.add(toField);

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
                counter += replaceField(be, s, ce);
            }
        } else {
            for (String fld : fieldStrings) {
                    counter += replaceField(be, fld, ce);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry be, String fieldname, NamedCompound ce) {
        if (!be.hasField(fieldname)) {
            return 0;
        }
        String txt = be.getField(fieldname).get();
        StringBuilder sb = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = fromString.length();
        while ((ind = txt.indexOf(fromString, piv)) >= 0) {
            counter++;
            sb.append(txt.substring(piv, ind)); // Text leading up to s1
            sb.append(toString); // Insert s2
            piv = ind + len1;
        }
        sb.append(txt.substring(piv));
        String newStr = sb.toString();
        be.setField(fieldname, newStr);
        ce.addEdit(new UndoableFieldChange(be, fieldname, txt, newStr));
        return counter;
    }
}
