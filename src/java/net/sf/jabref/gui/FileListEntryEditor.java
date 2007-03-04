package net.sf.jabref.gui;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.external.ExternalFileType;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

/**
 * This class produces a dialog box for editing a single file link from a Bibtex entry.
 *
 * The information to be edited includes the file description, the link itself and the
 * file type. The dialog also includes convenience buttons for quick linking.
 */
public class FileListEntryEditor {

    JDialog diag;
    JTextField link = new JTextField(), description = new JTextField();
    JButton ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));
    JComboBox types;

    private FileListEntry entry;
    private boolean okPressed = false;

    public FileListEntryEditor(JFrame parent, FileListEntry entry) {
        this.entry = entry;

        types = new JComboBox(Globals.prefs.getExternalFileTypeSelection());

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("left:pref, 4dlu, fill:150dlu", ""));
        builder.append(Globals.lang("Link"));
        builder.append(link);
        builder.nextLine();
        builder.append(Globals.lang("Description"));
        builder.append(description);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        builder.nextLine();
        builder.append(Globals.lang("File type"));
        builder.append(types);
        
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
                storeSettings(FileListEntryEditor.this.entry);
                okPressed = true;
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });
        link.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }
            public void focusLost(FocusEvent e) {
                if ((types.getSelectedIndex() == -1) &&
                        (link.getText().trim().length() > 0)) {
                    // Try to guess the file type:
                    String theLink = link.getText().trim();
                    int index = theLink.indexOf('.');
                    if ((index >= 0) && (index < theLink.length()-1)) {

                        ExternalFileType type = Globals.prefs.getExternalFileTypeByExt
                                (theLink.substring(index+1));
                        if (type != null)
                            types.setSelectedItem(type);
                            
                    }
                }
            }
        });

        diag = new JDialog(parent, Globals.lang("Edit file link"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        Util.placeDialog(diag, parent);

        setValues(entry);
    }

    public void setEntry(FileListEntry entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible) {
        if (visible)
            okPressed = false;
        diag.setVisible(visible);
    }

    public void setValues(FileListEntry entry) {
        description.setText(entry.getDescription());
        link.setText(entry.getLink());
        types.setSelectedItem(entry.getType());
    }

    public void storeSettings(FileListEntry entry) {
        entry.setDescription(description.getText().trim());
        entry.setLink(link.getText().trim());
        entry.setType((ExternalFileType)types.getSelectedItem());
    }

    public boolean okPressed() {
        return okPressed;
    }
}
