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

import net.sf.jabref.Globals;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * <p>Title: MergeDialog</p>
 * <p>Description: Asks for details about merge database operation.</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * @author Morten O. Alver
 */

public class MergeDialog extends JDialog {

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
        super(frame, title, modal);
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
        jPanel1.add(selector, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

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

