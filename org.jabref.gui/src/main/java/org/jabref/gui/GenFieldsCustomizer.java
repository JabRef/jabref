package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jabref.Globals;
import org.jabref.gui.entryeditor.EntryEditorTabList;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.Sizes;

public class GenFieldsCustomizer extends JabRefDialog {

    private final JPanel buttons = new JPanel();
    private final JButton ok = new JButton();
    private final JButton cancel = new JButton();
    private final JButton helpBut;
    private final JLabel jLabel1 = new JLabel();
    private final JPanel jPanel3 = new JPanel();
    private final JPanel jPanel4 = new JPanel();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JLabel jLabel2 = new JLabel();
    private final JTextArea fieldsArea = new JTextArea();
    private final GridBagLayout gridBagLayout2 = new GridBagLayout();
    private final JButton revert = new JButton();

    public GenFieldsCustomizer(JabRefFrame frame) {
        super(frame, Localization.lang("Set general fields"), false, GenFieldsCustomizer.class);
        helpBut = new HelpAction(HelpFile.GENERAL_FIELDS).getHelpButton();
        jbInit();
        setSize(new Dimension(650, 300));
    }

    private void jbInit() {
        ok.setText(Localization.lang("OK"));
        ok.addActionListener(e -> okActionPerformed());
        cancel.setText(Localization.lang("Cancel"));
        cancel.addActionListener(e -> dispose());
        jLabel1.setText(Localization.lang("Delimit fields with semicolon, ex.") + ": url;pdf;note");
        jPanel3.setLayout(gridBagLayout2);
        jPanel4.setBorder(BorderFactory.createEtchedBorder());
        jPanel4.setLayout(gridBagLayout1);
        jLabel2.setText(Localization.lang("General fields"));

        setFieldsText();

        revert.setText(Localization.lang("Default"));
        revert.addActionListener(e -> revertActionPerformed());
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
            }
        });

    }

    private void okActionPerformed() {
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
            String testString = BibtexKeyGenerator.cleanKey(parts[1],
                    Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
            if (!testString.equals(parts[1]) || (parts[1].indexOf('&') >= 0)) {
                // Report error and exit.
                JOptionPane.showMessageDialog(this, Localization.lang("Field names are not allowed to contain white space or the following "
                                + "characters") + ": # { } ~ , ^ &",
                        Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);

                return;
            }

            Globals.prefs.put((JabRefPreferences.CUSTOM_TAB_NAME + i), parts[0]);
            Globals.prefs.put((JabRefPreferences.CUSTOM_TAB_FIELDS + i), parts[1].toLowerCase(Locale.ROOT));
        }
        Globals.prefs.purgeSeries(JabRefPreferences.CUSTOM_TAB_NAME, i);
        Globals.prefs.purgeSeries(JabRefPreferences.CUSTOM_TAB_FIELDS, i);
        Globals.prefs.updateEntryEditorTabList();

        dispose();
    }

    private void setFieldsText() {
        StringBuilder sb = new StringBuilder();

        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            sb.append(tabList.getTabName(i));
            sb.append(':');
            sb.append(String.join(";", tabList.getTabFields(i)));
            sb.append('\n');
        }

        fieldsArea.setText(sb.toString());
    }

    private void revertActionPerformed() {
        StringBuilder sb = new StringBuilder();
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
