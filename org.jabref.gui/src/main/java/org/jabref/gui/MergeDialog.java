package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

/**
 * <p>Title: MergeDialog</p>
 * <p>Description: Asks for details about merge database operation.</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * @author Morten O. Alver
 */

public class MergeDialog extends JabRefDialog {

    private final JPanel panel1 = new JPanel();
    private final BorderLayout borderLayout1 = new BorderLayout();
    private final JPanel jPanel1 = new JPanel();
    private final JPanel jPanel2 = new JPanel();
    private final JButton ok = new JButton();
    private final JButton cancel = new JButton();
    private final JCheckBox entries = new JCheckBox();
    private final JCheckBox strings = new JCheckBox();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private final JCheckBox groups = new JCheckBox();
    private final JCheckBox selector = new JCheckBox();


    private boolean okPressed;

    public MergeDialog(JabRefFrame frame, String title, boolean modal) {
        super(frame, title, modal, MergeDialog.class);
        jbInit();
        pack();
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void jbInit() {
        panel1.setLayout(borderLayout1);
        ok.setText(Localization.lang("OK"));
        ok.addActionListener(e -> {
                okPressed = true;
                dispose();
        });
        cancel.setText(Localization.lang("Cancel"));
        cancel.addActionListener(e -> dispose());
        jPanel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel1.setLayout(gridBagLayout1);
        entries.setSelected(true);
        entries.setText(Localization.lang("Import entries"));
        strings.setSelected(true);
        strings.setText(Localization.lang("Import strings"));
        groups.setText(Localization.lang("Import group definitions"));
        selector.setText(Localization.lang("Import word selector definitions"));

        this.setModal(true);
        this.setResizable(false);
        getContentPane().add(panel1);
        panel1.add(jPanel2, BorderLayout.SOUTH);
        jPanel2.add(ok, null);
        jPanel2.add(cancel, null);
        panel1.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(entries, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(strings, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(groups, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(selector, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        // Key bindings:
        ActionMap am = jPanel1.getActionMap();
        InputMap im = jPanel1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    public boolean importEntries() {
        return entries.isSelected();
    }

    public boolean importGroups() {
        return groups.isSelected();
    }

    public boolean importStrings() {
        return strings.isSelected();
    }

    public boolean importSelectorWords() {
        return selector.isSelected();
    }
}


