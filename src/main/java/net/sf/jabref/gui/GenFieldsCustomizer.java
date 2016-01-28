/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.util.Iterator;

import javax.swing.*;
import net.sf.jabref.*;
import net.sf.jabref.gui.entryeditor.EntryEditorTabList;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.Sizes;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class GenFieldsCustomizer extends JDialog {

    private final JPanel buttons = new JPanel();
    private final JButton ok = new JButton();
    private final JButton cancel = new JButton();
    private JButton helpBut = new JButton();
    private final JLabel jLabel1 = new JLabel();
    private final JPanel jPanel3 = new JPanel();
    private final JPanel jPanel4 = new JPanel();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JLabel jLabel2 = new JLabel();
    private final JTextArea fieldsArea = new JTextArea();
    private final GridBagLayout gridBagLayout2 = new GridBagLayout();
    private final JabRefFrame parent;
    private final JButton revert = new JButton();


    public GenFieldsCustomizer(JabRefFrame frame/*, EntryCustomizationDialog diag*/) {
        super(frame, Localization.lang("Set general fields"), false);
        parent = frame;
        helpBut = new HelpAction(HelpFiles.generalFieldsHelp).getHelpButton();
        try {
            jbInit();
            setSize(new Dimension(650, 300));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() {
        ok.setText(Localization.lang("OK"));
        ok.addActionListener(new GenFieldsCustomizer_ok_actionAdapter(this));
        cancel.setText(Localization.lang("Cancel"));
        cancel.addActionListener(new GenFieldsCustomizer_cancel_actionAdapter(this));
        //buttons.setBackground(GUIGlobals.lightGray);
        jLabel1.setText(Localization.lang("Delimit fields with semicolon, ex.") + ": url;pdf;note");
        jPanel3.setLayout(gridBagLayout2);
        jPanel4.setBorder(BorderFactory.createEtchedBorder());
        jPanel4.setLayout(gridBagLayout1);
        jLabel2.setText(Localization.lang("General fields"));

        //    fieldsArea.setText(parent.prefs.get(JabRefPreferences.GENERAL_FIELDS));
        setFieldsText();

        //jPanel3.setBackground(GUIGlobals.lightGray);
        revert.setText(Localization.lang("Default"));
        revert.addActionListener(new GenFieldsCustomizer_revert_actionAdapter(this));
        this.getContentPane().add(buttons, BorderLayout.SOUTH);
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(revert);
        bb.addButton(cancel);
        bb.addStrut(Sizes.DLUX5);
        bb.addButton(helpBut);
        bb.addGlue();

        this.getContentPane().add(jPanel3, BorderLayout.CENTER);
        jPanel3.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel3.add(jPanel4, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 318, 193));
        jPanel4.add(jScrollPane1, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane1.getViewport().add(fieldsArea, null);
        jPanel4.add(jLabel2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        // Key bindings:
        ActionMap am = buttons.getActionMap();
        InputMap im = buttons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                //diag.requestFocus();
            }
        });

    }

    void ok_actionPerformed(ActionEvent e) {
        String[] lines = fieldsArea.getText().split("\n");
        int i = 0;
        for (; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length != 2) {
                // Report error and exit.
                String field = Localization.lang("field");
                JOptionPane.showMessageDialog(this, Localization.lang("Each line must be on the following form") + " '" +
                        Localization.lang("Tabname") + ':' + field + "1;" + field + "2;...;" + field + "N'",
                        Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            String testString = Util.checkLegalKey(parts[1]);
            if (!testString.equals(parts[1]) || (parts[1].indexOf('&') >= 0)) {
                // Report error and exit.
                JOptionPane.showMessageDialog(this, Localization.lang("Field names are not allowed to contain white space or the following "
                                + "characters") + ": # { } ~ , ^ &",
                        Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);

                return;
            }

            Globals.prefs.put((JabRefPreferences.CUSTOM_TAB_NAME + i), parts[0]);
            Globals.prefs.put((JabRefPreferences.CUSTOM_TAB_FIELDS + i), parts[1].toLowerCase());
        }
        Globals.prefs.purgeSeries(JabRefPreferences.CUSTOM_TAB_NAME, i);
        Globals.prefs.purgeSeries(JabRefPreferences.CUSTOM_TAB_FIELDS, i);
        Globals.prefs.updateEntryEditorTabList();
        /*
        String delimStr = fieldsArea.getText().replaceAll("\\s+","")
          .replaceAll("\\n+","").trim();
        parent.prefs.putStringArray(JabRefPreferences.GENERAL_FIELDS, Util.split(delimStr, ";"));
        */

        parent.removeCachedEntryEditors();
        dispose();
        //diag.requestFocus();
    }

    void cancel_actionPerformed(ActionEvent e) {
        dispose();
        //diag.requestFocus();
    }

    private void setFieldsText() {
        StringBuffer sb = new StringBuffer();

        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            sb.append(tabList.getTabName(i));
            sb.append(':');
            for (Iterator<String> j = tabList.getTabFields(i).iterator(); j
                    .hasNext();) {
                String field = j.next();
                sb.append(field);
                if (j.hasNext()) {
                    sb.append(';');
                }
            }
            sb.append('\n');
        }

        fieldsArea.setText(sb.toString());
    }

    void revert_actionPerformed(ActionEvent e) {
        StringBuffer sb = new StringBuffer();
        String name;
        String fields;
        int i = 0;
        while ((name = (String) Globals.prefs.defaults.get
                (JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i)) != null) {
            sb.append(name);
            fields = (String) Globals.prefs.defaults.get
                    (JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i);
            sb.append(':');
            sb.append(fields);
            sb.append('\n');
            i++;
        }
        fieldsArea.setText(sb.toString());

    }
}

class GenFieldsCustomizer_ok_actionAdapter implements java.awt.event.ActionListener {

    private final GenFieldsCustomizer adaptee;


    GenFieldsCustomizer_ok_actionAdapter(GenFieldsCustomizer adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.ok_actionPerformed(e);
    }
}

class GenFieldsCustomizer_cancel_actionAdapter implements java.awt.event.ActionListener {

    private final GenFieldsCustomizer adaptee;


    GenFieldsCustomizer_cancel_actionAdapter(GenFieldsCustomizer adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.cancel_actionPerformed(e);
    }
}

class GenFieldsCustomizer_revert_actionAdapter implements java.awt.event.ActionListener {

    private final GenFieldsCustomizer adaptee;


    GenFieldsCustomizer_revert_actionAdapter(GenFieldsCustomizer adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adaptee.revert_actionPerformed(e);
    }
}
