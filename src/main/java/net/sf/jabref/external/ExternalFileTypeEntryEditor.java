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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FileDialogs;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;

/**
 * This class produces a dialog box for editing an external file type.
 */
public class ExternalFileTypeEntryEditor {

    private JFrame fParent;
    private JDialog dParent;
    private JDialog diag;
    private final JTextField extension = new JTextField();
    private final JTextField name = new JTextField();
    private final JTextField mimeType = new JTextField();
    private final JTextField application = new JTextField();
    private String selectedIcon;
    private final JButton icon = new JButton(IconTheme.getImage("picture"));
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JRadioButton useDefault = new JRadioButton(Localization.lang("Default"));
    private final JRadioButton other = new JRadioButton("");
    final String emptyMessage = "<" + Localization.lang("Use default viewer") + ">";
    boolean applicationFieldEmpty;

    private ExternalFileType entry;
    private boolean okPressed;


    public ExternalFileTypeEntryEditor(JFrame parent, ExternalFileType entry) {
        fParent = parent;
        init(entry);
    }

    public ExternalFileTypeEntryEditor(JDialog parent, ExternalFileType entry) {
        dParent = parent;
        init(entry);
    }

    private void init(ExternalFileType entry) {
        this.entry = entry;
        icon.setText(null);

        ButtonGroup bg = new ButtonGroup();
        bg.add(useDefault);
        bg.add(other);

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", ""));
        builder.append(Localization.lang("Icon"));
        builder.append(icon);
        builder.nextLine();
        builder.append(Localization.lang("Name"));
        builder.append(name);
        builder.nextLine();
        builder.append(Localization.lang("Extension"));
        builder.append(extension);
        builder.nextLine();
        builder.append(Localization.lang("MIME type"));
        builder.append(mimeType);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.nextLine();
        builder.append(Localization.lang("Application"));
        JButton browseBut = new JButton(Localization.lang("Browse"));
        if (OS.WINDOWS) {
            builder.append(useDefault);
            builder.nextLine();
            JPanel p1 = new JPanel();
            builder.append(p1);
            JPanel p2 = new JPanel();
            application.setPreferredSize(new Dimension(300, application.getPreferredSize().height));
            BorderLayout bl = new BorderLayout();
            bl.setHgap(4);
            p2.setLayout(bl);
            p2.add(other, BorderLayout.WEST);
            p2.add(application, BorderLayout.CENTER);
            builder.append(p2);
            builder.append(browseBut);
        } else {
            builder.append(application);
            builder.append(browseBut);
        }
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed = true;
                storeSettings(ExternalFileTypeEntryEditor.this.entry);
                diag.dispose();

            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });

        icon.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String initSel = ExternalFileTypeEntryEditor.this.entry.getIconName();
                if (selectedIcon != null) {
                    initSel = selectedIcon;
                }
                IconSelection ic = new IconSelection(diag, initSel);
                ic.setVisible(true);
                if (ic.isOkPressed()) {
                    selectedIcon = ic.getSelectedIconKey();
                    icon.setIcon(IconTheme.getImage(selectedIcon));
                }
                //JOptionPane.showMessageDialog(null, "Sorry, the icon can unfortunately not be changed in this version of JabRef");
            }
        });

        if (OS.WINDOWS) {
            application.getDocument().addDocumentListener(new DocumentListener() {

                private void handle(DocumentEvent e) {
                    if (application.getText().isEmpty()) {
                        useDefault.setSelected(true);
                    } else {
                        other.setSelected(true);
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    handle(e);
                }

                @Override
                public void removeUpdate(DocumentEvent documentEvent) {
                    handle(documentEvent);
                }

                @Override
                public void changedUpdate(DocumentEvent documentEvent) {
                    handle(documentEvent);
                }
            });
        }

        if (dParent != null) {
            diag = new JDialog(dParent, Localization.lang("Edit file type"), true);
        } else {
            diag = new JDialog(fParent, Localization.lang("Edit file type"), true);
        }
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();

        BrowseListener browse = new BrowseListener(diag, application);
        browseBut.addActionListener(browse);

        if (dParent != null) {
            diag.setLocationRelativeTo(dParent);
        }
        else {
            diag.setLocationRelativeTo(fParent);
        //Util.placeDialog(diag, parent);
        }

        setValues(entry);
    }

    public void setEntry(ExternalFileType entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            okPressed = false;
        }
        diag.setVisible(visible);
    }

    private void setValues(ExternalFileType entry) {
        name.setText(entry.getName());
        extension.setText(entry.getExtension());
        mimeType.setText(entry.getMimeType());
        application.setText(entry.getOpenWith());
        icon.setIcon(entry.getIcon());
        if (application.getText().isEmpty()) {
            useDefault.setSelected(true);
        } else {
            other.setSelected(true);
        }
        selectedIcon = null;
    }

    private void storeSettings(ExternalFileType entry) {
        entry.setName(name.getText().trim());
        entry.setMimeType(mimeType.getText().trim());
        // Set extension, but remove initial dot if user has added that:
        String ext = extension.getText().trim();
        if (!ext.isEmpty() && ext.charAt(0) == '.') {
            entry.setExtension(ext.substring(1));
        } else {
            entry.setExtension(ext);
        }

        if (selectedIcon != null) {
            entry.setIconName(selectedIcon);
            entry.setIcon(IconTheme.getImage(entry.getIconName()));
        }
        if (!OS.WINDOWS) {
            entry.setOpenWith(application.getText().trim());
        } else {
            // On Windows, store application as empty if the "Default" option is selected,
            // or if the application name is empty:
            if (useDefault.isSelected() || application.getText().trim().isEmpty()) {
                entry.setOpenWith("");
            } else {
                entry.setOpenWith(application.getText().trim());
            }
        }
    }

    public boolean okPressed() {
        return okPressed;
    }


    class BrowseListener implements ActionListener {

        private final JTextField comp;


        public BrowseListener(JDialog parent, JTextField comp) {
            this.comp = comp;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File initial = new File(comp.getText().trim());
            if (comp.getText().trim().isEmpty()) {
                // Nothing in the field. Go to the last file dir used:
                initial = new File(Globals.prefs.get(JabRefPreferences.FILE_WORKING_DIRECTORY));
            }
            String chosen = FileDialogs.getNewFile(/*parent*/null, initial, Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                // Store the directory for next time:
                Globals.prefs.put(JabRefPreferences.FILE_WORKING_DIRECTORY, newFile.getParent());
                comp.setText(newFile.getPath());
                comp.requestFocus();
            }
        }
    }
}
