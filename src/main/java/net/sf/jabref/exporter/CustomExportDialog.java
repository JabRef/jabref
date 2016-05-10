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
package net.sf.jabref.exporter;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Dialog for creating or modifying custom exports.
 */
class CustomExportDialog extends JDialog {

    private final JTextField name = new JTextField(60);
    private final JTextField layoutFile = new JTextField(60);
    private final JTextField extension = new JTextField(60);
    private boolean okPressed;


    public CustomExportDialog(final JabRefFrame parent, final String exporterName, final String layoutFileName,
            final String extensionName) {
        this(parent);
        name.setText(exporterName);
        layoutFile.setText(layoutFileName);
        extension.setText(extensionName);
    }

    public CustomExportDialog(final JabRefFrame parent) {
        super(parent, Localization.lang("Edit custom export"), true);
        ActionListener okListener = e -> {

            // Check that there are no empty strings.
            if (layoutFile.getText().isEmpty() || name.getText().isEmpty() || extension.getText().isEmpty()
                    || !layoutFile.getText().endsWith(".layout")) {
                //JOptionPane.showMessageDialog
                //    (parent, Globals.lang("You must provide a name, a search "
                //			  +"string and a field name for this group."),
                //			  Globals.lang("Create group"),
                //     JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Handling of : and ; must also be done.

            okPressed = true;
            dispose();
        };
        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(okListener);
        name.addActionListener(okListener);
        layoutFile.addActionListener(okListener);
        extension.addActionListener(okListener);

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(cancelAction);

        JButton browse = new JButton(Localization.lang("Browse"));
        browse.addActionListener(e -> {
                File directory = new File(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
                String chosenStr = FileDialogs.getNewFile(parent, directory, ".layout",
                        JFileChooser.OPEN_DIALOG, false);
                if (chosenStr == null) {
                    return;
                }
                File chosen = new File(chosenStr);

                // Update working directory for layout files.
                Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, chosen.getParent());

                layoutFile.setText(chosen.getPath());
        });

        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        // Layout starts here.
        GridBagLayout gbl = new GridBagLayout();
        main.setLayout(gbl);
        main.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createEtchedBorder(),
                        Localization.lang("Export properties")));

        // Main panel:
        GridBagConstraints con = new GridBagConstraints();
        con.weightx = 0;
        con.gridwidth = 1;
        con.insets = new Insets(3, 5, 3, 5);
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        con.gridx = 0;
        con.gridy = 0;
        JLabel nl = new JLabel(Localization.lang("Export name") + ':');
        gbl.setConstraints(nl, con);
        main.add(nl);
        con.gridy = 1;
        JLabel nr = new JLabel(Localization.lang("Main layout file") + ':');
        gbl.setConstraints(nr, con);
        main.add(nr);
        con.gridy = 2;
        JLabel nf = new JLabel(Localization.lang("Extension") + ':');
        gbl.setConstraints(nf, con);
        main.add(nf);

        con.gridwidth = 2;
        con.weightx = 1;
        con.anchor = GridBagConstraints.WEST;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.gridy = 0;
        con.gridx = 1;
        gbl.setConstraints(name, con);
        main.add(name);
        con.gridy = 1;
        con.gridwidth = 1;
        gbl.setConstraints(layoutFile, con);
        main.add(layoutFile);
        con.gridx = 2;
        con.weightx = 0;
        gbl.setConstraints(browse, con);
        main.add(browse);
        con.weightx = 1;
        con.gridwidth = 2;
        con.gridx = 1;
        con.gridy = 2;
        gbl.setConstraints(extension, con);
        main.add(extension);

        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setSize(700, 200);

        setLocationRelativeTo(parent);
    }

    public boolean okPressed() {
        return okPressed;
    }

    public String layoutFile() {
        return layoutFile.getText();
    }

    public String name() {
        return name.getText();
    }

    public String extension() {
        String ext = extension.getText();
        if (ext.startsWith(".")) {
            return ext;
        } else if (ext.startsWith("*.")) {
            return ext.substring(1);
        } else {
            return '.' + ext;
        }
    }

}
